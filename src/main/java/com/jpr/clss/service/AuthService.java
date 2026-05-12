package com.jpr.clss.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpr.clss.dto.auth.AuthResponse;
import com.jpr.clss.dto.auth.AuthUserResponse;
import com.jpr.clss.dto.auth.LoginRequest;
import com.jpr.clss.dto.auth.OAuthStatusResponse;
import com.jpr.clss.dto.auth.RefreshRequest;
import com.jpr.clss.dto.auth.RegisterRequest;
import com.jpr.clss.entity.AuthProvider;
import com.jpr.clss.entity.EmailType;
import com.jpr.clss.entity.RefreshToken;
import com.jpr.clss.entity.Team;
import com.jpr.clss.entity.TeamMember;
import com.jpr.clss.entity.TeamRole;
import com.jpr.clss.entity.User;
import com.jpr.clss.exception.ApiException;
import com.jpr.clss.repository.RefreshTokenRepository;
import com.jpr.clss.repository.TeamMemberRepository;
import com.jpr.clss.repository.TeamRepository;
import com.jpr.clss.repository.UserRepository;
import com.jpr.clss.security.JwtService;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final AuditService auditService;
    private final EmailQueueService emailQueueService;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider;
    private final long refreshTokenDays;

    public AuthService(
        AuthenticationManager authenticationManager,
        PasswordEncoder passwordEncoder,
        UserRepository userRepository,
        TeamRepository teamRepository,
        TeamMemberRepository teamMemberRepository,
        RefreshTokenRepository refreshTokenRepository,
        JwtService jwtService,
        AuditService auditService,
        EmailQueueService emailQueueService,
        ObjectProvider<ClientRegistrationRepository> clientRegistrationRepositoryProvider,
        @Value("${app.jwt.refresh-token-days}") long refreshTokenDays
    ) {
        this.authenticationManager = authenticationManager;
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.auditService = auditService;
        this.emailQueueService = emailQueueService;
        this.clientRegistrationRepositoryProvider = clientRegistrationRepositoryProvider;
        this.refreshTokenDays = refreshTokenDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email already registered");
        }

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setAuthProvider(AuthProvider.LOCAL);
        userRepository.save(user);

        Team team = new Team();
        team.setName(request.teamName().trim());
        team.setSenderName(request.teamName().trim());
        team.setOwner(user);
        teamRepository.save(team);

        TeamMember teamMember = new TeamMember();
        teamMember.setTeam(team);
        teamMember.setUser(user);
        teamMember.setRole(TeamRole.OWNER);
        teamMemberRepository.save(teamMember);

        auditService.log(user, "USER_REGISTERED", "USER", user.getId(), Map.of("email", user.getEmail()), ipAddress);
        auditService.log(user, "TEAM_CREATED", "TEAM", team.getId(), Map.of("teamName", team.getName()), ipAddress);
        emailQueueService.queue(
            EmailType.BOOKING_LINK,
            user.getEmail(),
            team.getSenderName(),
            "Welcome to Appointley",
            "Your workspace for " + team.getName() + " is ready."
        );

        return createAuthResponse(user);
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );

        User user = userRepository.findByEmailIgnoreCase(request.email())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token not found"));

        if (refreshToken.getRevokedAt() != null || refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        refreshToken.setRevokedAt(Instant.now());
        User user = refreshToken.getUser();
        return createAuthResponse(user);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }
        refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> token.setRevokedAt(Instant.now()));
    }

    public AuthUserResponse me(User user) {
        return toUserResponse(user);
    }

    public OAuthStatusResponse oauthStatus() {
        boolean configured = isGoogleConfigured();
        return new OAuthStatusResponse(configured, configured ? "/oauth2/authorization/google" : null);
    }

    private AuthResponse createAuthResponse(User user) {
        revokeActiveRefreshTokens(user);
        String refreshTokenValue = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
            jwtService.generateAccessToken(user),
            refreshTokenValue,
            toUserResponse(user),
            isGoogleConfigured()
        );
    }

    public void revokeActiveRefreshTokens(User user) {
        List<RefreshToken> tokens = refreshTokenRepository.findByUserIdAndRevokedAtIsNull(user.getId());
        for (RefreshToken token : tokens) {
            token.setRevokedAt(Instant.now());
        }
    }

    private AuthUserResponse toUserResponse(User user) {
        return new AuthUserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getAuthProvider());
    }

    private boolean isGoogleConfigured() {
        ClientRegistrationRepository repository = clientRegistrationRepositoryProvider.getIfAvailable();
        return repository != null && repository.findByRegistrationId("google") != null;
    }
}
