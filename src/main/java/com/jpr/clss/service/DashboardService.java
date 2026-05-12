package com.jpr.clss.service;

import java.time.Instant;

import org.springframework.stereotype.Service;

import com.jpr.clss.dto.dashboard.DashboardSummaryResponse;
import com.jpr.clss.entity.MeetingStatus;
import com.jpr.clss.entity.Team;
import com.jpr.clss.entity.User;
import com.jpr.clss.repository.AvailabilityRuleRepository;
import com.jpr.clss.repository.BookingLinkRepository;
import com.jpr.clss.repository.MeetingRepository;
import com.jpr.clss.repository.TeamInviteRepository;

@Service
public class DashboardService {

    private final CurrentUserService currentUserService;
    private final TeamService teamService;
    private final MeetingRepository meetingRepository;
    private final TeamInviteRepository teamInviteRepository;
    private final BookingLinkRepository bookingLinkRepository;
    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final AuditService auditService;

    public DashboardService(
        CurrentUserService currentUserService,
        TeamService teamService,
        MeetingRepository meetingRepository,
        TeamInviteRepository teamInviteRepository,
        BookingLinkRepository bookingLinkRepository,
        AvailabilityRuleRepository availabilityRuleRepository,
        AuditService auditService
    ) {
        this.currentUserService = currentUserService;
        this.teamService = teamService;
        this.meetingRepository = meetingRepository;
        this.teamInviteRepository = teamInviteRepository;
        this.bookingLinkRepository = bookingLinkRepository;
        this.availabilityRuleRepository = availabilityRuleRepository;
        this.auditService = auditService;
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        User user = currentUserService.requireCurrentUser();
        Team team = teamService.getCurrentTeamOrThrow(user);
        return new DashboardSummaryResponse(
            meetingRepository.countByOrganizerIdAndStartsAtAfterAndStatus(user.getId(), Instant.now(), MeetingStatus.SCHEDULED),
            auditService.getRecentForActor(user.getId()).size(),
            teamInviteRepository.countByTeamIdAndAcceptedAtIsNullAndRevokedAtIsNullAndExpiresAtAfter(team.getId(), Instant.now()),
            bookingLinkRepository.countByCreatorIdAndActiveTrueAndExpirationDateAfter(user.getId(), Instant.now()),
            availabilityRuleRepository.findByUserIdOrderByDayOfWeekAscStartTimeAsc(user.getId()).size()
        );
    }
}
