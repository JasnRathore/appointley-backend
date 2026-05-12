package com.jpr.clss.dto.appointment;

import java.time.Instant;

public record PublicSlotResponse(
    Instant startsAt,
    Instant endsAt
) {
}
