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
import com.jpr.clss.service.TeamService;

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
    private final TeamService teamService;
    private final EmailTemplateService emailTemplateService;
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
        TeamService teamService,
        EmailTemplateService emailTemplateService,
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
        this.teamService = teamService;
        this.emailTemplateService = emailTemplateService;
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
        auditService.log(user, "USER_REGISTERED", "USER", user.getId(), Map.of("email", user.getEmail()), ipAddress);

        String teamName = (request.teamName() == null || request.teamName().isBlank() || request.teamName().equals("Invitation"))
            ? user.getFullName() + "'s Team"
            : request.teamName().trim();

        Team defaultTeam = new Team();
        defaultTeam.setName(teamName);
        defaultTeam.setSenderName(teamName);
        defaultTeam.setOwner(user);
        teamRepository.save(defaultTeam);

        TeamMember ownerMember = new TeamMember();
        ownerMember.setTeam(defaultTeam);
        ownerMember.setUser(user);
        ownerMember.setRole(TeamRole.OWNER);
        teamMemberRepository.save(ownerMember);

        auditService.log(user, "TEAM_CREATED", "TEAM", defaultTeam.getId(), Map.of("teamName", defaultTeam.getName()), ipAddress);

        String joinedTeamId = handleInvitation(user, request.inviteToken(), ipAddress);
        emailTemplateService.queueWithTemplate(
            EmailType.WELCOME,
            defaultTeam,
            user.getEmail(),
            Map.of("userName", user.getFullName())
        );

        return createAuthResponse(user, joinedTeamId != null ? joinedTeamId : defaultTeam.getId(), joinedTeamId);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password())
        );
 
        User user = userRepository.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        String joinedTeamId = handleInvitation(user, request.inviteToken(), ipAddress);

        return createAuthResponse(user, joinedTeamId, joinedTeamId);
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
        return createAuthResponse(user, null, null);
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

    private String handleInvitation(User user, String token, String ipAddress) {
        if (token == null || token.isBlank()) return null;
        return teamService.processInvitationForUser(user, token, ipAddress);
    }

    private AuthResponse createAuthResponse(User user, String preferredTeamId, String joinedTeamId) {
        revokeActiveRefreshTokens(user);
        String refreshTokenValue = UUID.randomUUID() + "." + UUID.randomUUID();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(refreshTokenValue);
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(Instant.now().plus(refreshTokenDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refreshToken);

        String activeTeamId = preferredTeamId;
        if (activeTeamId == null) {
            activeTeamId = teamMemberRepository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .findFirst()
                .map(m -> m.getTeam().getId())
                .orElse(null);
        }

        return new AuthResponse(
            jwtService.generateAccessToken(user),
            refreshTokenValue,
            toUserResponse(user),
            isGoogleConfigured(),
            activeTeamId,
            joinedTeamId
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
