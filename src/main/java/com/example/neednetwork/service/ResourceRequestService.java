
package com.example.neednetwork.service;

import com.example.neednetwork.entity.Resource;
import com.example.neednetwork.entity.ResourceRequest;

import com.example.neednetwork.repository.ResourceRepository;
import com.example.neednetwork.repository.ResourceRequestRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ResourceRequestService {

    private final ResourceRequestRepository requestRepository;
    private final ResourceRepository resourceRepository;
    private final NotificationService notificationService;

    private static final Set<String> VALID_URGENCY_LEVELS = Set.of(
            "EMERGENCY",
            "URGENT",
            "NORMAL",
            "PLANNED"
    );

    // Constructor injection
    public ResourceRequestService(
            ResourceRequestRepository requestRepository,
            ResourceRepository resourceRepository,
            NotificationService notificationService) {

        this.requestRepository = requestRepository;
        this.resourceRepository = resourceRepository;
        this.notificationService = notificationService;
    }

    // ==========================================
    // VALIDATE URGENCY DETAILS
    // ==========================================

    private void validateUrgency(ResourceRequest request) {

        String urgency = request.getUrgencyLevel();

        // Default urgency
        if (urgency == null || urgency.isBlank()) {
            urgency = "NORMAL";
        }

        urgency = urgency.trim().toUpperCase(Locale.ROOT);

        // Validate urgency level
        if (!VALID_URGENCY_LEVELS.contains(urgency)) {
            throw new IllegalArgumentException(
                    "Invalid urgency level. Choose EMERGENCY, "
                    + "URGENT, NORMAL or PLANNED."
            );
        }

        request.setUrgencyLevel(urgency);

        // Emergency and urgent requests require an explanation
        if (urgency.equals("EMERGENCY")
                || urgency.equals("URGENT")) {

            String urgencyReason = request.getUrgencyReason();

            if (urgencyReason == null || urgencyReason.isBlank()) {
                throw new IllegalArgumentException(
                        "Please explain why this request is urgent."
                );
            }

            urgencyReason = urgencyReason.trim();

            if (urgencyReason.length() > 1000) {
                throw new IllegalArgumentException(
                        "Urgency explanation cannot exceed 1000 characters."
                );
            }

            request.setUrgencyReason(urgencyReason);

        } else if (request.getUrgencyReason() != null) {

            String urgencyReason =
                    request.getUrgencyReason().trim();

            if (urgencyReason.length() > 1000) {
                throw new IllegalArgumentException(
                        "Urgency explanation cannot exceed 1000 characters."
                );
            }

            request.setUrgencyReason(
                    urgencyReason.isEmpty() ? null : urgencyReason
            );
        }
    }

    // ==========================================
    // CREATE BORROWING REQUEST
    // ==========================================

    @Transactional
    public ResourceRequest createRequest(ResourceRequest request) {

        // Validate urgency before saving
        validateUrgency(request);

        // Every new request starts as pending
        request.setStatus("PENDING");

        // New requests have no approval or return date
        request.setApprovedAt(null);
        request.setReturnedAt(null);

        ResourceRequest savedRequest =
                requestRepository.save(request);

        Resource resource = savedRequest.getResource();

        // Notify resource owner with urgency information
        notificationService.createNotification(
                resource.getOwnerEmail(),
                "New Resource Request",
                savedRequest.getRequesterName()
                        + " has requested your resource: "
                        + resource.getTitle()
                        + ". Priority: "
                        + savedRequest.getUrgencyLevel(),
                "REQUEST",
                savedRequest.getId()
        );

        return savedRequest;
    }

    // ==========================================
    // GET ALL REQUESTS - PRIORITY ORDER
    // ==========================================

    public List<ResourceRequest> getAllRequests() {

        return requestRepository.findAllByPriority();
    }

    // ==========================================
    // GET REQUESTS BY REQUESTER EMAIL
    // ==========================================

    public List<ResourceRequest> getRequestsByEmail(String email) {

        return requestRepository.findByRequesterEmailIgnoreCase(email);
    }

    // ==========================================
    // GET REQUESTS FOR RESOURCES OWNED BY A USER
    // ==========================================

    public List<ResourceRequest> getRequestsByResourceOwnerEmail(
            String email) {

        return requestRepository
                .findByResource_OwnerEmailIgnoreCase(email);
    }

    // ==========================================
    // APPROVE OR REJECT BORROWING REQUEST
    // ==========================================

    @Transactional
    public ResourceRequest updateRequestStatus(Long id, String status) {

        if (status == null) {
            throw new IllegalArgumentException(
                    "Status is required"
            );
        }

        String normalizedStatus =
                status.trim().toUpperCase(Locale.ROOT);

        if (!normalizedStatus.equals("APPROVED")
                && !normalizedStatus.equals("REJECTED")) {

            throw new IllegalArgumentException(
                    "Status must be APPROVED or REJECTED"
            );
        }

        // Lock borrowing request
        ResourceRequest request = requestRepository
                .findByIdForUpdate(id)
                .orElseThrow(() ->
                        new RuntimeException("Request not found")
                );

        // Only pending requests can be processed
        if (!"PENDING".equals(request.getStatus())) {

            throw new IllegalStateException(
                    "Only pending requests can be processed"
            );
        }

        // If approving, lock and check resource
        if ("APPROVED".equals(normalizedStatus)) {

            Long resourceId = request.getResource().getId();

            Resource resource = resourceRepository
                    .findByIdForUpdate(resourceId)
                    .orElseThrow(() ->
                            new RuntimeException("Resource not found")
                    );

            if (!resource.isAvailable()) {

                throw new IllegalStateException(
                        "This resource is already unavailable"
                );
            }

            resource.setAvailable(false);

            resourceRepository.save(resource);

            // Record actual approval date and time
            request.setApprovedAt(LocalDateTime.now());
        }

        // Update request status
        request.setStatus(normalizedStatus);

        ResourceRequest updatedRequest =
                requestRepository.save(request);

        // Notify requester
        String resourceTitle =
                request.getResource().getTitle();

        if ("APPROVED".equals(normalizedStatus)) {

            notificationService.createNotification(
                    request.getRequesterEmail(),
                    "Request Approved",
                    "Your request for "
                            + resourceTitle
                            + " has been approved.",
                    "APPROVED",
                    request.getId()
            );

        } else {

            notificationService.createNotification(
                    request.getRequesterEmail(),
                    "Request Rejected",
                    "Your request for "
                            + resourceTitle
                            + " has been rejected.",
                    "REJECTED",
                    request.getId()
            );
        }

        return updatedRequest;
    }

    // ==========================================
    // RETURN BORROWED RESOURCE
    // ==========================================

    @Transactional
    public ResourceRequest returnResource(Long requestId) {

        // Lock borrowing request
        ResourceRequest request = requestRepository
                .findByIdForUpdate(requestId)
                .orElseThrow(() ->
                        new RuntimeException("Request not found")
                );

        // Only approved requests can be returned
        if (!"APPROVED".equals(request.getStatus())) {

            throw new IllegalStateException(
                    "Only approved requests can be marked as returned"
            );
        }

        // Lock associated resource
        Long resourceId = request.getResource().getId();

        Resource resource = resourceRepository
                .findByIdForUpdate(resourceId)
                .orElseThrow(() ->
                        new RuntimeException("Resource not found")
                );

        if (resource.isAvailable()) {

            throw new IllegalStateException(
                    "This resource is already available"
            );
        }

        // Make resource available again
        resource.setAvailable(true);

        resourceRepository.save(resource);

        // Record actual return date and time
        request.setReturnedAt(LocalDateTime.now());

        // Preserve approvedAt when marking as returned
        request.setStatus("RETURNED");

        ResourceRequest updatedRequest =
                requestRepository.save(request);

        // Notify requester
        notificationService.createNotification(
                request.getRequesterEmail(),
                "Resource Returned",
                "The borrowing process for "
                        + resource.getTitle()
                        + " has been marked as returned.",
                "RETURNED",
                request.getId()
        );

        // Notify resource owner
        String ownerEmail = resource.getOwnerEmail();

        if (ownerEmail != null
                && !ownerEmail.equalsIgnoreCase(
                        request.getRequesterEmail())) {

            notificationService.createNotification(
                    ownerEmail,
                    "Resource Returned",
                    request.getRequesterName()
                            + " has returned your resource: "
                            + resource.getTitle(),
                    "RETURNED",
                    request.getId()
            );
        }

        return updatedRequest;
    }
}
