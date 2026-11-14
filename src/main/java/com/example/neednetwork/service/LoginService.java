
package com.example.neednetwork.service;

import com.example.neednetwork.dto.LoginRequest;
import com.example.neednetwork.dto.LoginResponse;
import com.example.neednetwork.entity.User;
import com.example.neednetwork.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public LoginService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {

        // Normalize email
        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Find user
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Invalid email or password."
                        )
                );

        // Verify password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new IllegalArgumentException(
                    "Invalid email or password."
            );
        }

        // Check account activation
        if (!user.isActive()) {
            throw new IllegalArgumentException(
                    "Your account has been deactivated. " +
                    "Please contact the administrator."
            );
        }

        // Check email verification
        if (!user.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Please verify your email before logging in. " +
                    "Check your inbox for the verification link."
            );
        }

        // Generate JWT only after all checks pass
        String token = jwtService.generateToken(user.getEmail());

        // Return safe user details
        return new LoginResponse(
                token,
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().name()
        );
    }
}
