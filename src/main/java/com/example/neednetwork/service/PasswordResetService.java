
package com.example.neednetwork.service;

import com.example.neednetwork.entity.PasswordResetToken;
import com.example.neednetwork.entity.User;
import com.example.neednetwork.repository.PasswordResetTokenRepository;
import com.example.neednetwork.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService) {

        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // 1. Request password reset
    @Transactional
    public void requestPasswordReset(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email address is required."
            );
        }

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElse(null);

        // Do not reveal whether the account exists
        if (user == null) {
            return;
        }

        // Generate a secure random token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        // Store only the SHA-256 hash
        String tokenHash = hashToken(rawToken);

        // Reuse the existing token record if available
        PasswordResetToken resetToken =
                tokenRepository.findByUserId(user.getId())
                        .orElseGet(PasswordResetToken::new);

        resetToken.setUser(user);
        resetToken.setTokenHash(tokenHash);
        resetToken.setExpiresAt(
                LocalDateTime.now().plusMinutes(15)
        );
        resetToken.setUsedAt(null);

        tokenRepository.save(resetToken);

        // Send reset email
        emailService.sendPasswordResetEmail(user, rawToken);
    }

    // 2. Reset password
    @Transactional
    public void resetPassword(
            String rawToken,
            String newPassword) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Reset token is required."
            );
        }

        if (newPassword == null ||
                newPassword.length() < 8 ||
                newPassword.length() > 72) {

            throw new IllegalArgumentException(
                    "Password must contain between 8 and 72 characters."
            );
        }

        // Hash the received token
        String tokenHash = hashToken(rawToken);

        // Find the stored token
        PasswordResetToken resetToken =
                tokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid password reset link."
                                )
                        );

        // Check whether token was already used
        if (resetToken.getUsedAt() != null) {
            throw new IllegalArgumentException(
                    "This password reset link has already been used."
            );
        }

        // Check token expiry
        if (resetToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Password reset link has expired. Please request a new one."
            );
        }

        User user = resetToken.getUser();

        // Encode the new password using BCrypt
        user.setPassword(
                passwordEncoder.encode(newPassword)
        );

        userRepository.save(user);

        // Mark the reset token as used
        resetToken.setUsedAt(LocalDateTime.now());

        tokenRepository.save(resetToken);
    }

    // 3. Generate SHA-256 hash
    private String hashToken(String token) {

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash = digest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hash);

        } catch (Exception exception) {

            throw new IllegalStateException(
                    "Unable to process reset token.",
                    exception
            );
        }
    }
}
