package com.corebaking.dto;

import com.corebaking.model.enums.TransferStatus;

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


