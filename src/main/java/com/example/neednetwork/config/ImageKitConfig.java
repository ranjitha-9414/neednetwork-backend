
package com.example.neednetwork.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.imagekit.client.ImageKitClient;
import io.imagekit.client.okhttp.ImageKitOkHttpClient;

@Configuration
public class ImageKitConfig {

    @Bean
    public ImageKitClient imageKitClient() {
        return ImageKitOkHttpClient.builder()
                .privateKey(System.getenv("IMAGEKIT_PRIVATE_KEY"))
                .build();
    }
}
