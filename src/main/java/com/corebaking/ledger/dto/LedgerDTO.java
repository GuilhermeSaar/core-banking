package com.corebaking.ledger.dto;

import com.corebaking.ledger.model.Ledger;
import com.corebaking.ledger.model.EntryType;

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
                x.getAccount().getId(),
                x.getTransfer().getId(),
                x.getEntryType(),
                x.getAmount(),
                x.getCreatedAt()
        );
    }
}
