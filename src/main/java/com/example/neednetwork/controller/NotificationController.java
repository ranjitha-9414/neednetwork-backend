
package com.example.neednetwork.controller;

import com.example.neednetwork.entity.Notification;
import com.example.neednetwork.service.NotificationService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "http://localhost:5173")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {

        this.notificationService = notificationService;
    }

    // Get all notifications for logged-in user
    @GetMapping
    public ResponseEntity<?> getAllNotifications(
            Authentication authentication) {

        String email = authentication.getName();

        List<Notification> notifications =
                notificationService.getAllNotifications(email);

        return ResponseEntity.ok(notifications);
    }

    // Get unread notifications
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(
            Authentication authentication) {

        String email = authentication.getName();

        List<Notification> notifications =
                notificationService.getUnreadNotifications(email);

        return ResponseEntity.ok(notifications);
    }

    // Get unread notification count
    @GetMapping("/count")
    public ResponseEntity<?> getUnreadCount(
            Authentication authentication) {

        String email = authentication.getName();

        long count = notificationService.getUnreadCount(email);

        return ResponseEntity.ok(
                Map.of("unreadCount", count)
        );
    }

    // Mark one notification as read
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            String email = authentication.getName();

            notificationService.markAsRead(id, email);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Notification marked as read."
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    // Mark all notifications as read
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(
            Authentication authentication) {

        String email = authentication.getName();

        notificationService.markAllAsRead(email);

        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "All notifications marked as read."
                )
        );
    }

    // Delete one notification
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            String email = authentication.getName();

            notificationService.deleteNotification(id, email);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Notification deleted successfully."
                    )
            );

        } catch (IllegalArgumentException e) {

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }
}
