package com.corebaking.account.dto;

import com.corebaking.account.model.Account;
import com.corebaking.account.model.StatusAccount;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record AccountResponse(
        String name,
        StatusAccount status,
        BigDecimal balance,
        LocalDateTime createdAt,
        UUID id
)
{

    public AccountResponse(Account account) {
        this(account.getName(), account.getStatus(), account.getBalance(), account.getCreatedAt(), account.getId());
    }
}
