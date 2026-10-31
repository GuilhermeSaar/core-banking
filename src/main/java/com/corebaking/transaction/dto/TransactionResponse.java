package com.corebaking.transaction.dto;

import com.corebaking.transfers.model.TransferStatus;
import com.corebaking.ledger.model.EntryType;

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
