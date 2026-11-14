
package com.example.neednetwork.service;

import com.example.neednetwork.entity.Notification;
import com.example.neednetwork.repository.NotificationRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(
            NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    // 1. Create notification
    public Notification createNotification(
            String recipientEmail,
            String title,
            String message,
            String type,
            Long relatedRequestId) {

        Notification notification = new Notification();

        notification.setRecipientEmail(recipientEmail);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedRequestId(relatedRequestId);
        notification.setRead(false);

        return notificationRepository.save(notification);
    }

    // 2. Get all notifications
    public List<Notification> getAllNotifications(String email) {

        return notificationRepository
                .findByRecipientEmailOrderByCreatedAtDesc(email);
    }

    // 3. Get unread notifications
    public List<Notification> getUnreadNotifications(String email) {

        return notificationRepository
                .findByRecipientEmailAndReadFalseOrderByCreatedAtDesc(
                        email
                );
    }

    // 4. Count unread notifications
    public long getUnreadCount(String email) {

        return notificationRepository
                .countByRecipientEmailAndReadFalse(email);
    }

    // 5. Mark one notification as read
    @Transactional
    public void markAsRead(Long notificationId, String email) {

        Notification notification = notificationRepository
                .findByIdAndRecipientEmail(notificationId, email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Notification not found."
                        )
                );

        notification.setRead(true);

        notificationRepository.save(notification);
    }

    // 6. Mark all notifications as read
    @Transactional
    public void markAllAsRead(String email) {

        List<Notification> notifications =
                notificationRepository
                        .findByRecipientEmailAndReadFalseOrderByCreatedAtDesc(
                                email
                        );

        for (Notification notification : notifications) {
            notification.setRead(true);
        }

        notificationRepository.saveAll(notifications);
    }

    // 7. Delete one notification
    @Transactional
    public void deleteNotification(Long notificationId, String email) {

        Notification notification = notificationRepository
                .findByIdAndRecipientEmail(notificationId, email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Notification not found."
                        )
                );

        notificationRepository.delete(notification);
    }
}
