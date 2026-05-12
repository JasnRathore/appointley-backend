package com.jpr.clss.dto.team;

import java.util.List;

public record TeamDetailsResponse(
    TeamSummaryResponse team,
    List<TeamMemberResponse> members,
    List<TeamInviteResponse> pendingInvites
) {
}
