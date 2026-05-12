package com.jpr.clss.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.jpr.clss.dto.appointment.AvailabilityRuleRequest;
import com.jpr.clss.dto.appointment.AvailabilityRuleResponse;
import com.jpr.clss.dto.appointment.BlockedDateRequest;
import com.jpr.clss.dto.appointment.BlockedDateResponse;
import com.jpr.clss.service.AvailabilityService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final AvailabilityService availabilityService;

    public CalendarController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping("/availability")
    public List<AvailabilityRuleResponse> availability() {
        return availabilityService.getAvailabilityRules();
    }

    @PutMapping("/availability")
    public List<AvailabilityRuleResponse> replaceAvailability(@Valid @RequestBody List<AvailabilityRuleRequest> requests) {
        return availabilityService.replaceAvailabilityRules(requests);
    }

    @GetMapping("/blocked-dates")
    public List<BlockedDateResponse> blockedDates() {
        return availabilityService.getBlockedDates();
    }

    @PostMapping("/blocked-dates")
    public BlockedDateResponse addBlockedDate(@Valid @RequestBody BlockedDateRequest request) {
        return availabilityService.addBlockedDate(request);
    }

    @DeleteMapping("/blocked-dates/{blockedDateId}")
    public void deleteBlockedDate(@PathVariable String blockedDateId) {
        availabilityService.deleteBlockedDate(blockedDateId);
    }
}
