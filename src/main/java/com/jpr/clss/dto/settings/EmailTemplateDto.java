package com.jpr.clss.dto.settings;

import com.jpr.clss.entity.EmailType;

public record EmailTemplateDto(
    String id,
    EmailType type,
    String subjectTemplate,
    String bodyTemplate,
    boolean active
) {}
