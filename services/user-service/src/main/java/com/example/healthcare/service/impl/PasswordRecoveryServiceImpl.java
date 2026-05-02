package com.example.healthcare.service.impl;

import com.example.healthcare.dto.ForgotPasswordRequestDTO;
import com.example.healthcare.dto.ForgotPasswordResetRequestDTO;
import com.example.healthcare.dto.ForgotPasswordVerifyRequestDTO;
import com.example.healthcare.dto.ForgotPasswordVerifyResponseDTO;
import com.example.healthcare.dto.MessageResponseDTO;
import com.example.healthcare.entity.AuthProvider;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.service.PasswordRecoveryService;
import com.example.healthcare.service.mail.EmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Transactional
public class PasswordRecoveryServiceImpl implements PasswordRecoveryService {

    private static final String GENERIC_REQUEST_MESSAGE = "If your account is eligible, an OTP has been sent to your email address.";
    private static final String OTP_INVALID_MESSAGE = "Invalid or expired OTP.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;
    private final EmailSender emailSender;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.password-reset.otp-length:6}")
    private int otpLength;

    @Value("${app.password-reset.otp-ttl-seconds:300}")
    private long otpTtlSeconds;

    @Value("${app.password-reset.otp-max-attempts:5}")
    private int otpMaxAttempts;

    @Value("${app.password-reset.reset-token-ttl-seconds:900}")
    private long resetTokenTtlSeconds;

    @Override
    public MessageResponseDTO requestOtp(ForgotPasswordRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        Optional<User> userOpt = userRepository.findByEmail(normalizedEmail);
        if (userOpt.isEmpty() || !isEligibleForRecovery(userOpt.get())) {
            return MessageResponseDTO.builder().message(GENERIC_REQUEST_MESSAGE).build();
        }

        User user = userOpt.get();
        String otp = generateNumericOtp();
        String otpKey = otpKey(normalizedEmail);
        String attemptsKey = otpAttemptsKey(normalizedEmail);

        redisTemplate.opsForValue().set(otpKey, hash(otp), Duration.ofSeconds(otpTtlSeconds));
        redisTemplate.opsForValue().set(attemptsKey, "0", Duration.ofSeconds(otpTtlSeconds));

        try {
            emailSender.send(user.getEmail(), "Serenity OTP Code", buildOtpMessage(otp));
        } catch (RuntimeException ex) {
            redisTemplate.delete(List.of(otpKey, attemptsKey));
            throw ex;
        }

        return MessageResponseDTO.builder().message(GENERIC_REQUEST_MESSAGE).build();
    }

    @Override
    public ForgotPasswordVerifyResponseDTO verifyOtp(ForgotPasswordVerifyRequestDTO request) {
        String normalizedEmail = normalizeEmail(request.getEmail());
        User user = userRepository.findByEmail(normalizedEmail)
                .filter(this::isEligibleForRecovery)
                .orElseThrow(() -> new IllegalStateException(OTP_INVALID_MESSAGE));

        String otpKey = otpKey(normalizedEmail);
        String attemptsKey = otpAttemptsKey(normalizedEmail);
        String storedOtpHash = redisTemplate.opsForValue().get(otpKey);
        if (!StringUtils.hasText(storedOtpHash)) {
            throw new IllegalStateException(OTP_INVALID_MESSAGE);
        }

        String submittedOtpHash = hash(request.getOtp().trim());
        if (!storedOtpHash.equals(submittedOtpHash)) {
            int attempts = parseInt(redisTemplate.opsForValue().get(attemptsKey)) + 1;
            if (attempts >= otpMaxAttempts) {
                redisTemplate.delete(List.of(otpKey, attemptsKey));
                throw new IllegalStateException("Too many incorrect OTP attempts. Please request a new OTP.");
            }

            long ttl = safeTtlSeconds(otpKey, otpTtlSeconds);
            redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts), Duration.ofSeconds(ttl));
            throw new IllegalStateException(OTP_INVALID_MESSAGE);
        }

        redisTemplate.delete(List.of(otpKey, attemptsKey));

        String resetToken = generateResetToken();
        redisTemplate.opsForValue().set(resetTokenKey(resetToken), user.getEmail(), Duration.ofSeconds(resetTokenTtlSeconds));

        return ForgotPasswordVerifyResponseDTO.builder()
                .message("OTP verified successfully.")
                .resetToken(resetToken)
                .build();
    }

    @Override
    public MessageResponseDTO resetPassword(ForgotPasswordResetRequestDTO request) {
        String token = request.getToken().trim();
        String resetKey = resetTokenKey(token);
        String email = redisTemplate.opsForValue().get(resetKey);
        if (!StringUtils.hasText(email)) {
            throw new IllegalStateException("Reset token has expired or is invalid.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Unable to reset password for this account."));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new IllegalStateException("Password reset is available only for local accounts.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisTemplate.delete(resetKey);

        return MessageResponseDTO.builder()
                .message("Password reset successful.")
                .build();
    }

    private boolean isEligibleForRecovery(User user) {
        return user.getAuthProvider() == AuthProvider.LOCAL
                && Boolean.TRUE.equals(user.getIsActive());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private String otpKey(String email) {
        return "pwd:otp:" + email;
    }

    private String otpAttemptsKey(String email) {
        return "pwd:otp:attempts:" + email;
    }

    private String resetTokenKey(String token) {
        return "pwd:reset:" + token;
    }

    private int parseInt(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private long safeTtlSeconds(String key, long fallbackSeconds) {
        Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        if (ttl == null || ttl <= 0) {
            return fallbackSeconds;
        }
        return ttl;
    }

    private String generateNumericOtp() {
        int safeLength = Math.max(4, Math.min(otpLength, 8));
        int min = (int) Math.pow(10, safeLength - 1);
        int max = (int) Math.pow(10, safeLength) - 1;
        int value = secureRandom.nextInt(max - min + 1) + min;
        return String.format("%0" + safeLength + "d", value);
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String buildOtpMessage(String otp) {
        return "Your Serenity OTP is " + otp + ". It expires in " + (otpTtlSeconds / 60) + " minutes.";
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to hash sensitive data.");
        }
    }
}
