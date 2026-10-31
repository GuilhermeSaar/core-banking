package com.corebaking.dto;

import java.math.BigDecimal;

public record TransactionRequest(
        BigDecimal amount
) {
}
