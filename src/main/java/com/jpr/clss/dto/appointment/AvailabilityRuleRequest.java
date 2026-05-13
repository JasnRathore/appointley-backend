package com.jpr.clss.dto.appointment;

import java.time.DayOfWeek;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AvailabilityRuleRequest(
    @NotNull DayOfWeek dayOfWeek,
    @NotBlank String startTime,
    @NotBlank String endTime,
    @NotNull @Min(15) @Max(240) Integer slotDurationMinutes,
    boolean active
) {
}
