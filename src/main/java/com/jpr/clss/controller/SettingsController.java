package com.jpr.clss.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpr.clss.dto.settings.EmailTemplateDto;
import com.jpr.clss.dto.settings.SettingsResponse;
import com.jpr.clss.dto.settings.UpdateEmailTemplateRequest;
import com.jpr.clss.dto.settings.UpdateSettingsRequest;
import com.jpr.clss.entity.EmailType;
import com.jpr.clss.service.SettingsService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsResponse settings() {
        return settingsService.getSettings();
    }

    @PutMapping
    public SettingsResponse updateSettings(@Valid @RequestBody UpdateSettingsRequest request) {
        return settingsService.updateSettings(request);
    }

    @GetMapping("/email-templates")
    public java.util.List<EmailTemplateDto> emailTemplates() {
        return settingsService.getEmailTemplates();
    }

    @PutMapping("/email-templates/{type}")
    public EmailTemplateDto updateEmailTemplate(
        @PathVariable EmailType type,
        @Valid @RequestBody UpdateEmailTemplateRequest request
    ) {
        return settingsService.updateEmailTemplate(type, request);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/email-templates/{type}")
    public void resetEmailTemplate(@PathVariable EmailType type) {
        settingsService.resetEmailTemplate(type);
    }

    @PutMapping("/password")
    public void updatePassword(@Valid @RequestBody com.jpr.clss.dto.settings.UpdatePasswordRequest request) {
        settingsService.updatePassword(request);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/account")
    public void deleteAccount() {
        settingsService.deleteAccount();
    }
}
