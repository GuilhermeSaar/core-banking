package com.corebaking.account.controller;

import com.corebaking.account.dto.AccountDTO;
import com.corebaking.account.dto.AccountRequest;
import com.corebaking.account.dto.AccountResponse;
import com.corebaking.account.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Contas")
@RestController
@RequestMapping("/api/v1")
@AllArgsConstructor
public class AccountController {

    private final AccountService service;

    @Operation(summary = "Criar conta", description = "Cria uma conta ativa com saldo inicial zero.")
    @PostMapping("/accounts")
    public ResponseEntity<AccountResponse> createAccount(@RequestBody AccountRequest request){

        var response = service.createAccount(request);
        return ResponseEntity.ok().body(response);
    }

    @Operation(summary = "Consultar conta e saldo")
    @GetMapping("/accounts/{id}")
    public ResponseEntity<AccountDTO> findBalance(@PathVariable UUID id){

        var balance = service.infoAcc(id);
        return ResponseEntity.ok().body(balance);
    }
}
