package com.corebaking.service;

import com.corebaking.account.model.Account;
import com.corebaking.account.model.StatusAccount;
import com.corebaking.account.repository.AccountRepository;
import com.corebaking.transfers.dto.TransferRequest;
import com.corebaking.transfers.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Testcontainers
public class TransferServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Account accountA;
    private Account accountB;
    private Account accountC;

    @BeforeEach
    void setUp() {
        // The test database is disposable; row-level protections forbid DELETE.
        jdbcTemplate.execute("TRUNCATE idempotency_records, ledger_entries, transfers, accounts");

        accountA = new Account("Account A");
        accountA.setStatus(StatusAccount.ACTIVE);
        accountA.setBalance(new BigDecimal("100.00"));
        accountA =  accountRepository.save(accountA);

        accountB = new Account("Account B");
        accountB.setStatus(StatusAccount.ACTIVE);
        accountB.setBalance(new BigDecimal("0.00"));
        accountB =  accountRepository.save(accountB);

        accountC = new Account("Account C");
        accountC.setStatus(StatusAccount.ACTIVE);
        accountC.setBalance(new BigDecimal("0.00"));
        accountC =  accountRepository.save(accountC);

    }

    @Test
    void shouldCompleteOnlyOneTransferWhenBalanceDoesNotCoverBoth() throws Exception {

        var requestB = new TransferRequest(
                accountA.getId(),
                accountB.getId(),
                new BigDecimal("80.00")
        );

        var  requestC = new TransferRequest(
                accountA.getId(),
                accountC.getId(),
                new BigDecimal("80.00")
        );

        var keyB = UUID.randomUUID();
        var keyC = UUID.randomUUID();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready =  new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try {
            Future<ResponseEntity<String>> futureB = executor.submit(() -> {

                ready.countDown();
                start.await();

                return sendTransfer(requestB, keyB);
            });

            Future<ResponseEntity<String>> futureC = executor.submit(() -> {

                ready.countDown();
                start.await();

                return sendTransfer(requestC, keyC);
            });

            assertTrue(
                    ready.await(5, TimeUnit.SECONDS),
                    "The two tasks are ready"
            );

            start.countDown();

            ResponseEntity<String> responseB = futureB.get(10, TimeUnit.SECONDS);
            ResponseEntity<String> responseC = futureC.get(10, TimeUnit.SECONDS);

            System.out.println("Answer to B: " + responseB);
            System.out.println("Answer to C: " + responseC);

            List<ResponseEntity<String>> responses = List.of(responseB, responseC);

            long qtdSuccess = responses.stream()
                    .filter(response -> response.getStatusCode().value() == 200)
                    .count();

            assertEquals(1, qtdSuccess, "A transfer must return exactly an HTTP status 200");

            Account accA = accountRepository.findById(accountA.getId())
                    .orElseThrow();

            Account accB = accountRepository.findById(accountB.getId())
                    .orElseThrow();

            Account accC = accountRepository.findById(accountC.getId())
                    .orElseThrow();

            System.out.println("Account A: " + accA.getBalance());
            System.out.println("Account B: " + accB.getBalance());
            System.out.println("Account C: " + accC.getBalance());

            assertEquals(
                    0,
                    accA.getBalance().compareTo(new BigDecimal("20.00")),
                    "Acc A must end with R$ 20.00"
            );

            boolean receivedB = accB.getBalance()
                    .compareTo(new BigDecimal("80.00")) == 0
                    && accC.getBalance()
                        .compareTo(new BigDecimal("0.00")) == 0;

            boolean receivedC = accC.getBalance()
                    .compareTo(new BigDecimal("80.00")) == 0
                    && accB.getBalance()
                        .compareTo(new BigDecimal("0.00")) == 0;

            assertTrue(receivedB || receivedC,
                    "One destination account should receive R$ 80.00, and the other, R$ 0.00");

            assertEquals(
                    1,
                    transferRepository.count(),
                    "There should be only one persisted transfer"
            );
        } finally {
            start.countDown();
            executor.shutdown();
        }
    }

    @Test
    void shouldProtectFinancialHistoryFromUpdatesAndDeletes() {
        UUID transferId = UUID.randomUUID();
        UUID ledgerId = UUID.randomUUID();
        jdbcTemplate.update("""
                INSERT INTO transfers (id, source_account_id, amount, status, created_at)
                VALUES (?, ?, 10.00, 'COMPLETED', CURRENT_TIMESTAMP)
                """, transferId, accountA.getId());
        jdbcTemplate.update("""
                INSERT INTO ledger_entries (id, account_id, transfer_id, entry_type, amount, created_at)
                VALUES (?, ?, ?, 'TRANSFER_SENT', 10.00, CURRENT_TIMESTAMP)
                """, ledgerId, accountA.getId(), transferId);

        for (String table : List.of("ledger_entries", "transfers")) {
            UUID id = table.equals("ledger_entries") ? ledgerId : transferId;
            for (String operation : List.of("UPDATE " + table + " SET amount = 20 WHERE id = ?",
                    "DELETE FROM " + table + " WHERE id = ?")) {
                DataAccessException error = assertThrows(DataAccessException.class,
                        () -> jdbcTemplate.update(operation, id));
                assertTrue(error.getMostSpecificCause().getMessage().contains("histórico financeiro (" + table + ")"));
            }
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM " + table + " WHERE id = ? AND amount = 10", Integer.class, id));
        }
    }

    private ResponseEntity<String> sendTransfer(TransferRequest request, UUID key) {

        var headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-key", key.toString());

        HttpEntity<TransferRequest> requestEntity = new HttpEntity<>(request, headers);

        return restTemplate.postForEntity("/api/v1/transfer", requestEntity, String.class);
    }
}
