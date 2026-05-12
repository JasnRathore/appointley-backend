package com.jpr.clss.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpr.clss.dto.appointment.AvailabilityRuleRequest;
import com.jpr.clss.dto.appointment.AvailabilityRuleResponse;
import com.jpr.clss.dto.appointment.BlockedDateRequest;
import com.jpr.clss.dto.appointment.BlockedDateResponse;
import com.jpr.clss.dto.appointment.PublicSlotResponse;
import com.jpr.clss.entity.AvailabilityRule;
import com.jpr.clss.entity.BlockedDate;
import com.jpr.clss.entity.BookingLink;
import com.jpr.clss.entity.Meeting;
import com.jpr.clss.entity.MeetingStatus;
import com.jpr.clss.entity.User;
import com.jpr.clss.exception.ApiException;
import com.jpr.clss.repository.AvailabilityRuleRepository;
import com.jpr.clss.repository.BlockedDateRepository;
import com.jpr.clss.repository.MeetingRepository;

@Service
public class AvailabilityService {

    private final AvailabilityRuleRepository availabilityRuleRepository;
    private final BlockedDateRepository blockedDateRepository;
    private final MeetingRepository meetingRepository;
    private final CurrentUserService currentUserService;

    public AvailabilityService(
        AvailabilityRuleRepository availabilityRuleRepository,
        BlockedDateRepository blockedDateRepository,
        MeetingRepository meetingRepository,
        CurrentUserService currentUserService
    ) {
        this.availabilityRuleRepository = availabilityRuleRepository;
        this.blockedDateRepository = blockedDateRepository;
        this.meetingRepository = meetingRepository;
        this.currentUserService = currentUserService;
    }

    public List<AvailabilityRuleResponse> getAvailabilityRules() {
        User user = currentUserService.requireCurrentUser();
        return availabilityRuleRepository.findByUserIdOrderByDayOfWeekAscStartTimeAsc(user.getId()).stream()
            .map(this::toRuleResponse)
            .toList();
    }

    @Transactional
    public List<AvailabilityRuleResponse> replaceAvailabilityRules(List<AvailabilityRuleRequest> requests) {
        User user = currentUserService.requireCurrentUser();
        availabilityRuleRepository.deleteByUserId(user.getId());
        List<AvailabilityRule> rules = new ArrayList<>();
        for (AvailabilityRuleRequest request : requests) {
            if (!request.startTime().isBefore(request.endTime())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Availability start time must be before end time");
            }
            AvailabilityRule rule = new AvailabilityRule();
            rule.setUser(user);
            rule.setDayOfWeek(request.dayOfWeek());
            rule.setStartTime(request.startTime());
            rule.setEndTime(request.endTime());
            rule.setSlotDurationMinutes(request.slotDurationMinutes());
            rule.setActive(request.active());
            rules.add(rule);
        }
        availabilityRuleRepository.saveAll(rules);
        return rules.stream().map(this::toRuleResponse).toList();
    }

    public List<BlockedDateResponse> getBlockedDates() {
        User user = currentUserService.requireCurrentUser();
        return blockedDateRepository.findByUserIdOrderByStartsAtAsc(user.getId()).stream().map(this::toBlockedDateResponse).toList();
    }

    @Transactional
    public BlockedDateResponse addBlockedDate(BlockedDateRequest request) {
        if (!request.startsAt().isBefore(request.endsAt())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Blocked date range is invalid");
        }
        User user = currentUserService.requireCurrentUser();
        BlockedDate blockedDate = new BlockedDate();
        blockedDate.setUser(user);
        blockedDate.setStartsAt(request.startsAt());
        blockedDate.setEndsAt(request.endsAt());
        blockedDate.setReason(request.reason());
        blockedDateRepository.save(blockedDate);
        return toBlockedDateResponse(blockedDate);
    }

