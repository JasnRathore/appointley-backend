package com.jpr.clss.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpr.clss.dto.team.CreateTeamRequest;
import com.jpr.clss.dto.team.InviteDetailsResponse;
import com.jpr.clss.dto.team.TeamDetailsResponse;
import com.jpr.clss.dto.team.TeamInviteRequest;
import com.jpr.clss.dto.team.TeamInviteResponse;
import com.jpr.clss.dto.team.TeamMemberResponse;
import com.jpr.clss.dto.team.TeamSummaryResponse;
import com.jpr.clss.dto.team.UpdateMemberRoleRequest;
import com.jpr.clss.dto.team.UpdateTeamRequest;
import com.jpr.clss.service.TeamService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public TeamSummaryResponse createTeam(@Valid @RequestBody CreateTeamRequest request, HttpServletRequest httpServletRequest) {
        return teamService.createTeam(request, httpServletRequest.getRemoteAddr());
    }

    @GetMapping
    public List<TeamSummaryResponse> getAllTeams() {
        return teamService.getAllUserTeams();
    }

    @GetMapping("/current")
    public TeamDetailsResponse currentTeam() {
        return teamService.getCurrentTeamDetails();
    }

    @PatchMapping("/current")
    public TeamDetailsResponse updateTeam(@Valid @RequestBody UpdateTeamRequest request, HttpServletRequest httpServletRequest) {
        return teamService.updateTeam(request, httpServletRequest.getRemoteAddr());
    }

    @DeleteMapping("/current")
    public void deleteTeam(HttpServletRequest httpServletRequest) {
        teamService.deleteTeam(httpServletRequest.getRemoteAddr());
    }

    @GetMapping("/current/members")
    public List<TeamMemberResponse> members() {
        return teamService.getCurrentTeamMembers();
    }

    @GetMapping("/current/invites")
    public List<TeamInviteResponse> invites() {
        return teamService.getCurrentTeamInvites();
    }

    @PostMapping("/current/invites")
    public TeamInviteResponse invite(@Valid @RequestBody TeamInviteRequest request, HttpServletRequest httpServletRequest) {
        return teamService.inviteMember(request, httpServletRequest.getRemoteAddr());
    }

    @DeleteMapping("/current/invites/{inviteId}")
    public void revokeInvite(@PathVariable String inviteId, HttpServletRequest httpServletRequest) {
        teamService.revokeInvite(inviteId, httpServletRequest.getRemoteAddr());
    }

    @PostMapping("/invites/{token}/accept")
    public TeamDetailsResponse acceptInvite(@PathVariable String token, HttpServletRequest httpServletRequest) {
        return teamService.acceptInvite(token, httpServletRequest.getRemoteAddr());
    }

    @GetMapping("/invites/{token}")
    public InviteDetailsResponse inviteDetails(@PathVariable String token) {
        return teamService.getPublicInviteDetails(token);
    }

    @PatchMapping("/current/members/{memberId}/role")
    public TeamMemberResponse updateRole(
        @PathVariable String memberId,
        @Valid @RequestBody UpdateMemberRoleRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return teamService.updateMemberRole(memberId, request, httpServletRequest.getRemoteAddr());
    }

    @DeleteMapping("/current/members/{memberId}")
    public void removeMember(@PathVariable String memberId, HttpServletRequest httpServletRequest) {
        teamService.removeMember(memberId, httpServletRequest.getRemoteAddr());
    }
}
