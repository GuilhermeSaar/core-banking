package com.corebaking.dto;

import com.corebaking.model.Account;
import com.corebaking.model.enums.StatusAccount;

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
