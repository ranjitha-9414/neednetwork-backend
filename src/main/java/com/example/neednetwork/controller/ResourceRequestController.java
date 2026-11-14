
package com.example.neednetwork.controller;

import com.example.neednetwork.entity.Resource;
import com.example.neednetwork.entity.ResourceRequest;
import com.example.neednetwork.entity.User;

import com.example.neednetwork.repository.ResourceRepository;
import com.example.neednetwork.repository.UserRepository;

import com.example.neednetwork.service.ResourceRequestService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.core.Authentication;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/requests")
@CrossOrigin(origins = "http://localhost:5173")
public class ResourceRequestController {

    private static final Logger log =
            LoggerFactory.getLogger(ResourceRequestController.class);

    private final ResourceRequestService requestService;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ResourceRequestController(
            ResourceRequestService requestService,
            ResourceRepository resourceRepository,
            UserRepository userRepository) {

        this.requestService = requestService;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    // CREATE ASSISTANCE REQUEST

    @PostMapping
    public ResponseEntity<?> createRequest(
            @RequestBody ResourceRequest request,
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        String email = authentication.getName();

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "User account not found"));
        }

        // Validate phone
        if (user.getPhone() == null ||
                !user.getPhone().matches("[0-9]{10}")) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Please update your account with a valid phone number"
                    ));
        }

        // Validate reason
        if (request.getReason() == null ||
                request.getReason().isBlank() ||
                request.getReason().trim().length() < 10 ||
                request.getReason().length() > 1000) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Enter a valid reason (10-1000 characters)"));
        }

        // Validate duration
        if (request.getDuration() == null ||
                request.getDuration().isBlank()) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Duration is required"));
        }

        // Validate pickup date
        if (request.getPickupDate() == null ||
                request.getPickupDate().isBefore(LocalDate.now())) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Select a valid pickup date"));
        }

        // Validate resource ID
        if (request.getResource() == null ||
                request.getResource().getId() == null ||
                request.getResource().getId() <= 0) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", "A valid resource ID is required"));
        }

        Long resourceId = request.getResource().getId();

        // Fetch the actual resource from the database
        Resource resource = resourceRepository
                .findById(resourceId)
                .orElse(null);

        if (resource == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "Resource not found"));
        }

        // Check availability
        if (!resource.isAvailable()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Resource is unavailable"));
        }

        // Never trust requester identity or request status from frontend
        request.setId(null);
        request.setRequesterName(user.getFullName());
        request.setRequesterEmail(user.getEmail());
        request.setRequesterPhone(user.getPhone());
        request.setResource(resource);
        request.setReturnedAt(null);
        request.setCreatedAt(null);
        request.setStatus("PENDING");

        try {

            ResourceRequest savedRequest =
                    requestService.createRequest(request);

            // Log request creation for debugging and auditing
            log.info(
                    "Assistance request created: requestId={}, resourceId={}",
                    savedRequest.getId(),
                    resourceId
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(savedRequest);

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    // GET ALL REQUESTS - ADMIN ONLY

    @GetMapping
    public ResponseEntity<?> getAllRequests(
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Only administrators can view all requests"
                    ));
        }

        return ResponseEntity.ok(
                requestService.getAllRequests()
        );
    }

    // GET LOGGED-IN USER REQUESTS

    @GetMapping("/by-email")
    public ResponseEntity<?> getRequestsByEmail(
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        return ResponseEntity.ok(
                requestService.getRequestsByEmail(authentication.getName())
        );
    }

    // GET RESOURCE OWNER REQUESTS

    @GetMapping("/owner")
    public ResponseEntity<?> getOwnerRequests(
            Authentication authentication) {

        if (!isAuthenticated(authentication)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        return ResponseEntity.ok(
                requestService.getRequestsByResourceOwnerEmail(
                        authentication.getName()
                )
        );
    }

    // APPROVE REQUEST

    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveRequest(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only administrators can approve requests"));
        }

        try {
            return ResponseEntity.ok(
                    requestService.updateRequestStatus(id, "APPROVED")
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", exception.getMessage()));

        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    // REJECT REQUEST

    @PutMapping("/{id}/reject")
    public ResponseEntity<?> rejectRequest(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Only administrators can reject requests"));
        }

        try {
            return ResponseEntity.ok(
                    requestService.updateRequestStatus(id, "REJECTED")
            );

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", exception.getMessage()));

        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    // MARK RESOURCE AS RETURNED

    @PutMapping("/{id}/return")
    public ResponseEntity<?> returnResource(
            @PathVariable Long id,
            Authentication authentication) {

        if (!isAdmin(authentication)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "Only administrators can mark resources as returned"
                    ));
        }

        try {
            return ResponseEntity.ok(
                    requestService.returnResource(id)
            );

        } catch (IllegalStateException exception) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", exception.getMessage()));

        } catch (RuntimeException exception) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", exception.getMessage()));
        }
    }

    // AUTHENTICATION CHECK

    private boolean isAuthenticated(Authentication authentication) {

        return authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName());
    }

    // ADMIN ROLE CHECK

    private boolean isAdmin(Authentication authentication) {

        return isAuthenticated(authentication)
                && authentication.getAuthorities().stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );
    }
}
