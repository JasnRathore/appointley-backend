package com.jpr.clss.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpr.clss.dto.appointment.MeetingResponse;
import com.jpr.clss.service.BookingService;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final BookingService bookingService;

    public MeetingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public List<MeetingResponse> meetings() {
        return bookingService.getMyMeetings();
    }

    @PostMapping("/{meetingId}/cancel")
    public MeetingResponse cancelMeeting(@PathVariable String meetingId, HttpServletRequest httpServletRequest) {
        return bookingService.cancelMeeting(meetingId, httpServletRequest.getRemoteAddr());
    }

    @PostMapping("/{meetingId}/reschedule")
    public MeetingResponse rescheduleMeeting(
        @PathVariable String meetingId,
        @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.jpr.clss.dto.appointment.RescheduleMeetingRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return bookingService.rescheduleMeeting(meetingId, request, httpServletRequest.getRemoteAddr());
    }
}
