package com.jpr.clss.dto.auth;

public record OAuthStatusResponse(
    boolean googleConfigured,
    String loginUrl
) {
}
