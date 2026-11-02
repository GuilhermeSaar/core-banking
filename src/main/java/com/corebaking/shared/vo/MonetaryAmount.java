package com.corebaking.shared.vo;

import com.corebaking.shared.exception.FinancialRuleException;

import java.math.BigDecimal;

public record MonetaryAmount(
        
        BigDecimal amount
) {

    public MonetaryAmount {

        if (amount == null) {
            throw new FinancialRuleException("MonetaryAmount cannot be null");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new FinancialRuleException("MonetaryAmount cannot be negative");
        }

        if (amount.scale() > 2) {
            throw new FinancialRuleException("decimal scale not permitted");
        }

    }
}
