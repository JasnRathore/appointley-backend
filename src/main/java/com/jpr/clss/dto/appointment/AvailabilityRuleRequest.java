package com.jpr.clss.dto.appointment;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AvailabilityRuleRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotNull LocalTime startTime,
    @NotNull LocalTime endTime,
    @NotNull @Min(15) @Max(240) Integer slotDurationMinutes,
    boolean active
) {
}
