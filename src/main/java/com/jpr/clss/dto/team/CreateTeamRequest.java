package com.jpr.clss.dto.team;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTeamRequest(@NotBlank @Size(min = 2, max = 120) String name) {
}
