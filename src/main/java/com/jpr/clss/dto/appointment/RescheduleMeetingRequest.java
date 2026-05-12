package com.jpr.clss.dto.appointment;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record RescheduleMeetingRequest(
    @NotNull Instant newStartsAt
) {}