    @Transactional
    public void deleteBlockedDate(String blockedDateId) {
        User user = currentUserService.requireCurrentUser();
        BlockedDate blockedDate = blockedDateRepository.findById(blockedDateId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Blocked date not found"));
        if (!blockedDate.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Blocked date is not yours");
        }
        blockedDateRepository.delete(blockedDate);
    }

    public List<PublicSlotResponse> generatePublicSlots(BookingLink bookingLink, int daysAhead) {
        User organizer = bookingLink.getCreator();
        ZoneId zoneId = ZoneId.of(bookingLink.getTimezone());
        Instant now = Instant.now();
        Instant upperBound = now.plus(daysAhead + 1L, java.time.temporal.ChronoUnit.DAYS);
        List<AvailabilityRule> rules = availabilityRuleRepository.findByUserIdOrderByDayOfWeekAscStartTimeAsc(organizer.getId());
        List<BlockedDate> blockedDates = blockedDateRepository.findByUserIdAndEndsAtAfterAndStartsAtBefore(organizer.getId(), now, upperBound);
        List<Meeting> meetings = meetingRepository.findByOrganizerIdAndStatusAndEndsAtAfterAndStartsAtBefore(
            organizer.getId(),
            MeetingStatus.SCHEDULED,
            now,
            upperBound
        );

        List<PublicSlotResponse> slots = new ArrayList<>();
        LocalDate startDate = LocalDate.now(zoneId);
        for (int offset = 0; offset < daysAhead; offset++) {
            LocalDate date = startDate.plusDays(offset);
            for (AvailabilityRule rule : rules) {
                if (!rule.isActive() || rule.getDayOfWeek() != date.getDayOfWeek()) {
                    continue;
                }
                LocalDateTime slotStart = LocalDateTime.of(date, rule.getStartTime());
                LocalDateTime ruleEnd = LocalDateTime.of(date, rule.getEndTime());
                while (!slotStart.plusMinutes(bookingLink.getDurationMinutes()).isAfter(ruleEnd)) {
                    ZonedDateTime zonedSlotStart = slotStart.atZone(zoneId);
                    Instant slotStartInstant = zonedSlotStart.toInstant();
                    Instant slotEndInstant = zonedSlotStart.plusMinutes(bookingLink.getDurationMinutes()).toInstant();
                    if (slotStartInstant.isAfter(bookingLink.getExpirationDate()) || slotStartInstant.isBefore(now)) {
                        slotStart = slotStart.plusMinutes(rule.getSlotDurationMinutes());
                        continue;
                    }
                    if (isBlocked(slotStartInstant, slotEndInstant, blockedDates) || isMeetingConflict(slotStartInstant, slotEndInstant, meetings)) {
                        slotStart = slotStart.plusMinutes(rule.getSlotDurationMinutes());
                        continue;
                    }
                    slots.add(new PublicSlotResponse(slotStartInstant, slotEndInstant));
                    slotStart = slotStart.plusMinutes(rule.getSlotDurationMinutes());
                }
            }
        }
        slots.sort(Comparator.comparing(PublicSlotResponse::startsAt));
        return slots;
    }

    public void assertBookableSlot(BookingLink bookingLink, Instant requestedStart) {
        boolean available = generatePublicSlots(bookingLink, 14).stream()
            .anyMatch(slot -> slot.startsAt().equals(requestedStart));
        if (!available) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Requested slot is not available");
        }
    }

    private boolean isBlocked(Instant startsAt, Instant endsAt, List<BlockedDate> blockedDates) {
        return blockedDates.stream().anyMatch(blockedDate -> overlaps(startsAt, endsAt, blockedDate.getStartsAt(), blockedDate.getEndsAt()));
    }

    private boolean isMeetingConflict(Instant startsAt, Instant endsAt, List<Meeting> meetings) {
        return meetings.stream().anyMatch(meeting -> overlaps(startsAt, endsAt, meeting.getStartsAt(), meeting.getEndsAt()));
    }

    private boolean overlaps(Instant requestedStart, Instant requestedEnd, Instant existingStart, Instant existingEnd) {
        return requestedStart.isBefore(existingEnd) && requestedEnd.isAfter(existingStart);
    }

    private AvailabilityRuleResponse toRuleResponse(AvailabilityRule rule) {
        return new AvailabilityRuleResponse(
            rule.getId(),
            rule.getDayOfWeek(),
            rule.getStartTime(),
            rule.getEndTime(),
            rule.getSlotDurationMinutes(),
            rule.isActive()
        );
    }

    private BlockedDateResponse toBlockedDateResponse(BlockedDate blockedDate) {
        return new BlockedDateResponse(blockedDate.getId(), blockedDate.getStartsAt(), blockedDate.getEndsAt(), blockedDate.getReason());
    }
}
