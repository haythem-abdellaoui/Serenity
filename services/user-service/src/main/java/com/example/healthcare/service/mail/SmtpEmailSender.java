package com.example.healthcare.service.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SmtpEmailSender implements EmailSender {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from:}")
    private String fromAddress;

    @Override
    public void send(String toEmail, String subject, String body) {
        if (!StringUtils.hasText(fromAddress)) {
            throw new IllegalStateException("Mail configuration is incomplete. Set app.mail.from or SMTP_USERNAME.");
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (RuntimeException ex) {
            throw new IllegalStateException("Failed to send OTP email. Please try again.", ex);
        }
    }
}
