package com.corebaking.ledger.controller;

import com.corebaking.ledger.dto.LedgerDTO;
import com.corebaking.ledger.service.LedgerService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class LedgerController {

    private final LedgerService service;

    @GetMapping("/{id}/ledger")
    public ResponseEntity<List<LedgerDTO>> findLedgers(@PathVariable UUID id){

        var  ledgers = service.getLedgers(id);
        return ResponseEntity.ok().body(ledgers);
    }
}
