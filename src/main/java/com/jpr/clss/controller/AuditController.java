package com.jpr.clss.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpr.clss.dto.audit.AuditLogResponse;
import com.jpr.clss.service.AuditService;
import com.jpr.clss.service.CurrentUserService;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditController {

    private final AuditService auditService;
    private final CurrentUserService currentUserService;

    public AuditController(AuditService auditService, CurrentUserService currentUserService) {
        this.auditService = auditService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public List<AuditLogResponse> logs() {
        return auditService.getRecentForActor(currentUserService.requireCurrentUser().getId());
    }
}
