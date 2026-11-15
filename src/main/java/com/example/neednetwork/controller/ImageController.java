
package com.example.neednetwork.controller;

import io.imagekit.client.ImageKitClient;
import io.imagekit.models.files.FileUploadParams;
import io.imagekit.models.files.FileUploadResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageKitClient imageKitClient;

    public ImageController(ImageKitClient imageKitClient) {
        this.imageKitClient = imageKitClient;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImages(

            @RequestParam(value = "files", required = false)
            List<MultipartFile> files,

            @RequestParam(value = "file", required = false)
            MultipartFile singleFile) {

        try {
            // Support both single and multiple image uploads
            List<MultipartFile> selectedFiles = new ArrayList<>();

            if (files != null) {
                selectedFiles.addAll(files);
            }

            if (singleFile != null && !singleFile.isEmpty()) {
                selectedFiles.add(singleFile);
            }

            selectedFiles.removeIf(
                    file -> file == null || file.isEmpty()
            );

            if (selectedFiles.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "Please select at least one image")
                );
            }

            // Maximum 5 images
            if (selectedFiles.size() > 5) {
                return ResponseEntity.badRequest().body(
                        Map.of("error", "You can upload a maximum of 5 images")
                );
            }

            List<String> imageUrls = new ArrayList<>();

            // Validate all images before uploading
            for (MultipartFile file : selectedFiles) {

                if (file.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest().body(
                            Map.of("error", "Each image must be 5 MB or smaller")
                    );
                }

                String contentType = file.getContentType();

                if (!List.of(
                        "image/jpeg",
                        "image/png",
                        "image/webp"
                ).contains(contentType)) {

                    return ResponseEntity.badRequest().body(
                            Map.of("error", "Only JPG, PNG and WEBP images are allowed")
                    );
                }
            }

            // Upload images to ImageKit
            for (MultipartFile file : selectedFiles) {

                String originalName = file.getOriginalFilename();

                String extension = originalName != null
                        && originalName.contains(".")
                        ? originalName.substring(
                                originalName.lastIndexOf(".")
                        )
                        : "";

                String fileName = UUID.randomUUID() + extension;

                FileUploadParams params = FileUploadParams.builder()
                        .file(file.getBytes())
                        .fileName(fileName)
                        .folder("/neednetwork")
                        .build();

                FileUploadResponse response =
                        imageKitClient.files().upload(params);

                String imageUrl = response.url()
                        .orElseThrow(() -> new RuntimeException("ImageKit did not return an image URL"));

                imageUrls.add(imageUrl);
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(
                    Map.of(
                            "imageUrls", imageUrls,
                            "imageUrl", imageUrls.get(0),
                            "message", "Images uploaded successfully"
                    )
            );

        } catch (Exception e) {

            // Keep detailed error information in backend logs
            e.printStackTrace();

            return ResponseEntity.internalServerError().body(
                    Map.of("error", "Image upload failed")
            );
        }
    }
}
