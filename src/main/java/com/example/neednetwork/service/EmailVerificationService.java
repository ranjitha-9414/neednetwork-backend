
package com.example.neednetwork.service;

import com.example.neednetwork.entity.EmailVerificationToken;
import com.example.neednetwork.entity.User;
import com.example.neednetwork.repository.EmailVerificationTokenRepository;
import com.example.neednetwork.repository.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final UserRepository userRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public EmailVerificationService(
            EmailVerificationTokenRepository tokenRepository,
            EmailService emailService,
            UserRepository userRepository) {

        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    // Generate token and send verification email
    @Transactional
    public void createAndSendVerificationToken(User user) {

        // Generate a secure random token
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        // Store only the hashed token in the database
        String tokenHash = hashToken(rawToken);

        // Find the existing token for this user.
        // If no token exists, create a new record.
        EmailVerificationToken verificationToken =
                tokenRepository.findByUserId(user.getId())
                        .orElseGet(EmailVerificationToken::new);

        // Set user details and update token information
        verificationToken.setUser(user);

        verificationToken.setTokenHash(tokenHash);

        verificationToken.setExpiresAt(
                LocalDateTime.now().plusMinutes(30)
        );

        // Reset used status when generating a new token
        verificationToken.setUsedAt(null);

        // Save new token or update existing token
        tokenRepository.save(verificationToken);

        // Send original token through email
        emailService.sendVerificationEmail(user, rawToken);
    }

    // Verify email
    @Transactional
    public void verifyEmail(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "Verification token is required."
            );
        }

        String tokenHash = hashToken(rawToken);

        EmailVerificationToken verificationToken =
                tokenRepository.findByTokenHash(tokenHash)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Invalid verification link."
                                )
                        );

        User user = verificationToken.getUser();

        // Already verified
        if (user.isEmailVerified()) {
            return;
        }

        // Check whether token was used
        if (verificationToken.getUsedAt() != null) {
            throw new IllegalArgumentException(
                    "This verification link has already been used."
            );
        }

        // Check expiry
        if (verificationToken.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new IllegalArgumentException(
                    "Verification link has expired. Please request a new one."
            );
        }

        // Mark email as verified
        user.setEmailVerified(true);

        // Mark token as used
        verificationToken.setUsedAt(LocalDateTime.now());
    }

    // Resend verification email
    @Transactional
    public void resendVerificationEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "Email address is required."
            );
        }

        String normalizedEmail = email.trim()
                .toLowerCase(Locale.ROOT);

        User user = userRepository.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "If an account exists, a verification email will be sent."
                        )
                );

        // Do not resend if email is already verified
        if (user.isEmailVerified()) {
            throw new IllegalArgumentException(
                    "Your email is already verified. Please log in."
            );
        }

        // Generate a fresh token and send a new email
        createAndSendVerificationToken(user);
    }

    // SHA-256 token hashing
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
                    "Unable to process verification token.",
                    exception
            );
        }
    }
}
