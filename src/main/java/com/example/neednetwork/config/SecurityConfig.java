
package com.example.neednetwork.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {

        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        http
            .csrf(csrf -> csrf.disable())

            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth

                // Registration, email verification and login are public
                .requestMatchers("/api/auth/**").permitAll()

                // Public homepage statistics (aggregate counts only)
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/home/statistics"
                ).permitAll()

                // Admin-only endpoints
                .requestMatchers("/api/admin/**").hasRole("ADMIN")

                // Only logged-in users can access their own resources
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/resources/mine"
                ).authenticated()

                // Anyone can browse resources and view resource details
                .requestMatchers(
                    HttpMethod.GET,
                    "/api/resources",
                    "/api/resources/*"
                ).permitAll()

                // Resource management requires authentication
                .requestMatchers("/api/resources/**").authenticated()

                // Anyone can view uploaded images
                .requestMatchers(
                    HttpMethod.GET,
                    "/uploads/**"
                ).permitAll()

                // Uploading images requires authentication
                .requestMatchers("/api/images/**").authenticated()

                // Borrowing requests require authentication
                .requestMatchers("/api/requests/**").authenticated()

                // All remaining endpoints require authentication
                .anyRequest().authenticated()
            )

            // Validate JWT before Spring's username/password filter
            .addFilterBefore(
                jwtAuthenticationFilter,
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
            List.of("http://localhost:5173")
        );

        configuration.setAllowedMethods(
            List.of("GET", "POST", "PUT", "DELETE", "OPTIONS")
        );

        configuration.setAllowedHeaders(
            List.of("Authorization", "Content-Type")
        );

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
