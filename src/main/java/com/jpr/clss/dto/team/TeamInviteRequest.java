package com.jpr.clss.dto.team;

import com.jpr.clss.entity.TeamRole;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record TeamInviteRequest(
    @Email String email,
    @NotNull TeamRole role
) {
}
