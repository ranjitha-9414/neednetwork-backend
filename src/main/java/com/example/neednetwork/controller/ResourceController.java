
package com.example.neednetwork.controller;

import com.example.neednetwork.dto.ResourceResponse;
import com.example.neednetwork.entity.Resource;
import com.example.neednetwork.entity.User;
import com.example.neednetwork.repository.ResourceRepository;
import com.example.neednetwork.repository.UserRepository;
import com.example.neednetwork.service.ResourceService;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resources")
@CrossOrigin(origins = "http://localhost:5173")
public class ResourceController {

    private final ResourceService resourceService;
    private final ResourceRepository resourceRepository;
    private final UserRepository userRepository;

    public ResourceController(
            ResourceService resourceService,
            ResourceRepository resourceRepository,
            UserRepository userRepository) {

        this.resourceService = resourceService;
        this.resourceRepository = resourceRepository;
        this.userRepository = userRepository;
    }

    // ==========================================
    // GET ALL RESOURCES - PUBLIC MARKETPLACE
    // ==========================================

    @GetMapping
    public ResponseEntity<List<ResourceResponse>> getAllResources() {

        List<ResourceResponse> resources =
                resourceService.getAllResources()
                        .stream()
                        .map(ResourceResponse::new)
                        .toList();

        return ResponseEntity.ok(resources);
    }

    // ==========================================
    // GET RESOURCE BY ID
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getResourceById(
            @PathVariable Long id) {

        return resourceService.getResourceById(id)
                .map(ResourceResponse::new)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ==========================================
    // GET LOGGED-IN USER RESOURCES
    // ==========================================

    @GetMapping("/mine")
    public ResponseEntity<List<ResourceResponse>> getMyResources(
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<ResourceResponse> resources =
                resourceRepository.findByOwnerEmailIgnoreCase(
                        authentication.getName()
                )
                .stream()
                .map(ResourceResponse::new)
                .toList();

        return ResponseEntity.ok(resources);
    }

    // ==========================================
    // ADD NEW RESOURCE
    // ==========================================

    @PostMapping
    public ResponseEntity<?> addResource(
            @RequestBody Resource resource,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Please login first"));
        }

        User user = userRepository
                .findByEmailIgnoreCase(authentication.getName())
                .orElse(null);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "User account not found"
                    ));
        }

        // Never accept owner details from the frontend
        resource.setId(null);
        resource.setOwnerName(user.getFullName());
        resource.setOwnerEmail(user.getEmail());
        resource.setOwnerPhone(user.getPhone());
        resource.setAvailable(true);

        Resource savedResource =
                resourceService.addResource(resource);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ResourceResponse(savedResource));
    }

    // ==========================================
    // UPDATE RESOURCE
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateResource(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body,
            Authentication authentication) {

        Resource existingResource = resourceService
                .getResourceById(id)
                .orElse(null);

        if (existingResource == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify owner or admin permission
        if (!canManageResource(authentication, existingResource)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You cannot update another user's resource"
                    ));
        }

        try {

            Resource resource = new Resource();

            // Update title
            resource.setTitle(
                    body.containsKey("title")
                            ? (String) body.get("title")
                            : existingResource.getTitle()
            );

            // Update category
            resource.setCategory(
                    body.containsKey("category")
                            ? (String) body.get("category")
                            : existingResource.getCategory()
            );

            // Update description
            resource.setDescription(
                    body.containsKey("description")
                            ? (String) body.get("description")
                            : existingResource.getDescription()
            );

            // Update condition
            resource.setCondition(
                    body.containsKey("condition")
                            ? (String) body.get("condition")
                            : existingResource.getCondition()
            );

            // Update location
            resource.setLocation(
                    body.containsKey("location")
                            ? (String) body.get("location")
                            : existingResource.getLocation()
            );

            // Preserve availability unless supplied
            boolean availabilityProvided =
                    body.containsKey("available");

            if (availabilityProvided) {

                Object availabilityValue = body.get("available");

                if (!(availabilityValue instanceof Boolean)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "message",
                                    "available must be true or false"
                            ));
                }

                resource.setAvailable((Boolean) availabilityValue);

            } else {

                resource.setAvailable(existingResource.isAvailable());
            }

            // Detect image update
            boolean imagesProvided =
                    body.containsKey("imageUrls");

            if (imagesProvided) {

                Object imagesObject = body.get("imageUrls");

                if (!(imagesObject instanceof List<?>)) {
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "message",
                                    "imageUrls must be an array"
                            ));
                }

                List<?> rawImages = (List<?>) imagesObject;
                List<String> images = new ArrayList<>();

                for (Object image : rawImages) {

                    if (!(image instanceof String)
                            || ((String) image).isBlank()) {

                        return ResponseEntity.badRequest()
                                .body(Map.of(
                                        "message",
                                        "Image URLs must be non-empty strings"
                                ));
                    }

                    images.add((String) image);
                }

                if (images.size() > 5) {
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "message",
                                    "Maximum 5 images are allowed"
                            ));
                }

                resource.setImageUrls(images);

                resource.setImageUrl(
                        images.isEmpty()
                                ? null
                                : images.get(0)
                );
            }

            // Preserve original owner information
            resource.setOwnerName(existingResource.getOwnerName());
            resource.setOwnerEmail(existingResource.getOwnerEmail());
            resource.setOwnerPhone(existingResource.getOwnerPhone());

            // Save changes through service
            Resource updatedResource =
                    resourceService.updateResource(
                            id,
                            resource,
                            imagesProvided,
                            availabilityProvided
                    );

            return ResponseEntity.ok(
                    new ResourceResponse(updatedResource)
            );

        } catch (IllegalStateException exception) {

            // Example: resource is currently borrowed
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            exception.getMessage()
                    ));

        } catch (ClassCastException exception) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            "Invalid field value in update request"
                    ));

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            exception.getMessage()
                    ));
        }
    }

    // ==========================================
    // DELETE RESOURCE
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteResource(
            @PathVariable Long id,
            Authentication authentication) {

        Resource existingResource = resourceService
                .getResourceById(id)
                .orElse(null);

        if (existingResource == null) {
            return ResponseEntity.notFound().build();
        }

        // Verify owner or admin permission
        if (!canManageResource(authentication, existingResource)) {

            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                            "message",
                            "You cannot delete another user's resource"
                    ));
        }

        try {

            resourceService.deleteResource(id);

            return ResponseEntity.noContent().build();

        } catch (IllegalStateException exception) {

            // Resource has borrowing history
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of(
                            "message",
                            exception.getMessage()
                    ));

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message",
                            exception.getMessage()
                    ));
        }
    }

    // ==========================================
    // OWNER / ADMIN AUTHORIZATION
    // ==========================================

    private boolean canManageResource(
            Authentication authentication,
            Resource resource) {

        if (authentication == null) {
            return false;
        }

        // Admin can manage all resources
        boolean isAdmin = authentication.getAuthorities()
                .stream()
                .anyMatch(authority ->
                        authority.getAuthority().equals("ROLE_ADMIN")
                );

        if (isAdmin) {
            return true;
        }

        // Verify resource ownership
        String loggedInEmail = authentication.getName();
        String ownerEmail = resource.getOwnerEmail();

        return ownerEmail != null
                && ownerEmail.equalsIgnoreCase(loggedInEmail);
    }
}
