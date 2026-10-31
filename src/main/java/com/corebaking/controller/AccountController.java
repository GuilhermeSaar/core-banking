package com.corebaking.controller;

import com.corebaking.dto.*;
import com.corebaking.service.AccountService;
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

    @PostMapping("/accounts/{id}/deposits")
    public ResponseEntity<TransactionResponse> deposits(
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            @PathVariable UUID id, @RequestBody TransactionRequest amount){

        System.out.print("Idempotency-Key: " + key + "\n");

        var deposit = service.deposit(id, amount.amount(), key);
        return ResponseEntity.ok().body(deposit);
    }

    @PostMapping("/accounts/{id}/withdraw")
    public ResponseEntity<TransactionResponse> withdrawals(
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            @PathVariable UUID id, @RequestBody TransactionRequest amount){

        System.out.print("Idempotency-Key: " + key + "\n");

        var deposit = service.withdraw(id, amount.amount(), key);
        return ResponseEntity.ok().body(deposit);
    }
}
