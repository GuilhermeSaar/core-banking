package com.corebaking.transfers.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        UUID sourceAccountId,
        UUID destinationAccountId,
        BigDecimal amount
) {
}
