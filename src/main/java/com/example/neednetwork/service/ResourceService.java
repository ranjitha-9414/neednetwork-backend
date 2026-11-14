
package com.example.neednetwork.service;

import com.example.neednetwork.entity.Resource;
import com.example.neednetwork.repository.ResourceRepository;
import com.example.neednetwork.repository.ResourceRequestRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final ResourceRequestRepository requestRepository;

    public ResourceService(
            ResourceRepository resourceRepository,
            ResourceRequestRepository requestRepository) {

        this.resourceRepository = resourceRepository;
        this.requestRepository = requestRepository;
    }

    // Get all resources
    public List<Resource> getAllResources() {
        return resourceRepository.findAll();
    }

    // Get resource by ID
    public java.util.Optional<Resource> getResourceById(Long id) {
        return resourceRepository.findById(id);
    }

    // Add a new resource
    @Transactional
    public Resource addResource(Resource resource) {

        List<String> images = resource.getImageUrls();

        // Backward compatibility with single image
        if ((images == null || images.isEmpty())
                && resource.getImageUrl() != null
                && !resource.getImageUrl().isBlank()) {

            images = new ArrayList<>();
            images.add(resource.getImageUrl());
            resource.setImageUrls(images);
        }

        if (images != null && !images.isEmpty()) {
            resource.setImageUrl(images.get(0));
        }

        return resourceRepository.save(resource);
    }

    // Update an existing resource
    @Transactional
    public Resource updateResource(
            Long id,
            Resource updatedResource,
            boolean imagesProvided,
            boolean availabilityProvided) {

        // Lock resource while updating
        Resource existingResource = resourceRepository
                .findByIdForUpdate(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource not found")
                );

        // Update basic details
        existingResource.setTitle(updatedResource.getTitle());
        existingResource.setCategory(updatedResource.getCategory());
        existingResource.setDescription(updatedResource.getDescription());
        existingResource.setCondition(updatedResource.getCondition());
        existingResource.setLocation(updatedResource.getLocation());

        // Protect borrowing availability
        if (availabilityProvided) {

            boolean requestedAvailability =
                    updatedResource.isAvailable();

            if (requestedAvailability && !existingResource.isAvailable()) {

                boolean activeBorrowing =
                        requestRepository.existsByResource_IdAndStatus(
                                id, "APPROVED"
                        );

                if (activeBorrowing) {
                    throw new IllegalStateException(
                            "This resource is currently borrowed. "
                            + "Only the admin can mark it as returned."
                    );
                }
            }

            existingResource.setAvailable(requestedAvailability);
        }

        // Update images only when supplied
        if (imagesProvided) {

            List<String> updatedImages =
                    updatedResource.getImageUrls();

            if (updatedImages == null) {
                updatedImages = new ArrayList<>();
            }

            if (updatedImages.size() > 5) {
                throw new IllegalArgumentException(
                        "Maximum 5 images are allowed"
                );
            }

            existingResource.setImageUrls(
                    new ArrayList<>(updatedImages)
            );

            existingResource.setImageUrl(
                    updatedImages.isEmpty()
                            ? null
                            : updatedImages.get(0)
            );
        }

        return resourceRepository.save(existingResource);
    }

    // Backward-compatible update method
    @Transactional
    public Resource updateResource(
            Long id,
            Resource updatedResource) {

        return updateResource(
                id,
                updatedResource,
                updatedResource.getImageUrls() != null,
                true
        );
    }

    // Delete resource safely
    @Transactional
    public void deleteResource(Long id) {

        Resource resource = resourceRepository
                .findByIdForUpdate(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Resource not found")
                );

        // Preserve borrowing history
        boolean hasRequestHistory =
                requestRepository.existsByResource_Id(id);

        if (hasRequestHistory) {
            throw new IllegalStateException(
                    "This resource cannot be deleted because "
                    + "it has borrowing request history."
            );
        }

        resourceRepository.delete(resource);
    }
}
