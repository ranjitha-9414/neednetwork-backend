
package com.example.neednetwork.config;

import com.example.neednetwork.entity.User;
import com.example.neednetwork.repository.UserRepository;
import com.example.neednetwork.service.JwtService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {

        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Continue if no Bearer token is provided
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Validate JWT
        if (!jwtService.isTokenValid(token)) {

            SecurityContextHolder.clearContext();

            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid or expired authentication token."
            );

            return;
        }

        try {

            String email = jwtService.extractEmail(token);

            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                // Fetch current user from database
                User user = userRepository.findByEmailIgnoreCase(email)
                        .orElse(null);

                // Reject deleted or missing accounts
                if (user == null) {

                    SecurityContextHolder.clearContext();

                    writeError(
                            response,
                            HttpServletResponse.SC_UNAUTHORIZED,
                            "User account not found."
                    );

                    return;
                }

                // Check current account activation status
                if (!user.isActive()) {

                    SecurityContextHolder.clearContext();

                    writeError(
                            response,
                            HttpServletResponse.SC_FORBIDDEN,
                            "Your account has been deactivated. Please contact the administrator."
                    );

                    return;
                }

                // Get the latest role from database
                List<SimpleGrantedAuthority> authorities = List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().name()
                        )
                );

                // Create authentication
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getEmail(),
                                null,
                                authorities
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                // Set authentication in security context
                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);
            }

        } catch (Exception ex) {

            SecurityContextHolder.clearContext();

            writeError(
                    response,
                    HttpServletResponse.SC_UNAUTHORIZED,
                    "Invalid authentication token."
            );

            return;
        }

        // Continue request
        filterChain.doFilter(request, response);
    }

    // Common JSON error response
    private void writeError(
            HttpServletResponse response,
            int status,
            String message) throws IOException {

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        response.getWriter().write(
                "{\"message\":\"" + message + "\"}"
        );
    }
}
