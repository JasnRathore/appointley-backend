package com.jpr.clss.dto.appointment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record BookingLinkRequest(
    @NotBlank String title,
    String description,
    @Min(1) @Max(30) Integer expirationDays,
    @Min(15) @Max(240) Integer durationMinutes,
    @NotBlank String timezone,
    String recipientEmail,
    @Min(1) Integer maxUsages
) {
}
