package com.corebaking.integration.service;

import com.corebaking.account.model.Account;
import com.corebaking.account.model.StatusAccount;
import com.corebaking.account.repository.AccountRepository;
import com.corebaking.transaction.dto.TransactionRequest;
import com.corebaking.transfers.dto.TransferRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.aopalliance.intercept.MethodInterceptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
class TransferDepositLockIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private Account source;
    private Account destination;

    @BeforeEach
    void setUp() {
        jdbcTemplate.execute("TRUNCATE idempotency_records, ledger_entries, transfers, accounts");
        source = createAccount("Source", "100.00");
        destination = createAccount("Destination", "0.00");
    }

    @Test
    void shouldWaitForDepositToReleaseDestinationAccountLock() throws Exception {
        CountDownLatch sourceLocked = new CountDownLatch(1);
        CountDownLatch destinationLockedByDeposit = new CountDownLatch(1);
        CountDownLatch transferMayContinue = new CountDownLatch(1);
        CountDownLatch depositMayCommit = new CountDownLatch(1);
        CountDownLatch transferAttemptingDestination = new CountDownLatch(1);
        AtomicBoolean firstDestinationLock = new AtomicBoolean(true);
        AtomicInteger transferBackend = new AtomicInteger();
        AtomicInteger depositBackend = new AtomicInteger();

        // Coordinate the real repository calls without replacing the PostgreSQL queries.
        MethodInterceptor coordination = invocation -> {
            if (!invocation.getMethod().getName().equals("findByIdWithLock")) {
                return invocation.proceed();
            }
            UUID accountId = (UUID) invocation.getArguments()[0];
            if (accountId.equals(source.getId())) {
                Object result = invocation.proceed();
                transferBackend.set(currentBackendPid());
                sourceLocked.countDown();
                awaitSignal(transferMayContinue, "transfer may continue");
                return result;
            }
            if (accountId.equals(destination.getId()) && firstDestinationLock.getAndSet(false)) {
                Object result = invocation.proceed();
                depositBackend.set(currentBackendPid());
                destinationLockedByDeposit.countDown();
                awaitSignal(depositMayCommit, "deposit may commit");
                return result;
            }
            if (accountId.equals(destination.getId())) {
                transferAttemptingDestination.countDown();
            }
            return invocation.proceed();
        };
        Advised repositoryProxy = (Advised) accountRepository;
        repositoryProxy.addAdvice(0, coordination);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<ResponseEntity<String>> transfer = executor.submit(() -> post(
                    "/api/v1/transfer", new TransferRequest(source.getId(), destination.getId(),
                            new BigDecimal("40.00"))));
            assertTrue(sourceLocked.await(10, TimeUnit.SECONDS), "Transfer did not lock source account");

            Future<ResponseEntity<String>> deposit = executor.submit(() -> post(
                    "/api/v1/transaction/" + destination.getId() + "/deposits",
                    new TransactionRequest(new BigDecimal("30.00"))));
            assertTrue(destinationLockedByDeposit.await(10, TimeUnit.SECONDS),
                    "Deposit did not lock destination account");

            transferMayContinue.countDown();
            assertTrue(transferAttemptingDestination.await(10, TimeUnit.SECONDS),
                    "Transfer did not attempt to lock destination account");

            await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertTrue(Boolean.TRUE.equals(
                    jdbcTemplate.queryForObject("""
                            SELECT EXISTS (
                                SELECT 1 FROM pg_stat_activity
                                WHERE pid = ? AND wait_event_type = 'Lock'
                                  AND ? = ANY(pg_blocking_pids(pid))
                            )
                            """, Boolean.class, transferBackend.get(), depositBackend.get())),
                    "PostgreSQL should show the transfer waiting for the deposit"));
            assertFalse(transfer.isDone(), "Transfer should be waiting for the deposit to commit");

            depositMayCommit.countDown();
            assertEquals(200, deposit.get(10, TimeUnit.SECONDS).getStatusCode().value());
            assertEquals(200, transfer.get(10, TimeUnit.SECONDS).getStatusCode().value());

            assertEquals(0, accountRepository.findById(source.getId()).orElseThrow()
                    .getBalance().compareTo(new BigDecimal("60.00")));
            assertEquals(0, accountRepository.findById(destination.getId()).orElseThrow()
                    .getBalance().compareTo(new BigDecimal("70.00")));
            assertEquals(1, ledgerCount(source.getId(), "TRANSFER_SENT", "40.00"));
            assertEquals(1, ledgerCount(destination.getId(), "TRANSFERENCIA_RECEIVED", "40.00"));
            assertEquals(1, ledgerCount(destination.getId(), "DEPOSIT", "30.00"));
            assertEquals(3, jdbcTemplate.queryForObject("SELECT count(*) FROM ledger_entries", Integer.class));
        } finally {
            transferMayContinue.countDown();
            depositMayCommit.countDown();
            executor.shutdownNow();
            executor.awaitTermination(15, TimeUnit.SECONDS);
            repositoryProxy.removeAdvice(coordination);
        }
    }

    private Account createAccount(String name, String balance) {
        Account account = new Account(name);
        account.setStatus(StatusAccount.ACTIVE);
        account.setBalance(new BigDecimal(balance));
        return accountRepository.save(account);
    }

    private int currentBackendPid() {
        return ((Number) entityManager.createNativeQuery("SELECT pg_backend_pid()").getSingleResult()).intValue();
    }

    private static void awaitSignal(CountDownLatch latch, String description) throws InterruptedException {
        if (!latch.await(30, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out waiting for " + description);
        }
    }

    private ResponseEntity<String> post(String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", UUID.randomUUID().toString());
        return restTemplate.postForEntity(path, new HttpEntity<>(body, headers), String.class);
    }

    private int ledgerCount(UUID accountId, String entryType, String amount) {
        return jdbcTemplate.queryForObject("""
                SELECT count(*) FROM ledger_entries
                WHERE account_id = ? AND entry_type = ? AND amount = ?
                """, Integer.class, accountId, entryType, new BigDecimal(amount));
    }
}
