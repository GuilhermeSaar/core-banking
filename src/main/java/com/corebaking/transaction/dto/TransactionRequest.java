package com.corebaking.transaction.dto;

import java.math.BigDecimal;

public record TransactionRequest(
        BigDecimal amount
) {
}
