package com.jpr.clss.dto.appointment;

import java.time.Instant;

public record BookingLinkResponse(
    String id,
    String token,
    String title,
    String description,
    Instant expirationDate,
    boolean active,
    Integer durationMinutes,
    String timezone,
    String bookingUrl,
    String recipientEmail,
    boolean oneTimeUse
) {
}
