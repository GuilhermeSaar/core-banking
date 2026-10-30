package com.corebaking.model.vo;

import java.math.BigDecimal;

public record MonetaryAmount(
        BigDecimal amount
) {

    public MonetaryAmount {

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }

        if (amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
