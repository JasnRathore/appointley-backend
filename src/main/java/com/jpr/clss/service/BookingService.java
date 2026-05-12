package com.jpr.clss.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jpr.clss.dto.appointment.BookMeetingRequest;
import com.jpr.clss.dto.appointment.BookingLinkRequest;
import com.jpr.clss.dto.appointment.BookingLinkResponse;
import com.jpr.clss.dto.appointment.MeetingResponse;
import com.jpr.clss.dto.appointment.PublicBookingLinkResponse;
import com.jpr.clss.dto.appointment.RescheduleMeetingRequest;
import com.jpr.clss.entity.BookingLink;
import com.jpr.clss.entity.EmailType;
import com.jpr.clss.entity.Meeting;
import com.jpr.clss.entity.MeetingStatus;
import com.jpr.clss.entity.Team;
import com.jpr.clss.entity.User;
import com.jpr.clss.exception.ApiException;
import com.jpr.clss.repository.BookingLinkRepository;
import com.jpr.clss.repository.MeetingRepository;

@Service
public class BookingService {

    private final BookingLinkRepository bookingLinkRepository;
    private final MeetingRepository meetingRepository;
    private final CurrentUserService currentUserService;
    private final TeamService teamService;
    private final AvailabilityService availabilityService;
    private final AuditService auditService;
    private final EmailTemplateService emailTemplateService;
    private final NotificationService notificationService;
    private final String frontendBaseUrl;

    public BookingService(
        BookingLinkRepository bookingLinkRepository,
        MeetingRepository meetingRepository,
        CurrentUserService currentUserService,
        TeamService teamService,
        AvailabilityService availabilityService,
        AuditService auditService,
        EmailTemplateService emailTemplateService,
        NotificationService notificationService,
        @Value("${app.frontend.base-url}") String frontendBaseUrl
    ) {
        this.bookingLinkRepository = bookingLinkRepository;
        this.meetingRepository = meetingRepository;
        this.currentUserService = currentUserService;
        this.teamService = teamService;
        this.availabilityService = availabilityService;
        this.auditService = auditService;
        this.emailTemplateService = emailTemplateService;
        this.notificationService = notificationService;
        this.frontendBaseUrl = frontendBaseUrl;
    }

    @Transactional(readOnly = true)
    public List<BookingLinkResponse> getMyBookingLinks() {
        User user = currentUserService.requireCurrentUser();
        return bookingLinkRepository.findByCreatorIdOrderByCreatedAtDesc(user.getId()).stream()
            .map(this::toBookingLinkResponse)
            .toList();
    }

    @Transactional
    public BookingLinkResponse createBookingLink(BookingLinkRequest request, String ipAddress) {
        User user = currentUserService.requireCurrentUser();
        Team team = teamService.getCurrentTeamOrThrow(user);
        BookingLink bookingLink = new BookingLink();
        bookingLink.setToken(UUID.randomUUID().toString().replace("-", ""));
        bookingLink.setCreator(user);
        bookingLink.setTeam(team);
        bookingLink.setTitle(request.title().trim());
        bookingLink.setDescription(request.description());
        bookingLink.setExpirationDate(Instant.now().plus(request.expirationDays() == null ? 10 : request.expirationDays(), ChronoUnit.DAYS));
        bookingLink.setDurationMinutes(request.durationMinutes() == null ? 30 : request.durationMinutes());
        bookingLink.setTimezone(request.timezone().trim());
        bookingLink.setRecipientEmail(request.recipientEmail());
        bookingLink.setOneTimeUse(request.oneTimeUse());
        bookingLinkRepository.save(bookingLink);

        String bookingUrl = frontendBaseUrl + "/book/" + bookingLink.getToken();
        auditService.log(user, "BOOKING_LINK_CREATED", "BOOKING_LINK", bookingLink.getId(), Map.of("token", bookingLink.getToken()), ipAddress);
        
        String targetEmail = bookingLink.getRecipientEmail() != null ? bookingLink.getRecipientEmail() : user.getEmail();
        String emailMessage = bookingLink.getRecipientEmail() != null 
            ? "You've been invited to book a meeting with " + user.getFullName() + ". Use this link: " + bookingUrl 
            : "Share this link with clients: " + bookingUrl;

        emailTemplateService.queueWithTemplate(
            EmailType.BOOKING_LINK,
            team,
            targetEmail,
            Map.of("organizerName", user.getFullName(), "bookingUrl", bookingUrl, "recipientEmail", bookingLink.getRecipientEmail() != null ? bookingLink.getRecipientEmail() : ""),
            bookingLink.getRecipientEmail() != null ? "Meeting Invitation" : "Your booking link is ready",
            emailMessage
        );
        return toBookingLinkResponse(bookingLink);
    }

