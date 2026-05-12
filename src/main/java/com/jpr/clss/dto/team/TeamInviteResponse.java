package com.jpr.clss.dto.team;

import java.time.Instant;

import com.jpr.clss.entity.TeamRole;

public record TeamInviteResponse(
    String id,
    String token,
    String email,
    TeamRole role,
    Instant expiresAt,
    boolean accepted,
    boolean revoked
) {
}
