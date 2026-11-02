package com.corebaking.shared.exception.dto;

import java.time.Instant;
import java.util.List;

public record ResponseError(

        Instant timestamp,
        int status,
        String error,
        String path,
        List<FieldErrors> errors
) {
}
