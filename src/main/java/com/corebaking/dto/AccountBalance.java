package com.corebaking.dto;

import com.corebaking.model.Account;

import java.math.BigDecimal;

public record AccountBalance(
        BigDecimal amount
) {

    public AccountBalance(Account acc) {
        this(acc.getBalance());
    }
}
