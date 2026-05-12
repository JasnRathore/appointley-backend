package com.jpr.clss.dto.team;

import com.jpr.clss.entity.TeamRole;

import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(@NotNull TeamRole role) {
}
