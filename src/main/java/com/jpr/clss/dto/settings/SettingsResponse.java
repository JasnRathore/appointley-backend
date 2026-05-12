package com.jpr.clss.dto.settings;

public record SettingsResponse(
    String fullName,
    String email,
    String teamName,
    String senderName,
    boolean oauthEnabled,
    boolean emailOnBooking,
    boolean inAppOnBooking,
    boolean weeklyDigest,
    boolean marketingEmails
) {
}
