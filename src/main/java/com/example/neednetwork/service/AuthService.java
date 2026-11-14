
package com.example.neednetwork.service;

import com.example.neednetwork.dto.RegisterRequest;
import com.example.neednetwork.entity.User;
import com.example.neednetwork.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public User register(RegisterRequest request) {

        String email = request.getEmail()
                .trim()
                .toLowerCase(Locale.ROOT);

        // Check whether the email already exists
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalArgumentException(
                    "An account with this email already exists."
            );
        }

        // Validate the requested role
        User.Role role;

        try {
            role = User.Role.valueOf(
                    request.getRole().trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid user role.");
        }

        // Never allow public registration as ADMIN
        if (role == User.Role.ADMIN) {
            throw new IllegalArgumentException(
                    "Admin registration is not allowed."
            );
        }

        // Create user
        User user = new User();

        user.setFullName(request.getFullName().trim());
        user.setEmail(email);
        user.setPhone(request.getPhone().trim());

        // Encrypt password before saving
        user.setPassword(
                passwordEncoder.encode(request.getPassword())
        );

        user.setRole(role);

        // New accounts must verify their email
        user.setEmailVerified(false);

        // Save user first to generate the database ID
        User savedUser = userRepository.save(user);

        // Generate token and send verification email
        emailVerificationService.createAndSendVerificationToken(
                savedUser
        );

        return savedUser;
    }
}
