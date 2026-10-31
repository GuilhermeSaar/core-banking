package com.corebaking.service;

import com.corebaking.account.model.Account;
import com.corebaking.account.model.StatusAccount;
import com.corebaking.account.repository.AccountRepository;
import com.corebaking.idempotency.repository.IdempotencyRepository;
import com.corebaking.ledger.repository.LedgerRepository;
import com.corebaking.shared.exception.InsufficientBalanceException;
import com.corebaking.transfers.dto.TransferRequest;
import com.corebaking.transfers.repository.TransferRepository;
import com.corebaking.transfers.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class TransferServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransferService transferService;

    @Autowired
    private TransferRepository transferRepository;

    @Autowired
    private LedgerRepository ledgerRepository;

    @Autowired
    private IdempotencyRepository idempotencyRepository;

    private Account sourceAccount;
    private Account destAccount;

    @BeforeEach
    void setUp() {
        // clean tables
        idempotencyRepository.deleteAll();
        ledgerRepository.deleteAll();
        transferRepository.deleteAll();
        accountRepository.deleteAll();

        sourceAccount = new Account("Source acc");
        sourceAccount.setStatus(StatusAccount.ACTIVE);
        sourceAccount.setBalance(new BigDecimal("500.00"));
        sourceAccount =  accountRepository.save(sourceAccount);

        destAccount = new Account("Dest acc");
        destAccount.setStatus(StatusAccount.ACTIVE);
        destAccount.setBalance(new BigDecimal("100.00"));
        destAccount =  accountRepository.save(destAccount);
    }

    @Test
    void shouldCompleteTransferSuccessfully() {

        var request = new TransferRequest(sourceAccount.getId(), destAccount.getId(), new BigDecimal("200.00"));
        var idempotencyKey = UUID.randomUUID();

        var response = transferService.transfer(request, idempotencyKey);

        assertNotNull(response);
        assertNotNull(response.transferId());
        assertEquals(new BigDecimal("200.00"), response.amount());
        assertEquals(sourceAccount.getId(), response.sourceAccountId());
        assertEquals(destAccount.getId(), response.destinationAccountId());

        Account updateSource = accountRepository.findById(sourceAccount.getId()).get();
        Account updateDest = accountRepository.findById(destAccount.getId()).get();

        assertEquals(0, new BigDecimal("300.00").compareTo(updateSource.getBalance()));
        assertEquals(0, new BigDecimal("300.00").compareTo(updateDest.getBalance()));
    }

    @Test
    void shouldPreventDoubleSpendingOnConcurrentTransfers() throws Exception {
        sourceAccount.setBalance(new BigDecimal("100.00"));
        sourceAccount = accountRepository.save(sourceAccount);

        UUID sourceId = sourceAccount.getId();
        UUID firstDestinationId = accountRepository.save(new Account("Destino 1")).getId();
        UUID secondDestinationId = accountRepository.save(new Account("Destino 2")).getId();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Boolean> first = executor.submit(() -> transferAfterStart(sourceId, firstDestinationId, ready, start));
            Future<Boolean> second = executor.submit(() -> transferAfterStart(sourceId, secondDestinationId, ready, start));
            assertTrue(ready.await(10, TimeUnit.SECONDS), "As duas tarefas devem estar prontas");
            start.countDown();

            boolean firstSucceeded = first.get(30, TimeUnit.SECONDS);
            boolean secondSucceeded = second.get(30, TimeUnit.SECONDS);
            assertNotEquals(firstSucceeded, secondSucceeded, "Apenas uma transferência deve ser concluída");

            assertEquals(0, BigDecimal.ZERO.compareTo(accountRepository.findById(sourceId).orElseThrow().getBalance()));
            assertEquals(0, (firstSucceeded ? new BigDecimal("100.00") : BigDecimal.ZERO)
                    .compareTo(accountRepository.findById(firstDestinationId).orElseThrow().getBalance()));
            assertEquals(0, (secondSucceeded ? new BigDecimal("100.00") : BigDecimal.ZERO)
                    .compareTo(accountRepository.findById(secondDestinationId).orElseThrow().getBalance()));
            assertEquals(1, transferRepository.count());
        } finally {
            start.countDown();
            executor.shutdownNow();
        }
    }

    private boolean transferAfterStart(UUID sourceId, UUID destinationId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            transferService.transfer(new TransferRequest(sourceId, destinationId, new BigDecimal("100.00")), UUID.randomUUID());
            return true;
        } catch (InsufficientBalanceException e) {
            return false;
        }
    }
}
