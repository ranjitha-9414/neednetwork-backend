
package com.example.neednetwork.repository;

import com.example.neednetwork.entity.Notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    // Get all notifications for a user
    List<Notification> findByRecipientEmailOrderByCreatedAtDesc(
            String recipientEmail
    );

    // Get only unread notifications
    List<Notification> findByRecipientEmailAndReadFalseOrderByCreatedAtDesc(
            String recipientEmail
    );

    // Count unread notifications
    long countByRecipientEmailAndReadFalse(
            String recipientEmail
    );

    // Find a notification belonging to a particular user
    java.util.Optional<Notification> findByIdAndRecipientEmail(
            Long id,
            String recipientEmail
    );
}
