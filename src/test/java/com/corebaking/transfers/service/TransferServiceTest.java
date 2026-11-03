package com.corebaking.transfers.service;

import com.corebaking.account.model.Account;
import com.corebaking.account.model.StatusAccount;
import com.corebaking.account.repository.AccountRepository;
import com.corebaking.idempotency.model.Idempotency;
import com.corebaking.idempotency.model.IdempotencyStatus;
import com.corebaking.idempotency.repository.IdempotencyRepository;
import com.corebaking.ledger.model.EntryNature;
import com.corebaking.ledger.model.EntryType;
import com.corebaking.ledger.model.Ledger;
import com.corebaking.ledger.repository.LedgerRepository;
import com.corebaking.transfers.service.TransferService;
import com.corebaking.shared.exception.DuplicateAccountException;
import com.corebaking.shared.exception.FinancialRuleException;
import com.corebaking.shared.exception.IdempotencyConflictException;
import com.corebaking.shared.exception.InvalidIdempotencyKeyException;
import com.corebaking.shared.exception.ResourceNotFoundException;
import com.corebaking.shared.utils.GenerateHash;
import com.corebaking.transfers.dto.TransferRequest;
import com.corebaking.transfers.dto.TransferResponse;
import com.corebaking.transfers.model.Transfer;
import com.corebaking.transfers.model.TransferStatus;
import com.corebaking.transfers.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    private static final UUID SOURCE_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID DESTINATION_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("33333333-3333-4333-8333-333333333333");
    private static final BigDecimal AMOUNT = new BigDecimal("25.50");

    @Mock
    private TransferRepository transferRepository;

    @Mock
    private LedgerRepository ledgerRepository;

    @Mock
    private AccountRepository accRepository;

    @Mock
    private IdempotencyRepository idempotencyRepository;

    @InjectMocks
    private TransferService transferService;

    private Account source;
    private Account destination;
    private TransferRequest request;

    @BeforeEach
    void setUp() {
        source = account(SOURCE_ID, "Source", new BigDecimal("100.00"));
        destination = account(DESTINATION_ID, "Destination", new BigDecimal("10.00"));
        request = new TransferRequest(SOURCE_ID, DESTINATION_ID, AMOUNT);
    }

    @Test
    void shouldRejectNullIdempotencyKey() {
        assertThrows(InvalidIdempotencyKeyException.class,
                () -> transferService.transfer(request, null));

        verifyNoInteractions(transferRepository, ledgerRepository, accRepository, idempotencyRepository);
    }

    @Test
    void shouldThrowWhenSourceAccountDoesNotExist() {
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(accRepository.findByIdWithLock(SOURCE_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transferService.transfer(request, IDEMPOTENCY_KEY));

        verify(accRepository, never()).findByIdWithLock(DESTINATION_ID);
        verifyNoWrites();
    }

    @Test
    void shouldThrowWhenDestinationAccountDoesNotExist() {
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(accRepository.findByIdWithLock(SOURCE_ID)).thenReturn(Optional.of(source));
        when(accRepository.findByIdWithLock(DESTINATION_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> transferService.transfer(request, IDEMPOTENCY_KEY));

        verifyNoWrites();
    }

    @Test
    void shouldRejectTransferWhenSourceAccountIsBlocked() {
        source.setStatus(StatusAccount.BLOCKED);
        givenAccountsFound();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

        assertThrows(DuplicateAccountException.class,
                () -> transferService.transfer(request, IDEMPOTENCY_KEY));

        verifyNoWrites();
    }

    @Test
    void shouldRejectTransferWhenDestinationAccountIsBlocked() {
        destination.setStatus(StatusAccount.BLOCKED);
        givenAccountsFound();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

        assertThrows(DuplicateAccountException.class,
                () -> transferService.transfer(request, IDEMPOTENCY_KEY));

        verifyNoWrites();
    }

    @Test
    void shouldRejectTransferBetweenSameAccount() {
        TransferRequest sameAccountRequest = new TransferRequest(SOURCE_ID, SOURCE_ID, AMOUNT);
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(accRepository.findByIdWithLock(SOURCE_ID)).thenReturn(Optional.of(source));

        assertThrows(IdempotencyConflictException.class,
                () -> transferService.transfer(sameAccountRequest, IDEMPOTENCY_KEY));

        verifyNoWrites();
    }

    @Test
    void shouldRejectIdempotencyKeyReusedWithDifferentRequest() {
        givenAccountsFound();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(new Idempotency(IDEMPOTENCY_KEY, "different-hash",
                        IdempotencyStatus.SUCCESS, new Transfer())));

        assertThrows(IdempotencyConflictException.class,
                () -> transferService.transfer(request, IDEMPOTENCY_KEY));

        verifyNoWrites();
    }

    @Test
    void shouldReturnPreviousTransferWhenRequestIsIdempotent() {
        UUID transferId = UUID.randomUUID();
        Transfer previousTransfer = new Transfer(source, destination, AMOUNT, TransferStatus.COMPLETED);
        previousTransfer.setId(transferId);
        String requestHash = GenerateHash.hash(SOURCE_ID, DESTINATION_ID, AMOUNT);
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY))
                .thenReturn(Optional.of(new Idempotency(IDEMPOTENCY_KEY, requestHash,
                        IdempotencyStatus.SUCCESS, previousTransfer)));
        givenAccountsFound();

        TransferResponse response = transferService.transfer(request, IDEMPOTENCY_KEY);

        assertEquals(transferId, response.transferId());
        assertEquals(TransferStatus.COMPLETED, response.status());
        assertEquals(AMOUNT, response.amount());
        assertEquals(SOURCE_ID, response.sourceAccountId());
        assertEquals(DESTINATION_ID, response.destinationAccountId());
        assertEquals(new BigDecimal("100.00"), source.getBalance());
        assertEquals(new BigDecimal("10.00"), destination.getBalance());
        verifyNoWrites();
    }

    @Test
    void shouldRejectTransferWhenSourceBalanceIsInsufficient() {
        source.setBalance(new BigDecimal("25.49"));
        givenAccountsFound();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());

        assertThrows(FinancialRuleException.class,
                () -> transferService.transfer(request, IDEMPOTENCY_KEY));

        verifyNoWrites();
    }

    @Test
    void shouldRejectAmountWithMoreThanTwoDecimalPlaces() {
        givenAccountsFound();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        TransferRequest invalidAmountRequest = new TransferRequest(
                SOURCE_ID, DESTINATION_ID, new BigDecimal("10.001"));

        assertThrows(FinancialRuleException.class,
                () -> transferService.transfer(invalidAmountRequest, IDEMPOTENCY_KEY));

        verifyNoWrites();
    }

    @Test
    void shouldRejectZeroAmount() {
        givenAccountsFound();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        TransferRequest zeroAmountRequest = new TransferRequest(
                SOURCE_ID, DESTINATION_ID, BigDecimal.ZERO);

        assertThrows(FinancialRuleException.class,
                () -> transferService.transfer(zeroAmountRequest, IDEMPOTENCY_KEY));

        verifyNoWrites();
    }

    @Test
    void shouldTransferAmountAndPersistTransferLedgerEntriesAndIdempotencyRecord() {
        UUID transferId = UUID.randomUUID();
        givenAccountsFound();
        when(idempotencyRepository.findByIdempotencyKey(IDEMPOTENCY_KEY)).thenReturn(Optional.empty());
        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer transfer = invocation.getArgument(0);
            transfer.setId(transferId);
            return transfer;
        });

        TransferResponse response = transferService.transfer(request, IDEMPOTENCY_KEY);

        assertEquals(transferId, response.transferId());
        assertEquals(TransferStatus.COMPLETED, response.status());
        assertEquals(AMOUNT, response.amount());
        assertEquals(SOURCE_ID, response.sourceAccountId());
        assertEquals(DESTINATION_ID, response.destinationAccountId());
        assertEquals(new BigDecimal("74.50"), source.getBalance());
        assertEquals(new BigDecimal("35.50"), destination.getBalance());

        ArgumentCaptor<Transfer> transferCaptor = ArgumentCaptor.forClass(Transfer.class);
        verify(transferRepository).save(transferCaptor.capture());
        Transfer savedTransfer = transferCaptor.getValue();
        assertEquals(source, savedTransfer.getSourceAccount());
        assertEquals(destination, savedTransfer.getDestinationAccount());
        assertEquals(AMOUNT, savedTransfer.getAmount());
        assertEquals(TransferStatus.COMPLETED, savedTransfer.getStatus());

        ArgumentCaptor<Ledger> ledgerCaptor = ArgumentCaptor.forClass(Ledger.class);
        verify(ledgerRepository, times(2)).save(ledgerCaptor.capture());
        assertEquals(2, ledgerCaptor.getAllValues().size());
        Ledger debit = ledgerCaptor.getAllValues().get(0);
        assertEquals(source, debit.getAccount());
        assertEquals(savedTransfer, debit.getTransfer());
        assertEquals(EntryType.TRANSFER_SENT, debit.getEntryType());
        assertEquals(AMOUNT, debit.getAmount());
        assertEquals(EntryNature.DEBIT, debit.getNature());
        Ledger credit = ledgerCaptor.getAllValues().get(1);
        assertEquals(destination, credit.getAccount());
        assertEquals(savedTransfer, credit.getTransfer());
        assertEquals(EntryType.TRANSFERENCIA_RECEIVED, credit.getEntryType());
        assertEquals(AMOUNT, credit.getAmount());
        assertEquals(EntryNature.CREDIT, credit.getNature());

        ArgumentCaptor<Idempotency> idempotencyCaptor = ArgumentCaptor.forClass(Idempotency.class);
        verify(idempotencyRepository).save(idempotencyCaptor.capture());
        Idempotency savedIdempotency = idempotencyCaptor.getValue();
        assertEquals(IDEMPOTENCY_KEY, savedIdempotency.getIdempotencyKey());
        assertEquals(GenerateHash.hash(SOURCE_ID, DESTINATION_ID, AMOUNT), savedIdempotency.getRequestHash());
        assertEquals(IdempotencyStatus.SUCCESS, savedIdempotency.getStatus());
        assertEquals(savedTransfer, savedIdempotency.getTransfer());
    }

    private Account account(UUID id, String name, BigDecimal balance) {
        Account account = new Account(name);
        account.setId(id);
        account.setStatus(StatusAccount.ACTIVE);
        account.setBalance(balance);
        return account;
    }

    private void givenAccountsFound() {
        when(accRepository.findByIdWithLock(SOURCE_ID)).thenReturn(Optional.of(source));
        when(accRepository.findByIdWithLock(DESTINATION_ID)).thenReturn(Optional.of(destination));
    }

    private void verifyNoWrites() {
        verify(transferRepository, never()).save(any(Transfer.class));
        verify(ledgerRepository, never()).save(any(Ledger.class));
        verify(idempotencyRepository, never()).save(any(Idempotency.class));
    }
}
