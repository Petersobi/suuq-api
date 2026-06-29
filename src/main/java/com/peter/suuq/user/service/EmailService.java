package com.peter.suuq.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base.url}")
    private String baseUrl;

    public void sendPasswordResetEmail(String toEmail, String token) {
        String resetLink = baseUrl + "/api/auth/reset-password?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Suuq - Password Reset Request");
        message.setText(
                "Hello,\n\n" +
                        "You requested a password reset for your Suuq account.\n\n" +
                        "Click the link below to reset your password (valid for 15 minutes):\n\n" +
                        resetLink + "\n\n" +
                        "If you did not request this, please ignore this email.\n\n" +
                        "The Suuq Team"
        );

        mailSender.send(message);
    }

    public void sendPasswordChangedEmail(String toEmail, String fullName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Suuq - Password Changed Successfully");
        message.setText(
                "Hello " + fullName + ",\n\n" +
                        "Your Suuq account password was changed successfully.\n\n" +
                        "If you did not make this change, please contact us immediately " +
                        "by replying to this email.\n\n" +
                        "The Suuq Team"
        );

        mailSender.send(message);
    }

    public void sendOrderConfirmationEmail(String toEmail, String fullName,
                                           Long orderId, String totalAmount) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Suuq - Order Confirmation #" + orderId);
        message.setText(
                "Hello " + fullName + ",\n\n" +
                        "Thank you for your order! Here's your order summary:\n\n" +
                        "Order ID: #" + orderId + "\n" +
                        "Total Amount: ₦" + totalAmount + "\n" +
                        "Status: PENDING\n\n" +
                        "We'll notify you as your order progresses.\n\n" +
                        "The Suuq Team"
        );

        mailSender.send(message);
    }
}