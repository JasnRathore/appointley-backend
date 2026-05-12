package com.jpr.clss.dto.team;

public record TeamSummaryResponse(
    String id,
    String name,
    String ownerId,
    String senderName,
    long memberCount
) {
}
