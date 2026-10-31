package com.corebaking.transfers.controller;

import com.corebaking.transfers.dto.TransferRequest;
import com.corebaking.transfers.dto.TransferResponse;
import com.corebaking.transfers.service.TransferService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1")
@AllArgsConstructor
public class TransferController {

    private final TransferService service;

    @PostMapping("/transfer")
    public ResponseEntity<TransferResponse> transfer(
            @RequestHeader(value = "Idempotency-Key", required = false) UUID key,
            @RequestBody TransferRequest request){

        System.out.print("Idempotency-Key: " + key + "\n");

        var transfer = service.transfer(request, key);
        return ResponseEntity.ok().body(transfer);
    }

}
