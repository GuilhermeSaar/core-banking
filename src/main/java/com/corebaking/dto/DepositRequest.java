package com.corebaking.dto;

import java.math.BigDecimal;

public record DepositRequest(
        BigDecimal amount
) {
}
