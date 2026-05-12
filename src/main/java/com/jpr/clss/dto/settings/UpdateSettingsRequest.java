package com.jpr.clss.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateSettingsRequest(
    @NotBlank @Size(min = 2, max = 120) String fullName,
    @NotBlank @Size(min = 2, max = 120) String teamName,
    @NotBlank @Size(min = 2, max = 120) String senderName,
    boolean emailOnBooking,
    boolean inAppOnBooking,
    boolean weeklyDigest,
    boolean marketingEmails
) {
}