    @Transactional(readOnly = true)
    public PublicBookingLinkResponse getPublicBookingLink(String token) {
        BookingLink bookingLink = findActiveLinkByToken(token);
        return new PublicBookingLinkResponse(
            bookingLink.getTitle(),
            bookingLink.getDescription(),
            bookingLink.getTimezone(),
            bookingLink.getDurationMinutes(),
            bookingLink.getExpirationDate(),
            availabilityService.generatePublicSlots(bookingLink, 14),
            bookingLink.getRecipientEmail()
        );
    }

    @Transactional
    public MeetingResponse bookMeeting(String token, BookMeetingRequest request, String ipAddress) {
        BookingLink bookingLink = findActiveLinkByToken(token);
        
        // If it's a targeted link, ensure the email matches if specified
        if (bookingLink.getRecipientEmail() != null && !bookingLink.getRecipientEmail().equalsIgnoreCase(request.clientEmail().trim())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This link is reserved for another recipient.");
        }

        availabilityService.assertBookableSlot(bookingLink, request.startsAt());

        Meeting meeting = new Meeting();
        meeting.setBookingLink(bookingLink);
        meeting.setOrganizer(bookingLink.getCreator());
        meeting.setClientName(request.clientName().trim());
        meeting.setClientEmail(request.clientEmail().trim().toLowerCase());
        meeting.setStartsAt(request.startsAt());
        meeting.setEndsAt(request.startsAt().plus(bookingLink.getDurationMinutes(), ChronoUnit.MINUTES));
        meeting.setTimezone(bookingLink.getTimezone());
        meeting.setNotes(request.notes());
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meetingRepository.save(meeting);

        if (bookingLink.isOneTimeUse()) {
            bookingLink.setActive(false);
            bookingLinkRepository.save(bookingLink);
        }

        auditService.log(bookingLink.getCreator(), "MEETING_BOOKED", "MEETING", meeting.getId(), Map.of("clientEmail", meeting.getClientEmail()), ipAddress);
        
        // In-app notification for the organizer
        notificationService.createNotification(
            bookingLink.getCreator(),
            "New meeting booked",
            meeting.getClientName() + " booked a " + bookingLink.getDurationMinutes() + "-min meeting for " + meeting.getStartsAt(),
            "BOOKING",
            "/meetings"
        );

        // Email notifications respecting user preferences
        Map<String, String> vars = Map.of(
            "clientName", meeting.getClientName(),
            "clientEmail", meeting.getClientEmail(),
            "meetingTime", meeting.getStartsAt().toString(),
            "organizerName", bookingLink.getCreator().getFullName(),
            "duration", String.valueOf(bookingLink.getDurationMinutes())
        );

        if (bookingLink.getCreator().isEmailOnBooking()) {
            emailTemplateService.queueWithTemplate(
                EmailType.MEETING_BOOKED_ORGANIZER,
                bookingLink.getTeam(),
                bookingLink.getCreator().getEmail(),
                vars,
                "New client meeting booked",
                "A new meeting was booked by {{clientName}} for {{meetingTime}}"
            );
        }
        emailTemplateService.queueWithTemplate(
            EmailType.MEETING_BOOKED_CLIENT,
            bookingLink.getTeam(),
            meeting.getClientEmail(),
            vars,
            "Booking confirmed",
            "Your meeting is confirmed for {{meetingTime}}"
        );
        return toMeetingResponse(meeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMyMeetings() {
        User user = currentUserService.requireCurrentUser();
        return meetingRepository.findByOrganizerIdOrderByStartsAtAsc(user.getId()).stream()
            .map(this::toMeetingResponse)
            .toList();
    }

    @Transactional
    public MeetingResponse cancelMeeting(String meetingId, String ipAddress) {
        User user = currentUserService.requireCurrentUser();
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Meeting not found"));
        if (!meeting.getOrganizer().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Meeting is not yours");
        }
        meeting.setStatus(MeetingStatus.CANCELLED);
        meeting.setCancelledAt(Instant.now());
        auditService.log(user, "BOOKING_CANCELLED", "MEETING", meeting.getId(), Map.of("clientEmail", meeting.getClientEmail()), ipAddress);
        
        notificationService.createNotification(
            user,
            "Meeting Canceled",
            "Your meeting with " + meeting.getClientName() + " on " + meeting.getStartsAt() + " has been canceled.",
            "ALERT",
            "/meetings"
        );
        
        return toMeetingResponse(meeting);
    }

    @Transactional
    public MeetingResponse rescheduleMeeting(String meetingId, RescheduleMeetingRequest request, String ipAddress) {
        User user = currentUserService.requireCurrentUser();
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Meeting not found"));
        
        if (!meeting.getOrganizer().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Meeting is not yours");
        }

        BookingLink bookingLink = meeting.getBookingLink();
        availabilityService.assertBookableSlot(bookingLink, request.newStartsAt());

        Instant oldStart = meeting.getStartsAt();
        meeting.setStartsAt(request.newStartsAt());
        meeting.setEndsAt(request.newStartsAt().plus(bookingLink.getDurationMinutes(), ChronoUnit.MINUTES));
        meeting.setStatus(MeetingStatus.SCHEDULED); // Re-schedule if it was cancelled/pushed
        meetingRepository.save(meeting);

        auditService.log(user, "MEETING_RESCHEDULED", "MEETING", meeting.getId(), 
            Map.of("oldStart", oldStart.toString(), "newStart", meeting.getStartsAt().toString()), ipAddress);
        
        notificationService.createNotification(
            user,
            "Meeting Rescheduled",
            "Meeting with " + meeting.getClientName() + " moved from " + oldStart + " to " + meeting.getStartsAt(),
            "BOOKING",
            "/meetings"
        );

        Map<String, String> vars = Map.of(
            "clientName", meeting.getClientName(),
            "clientEmail", meeting.getClientEmail(),
            "meetingTime", meeting.getStartsAt().toString(),
            "oldMeetingTime", oldStart.toString(),
            "organizerName", meeting.getOrganizer().getFullName()
        );

        emailTemplateService.queueWithTemplate(
            EmailType.MEETING_RESCHEDULED_CLIENT,
            bookingLink.getTeam(),
            meeting.getClientEmail(),
            vars,
            "Meeting Rescheduled",
            "Your meeting has been rescheduled from {{oldMeetingTime}} to {{meetingTime}}"
        );

        return toMeetingResponse(meeting);
    }

    private BookingLink findActiveLinkByToken(String token) {
        BookingLink bookingLink = bookingLinkRepository.findByToken(token)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking link not found"));
        if (!bookingLink.isActive() || bookingLink.getExpirationDate().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Booking link has expired or is no longer active");
        }
        return bookingLink;
    }

    private BookingLinkResponse toBookingLinkResponse(BookingLink bookingLink) {
        return new BookingLinkResponse(
            bookingLink.getId(),
            bookingLink.getToken(),
            bookingLink.getTitle(),
            bookingLink.getDescription(),
            bookingLink.getExpirationDate(),
            bookingLink.isActive(),
            bookingLink.getDurationMinutes(),
            bookingLink.getTimezone(),
            frontendBaseUrl + "/book/" + bookingLink.getToken(),
            bookingLink.getRecipientEmail(),
            bookingLink.isOneTimeUse()
        );
    }

    private MeetingResponse toMeetingResponse(Meeting meeting) {
        return new MeetingResponse(
            meeting.getId(),
            meeting.getBookingLink().getTitle(),
            meeting.getBookingLink().getToken(),
            meeting.getClientName(),
            meeting.getClientEmail(),
            meeting.getStartsAt(),
            meeting.getEndsAt(),
            meeting.getStatus(),
            meeting.getTimezone(),
            meeting.getNotes()
        );
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeetingPublic(String meetingId) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Meeting not found"));
        return toMeetingResponse(meeting);
    }

    @Transactional
    public MeetingResponse cancelMeetingPublic(String meetingId, String ipAddress) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Meeting not found"));
        
        if (meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Meeting is already cancelled");
        }

        meeting.setStatus(MeetingStatus.CANCELLED);
        meeting.setCancelledAt(Instant.now());
        meetingRepository.save(meeting);

        auditService.log(null, "MEETING_CANCELLED_BY_CLIENT", "MEETING", meeting.getId(), Map.of("clientEmail", meeting.getClientEmail()), ipAddress);
        
        notificationService.createNotification(
            meeting.getOrganizer(),
            "Meeting Canceled by Client",
            meeting.getClientName() + " has canceled the meeting on " + meeting.getStartsAt(),
            "ALERT",
            "/meetings"
        );

        Map<String, String> vars = Map.of(
            "clientName", meeting.getClientName(),
            "clientEmail", meeting.getClientEmail(),
            "meetingTime", meeting.getStartsAt().toString(),
            "organizerName", meeting.getOrganizer().getFullName()
        );

        emailTemplateService.queueWithTemplate(
            EmailType.MEETING_CANCELLED_ORGANIZER,
            meeting.getBookingLink().getTeam(),
            meeting.getOrganizer().getEmail(),
            vars,
            "Meeting Canceled by Client",
            "{{clientName}} has canceled their meeting scheduled for {{meetingTime}}"
        );

        return toMeetingResponse(meeting);
    }

    @Transactional
    public MeetingResponse rescheduleMeetingPublic(String meetingId, RescheduleMeetingRequest request, String ipAddress) {
        Meeting meeting = meetingRepository.findById(meetingId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Meeting not found"));

        BookingLink bookingLink = meeting.getBookingLink();
        availabilityService.assertBookableSlot(bookingLink, request.newStartsAt());

        Instant oldStart = meeting.getStartsAt();
        meeting.setStartsAt(request.newStartsAt());
        meeting.setEndsAt(request.newStartsAt().plus(bookingLink.getDurationMinutes(), ChronoUnit.MINUTES));
        meeting.setStatus(MeetingStatus.SCHEDULED);
        meetingRepository.save(meeting);

        auditService.log(null, "MEETING_RESCHEDULED_BY_CLIENT", "MEETING", meeting.getId(), 
            Map.of("oldStart", oldStart.toString(), "newStart", meeting.getStartsAt().toString()), ipAddress);
        
        notificationService.createNotification(
            meeting.getOrganizer(),
            "Meeting Rescheduled by Client",
            meeting.getClientName() + " moved the meeting from " + oldStart + " to " + meeting.getStartsAt(),
            "BOOKING",
            "/meetings"
        );

        Map<String, String> vars = Map.of(
            "clientName", meeting.getClientName(),
            "clientEmail", meeting.getClientEmail(),
            "meetingTime", meeting.getStartsAt().toString(),
            "oldMeetingTime", oldStart.toString(),
            "organizerName", meeting.getOrganizer().getFullName()
        );

        emailTemplateService.queueWithTemplate(
            EmailType.MEETING_RESCHEDULED_ORGANIZER,
            bookingLink.getTeam(),
            meeting.getOrganizer().getEmail(),
            vars,
            "Meeting Rescheduled by Client",
            "{{clientName}} has rescheduled their meeting from {{oldMeetingTime}} to {{meetingTime}}"
        );

        return toMeetingResponse(meeting);
    }
}
