package com.corebaking.transaction.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record TransactionRequest(
        @Schema(description = "Valor positivo da movimentação", example = "100.00")
        
        @NotNull(message = "Value cannot be null")
        @Positive(message = "Value must be positive")
        BigDecimal amount
) {
}
