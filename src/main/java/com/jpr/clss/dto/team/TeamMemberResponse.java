package com.jpr.clss.dto.team;

import java.time.Instant;

import com.jpr.clss.entity.TeamRole;

public record TeamMemberResponse(
    String id,
    String userId,
    String fullName,
    String email,
    TeamRole role,
    Instant joinedAt
) {
}
