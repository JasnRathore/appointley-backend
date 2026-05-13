package com.jpr.clss.dto.settings;

import com.jpr.clss.entity.EmailType;

public record EmailTemplateDto(
    EmailType type,
    String displayName,
    String description,
    String subjectTemplate,
    String bodyTemplate,
    String defaultSubjectTemplate,
    String defaultBodyTemplate,
    boolean active,
    boolean customized,
    java.util.List<String> variables
) {}
