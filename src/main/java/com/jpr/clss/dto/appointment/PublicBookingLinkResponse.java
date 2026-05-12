package com.jpr.clss.dto.appointment;

import java.time.Instant;
import java.util.List;

public record PublicBookingLinkResponse(
    String title,
    String description,
    String timezone,
    Integer durationMinutes,
    Instant expiresAt,
    List<PublicSlotResponse> slots,
    String recipientEmail
) {
}
