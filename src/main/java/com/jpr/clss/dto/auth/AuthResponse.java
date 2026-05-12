package com.jpr.clss.dto.auth;

public record AuthResponse(
    String accessToken,
    String refreshToken,
    AuthUserResponse user,
    boolean oauthEnabled
) {
}
