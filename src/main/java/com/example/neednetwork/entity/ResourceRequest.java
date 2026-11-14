
package com.example.neednetwork.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "resource_requests")
public class ResourceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "resource_id", nullable = false)
    private Resource resource;

    @Column(nullable = false)
    private String requesterName;

    @Column(nullable = false)
    private String requesterEmail;

    @Column(length = 1000)
    private String reason;

    @Column(nullable = false)
    private String status = "PENDING";

    private LocalDateTime createdAt;

    // Actual approval date and time
    private LocalDateTime approvedAt;

    @Column(nullable = false)
    private String requesterPhone;

    @Column(nullable = false)
    private String duration;

    @Column(nullable = false)
    private LocalDate pickupDate;

    // Actual return date and time
    private LocalDateTime returnedAt;

    @Column(name = "urgency_level", length = 20)
    private String urgencyLevel = "NORMAL";

    @Column(name = "urgency_reason", length = 1000)
    private String urgencyReason;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = "PENDING";
        }

        if (urgencyLevel == null || urgencyLevel.isBlank()) {
            urgencyLevel = "NORMAL";
        }
    }

    public ResourceRequest() {
    }

    // ID
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // Resource
    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    // Requester name
    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    // Requester email
    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    // Reason
    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    // Status
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Created date
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    // Approved date
    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    // Phone
    public String getRequesterPhone() {
        return requesterPhone;
    }

    public void setRequesterPhone(String requesterPhone) {
        this.requesterPhone = requesterPhone;
    }

    // Duration
    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    // Pickup date
    public LocalDate getPickupDate() {
        return pickupDate;
    }

    public void setPickupDate(LocalDate pickupDate) {
        this.pickupDate = pickupDate;
    }

    // Returned date
    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }

    // Urgency level
    public String getUrgencyLevel() {
        return urgencyLevel;
    }

    public void setUrgencyLevel(String urgencyLevel) {
        this.urgencyLevel = urgencyLevel;
    }

    // Urgency reason
    public String getUrgencyReason() {
        return urgencyReason;
    }

    public void setUrgencyReason(String urgencyReason) {
        this.urgencyReason = urgencyReason;
    }
}
