package com.corebaking.transaction.controller;

import com.corebaking.transaction.dto.TransactionRequest;
import com.corebaking.transaction.dto.TransactionResponse;
import com.corebaking.transaction.service.TransactionService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @PostMapping("/transaction/{id}/deposits")
    public ResponseEntity<TransactionResponse> deposits(
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            @PathVariable UUID id, @RequestBody TransactionRequest amount){

        System.out.print("Idempotency-Key: " + key + "\n");

        var deposit = service.deposit(id, amount.amount(), key);
        return ResponseEntity.ok().body(deposit);
    }

    @PostMapping("/transaction/{id}/withdraw")
    public ResponseEntity<TransactionResponse> withdrawals(
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            @PathVariable UUID id, @RequestBody TransactionRequest amount){

        System.out.print("Idempotency-Key: " + key + "\n");

        var deposit = service.withdraw(id, amount.amount(), key);
        return ResponseEntity.ok().body(deposit);
    }
}
