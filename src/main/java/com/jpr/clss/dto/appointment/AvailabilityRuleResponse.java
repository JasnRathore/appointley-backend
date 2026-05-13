package com.jpr.clss.dto.appointment;

import java.time.DayOfWeek;

public record AvailabilityRuleResponse(
    String id,
    DayOfWeek dayOfWeek,
    String startTime,
    String endTime,
    Integer slotDurationMinutes,
    boolean active
) {
}
