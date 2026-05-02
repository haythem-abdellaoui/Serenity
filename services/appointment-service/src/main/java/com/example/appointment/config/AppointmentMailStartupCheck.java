package com.example.appointment.config;

import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Appointment emails use <strong>SMTP</strong> (JavaMail), not the Gmail REST API.
 * Validates configuration at startup so missing app passwords or wrong host show up in logs immediately.
 */
@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class AppointmentMailStartupCheck implements ApplicationRunner {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Appointment mail uses SMTP (JavaMail), not the Gmail API. Host={}, user configured={}",
                StringUtils.hasText(mailHost) ? mailHost : "(none)",
                StringUtils.hasText(mailUsername));

        if (!StringUtils.hasText(mailUsername)) {
            log.warn("spring.mail.username / MAIL_USERNAME is not set — booking, confirm, and reminder emails will not be sent.");
            return;
        }
        if (!StringUtils.hasText(mailPassword)) {
            log.warn("spring.mail.password / MAIL_PASSWORD is not set — SMTP login will fail until you set the env var.");
            return;
        }

        JavaMailSender sender = mailSenderProvider.getIfAvailable();
        if (sender == null) {
            log.warn("JavaMailSender bean is missing — check spring.mail.host (default smtp.gmail.com) and spring-boot-starter-mail.");
            return;
        }
        if (sender instanceof JavaMailSenderImpl impl) {
            try {
                impl.testConnection();
                log.info("SMTP test connection OK (host={}, port={}, user={})", impl.getHost(), impl.getPort(), impl.getUsername());
            } catch (MessagingException e) {
                log.error("SMTP test connection FAILED — no appointment emails will work until this is fixed: {}", e.getMessage(), e);
            }
        }
    }
}
