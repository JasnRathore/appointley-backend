package com.jpr.clss.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpr.clss.dto.auth.OAuthStatusResponse;
import com.jpr.clss.dto.settings.EmailTemplateDto;
import com.jpr.clss.dto.settings.SettingsResponse;
import com.jpr.clss.dto.settings.UpdateEmailTemplateRequest;
import com.jpr.clss.dto.settings.UpdateSettingsRequest;
import com.jpr.clss.entity.EmailType;
import com.jpr.clss.entity.Team;
import com.jpr.clss.entity.User;

@Service
public class SettingsService {

    private final CurrentUserService currentUserService;
    private final TeamService teamService;
    private final AuthService authService;
    private final EmailTemplateService emailTemplateService;

    private final com.jpr.clss.repository.AuditLogRepository auditLogRepository;
    private final com.jpr.clss.repository.NotificationRepository notificationRepository;
    private final com.jpr.clss.repository.RefreshTokenRepository refreshTokenRepository;
    private final com.jpr.clss.repository.AvailabilityRuleRepository availabilityRuleRepository;
    private final com.jpr.clss.repository.BlockedDateRepository blockedDateRepository;
    private final com.jpr.clss.repository.MeetingRepository meetingRepository;
    private final com.jpr.clss.repository.BookingLinkRepository bookingLinkRepository;
    private final com.jpr.clss.repository.TeamRepository teamRepository;
    private final com.jpr.clss.repository.TeamMemberRepository teamMemberRepository;
    private final com.jpr.clss.repository.UserRepository userRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public SettingsService(
        CurrentUserService currentUserService, 
        TeamService teamService, 
        AuthService authService,
        EmailTemplateService emailTemplateService,
        com.jpr.clss.repository.AuditLogRepository auditLogRepository,
        com.jpr.clss.repository.NotificationRepository notificationRepository,
        com.jpr.clss.repository.RefreshTokenRepository refreshTokenRepository,
        com.jpr.clss.repository.AvailabilityRuleRepository availabilityRuleRepository,
        com.jpr.clss.repository.BlockedDateRepository blockedDateRepository,
        com.jpr.clss.repository.MeetingRepository meetingRepository,
        com.jpr.clss.repository.BookingLinkRepository bookingLinkRepository,
        com.jpr.clss.repository.TeamRepository teamRepository,
        com.jpr.clss.repository.TeamMemberRepository teamMemberRepository,
        com.jpr.clss.repository.UserRepository userRepository,
        org.springframework.security.crypto.password.PasswordEncoder passwordEncoder
    ) {
        this.currentUserService = currentUserService;
        this.teamService = teamService;
        this.authService = authService;
        this.emailTemplateService = emailTemplateService;
        this.auditLogRepository = auditLogRepository;
        this.notificationRepository = notificationRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.availabilityRuleRepository = availabilityRuleRepository;
        this.blockedDateRepository = blockedDateRepository;
        this.meetingRepository = meetingRepository;
        this.bookingLinkRepository = bookingLinkRepository;
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public SettingsResponse getSettings() {
        User user = currentUserService.requireCurrentUser();
        Team team = teamService.getCurrentTeamOrThrow(user);
        OAuthStatusResponse oauthStatus = authService.oauthStatus();
        return new SettingsResponse(
            user.getFullName(), 
            user.getEmail(), 
            team.getName(), 
            team.getSenderName(), 
            oauthStatus.googleConfigured(),
            user.isEmailOnBooking(),
            user.isInAppOnBooking(),
            user.isWeeklyDigest(),
            user.isMarketingEmails()
        );
    }

    @Transactional
    public SettingsResponse updateSettings(UpdateSettingsRequest request) {
        User user = currentUserService.requireCurrentUser();
        Team team = teamService.getCurrentTeamOrThrow(user);
        
        user.setFullName(request.fullName().trim());
        user.setEmailOnBooking(request.emailOnBooking());
        user.setInAppOnBooking(request.inAppOnBooking());
        user.setWeeklyDigest(request.weeklyDigest());
        user.setMarketingEmails(request.marketingEmails());
        
        team.setName(request.teamName().trim());
        team.setSenderName(request.senderName().trim());
        
        OAuthStatusResponse oauthStatus = authService.oauthStatus();
        return new SettingsResponse(
            user.getFullName(), 
            user.getEmail(), 
            team.getName(), 
            team.getSenderName(), 
            oauthStatus.googleConfigured(),
            user.isEmailOnBooking(),
            user.isInAppOnBooking(),
            user.isWeeklyDigest(),
            user.isMarketingEmails()
        );
    }

    @Transactional(readOnly = true)
    public java.util.List<EmailTemplateDto> getEmailTemplates() {
        User user = currentUserService.requireCurrentUser();
        Team team = teamService.getCurrentTeamOrThrow(user);
        return emailTemplateService.getTemplatesForTeam(team);
    }

    @Transactional
    public EmailTemplateDto updateEmailTemplate(EmailType type, UpdateEmailTemplateRequest request) {
        User user = currentUserService.requireCurrentUser();
        Team team = teamService.getCurrentTeamOrThrow(user);
        return emailTemplateService.updateTemplate(team, type, request);
    }

    @Transactional
    public void resetEmailTemplate(EmailType type) {
        User user = currentUserService.requireCurrentUser();
        Team team = teamService.getCurrentTeamOrThrow(user);
        emailTemplateService.resetTemplate(team, type);
    }

    @Transactional
    public void updatePassword(com.jpr.clss.dto.settings.UpdatePasswordRequest request) {
        User user = currentUserService.requireCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new com.jpr.clss.exception.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid current password");
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        authService.revokeActiveRefreshTokens(user);
    }

    @Transactional
    public void deleteAccount() {
        User user = currentUserService.requireCurrentUser();
        String userId = user.getId();

        auditLogRepository.deleteByActorId(userId);
        notificationRepository.deleteByUserId(userId);
        refreshTokenRepository.deleteByUserId(userId);
        availabilityRuleRepository.deleteByUserId(userId);
        blockedDateRepository.deleteByUserId(userId);
        meetingRepository.deleteByOrganizerId(userId);
        bookingLinkRepository.deleteByCreatorId(userId);

        java.util.List<Team> ownedTeams = teamRepository.findByOwnerId(userId);
        for (Team team : ownedTeams) {
            java.util.List<com.jpr.clss.entity.TeamMember> members = teamMemberRepository.findByTeamIdOrderByCreatedAtAsc(team.getId());
            com.jpr.clss.entity.TeamMember newOwner = members.stream()
                .filter(m -> !m.getUser().getId().equals(userId) && m.getRole() == com.jpr.clss.entity.TeamRole.OWNER)
                .findFirst()
                .orElse(null);
            
            if (newOwner != null) {
                team.setOwner(newOwner.getUser());
            } else {
                teamRepository.delete(team);
            }
        }
        
        java.util.List<com.jpr.clss.entity.TeamMember> userMemberships = teamMemberRepository.findByUserIdOrderByCreatedAtAsc(userId);
        teamMemberRepository.deleteAll(userMemberships);

        userRepository.delete(user);
    }
}
