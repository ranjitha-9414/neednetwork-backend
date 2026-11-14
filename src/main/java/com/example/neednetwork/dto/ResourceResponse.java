
package com.example.neednetwork.dto;

import com.example.neednetwork.entity.Resource;

import java.time.LocalDateTime;
import java.util.List;

public class ResourceResponse {

    private Long id;
    private String title;
    private String category;
    private String description;
    private String condition;
    private String location;

    private String imageUrl;
    private List<String> imageUrls;

    private String ownerName;

    private boolean available;
    private LocalDateTime createdAt;

    public ResourceResponse(Resource resource) {

        this.id = resource.getId();
        this.title = resource.getTitle();
        this.category = resource.getCategory();
        this.description = resource.getDescription();
        this.condition = resource.getCondition();
        this.location = resource.getLocation();

        this.imageUrl = resource.getImageUrl();

        this.imageUrls = resource.getImageUrls() == null
                ? List.of()
                : List.copyOf(resource.getImageUrls());

        this.ownerName = resource.getOwnerName();

        this.available = resource.isAvailable();
        this.createdAt = resource.getCreatedAt();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public String getCondition() {
        return condition;
    }

    public String getLocation() {
        return location;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isAvailable() {
        return available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
