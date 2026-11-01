package com.corebaking.transfers.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @Schema(description = "Conta de origem", example = "11111111-1111-4111-8111-111111111111") UUID sourceAccountId,
        @Schema(description = "Conta de destino", example = "22222222-2222-4222-8222-222222222222") UUID destinationAccountId,
        @Schema(description = "Valor positivo da transferência", example = "100.00") BigDecimal amount
) {
}
