package com.jpr.clss.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpr.clss.dto.settings.SettingsResponse;
import com.jpr.clss.dto.settings.UpdateSettingsRequest;
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

    @PutMapping("/password")
    public void updatePassword(@Valid @RequestBody com.jpr.clss.dto.settings.UpdatePasswordRequest request) {
        settingsService.updatePassword(request);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/account")
    public void deleteAccount() {
        settingsService.deleteAccount();
    }
}
