package com.corebaking.ledger.dto;

import com.corebaking.ledger.model.EntryNature;
import com.corebaking.ledger.model.Ledger;
import com.corebaking.ledger.model.EntryType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record LedgerResponse(

        UUID accountId,
        UUID transferId,
        EntryType entryType,
        BigDecimal amount,
        EntryNature nature,
        LocalDateTime createdAt
) {
    public LedgerResponse(Ledger data) {

        this(
                data.getAccount().getId(),
                data.getTransfer().getId(),
                data.getEntryType(),
                data.getAmount(),
                data.getNature(),
                data.getCreatedAt()
        );
    }
}
