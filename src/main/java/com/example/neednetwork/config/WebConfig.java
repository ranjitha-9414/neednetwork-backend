
package com.example.neednetwork.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.io.IOException;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        try {
            Path uploadPath = Paths.get("uploads")
                    .toAbsolutePath()
                    .normalize();

            Files.createDirectories(uploadPath);

            registry.addResourceHandler("/uploads/**")
                    .addResourceLocations(uploadPath.toUri().toString());

        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }
}
