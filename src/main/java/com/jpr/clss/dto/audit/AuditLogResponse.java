package com.jpr.clss.dto.audit;

import java.time.Instant;

public record AuditLogResponse(
    String id,
    String actorId,
    String actionType,
    String entityType,
    String entityId,
    String metadata,
    String ipAddress,
    Instant createdAt
) {
}
