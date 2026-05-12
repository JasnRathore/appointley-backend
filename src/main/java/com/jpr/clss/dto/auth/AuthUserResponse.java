package com.jpr.clss.dto.auth;

import com.jpr.clss.entity.AuthProvider;

public record AuthUserResponse(
    String id,
    String fullName,
    String email,
    AuthProvider authProvider
) {
}
