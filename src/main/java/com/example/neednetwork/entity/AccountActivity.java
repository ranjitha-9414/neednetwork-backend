
package com.example.neednetwork.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "account_activity_history")
public class AccountActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // User whose account was changed
    @Column(nullable = false)
    private Long targetUserId;

    @Column(nullable = false)
    private String targetUserName;

    @Column(nullable = false)
    private String targetUserEmail;

    // Administrator who performed the action
    @Column(nullable = false)
    private String adminEmail;

    // Previous and new account status
    @Column(nullable = false)
    private boolean previousStatus;

    @Column(nullable = false)
    private boolean newStatus;

    // ACTIVATED or DEACTIVATED
    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetUserName() {
        return targetUserName;
    }

    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }

    public String getTargetUserEmail() {
        return targetUserEmail;
    }

    public void setTargetUserEmail(String targetUserEmail) {
        this.targetUserEmail = targetUserEmail;
    }

    public String getAdminEmail() {
        return adminEmail;
    }

    public void setAdminEmail(String adminEmail) {
        this.adminEmail = adminEmail;
    }

    public boolean isPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(boolean previousStatus) {
        this.previousStatus = previousStatus;
    }

    public boolean isNewStatus() {
        return newStatus;
    }

    public void setNewStatus(boolean newStatus) {
        this.newStatus = newStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    
}
