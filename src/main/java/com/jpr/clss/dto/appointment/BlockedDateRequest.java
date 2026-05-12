package com.jpr.clss.dto.appointment;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record BlockedDateRequest(
    @NotNull Instant startsAt,
    @NotNull Instant endsAt,
    String reason
) {
}
