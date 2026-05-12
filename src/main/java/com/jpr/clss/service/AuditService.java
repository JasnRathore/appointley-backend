package com.jpr.clss.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.jpr.clss.dto.audit.AuditLogResponse;
import com.jpr.clss.entity.AuditLog;
import com.jpr.clss.entity.User;
import com.jpr.clss.repository.AuditLogRepository;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    public void log(User actor, String actionType, String entityType, String entityId, Map<String, Object> metadata, String ipAddress) {
        AuditLog auditLog = new AuditLog();
        auditLog.setActorId(actor == null ? null : actor.getId());
        auditLog.setActionType(actionType);
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setMetadata(toJson(metadata));
        auditLog.setIpAddress(ipAddress);
        auditLogRepository.save(auditLog);
    }

    public List<AuditLogResponse> getRecentForActor(String actorId) {
        return auditLogRepository.findTop10ByActorIdOrderByCreatedAtDesc(actorId).stream()
            .map(log -> new AuditLogResponse(
                log.getId(),
                log.getActorId(),
                log.getActionType(),
                log.getEntityType(),
                log.getEntityId(),
                log.getMetadata(),
                log.getIpAddress(),
                log.getCreatedAt()
            ))
            .toList();
    }

    private String toJson(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "{}";
        }
        return metadata.entrySet().stream()
            .map(entry -> "\"" + entry.getKey() + "\":\"" + String.valueOf(entry.getValue()) + "\"")
            .collect(Collectors.joining(",", "{", "}"));
    }
}
