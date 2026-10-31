package com.corebaking.account.service;

import com.corebaking.account.dto.AccountBalance;
import com.corebaking.account.dto.AccountRequest;
import com.corebaking.account.dto.AccountResponse;
import com.corebaking.account.model.Account;
import com.corebaking.account.repository.AccountRepository;
import com.corebaking.shared.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@AllArgsConstructor
public class AccountService {

    private final AccountRepository accRepository;

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
}
