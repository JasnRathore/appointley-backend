package com.jpr.clss.dto.team;

import com.jpr.clss.entity.TeamRole;

public record InviteDetailsResponse(
    String teamName,
    String inviterName,
    TeamRole role,
    String email,
    boolean isExistingUser,
    boolean isExpired,
    boolean isAccepted,
    boolean isRevoked
) {}
