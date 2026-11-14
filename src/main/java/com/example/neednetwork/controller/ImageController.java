
package com.example.neednetwork.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/images")
@CrossOrigin(origins = "http://localhost:5173")
public class ImageController {

    private final Path uploadPath =
            Paths.get("uploads").toAbsolutePath().normalize();

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImages(

            @RequestParam(value = "files", required = false)
            List<MultipartFile> files,

            // Supports the old single-image frontend
            @RequestParam(value = "file", required = false)
            MultipartFile singleFile) {

        List<String> imageUrls = new ArrayList<>();

        try {

            // Support both single and multiple file uploads
            if (files == null) {
                files = new ArrayList<>();
            }

            if (singleFile != null && !singleFile.isEmpty()) {
                files.add(singleFile);
            }

            // Remove empty file entries
            files.removeIf(file ->
                    file == null || file.isEmpty()
            );

            if (files.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "Please select at least one image"
                        ));
            }

            // Maximum 5 images
            if (files.size() > 5) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                                "error",
                                "You can upload a maximum of 5 images"
                        ));
            }

            Files.createDirectories(uploadPath);

            for (MultipartFile file : files) {

                // Maximum 5 MB per image
                if (file.getSize() > 5 * 1024 * 1024) {
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "error",
                                    "Each image must be 5 MB or smaller"
                            ));
                }

                String extension;

                String contentType = file.getContentType();

                if ("image/jpeg".equals(contentType)) {
                    extension = ".jpg";
                } else if ("image/png".equals(contentType)) {
                    extension = ".png";
                } else if ("image/webp".equals(contentType)) {
                    extension = ".webp";
                } else {
                    return ResponseEntity.badRequest()
                            .body(Map.of(
                                    "error",
                                    "Only JPG, PNG and WEBP images are allowed"
                            ));
                }

                String fileName = UUID.randomUUID() + extension;

                Path destination =
                        uploadPath.resolve(fileName).normalize();

                if (!destination.startsWith(uploadPath)) {
                    return ResponseEntity.badRequest().build();
                }

                Files.copy(
                        file.getInputStream(),
                        destination,
                        StandardCopyOption.REPLACE_EXISTING
                );

                String imageUrl =
                        "http://localhost:8080/uploads/" + fileName;

                imageUrls.add(imageUrl);
            }

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "imageUrls", imageUrls,
                            "imageUrl", imageUrls.get(0),
                            "message", "Images uploaded successfully"
                    ));

        } catch (IOException e) {

            return ResponseEntity.internalServerError()
                    .body(Map.of(
                            "error",
                            "Image upload failed"
                    ));
        }
    }
}
