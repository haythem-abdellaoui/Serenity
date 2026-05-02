package com.example.appointment.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Sends appointment emails via <strong>SMTP</strong> ({@link JavaMailSender}), not the Gmail REST API.
 * Requires {@code spring.mail.username}, {@code spring.mail.password} (Gmail: app password), and host (default smtp.gmail.com).
 */
@Slf4j
@Service
public class AppointmentMailService {

    private static final AtomicBoolean LOGGED_MISSING_FROM = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_MISSING_SENDER = new AtomicBoolean(false);

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public AppointmentMailService(ObjectProvider<JavaMailSender> mailSender) {
        this.mailSender = mailSender;
    }

    public void sendReminderEmail(String toEmail, String subject, String body) {
        if (!StringUtils.hasText(toEmail)) {
            return;
        }
        if (!StringUtils.hasText(fromAddress)) {
            if (LOGGED_MISSING_FROM.compareAndSet(false, true)) {
                log.warn("spring.mail.username / MAIL_USERNAME is not set — appointment emails will not be sent. "
                        + "Also set MAIL_HOST and MAIL_PASSWORD (e.g. Gmail SMTP + app password).");
            }
            return;
        }
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null) {
            if (LOGGED_MISSING_SENDER.compareAndSet(false, true)) {
                log.warn("JavaMailSender is not available — set MAIL_HOST (e.g. smtp.gmail.com) and mail auth "
                        + "so Spring can create the mail sender.");
            }
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromAddress);
            msg.setTo(toEmail);
            msg.setSubject(subject);
            msg.setText(body);
            sender.send(msg);
            log.info("Sent appointment email to {}", toEmail);
        } catch (Exception e) {
            log.warn("Failed to send appointment email to {}: {}", toEmail, e.getMessage(), e);
        }
    }
}
