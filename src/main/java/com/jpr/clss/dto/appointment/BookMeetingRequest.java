package com.jpr.clss.dto.appointment;

import java.time.Instant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record BookMeetingRequest(
    @NotBlank String clientName,
    @NotBlank @Email String clientEmail,
    @NotNull Instant startsAt,
    String notes
) {
}
