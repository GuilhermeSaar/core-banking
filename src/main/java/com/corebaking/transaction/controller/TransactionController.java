package com.corebaking.transaction.controller;

import com.corebaking.transaction.dto.TransactionRequest;
import com.corebaking.transaction.dto.TransactionResponse;
import com.corebaking.transaction.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Movimentações")
@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class TransactionController {

    private final TransactionService service;

    @Operation(summary = "Depositar", description = "Credita um valor positivo na conta identificada pelo id.")
    @PostMapping("/transaction/{id}/deposits")
    public ResponseEntity<TransactionResponse> deposits(
            @Parameter(description = "UUID único da operação. Reutilize a chave com os mesmos dados para repetição idempotente.",
                    required = true, example = "33333333-3333-4333-8333-333333333333")
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            @PathVariable UUID id, @RequestBody TransactionRequest amount){

        System.out.print("Idempotency-Key: " + key + "\n");

        var deposit = service.deposit(id, amount.amount(), key);
        return ResponseEntity.ok().body(deposit);
    }

    @Operation(summary = "Sacar", description = "Debita um valor positivo da conta identificada pelo id.")
    @PostMapping("/transaction/{id}/withdraw")
    public ResponseEntity<TransactionResponse> withdrawals(
            @Parameter(description = "UUID único da operação. Reutilize a chave com os mesmos dados para repetição idempotente.",
                    required = true, example = "33333333-3333-4333-8333-333333333333")
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            @PathVariable UUID id, @RequestBody TransactionRequest amount){

        System.out.print("Idempotency-Key: " + key + "\n");

        var deposit = service.withdraw(id, amount.amount(), key);
        return ResponseEntity.ok().body(deposit);
    }
}
