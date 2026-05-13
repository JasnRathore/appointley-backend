package com.jpr.clss.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.jpr.clss.entity.EmailType;
import com.jpr.clss.exception.ApiException;

@Component
public class EmailTemplateCatalog {

    private final Map<EmailType, EmailTemplateDefinition> definitions;

    public EmailTemplateCatalog() {
        Map<EmailType, EmailTemplateDefinition> items = new LinkedHashMap<>();
        register(items, EmailType.WELCOME, "Welcome Email", "Sent when a new workspace is created.",
            "Welcome to {{teamName}}",
            "Hi {{userName}},\n\nYour Appointley workspace for {{teamName}} is ready.\n\nYou can start sharing booking links as {{senderName}} right away.",
            List.of("userName", "teamName", "senderName"));
        register(items, EmailType.TEAM_INVITE, "Team Invitation", "Sent when a teammate is invited into the workspace.",
            "You're invited to join {{teamName}}",
            "Hi,\n\n{{inviterName}} has invited you to join the {{teamName}} workspace on Appointley as a {{role}}.\n\nClick the link below to accept your invitation and get started:\n\n{{inviteLink}}\n\nThis invitation will expire in 7 days.",
            List.of("inviteeEmail", "inviterName", "role", "inviteToken", "inviteLink", "teamName", "senderName"));
        register(items, EmailType.ROLE_UPDATED, "Role Updated", "Sent when a member's team role changes.",
            "Your role in {{teamName}} has changed",
            "Hi {{memberName}},\n\nYour role in {{teamName}} is now {{role}}.",
            List.of("memberName", "role", "teamName", "senderName"));
        register(items, EmailType.BOOKING_LINK, "Booking Link", "Sent when a booking link is generated or shared.",
            "Booking Invitation from {{organizerName}}",
            "Hello,\n\n{{organizerName}} from {{teamName}} has invited you to book an appointment.\n\nYou can select a convenient time using the link below:\n\n{{bookingUrl}}\n\n--\n{{senderName}}",
            List.of("organizerName", "bookingUrl", "recipientEmail", "bookingLinkMessage", "teamName", "senderName"));
        register(items, EmailType.MEETING_BOOKED_ORGANIZER, "Meeting Booked: Organizer", "Sent to the organizer after a client books a meeting.",
            "New Appointment: {{clientName}}",
            "Hello {{organizerName}},\n\n{{clientName}} ({{clientEmail}}) has just booked a new meeting with you.\n\nDate/Time: {{meetingTime}}\nDuration: {{duration}} minutes\n\nView your full schedule in the Appointley dashboard.",
            List.of("clientName", "clientEmail", "meetingTime", "duration", "organizerName", "teamName", "senderName"));
        register(items, EmailType.MEETING_BOOKED_CLIENT, "Meeting Booked: Client", "Sent to the client after a booking is confirmed.",
            "Confirmed: {{clientName}} + {{organizerName}}",
            "Hi {{clientName}},\n\nYour appointment with {{organizerName}} has been confirmed.\n\nEvent Details:\n- Date/Time: {{meetingTime}}\n- Duration: {{duration}} minutes\n\nYou can manage or reschedule your booking at any time here:\n{{manageBookingUrl}}\n\nLooking forward to seeing you!\n\n--\n{{senderName}} (via {{teamName}})",
            List.of("clientName", "clientEmail", "meetingTime", "duration", "organizerName", "manageBookingUrl", "teamName", "senderName"));
        register(items, EmailType.MEETING_CANCELLED_ORGANIZER, "Meeting Cancelled: Organizer", "Sent to the organizer when a client cancels a meeting.",
            "Cancelled: {{clientName}} at {{meetingTime}}",
            "Hello {{organizerName}},\n\n{{clientName}} ({{clientEmail}}) has cancelled their appointment originally scheduled for {{meetingTime}}.\n\nThis slot is now available for other clients to book.",
            List.of("clientName", "clientEmail", "meetingTime", "organizerName", "teamName", "senderName"));
        register(items, EmailType.MEETING_CANCELLED_CLIENT, "Meeting Cancelled: Client", "Sent to the client when the organizer cancels a meeting.",
            "Meeting Cancellation: {{organizerName}}",
            "Hi {{clientName}},\n\nYour appointment with {{organizerName}} scheduled for {{meetingTime}} has been cancelled.\n\nWe apologize for any inconvenience. You can book a new time here if needed:\n{{bookingUrl}}",
            List.of("clientName", "clientEmail", "meetingTime", "organizerName", "teamName", "senderName", "bookingUrl"));
        register(items, EmailType.MEETING_RESCHEDULED_ORGANIZER, "Meeting Rescheduled: Organizer", "Sent to the organizer when a client reschedules a meeting.",
            "Rescheduled: {{clientName}}",
            "Hello {{organizerName}},\n\n{{clientName}} has moved their appointment.\n\nOld Time: {{oldMeetingTime}}\nNew Time: {{meetingTime}}\n\nYour calendar has been updated automatically.",
            List.of("clientName", "clientEmail", "meetingTime", "oldMeetingTime", "organizerName", "teamName", "senderName"));
        register(items, EmailType.MEETING_RESCHEDULED_CLIENT, "Meeting Rescheduled: Client", "Sent to the client when the organizer reschedules a meeting.",
            "Rescheduled: {{clientName}} + {{organizerName}}",
            "Hi {{clientName}},\n\nYour appointment with {{organizerName}} has been moved to a new time.\n\nOld Time: {{oldMeetingTime}}\nNew Time: {{meetingTime}}\n\nYou can manage this booking here:\n{{manageBookingUrl}}",
            List.of("clientName", "clientEmail", "meetingTime", "oldMeetingTime", "organizerName", "teamName", "senderName", "manageBookingUrl"));
        this.definitions = Map.copyOf(items);
    }

    public List<EmailTemplateDefinition> getTeamManagedDefinitions() {
        return List.copyOf(definitions.values());
    }

    public EmailTemplateDefinition requireDefinition(EmailType type) {
        EmailTemplateDefinition definition = definitions.get(type);
        if (definition == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Email template type is not configurable");
        }
        return definition;
    }

    private void register(
        Map<EmailType, EmailTemplateDefinition> items,
        EmailType type,
        String displayName,
        String description,
        String defaultSubjectTemplate,
        String defaultBodyTemplate,
        List<String> variables
    ) {
        items.put(type, new EmailTemplateDefinition(type, displayName, description, defaultSubjectTemplate, defaultBodyTemplate, variables));
    }

    public record EmailTemplateDefinition(
        EmailType type,
        String displayName,
        String description,
        String defaultSubjectTemplate,
        String defaultBodyTemplate,
        List<String> variables
    ) {}
}
