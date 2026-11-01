package com.corebaking.account.dto;

import com.corebaking.account.model.Account;
import com.corebaking.account.model.StatusAccount;

import java.math.BigDecimal;

public record AccountDTO(
        String name,
        StatusAccount status,
        BigDecimal balance
) {

    public AccountDTO(Account acc) {
        this(acc.getName(), acc.getStatus(), acc.getBalance());
    }
}
