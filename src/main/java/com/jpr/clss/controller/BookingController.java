package com.jpr.clss.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jpr.clss.dto.appointment.BookMeetingRequest;
import com.jpr.clss.dto.appointment.BookingLinkRequest;
import com.jpr.clss.dto.appointment.BookingLinkResponse;
import com.jpr.clss.dto.appointment.MeetingResponse;
import com.jpr.clss.dto.appointment.PublicBookingLinkResponse;
import com.jpr.clss.service.BookingService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/api/booking-links")
    public List<BookingLinkResponse> bookingLinks() {
        return bookingService.getMyBookingLinks();
    }

    @PostMapping("/api/booking-links")
    public BookingLinkResponse createBookingLink(@Valid @RequestBody BookingLinkRequest request, HttpServletRequest httpServletRequest) {
        return bookingService.createBookingLink(request, httpServletRequest.getRemoteAddr());
    }

    @GetMapping("/api/public/booking-links/{token}")
    public PublicBookingLinkResponse publicBookingLink(@PathVariable String token) {
        return bookingService.getPublicBookingLink(token);
    }

    @PostMapping("/api/public/booking-links/{token}/meetings")
    public MeetingResponse bookMeeting(
        @PathVariable String token,
        @Valid @RequestBody BookMeetingRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return bookingService.bookMeeting(token, request, httpServletRequest.getRemoteAddr());
    }

    @GetMapping("/api/public/meetings/{meetingId}")
    public MeetingResponse publicMeetingDetails(@PathVariable String meetingId) {
        return bookingService.getMeetingPublic(meetingId);
    }

    @PostMapping("/api/public/meetings/{meetingId}/cancel")
    public MeetingResponse publicCancelMeeting(@PathVariable String meetingId, HttpServletRequest httpServletRequest) {
        return bookingService.cancelMeetingPublic(meetingId, httpServletRequest.getRemoteAddr());
    }

    @PostMapping("/api/public/meetings/{meetingId}/reschedule")
    public MeetingResponse publicRescheduleMeeting(
        @PathVariable String meetingId,
        @Valid @RequestBody com.jpr.clss.dto.appointment.RescheduleMeetingRequest request,
        HttpServletRequest httpServletRequest
    ) {
        return bookingService.rescheduleMeetingPublic(meetingId, request, httpServletRequest.getRemoteAddr());
    }
}
