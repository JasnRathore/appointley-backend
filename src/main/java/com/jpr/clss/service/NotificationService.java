package com.jpr.clss.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.jpr.clss.entity.Notification;
import com.jpr.clss.entity.User;
import com.jpr.clss.repository.NotificationRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(NotificationRepository notificationRepository, CurrentUserService currentUserService) {
        this.notificationRepository = notificationRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public List<Notification> getMyNotifications() {
        User user = currentUserService.requireCurrentUser();
        return notificationRepository.findByUserOrderByCreatedAtDesc(user);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount() {
        User user = currentUserService.requireCurrentUser();
        return notificationRepository.countByUserAndIsRead(user, false);
    }

    @Transactional
    public void markAsRead(String notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification != null && notification.getUser().getId().equals(currentUserService.requireCurrentUser().getId())) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead() {
        User user = currentUserService.requireCurrentUser();
        List<Notification> unread = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        for (Notification n : unread) {
            n.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }

    @Transactional
    public void createNotification(User user, String title, String message, String type, String actionUrl) {
        if (!user.isInAppOnBooking() && "BOOKING".equals(type)) {
            return;
        }
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setActionUrl(actionUrl);
        notificationRepository.save(notification);
    }
}
