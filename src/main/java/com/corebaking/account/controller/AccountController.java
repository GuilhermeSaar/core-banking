package com.corebaking.account.controller;

import com.corebaking.account.dto.AccountBalance;
import com.corebaking.account.dto.AccountRequest;
import com.corebaking.account.dto.AccountResponse;
import com.corebaking.account.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class AccountController {

    private final AccountService service;

    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody AccountRequest request){

        var response = service.createAccount(request);
        return ResponseEntity.ok().body(response);
    }

    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountBalance> findBalance(@PathVariable UUID id){

        var balance = service.getBalance(id);
        return ResponseEntity.ok().body(balance);
    }
}
