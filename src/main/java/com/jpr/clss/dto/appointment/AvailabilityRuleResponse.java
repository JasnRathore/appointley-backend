package com.jpr.clss.dto.appointment;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AvailabilityRuleResponse(
    String id,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    Integer slotDurationMinutes,
    boolean active
) {
}
