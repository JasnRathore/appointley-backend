package com.jpr.clss.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpr.clss.dto.team.CreateTeamRequest;
import com.jpr.clss.dto.team.InviteDetailsResponse;
import com.jpr.clss.dto.team.TeamDetailsResponse;
import com.jpr.clss.dto.team.TeamInviteRequest;
import com.jpr.clss.dto.team.TeamInviteResponse;
import com.jpr.clss.dto.team.TeamMemberResponse;
import com.jpr.clss.dto.team.TeamSummaryResponse;
import com.jpr.clss.dto.team.UpdateMemberRoleRequest;
import com.jpr.clss.dto.team.UpdateTeamRequest;
import com.jpr.clss.entity.EmailType;
import com.jpr.clss.entity.Team;
import com.jpr.clss.entity.TeamInvite;
import com.jpr.clss.entity.TeamMember;
import com.jpr.clss.entity.TeamRole;
import com.jpr.clss.entity.User;
import com.jpr.clss.exception.ApiException;
import com.jpr.clss.repository.TeamInviteRepository;
import com.jpr.clss.repository.TeamMemberRepository;
import com.jpr.clss.repository.TeamRepository;
import com.jpr.clss.repository.UserRepository;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final CurrentUserService currentUserService;
    private final AuditService auditService;
    private final EmailTemplateService emailTemplateService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final jakarta.servlet.http.HttpServletRequest request;
    private final String frontendBaseUrl;

    public TeamService(
        TeamRepository teamRepository,
        TeamMemberRepository teamMemberRepository,
        TeamInviteRepository teamInviteRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        AuditService auditService,
        EmailTemplateService emailTemplateService,
        NotificationService notificationService,
        jakarta.servlet.http.HttpServletRequest request,
        @org.springframework.beans.factory.annotation.Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamInviteRepository = teamInviteRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.auditService = auditService;
        this.emailTemplateService = emailTemplateService;
        this.notificationService = notificationService;
        this.request = request;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional
    public TeamSummaryResponse createTeam(CreateTeamRequest request, String ipAddress) {
        User user = currentUserService.requireCurrentUser();
        Team team = new Team();
        team.setName(request.name().trim());
        team.setSenderName(request.name().trim());
        team.setOwner(user);
        teamRepository.save(team);

        TeamMember member = new TeamMember();
        member.setTeam(team);
        member.setUser(user);
        member.setRole(TeamRole.OWNER);
        teamMemberRepository.save(member);

        auditService.log(user, "TEAM_CREATED", "TEAM", team.getId(), Map.of("name", team.getName()), ipAddress);
        return toSummary(team);
    }

    @Transactional(readOnly = true)
    public List<TeamSummaryResponse> getAllUserTeams() {
        User user = currentUserService.requireCurrentUser();
        return teamMemberRepository.findByUserIdOrderByCreatedAtAsc(user.getId())
            .stream()
            .map(member -> toSummary(member.getTeam()))
            .toList();
    }

    @Transactional(readOnly = true)
    public TeamDetailsResponse getCurrentTeamDetails() {
        Team team = getCurrentTeamOrThrow(currentUserService.requireCurrentUser());
        return toTeamDetailsResponse(team);
    }
 
    private TeamDetailsResponse toTeamDetailsResponse(Team team) {
        return new TeamDetailsResponse(
            toSummary(team),
            teamMemberRepository.findByTeamIdOrderByCreatedAtAsc(team.getId()).stream().map(this::toMemberResponse).toList(),
            teamInviteRepository.findByTeamIdAndAcceptedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(team.getId()).stream().map(this::toInviteResponse).toList()
        );
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getCurrentTeamMembers() {
        Team team = getCurrentTeamOrThrow(currentUserService.requireCurrentUser());
        return teamMemberRepository.findByTeamIdOrderByCreatedAtAsc(team.getId()).stream().map(this::toMemberResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<TeamInviteResponse> getCurrentTeamInvites() {
        Team team = getCurrentTeamOrThrow(currentUserService.requireCurrentUser());
        return teamInviteRepository.findByTeamIdAndAcceptedAtIsNullAndRevokedAtIsNullOrderByCreatedAtDesc(team.getId()).stream().map(this::toInviteResponse).toList();
    }

    @Transactional
    public TeamInviteResponse inviteMember(TeamInviteRequest request, String ipAddress) {
        User actor = currentUserService.requireCurrentUser();
        TeamMember actorMembership = getCurrentMembershipOrThrow(actor);
        assertInvitePermission(actorMembership.getRole());

        TeamInvite invite = new TeamInvite();
        invite.setToken(UUID.randomUUID().toString());
        invite.setEmail(request.email().trim().toLowerCase());
        invite.setRole(request.role());
        invite.setTeam(actorMembership.getTeam());
        invite.setInvitedBy(actor);
        invite.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        teamInviteRepository.save(invite);

        auditService.log(actor, "MEMBER_INVITED", "TEAM_INVITE", invite.getId(), Map.of("email", invite.getEmail(), "role", invite.getRole().name()), ipAddress);
        
        String inviteLink = frontendBaseUrl + "/invite/" + invite.getToken();
        
        emailTemplateService.queueWithTemplate(
            EmailType.TEAM_INVITE,
            actorMembership.getTeam(),
            invite.getEmail(),
            Map.of(
                "inviteeEmail", invite.getEmail(),
                "inviterName", actor.getFullName(),
                "role", invite.getRole().name(),
                "inviteToken", invite.getToken(),
                "inviteLink", inviteLink
            )
        );

        // If user already exists, send in-app notification
        userRepository.findByEmailIgnoreCase(invite.getEmail()).ifPresent(invitedUser -> {
            notificationService.createNotification(
                invitedUser,
                "Team Invitation",
                actor.getFullName() + " invited you to join " + actorMembership.getTeam().getName(),
                "INVITATION",
                "/invite/" + invite.getToken()
            );
        });

        return toInviteResponse(invite);
    }

    @Transactional
    public TeamDetailsResponse updateTeam(UpdateTeamRequest request, String ipAddress) {
        User user = currentUserService.requireCurrentUser();
        TeamMember membership = getCurrentMembershipOrThrow(user);
        
        if (membership.getRole() != TeamRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the team owner can rename the team");
        }

        Team team = membership.getTeam();
        String oldName = team.getName();
        team.setName(request.name());
        teamRepository.save(team);

        auditService.log(user, "TEAM_RENAMED", "TEAM", team.getId(), Map.of("oldName", oldName, "newName", request.name()), ipAddress);

        return toTeamDetailsResponse(team);
    }

    @Transactional
    public void deleteTeam(String ipAddress) {
        User user = currentUserService.requireCurrentUser();
        TeamMember membership = getCurrentMembershipOrThrow(user);

        if (membership.getRole() != TeamRole.OWNER) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only the team owner can delete the team");
        }

        Team team = membership.getTeam();
        
        auditService.log(user, "TEAM_DELETED", "TEAM", team.getId(), Map.of("name", team.getName()), ipAddress);
        
        teamInviteRepository.deleteByTeamId(team.getId());
        teamMemberRepository.deleteByTeamId(team.getId());
        teamRepository.delete(team);
    }

    @Transactional
    public void revokeInvite(String inviteId, String ipAddress) {
        User actor = currentUserService.requireCurrentUser();
        TeamMember actorMembership = getCurrentMembershipOrThrow(actor);
        assertInvitePermission(actorMembership.getRole());

        TeamInvite invite = teamInviteRepository.findById(inviteId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));

        if (!invite.getTeam().getId().equals(actorMembership.getTeam().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Invite is not from your team");
        }

        invite.setRevokedAt(Instant.now());
        auditService.log(actor, "INVITE_REVOKED", "TEAM_INVITE", inviteId, Map.of("email", invite.getEmail()), ipAddress);
    }

    @Transactional
    public TeamDetailsResponse acceptInvite(String token, String ipAddress) {
        User user = currentUserService.requireCurrentUser();
        TeamInvite invite = teamInviteRepository.findByToken(token)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));

        processAcceptance(user, invite, ipAddress);
        return toTeamDetailsResponse(invite.getTeam());
    }

    @Transactional(readOnly = true)
    public InviteDetailsResponse getPublicInviteDetails(String token) {
        TeamInvite invite = teamInviteRepository.findByToken(token)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));

        boolean isExpired = invite.getExpiresAt().isBefore(Instant.now()) || invite.getAcceptedAt() != null || invite.getRevokedAt() != null;
        boolean exists = userRepository.existsByEmailIgnoreCase(invite.getEmail());

        return new InviteDetailsResponse(
            invite.getTeam().getName(),
            invite.getInvitedBy().getFullName(),
            invite.getRole(),
            invite.getEmail(),
            exists,
            invite.getExpiresAt().isBefore(Instant.now()),
            invite.getAcceptedAt() != null,
            invite.getRevokedAt() != null
        );
    }

    @Transactional
    public String processInvitationForUser(User user, String token, String ipAddress) {
        if (token == null || token.isBlank()) return null;
        
        TeamInvite invite = teamInviteRepository.findByToken(token)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Invite not found"));
            
        if (invite.getAcceptedAt() != null) {
            return invite.getTeam().getId();
        }

        if (invite.getExpiresAt().isBefore(Instant.now()) || invite.getRevokedAt() != null) {
            return null;
        }

        return processAcceptance(user, invite, ipAddress);
    }

    private String processAcceptance(User user, TeamInvite invite, String ipAddress) {
        if (invite.getRevokedAt() != null || invite.getAcceptedAt() != null || invite.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invite is no longer valid");
        }
        if (teamMemberRepository.findByTeamIdAndUserId(invite.getTeam().getId(), user.getId()).isPresent()) {
            // Already a member, just mark invite as accepted if it's not already
            if (invite.getAcceptedAt() == null) {
                invite.setAcceptedAt(Instant.now());
                teamInviteRepository.save(invite);
            }
            return invite.getTeam().getId();
        }

        TeamMember member = new TeamMember();
        member.setTeam(invite.getTeam());
        member.setUser(user);
        member.setRole(invite.getRole());
        teamMemberRepository.save(member);

        invite.setAcceptedAt(Instant.now());
        auditService.log(user, "MEMBER_JOINED", "TEAM", invite.getTeam().getId(), Map.of("role", invite.getRole().name()), ipAddress);
        
        notificationService.createNotification(
            invite.getInvitedBy(),
            "Member Joined",
            user.getFullName() + " has joined the team " + invite.getTeam().getName(),
            "SYSTEM",
            "/team"
        );

        return invite.getTeam().getId();
    }

    @Transactional
    public TeamMemberResponse updateMemberRole(String memberId, UpdateMemberRoleRequest request, String ipAddress) {
        User actor = currentUserService.requireCurrentUser();
        TeamMember actorMembership = getCurrentMembershipOrThrow(actor);
        assertAdminPermission(actorMembership.getRole());
        if (request.role() == TeamRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Direct owner reassignment is not supported");
        }

        TeamMember member = teamMemberRepository.findById(memberId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team member not found"));

        if (!member.getTeam().getId().equals(actorMembership.getTeam().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Member is not in your team");
        }
        if (member.getRole() == TeamRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Owner role cannot be changed here");
        }

        member.setRole(request.role());
        auditService.log(actor, "ROLE_UPDATED", "TEAM_MEMBER", member.getId(), Map.of("role", request.role().name()), ipAddress);
        emailTemplateService.queueWithTemplate(
            EmailType.ROLE_UPDATED,
            member.getTeam(),
            member.getUser().getEmail(),
            Map.of(
                "memberName", member.getUser().getFullName(),
                "role", request.role().name()
            )
        );
        
        notificationService.createNotification(
            member.getUser(),
            "Role Updated",
            "Your role in " + member.getTeam().getName() + " has been updated to " + request.role().name(),
            "SYSTEM",
            "/settings"
        );
        
        return toMemberResponse(member);
    }

    @Transactional
    public void removeMember(String memberId, String ipAddress) {
        User actor = currentUserService.requireCurrentUser();
        TeamMember actorMembership = getCurrentMembershipOrThrow(actor);
        assertAdminPermission(actorMembership.getRole());

        TeamMember member = teamMemberRepository.findById(memberId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Team member not found"));

        if (!member.getTeam().getId().equals(actorMembership.getTeam().getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Member is not in your team");
        }
        if (member.getRole() == TeamRole.OWNER) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Owner cannot be removed");
        }

        teamMemberRepository.delete(member);
        auditService.log(actor, "MEMBER_REMOVED", "TEAM_MEMBER", memberId, Map.of("userId", member.getUser().getId()), ipAddress);
    }

    public Team getCurrentTeamOrThrow(User user) {
        return getCurrentMembershipOrThrow(user).getTeam();
    }

    public TeamMember getCurrentMembershipOrThrow(User user) {
        String teamId = request.getHeader("X-Team-ID");
        if (teamId != null && !teamId.isBlank()) {
            return teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "You are not a member of this team"));
        }

        return teamMemberRepository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream().findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No team membership found"));
    }

    private void assertInvitePermission(TeamRole role) {
        if (!(role == TeamRole.OWNER || role == TeamRole.ADMIN || role == TeamRole.MANAGER)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to invite members");
        }
    }

    private void assertAdminPermission(TeamRole role) {
        if (!(role == TeamRole.OWNER || role == TeamRole.ADMIN)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You do not have permission to manage members");
        }
    }

    private TeamSummaryResponse toSummary(Team team) {
        return new TeamSummaryResponse(
            team.getId(),
            team.getName(),
            team.getOwner().getId(),
            team.getSenderName(),
            teamMemberRepository.countByTeamId(team.getId())
        );
    }

    private TeamMemberResponse toMemberResponse(TeamMember member) {
        return new TeamMemberResponse(
            member.getId(),
            member.getUser().getId(),
            member.getUser().getFullName(),
            member.getUser().getEmail(),
            member.getRole(),
            member.getCreatedAt()
        );
    }

    private TeamInviteResponse toInviteResponse(TeamInvite invite) {
        return new TeamInviteResponse(
            invite.getId(),
            invite.getToken(),
            invite.getEmail(),
            invite.getRole(),
            invite.getExpiresAt(),
            invite.getAcceptedAt() != null,
            invite.getRevokedAt() != null
        );
    }
}
