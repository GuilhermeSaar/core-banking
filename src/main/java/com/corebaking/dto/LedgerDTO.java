package com.corebaking.dto;

import com.corebaking.model.Ledger;
import com.corebaking.model.enums.EntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LedgerDTO(

        UUID accountId,
        UUID transferId,
        EntryType entryType,
        BigDecimal amount,
        LocalDateTime createdAt
) {
    public LedgerDTO(Ledger x) {

        this(
                x.getId(),
                x.getTransfer().getId(),
                x.getEntryType(),
                x.getAmount(),
                x.getCreatedAt()
        );
    }
}
