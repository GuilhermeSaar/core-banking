package com.corebaking.transfers.controller;

import com.corebaking.transfers.dto.TransferRequest;
import com.corebaking.transfers.dto.TransferResponse;
import com.corebaking.transfers.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Transferências")
@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class TransferController {

    private final TransferService service;

    @Operation(summary = "Transferir entre contas", description = "Transfere um valor positivo entre contas ativas distintas.")
    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @Parameter(description = "UUID único da operação. Reutilize a chave com os mesmos dados para repetição idempotente.",
                    required = true, example = "33333333-3333-4333-8333-333333333333")
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            @RequestBody TransferRequest request){

        System.out.print("Idempotency-Key: " + key + "\n");

        var transfer = service.transfer(request, key);
        return ResponseEntity.ok().body(transfer);
    }

}
