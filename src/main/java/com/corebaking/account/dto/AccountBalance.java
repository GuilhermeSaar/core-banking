package com.corebaking.account.dto;

import com.corebaking.account.model.Account;

import java.math.BigDecimal;

public record AccountBalance(
        BigDecimal amount
) {

    public AccountBalance(Account acc) {
        this(acc.getBalance());
    }
}
