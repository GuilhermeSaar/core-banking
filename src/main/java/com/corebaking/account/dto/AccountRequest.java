package com.corebaking.account.dto;

import io.swagger.v3.oas.annotations.media.Schema;
public record AccountRequest(@Schema(description = "Nome da conta", example = "Conta Exemplo") String name) {
}
