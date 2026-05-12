package com.jpr.clss.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.jpr.clss.entity.AuditLog;

public interface AuditLogRepository extends JpaRepository<AuditLog, String> {
    List<AuditLog> findTop10ByActorIdOrderByCreatedAtDesc(String actorId);
    void deleteByActorId(String actorId);
}
