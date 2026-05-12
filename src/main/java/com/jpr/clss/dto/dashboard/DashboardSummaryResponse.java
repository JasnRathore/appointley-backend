package com.jpr.clss.dto.dashboard;

public record DashboardSummaryResponse(
    long upcomingMeetings,
    long recentActivity,
    long pendingInvites,
    long activeBookingLinks,
    long availabilityRules
) {
}
