package com.corebaking.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record TransactionRequest(
        @Schema(description = "Valor positivo da movimentação", example = "100.00") BigDecimal amount
) {
}
