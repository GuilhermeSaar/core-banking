package com.corebaking.transfers.service;

import com.corebaking.account.repository.AccountRepository;
import com.corebaking.idempotency.model.Idempotency;
import com.corebaking.idempotency.model.IdempotencyStatus;
import com.corebaking.idempotency.repository.IdempotencyRepository;
import com.corebaking.ledger.model.EntryNature;
import com.corebaking.ledger.model.EntryType;
import com.corebaking.ledger.model.Ledger;
import com.corebaking.ledger.repository.LedgerRepository;
import com.corebaking.shared.exception.*;
import com.corebaking.shared.utils.GenerateHash;
import com.corebaking.shared.vo.MonetaryAmount;
import com.corebaking.transfers.dto.TransferRequest;
import com.corebaking.transfers.dto.TransferResponse;
import com.corebaking.transfers.model.Transfer;
import com.corebaking.transfers.model.TransferStatus;
import com.corebaking.transfers.repository.TransferRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
@AllArgsConstructor
public class TransferService {

    private final TransferRepository transferRepository;
    private final LedgerRepository ledgerRepository;
    private final AccountRepository accRepository;
    private final IdempotencyRepository idempotencyRepository;

    //RF05 — Realizar transferência
    //• O sistema deve permitir transferir dinheiro entre duas contas.
    //• A conta de origem e a conta de destino devem existir e estar ativas.
    //• As contas devem ser diferentes.
    //• O valor deve ser positivo.
    //• A conta de origem deve possuir saldo suficiente.
    //• O débito e o crédito devem ser concluídos juntos ou revertidos juntos.
    //• Cada transferência deve possuir um identificador único.
    @Transactional
    public TransferResponse transfer(TransferRequest request, UUID idempotencyKey) {

        var monetaryAmount = new MonetaryAmount(request.amount());
        var transactionAmount = monetaryAmount.amount();

        if (idempotencyKey == null) throw new InvalidIdempotencyKeyException( "Key cannot be null");

        var currentHash = GenerateHash.hash(request.sourceAccountId(), request.destinationAccountId(), request.amount());
        var idempotencyOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        var accSource = accRepository.findByIdWithLock(request.sourceAccountId()).orElseThrow(
                () -> new ResourceNotFoundException("account not found")
        );
        var accDest = accRepository.findByIdWithLock(request.destinationAccountId()).orElseThrow(
                () -> new ResourceNotFoundException("account not found")
        );

        if (accSource.isBlocked() || accDest.isBlocked()) throw new DuplicateAccountException("Transfer is blocked");

        if (accSource.equals(accDest)) {
            throw new IdempotencyConflictException("It is not possible to complete the transaction");
        }

        if (idempotencyOpt.isPresent()) {
            var idempotency = idempotencyOpt.get();

            if  (!idempotency.getRequestHash().equals(currentHash)) {
                throw new IdempotencyConflictException("Incompatible hashes");
            }

            var transfer = idempotency.getTransfer();
            var transferId = transfer.getId();

            return new TransferResponse(transferId, transfer.getStatus(),
                    transfer.getAmount(), accSource.getId(), accDest.getId());
        }

        if (accSource.getBalance().compareTo(transactionAmount) < 0) {
            throw new FinancialRuleException("Insufficient balance");
        }

        var balanceSource = accSource.getBalance().subtract(transactionAmount);
        accSource.setBalance(balanceSource);

        var balanceDest = accDest.getBalance().add(transactionAmount);
        accDest.setBalance(balanceDest);

        var status = TransferStatus.COMPLETED;

        var transfer =  transferRepository.save(new Transfer(
                accSource, accDest, transactionAmount,
                status
        ));

        ledgerRepository.save(new Ledger(
                accSource, transfer, EntryType.TRANSFER_SENT, transactionAmount, EntryNature.DEBIT
        ));

        ledgerRepository.save(new Ledger(
                accDest, transfer,  EntryType.TRANSFERENCIA_RECEIVED, transactionAmount, EntryNature.CREDIT
        ));

        idempotencyRepository.save(new Idempotency(
                idempotencyKey, currentHash, IdempotencyStatus.SUCCESS, transfer
        ));

        return new TransferResponse(
                transfer.getId(), status,
                transactionAmount,
                accSource.getId(),
                accDest.getId()
        );
    }
}
