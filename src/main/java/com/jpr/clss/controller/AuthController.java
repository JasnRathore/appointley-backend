package com.jpr.clss.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpr.clss.dto.auth.AuthResponse;
import com.jpr.clss.dto.auth.AuthUserResponse;
import com.jpr.clss.dto.auth.LoginRequest;
import com.jpr.clss.dto.auth.OAuthStatusResponse;
import com.jpr.clss.dto.auth.RefreshRequest;
import com.jpr.clss.dto.auth.RegisterRequest;
import com.jpr.clss.service.AuthService;
import com.jpr.clss.service.CurrentUserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    public AuthController(AuthService authService, CurrentUserService currentUserService) {
        this.authService = authService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest httpServletRequest) {
        return authService.register(request, httpServletRequest.getRemoteAddr());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, Boolean>> logout(@RequestHeader(name = "X-Refresh-Token", required = false) String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok(Map.of("success", true));
    }

    @PostMapping("/logout-everywhere")
    public ResponseEntity<Map<String, Boolean>> logoutEverywhere() {
        authService.revokeActiveRefreshTokens(currentUserService.requireCurrentUser());
        return ResponseEntity.ok(Map.of("success", true));
    }

    @GetMapping("/me")
    public AuthUserResponse me() {
        return authService.me(currentUserService.requireCurrentUser());
    }

    @GetMapping("/oauth/status")
    public OAuthStatusResponse oauthStatus() {
        return authService.oauthStatus();
    }
}
