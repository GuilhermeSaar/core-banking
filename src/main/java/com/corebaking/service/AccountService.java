package com.corebaking.service;

import com.corebaking.dto.AccountBalance;
import com.corebaking.dto.AccountRequest;
import com.corebaking.dto.AccountResponse;
import com.corebaking.dto.TransactionResponse;
import com.corebaking.exception.ResourceNotFoundException;
import com.corebaking.model.Account;
import com.corebaking.model.Idempotency;
import com.corebaking.model.Ledger;
import com.corebaking.model.Transfer;
import com.corebaking.model.enums.EntryType;
import com.corebaking.model.enums.IdempotencyStatus;
import com.corebaking.model.enums.TransferStatus;
import com.corebaking.model.vo.MonetaryAmount;
import com.corebaking.repository.AccountRepository;
import com.corebaking.repository.IdempotencyRepository;
import com.corebaking.repository.LedgerRepository;
import com.corebaking.repository.TransferRepository;
import com.corebaking.utils.GenerateHash;
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
public class AccountService {

    private final AccountRepository accRepository;
    private final TransferRepository transferRepository;
    private final LedgerRepository ledgerRepository;
    private final IdempotencyRepository  idempotencyRepository;

    //RF01 — Criar conta
    //• O sistema deve permitir a abertura de uma conta digital.
    //• Cada conta deve possuir um identificador único.
    //• A conta deve iniciar com saldo zero.
    //• Deve possuir status, como ACTIVE ou BLOCKED.
    //• Uma conta bloqueada não pode realizar movimentações.
    @Transactional
    public AccountResponse createAccount(AccountRequest request) {

        if (accRepository.existsByName(request.name())) throw new IllegalArgumentException("Account already exists");

        var account = accRepository.save(new Account(request.name()));

        return new AccountResponse(account);
    }

    //RF02 — Consultar conta e saldo
    //• O sistema deve permitir consultar os dados de uma conta e seu saldo atual.
    @Transactional(readOnly = true)
    public AccountBalance getBalance(UUID id){

        var acc = accRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException ("Account not found")
        );
        return new AccountBalance(acc);
    }

    //RF03 — Realizar depósito
    //• O sistema deve permitir adicionar fundos a uma conta ativa.
    //• O valor deve ser positivo.
    //• A operação deve gerar um registro financeiro.
    //• A alteração do saldo e o registro do lançamento devem ser atômicos.
    @Transactional
    public TransactionResponse deposit(UUID id, BigDecimal amount, UUID idempotencyKey) {

        if (idempotencyKey == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Key cannot be null");

        var currentHash = GenerateHash.hash(id, amount);

        MonetaryAmount monetaryAmount = new MonetaryAmount(amount);

        var acc = accRepository.findByIdWithLock(id).orElseThrow(
                () -> new ResourceNotFoundException("account not found")
        );

        if (acc.isBlocked()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is blocked");
        }

        Optional<Idempotency> idempotencyOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);

       if (idempotencyOpt.isPresent()) {

           var idempotency = idempotencyOpt.get();

           if (!idempotency.getRequestHash().equals(currentHash)) {
               throw new ResponseStatusException(HttpStatus.CONFLICT, "Incompatible hashes");
           }


           var transfer = idempotency.getTransfer();
           var transferId = transfer.getId();
           var ledger = ledgerRepository.findByTransferId(transferId);

           return new TransactionResponse(transferId, acc.getId(), ledger.getEntryType(),
                   transfer.getAmount(), transfer.getStatus(), transfer.getCreatedAt());

       }

        var entryType = EntryType.DEPOSIT;

        var balance =  acc.getBalance().add(monetaryAmount.amount());
        acc.setBalance(balance);

        var status = TransferStatus.COMPLETED;
        var transfer = transferRepository.save(new Transfer(
                acc, monetaryAmount.amount(), status
        ));

        idempotencyRepository.save(new Idempotency(idempotencyKey,
                currentHash, IdempotencyStatus.SUCCESS, transfer));

        ledgerRepository.save(new Ledger(acc, transfer, entryType, monetaryAmount.amount()));

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

        if (idempotencyKey == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Key cannot be null");

        var currentHash = GenerateHash.hash(id, amount);

        var acc = accRepository.findByIdWithLock(id).orElseThrow(
                () -> new ResourceNotFoundException("account not found")
        );

        MonetaryAmount monetaryAmount = new MonetaryAmount(amount);

        if (acc.isBlocked() || acc.getBalance().compareTo(monetaryAmount.amount()) < 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account is blocked");
        }

        Optional<Idempotency> idempotencyOpt = idempotencyRepository.findByIdempotencyKey(idempotencyKey);

        if (idempotencyOpt.isPresent()) {

            var idempotency = idempotencyOpt.get();

            if (!idempotency.getRequestHash().equals(currentHash)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Incompatible hashes");
            }

            var transfer = idempotency.getTransfer();
            var transferId = transfer.getId();
            var ledger = ledgerRepository.findByTransferId(transferId);

            return new TransactionResponse(transferId, acc.getId(), ledger.getEntryType(),
                    transfer.getAmount(), transfer.getStatus(), transfer.getCreatedAt());
        }

        var entryType = EntryType.WITHDRAWAL;
        var balance = acc.getBalance().subtract(monetaryAmount.amount());
        acc.setBalance(balance);

        var status = TransferStatus.COMPLETED;
        var transfer = transferRepository.save(new Transfer(
                acc, monetaryAmount.amount(), status
        ));

        idempotencyRepository.save(new Idempotency(idempotencyKey,
                currentHash, IdempotencyStatus.SUCCESS, transfer));

        ledgerRepository.save(new Ledger(acc, transfer, entryType, monetaryAmount.amount()));

        return new TransactionResponse(
                transfer.getId(), acc.getId(), entryType,
                monetaryAmount.amount(), status, transfer.getCreatedAt()
        );
    }





























}
