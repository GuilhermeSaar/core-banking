package com.corebaking.dto;

import java.util.List;

public record LedgersResponse(
        List<LedgerDTO> ledgers
) {
}
