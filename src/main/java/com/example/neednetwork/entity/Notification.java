
package com.example.neednetwork.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Email address of the notification recipient
    @Column(nullable = false)
    private String recipientEmail;

    // Notification heading
    @Column(nullable = false, length = 150)
    private String title;

    // Notification description
    @Column(nullable = false, length = 500)
    private String message;

    // REQUEST, APPROVED, REJECTED, RETURNED
    @Column(nullable = false, length = 50)
    private String type;

    // Related resource request
    private Long relatedRequestId;

    // Read/unread status
    @Column(name = "is_read", nullable = false)
    private boolean read = false;

    // Notification creation time
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Default constructor
    public Notification() {
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRecipientEmail() {
        return recipientEmail;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getRelatedRequestId() {
        return relatedRequestId;
    }

    public void setRelatedRequestId(Long relatedRequestId) {
        this.relatedRequestId = relatedRequestId;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
