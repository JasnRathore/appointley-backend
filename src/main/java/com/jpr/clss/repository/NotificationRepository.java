package com.jpr.clss.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.jpr.clss.entity.Notification;
import com.jpr.clss.entity.User;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    List<Notification> findByUserAndIsReadOrderByCreatedAtDesc(User user, boolean isRead);
    long countByUserAndIsRead(User user, boolean isRead);
    void deleteByUserId(String userId);
}
