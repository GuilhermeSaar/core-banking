package com.corebaking.ledger.dto;

import java.util.List;

public record LedgersResponse(
        List<LedgerResponse> ledgers
) {
}
