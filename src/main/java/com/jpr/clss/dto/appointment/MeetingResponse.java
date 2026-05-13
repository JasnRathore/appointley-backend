package com.jpr.clss.dto.appointment;

import java.time.Instant;

import com.jpr.clss.entity.MeetingStatus;

public record MeetingResponse(
    String id,
    String bookingLinkTitle,
    String bookingLinkToken,
    String clientName,
    String clientEmail,
    Instant startsAt,
    Instant endsAt,
    MeetingStatus status,
    String timezone,
    String notes,
    boolean manageable
) {
}
