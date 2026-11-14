
package com.example.neednetwork.controller;

import com.example.neednetwork.dto.LoginRequest;
import com.example.neednetwork.dto.LoginResponse;
import com.example.neednetwork.dto.RegisterRequest;
import com.example.neednetwork.entity.User;
import com.example.neednetwork.service.AuthService;
import com.example.neednetwork.service.LoginService;
import com.example.neednetwork.service.EmailVerificationService;
import com.example.neednetwork.service.PasswordResetService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {

    private final AuthService authService;
    private final LoginService loginService;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    public AuthController(
            AuthService authService,
            LoginService loginService,
            EmailVerificationService emailVerificationService,
            PasswordResetService passwordResetService) {

        this.authService = authService;
        this.loginService = loginService;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
    }

    // Registration API
    @PostMapping("/register")
    public ResponseEntity<?> register(
            @Valid @RequestBody RegisterRequest request) {

        try {
            User user = authService.register(request);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(Map.of(
                            "message",
                            "Registration successful! Please check your email to verify your account.",
                            "email",
                            user.getEmail()
                    ));

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    // Email verification API
    @GetMapping("/verify-email")
    public ResponseEntity<?> verifyEmail(
            @RequestParam String token) {

        try {
            emailVerificationService.verifyEmail(token);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Your email has been verified successfully. You can now log in."
                    )
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    // Resend verification email API
    @PostMapping("/resend-verification")
    public ResponseEntity<?> resendVerificationEmail(
            @RequestBody Map<String, String> request) {

        try {
            String email = request.get("email");

            emailVerificationService.resendVerificationEmail(email);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "A new verification email has been sent. Please check your inbox."
                    )
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    // Login API
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginRequest request) {

        try {
            LoginResponse response = loginService.login(request);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException ex) {

            String message = ex.getMessage();

            // Return 403 for inactive accounts and unverified emails
            if (message != null &&
                    (message.startsWith(
                            "Please verify your email")
                    || message.startsWith(
                            "Your account has been deactivated"))) {

                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of("message", message));
            }

            // Return 401 for invalid credentials
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            message != null
                                    ? message
                                    : "Invalid email or password."
                    ));
        }
    }

    // Forgot password API
    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(
            @RequestBody Map<String, String> request) {

        try {
            String email = request.get("email");

            passwordResetService.requestPasswordReset(email);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "If an account exists with that email, a password reset link will be sent."
                    )
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }

    // Reset password API
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(
            @RequestBody Map<String, String> request) {

        try {
            String token = request.get("token");
            String newPassword = request.get("newPassword");

            passwordResetService.resetPassword(token, newPassword);

            return ResponseEntity.ok(
                    Map.of(
                            "message",
                            "Your password has been reset successfully. You can now log in."
                    )
            );

        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("message", ex.getMessage()));
        }
    }
}
