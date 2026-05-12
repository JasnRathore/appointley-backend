package com.jpr.clss.dto.appointment;

import java.time.Instant;

public record BlockedDateResponse(
    String id,
    Instant startsAt,
    Instant endsAt,
    String reason
) {
}
