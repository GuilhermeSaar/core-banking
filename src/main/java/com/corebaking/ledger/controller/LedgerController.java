package com.corebaking.ledger.controller;

import com.corebaking.ledger.dto.LedgerResponse;
import com.corebaking.ledger.service.LedgerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Extratos")
@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class LedgerController {

    private final LedgerService service;

    @Operation(summary = "Consultar extrato", description = "Retorna os lançamentos da conta; uma conta sem lançamentos retorna uma lista vazia.")
    @GetMapping("/{id}/ledger")
    public ResponseEntity<List<LedgerResponse>> findLedgers(@PathVariable UUID id){

        var  ledgers = service.getLedgers(id);
        return ResponseEntity.ok().body(ledgers);
    }
}
