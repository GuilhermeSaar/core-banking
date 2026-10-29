package com.corebaking.service;

import com.corebaking.exception.ResourceNotFoundException;
import com.corebaking.model.Account;
import com.corebaking.dto.AccountBalance;
import com.corebaking.dto.AccountRequest;
import com.corebaking.dto.AccountResponse;
import com.corebaking.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Service
@AllArgsConstructor
public class AccountService {

    private final AccountRepository repository;

    @Transactional
    public AccountResponse createAccount(AccountRequest request) {

        if (repository.existsByName(request.name())) throw new IllegalArgumentException("Account already exists");

        UUID id = UUID.nameUUIDFromBytes(request.name().getBytes(StandardCharsets.UTF_8));
        var account = repository.save(new Account(id, request.name()));

        return new AccountResponse(account);
    }

    @Transactional(readOnly = true)
    public AccountBalance getBalance(Long id){

        var acc = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException ("Account not found")
        );
        return new AccountBalance(acc);
    }
}
