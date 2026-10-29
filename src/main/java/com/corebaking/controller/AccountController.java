package com.corebaking.controller;

import com.corebaking.dto.AccountBalance;
import com.corebaking.dto.AccountRequest;
import com.corebaking.dto.AccountResponse;
import com.corebaking.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<AccountBalance> findBalance(@PathVariable Long id){

        var balance = service.getBalance(id);
        return ResponseEntity.ok().body(balance);
    }
}
