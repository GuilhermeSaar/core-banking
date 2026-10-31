package com.corebaking.dto;

import com.corebaking.model.enums.TransferStatus;
import com.corebaking.model.enums.EntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record TransactionResponse(

        UUID transactionId,
        UUID accountId,
        EntryType type,
        BigDecimal amount,
        TransferStatus status,
        LocalDateTime createdAt
) {
}
