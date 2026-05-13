package com.jpr.clss.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 2, max = 120) String fullName,
    String teamName,
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8, max = 120) String password,
    String inviteToken
) {
}
