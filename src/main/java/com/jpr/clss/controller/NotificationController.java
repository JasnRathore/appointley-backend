package com.jpr.clss.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.jpr.clss.entity.Notification;
import com.jpr.clss.service.NotificationService;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public List<Notification> getNotifications() {
        return notificationService.getMyNotifications();
    }

    @GetMapping("/unread-count")
    public long getUnreadCount() {
        return notificationService.getUnreadCount();
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
    }

    @PostMapping("/mark-all-read")
    public void markAllAsRead() {
        notificationService.markAllAsRead();
    }
}
