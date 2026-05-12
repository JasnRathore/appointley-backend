package com.jpr.clss.dto.settings;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateEmailTemplateRequest(
    @NotBlank @Size(max = 500) String subjectTemplate,
    @NotBlank @Size(max = 5000) String bodyTemplate,
    boolean active
) {}
