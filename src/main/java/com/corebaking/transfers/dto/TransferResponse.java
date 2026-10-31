package com.corebaking.transfers.dto;

import com.corebaking.transfers.model.TransferStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferResponse(
        UUID transferId,
        TransferStatus status,
        BigDecimal amount,
        UUID sourceAccountId,
        UUID destinationAccountId
) {
}


