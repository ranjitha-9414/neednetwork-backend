
package com.example.neednetwork.service;

import com.example.neednetwork.entity.User;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${app.base-url:http://localhost:5173}")
    private String baseUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // Email verification
    public void sendVerificationEmail(User user, String token) {

        String verificationLink =
                baseUrl + "/verify-email?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject("Verify Your NeedNetwork Account");

        message.setText(
                "Hello " + user.getFullName() + ",\n\n" +
                "Welcome to NeedNetwork!\n\n" +
                "Please verify your email address using this link:\n" +
                verificationLink + "\n\n" +
                "This link expires in 30 minutes.\n\n" +
                "If you did not create this account, please ignore this email.\n\n" +
                "Regards,\nNeedNetwork Team"
        );

        mailSender.send(message);
    }

    // Password reset email
    public void sendPasswordResetEmail(User user, String token) {

        String resetLink =
                baseUrl + "/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(user.getEmail());
        message.setSubject("Reset Your NeedNetwork Password");

        message.setText(
                "Hello " + user.getFullName() + ",\n\n" +
                "We received a request to reset your NeedNetwork password.\n\n" +
                "Click the link below to create a new password:\n" +
                resetLink + "\n\n" +
                "This link expires in 15 minutes and can only be used once.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Regards,\nNeedNetwork Team"
        );

        mailSender.send(message);
    }
}
