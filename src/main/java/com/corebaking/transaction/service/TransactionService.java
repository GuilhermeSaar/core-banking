package com.corebaking.transaction.service;

import com.corebaking.account.repository.AccountRepository;
import com.corebaking.ledger.model.EntryNature;
import com.corebaking.shared.exception.*;
import com.corebaking.transaction.dto.TransactionResponse;
import com.corebaking.ledger.model.EntryType;
import com.corebaking.ledger.model.Ledger;
import com.corebaking.ledger.repository.LedgerRepository;
import com.corebaking.idempotency.model.Idempotency;
import com.corebaking.idempotency.model.IdempotencyStatus;
import com.corebaking.shared.vo.MonetaryAmount;
import com.corebaking.idempotency.repository.IdempotencyRepository;
import com.corebaking.transfers.model.Transfer;
import com.corebaking.transfers.model.TransferStatus;
import com.corebaking.transfers.repository.TransferRepository;
import com.corebaking.shared.utils.GenerateHash;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class TransactionService {

    private final TransferRepository transferRepository;
    private final LedgerRepository ledgerRepository;
    private final IdempotencyRepository idempotencyRepository;
    private final AccountRepository accRepository;

    //RF03 — Realizar depósito
    //• O sistema deve permitir adicionar fundos a uma conta ativa.
    //• O valor deve ser positivo.
    //• A operação deve gerar um registro financeiro.
    //• A alteração do saldo e o registro do lançamento devem ser atômicos.
    @Transactional
    public TransactionResponse deposit(UUID id, BigDecimal amount, UUID idempotencyKey) {

        var entryType = EntryType.DEPOSIT;

        if (idempotencyKey == null) throw new InvalidIdempotencyKeyException("Key cannot be null");

        var currentHash = GenerateHash.hash(id, entryType, amount);

        MonetaryAmount monetaryAmount = new MonetaryAmount(amount);

        var acc = accRepository.findByIdWithLock(id).orElseThrow(
                () -> new ResourceNotFoundException("account not found")
        );

        if (acc.isBlocked()) {
            throw new AccountBlockedException("Account is blocked");
        }

        Optional<Idempotency> idempotencyOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (idempotencyOpt.isPresent()) {

            var idempotency = idempotencyOpt.get();

            if (!idempotency.getRequestHash().equals(currentHash)) {
                throw new IdempotencyConflictException("Incompatible hashes");
            }

            var transfer = idempotency.getTransfer();
            var transferId = transfer.getId();
            var ledger = ledgerRepository.findByTransferId(transferId);

            return new TransactionResponse(transferId, acc.getId(), ledger.getEntryType(),
                    transfer.getAmount(), transfer.getStatus(), transfer.getCreatedAt());

        }

        var balance =  acc.getBalance().add(monetaryAmount.amount());
        acc.setBalance(balance);

        var status = TransferStatus.COMPLETED;
        var transfer = transferRepository.save(new Transfer(
                acc, monetaryAmount.amount(), status
        ));

        idempotencyRepository.save(new Idempotency(idempotencyKey,
                currentHash, IdempotencyStatus.SUCCESS, transfer));

        ledgerRepository.save(new Ledger(acc, transfer, entryType, monetaryAmount.amount(), EntryNature.CREDIT));

        return new TransactionResponse(
                transfer.getId(), acc.getId(), entryType,
                monetaryAmount.amount(), status, transfer.getCreatedAt()
        );
    }

    //RF04 — Realizar saque
    //• O sistema deve permitir retirar fundos de uma conta ativa.
    //• O valor deve ser positivo.
    //• Não pode haver saldo negativo.
    //• A operação deve gerar um lançamento de débito.
    @Transactional
    public TransactionResponse withdraw(UUID id, BigDecimal amount, UUID idempotencyKey) {

        MonetaryAmount monetaryAmount = new MonetaryAmount(amount);
        BigDecimal transferAmount = monetaryAmount.amount();

        if (idempotencyKey == null) throw new InvalidIdempotencyKeyException("Key cannot be null");

        var entryType = EntryType.WITHDRAWAL;
        var currentHash = GenerateHash.hash(id, entryType, amount);

        var acc = accRepository.findByIdWithLock(id).orElseThrow(
                () -> new ResourceNotFoundException("account not found")
        );

        if (acc.isBlocked()) {
            throw new AccountBlockedException("Account is blocked");
        }

        Optional<Idempotency> idempotencyOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (idempotencyOpt.isPresent()) {

            var idempotency = idempotencyOpt.get();

            if (!idempotency.getRequestHash().equals(currentHash)) {
                throw new IdempotencyConflictException("Incompatible hashes");
            }

            var transfer = idempotency.getTransfer();
            var transferId = transfer.getId();
            var ledger = ledgerRepository.findByTransferId(transferId);

            return new TransactionResponse(transferId, acc.getId(), ledger.getEntryType(),
                    transfer.getAmount(), transfer.getStatus(), transfer.getCreatedAt());
        }

        if (acc.getBalance().compareTo(transferAmount) < 0) {
            throw new FinancialRuleException("Insufficient balance");
        }

        var balance = acc.getBalance().subtract(monetaryAmount.amount());
        acc.setBalance(balance);

        var status = TransferStatus.COMPLETED;
        var transfer = transferRepository.save(new Transfer(
                acc, monetaryAmount.amount(), status
        ));

        idempotencyRepository.save(new Idempotency(idempotencyKey,
                currentHash, IdempotencyStatus.SUCCESS, transfer));

        ledgerRepository.save(new Ledger(acc, transfer, entryType, monetaryAmount.amount(), EntryNature.DEBIT));

        return new TransactionResponse(
                transfer.getId(), acc.getId(), entryType,
                monetaryAmount.amount(), status, transfer.getCreatedAt()
        );
    }
}
