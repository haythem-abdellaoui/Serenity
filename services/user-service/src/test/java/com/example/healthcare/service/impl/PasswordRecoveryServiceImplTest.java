package com.example.healthcare.service.impl;

import com.example.healthcare.dto.ForgotPasswordRequestDTO;
import com.example.healthcare.dto.ForgotPasswordResetRequestDTO;
import com.example.healthcare.dto.ForgotPasswordVerifyRequestDTO;
import com.example.healthcare.dto.ForgotPasswordVerifyResponseDTO;
import com.example.healthcare.entity.AuthProvider;
import com.example.healthcare.entity.User;
import com.example.healthcare.repository.UserRepository;
import com.example.healthcare.service.mail.EmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private EmailSender emailSender;
    @Mock private ValueOperations<String, String> valueOps;

    @InjectMocks private PasswordRecoveryServiceImpl service;

    private void setConfig(int otpLength, long otpTtlSeconds, int otpMaxAttempts, long resetTokenTtlSeconds) {
        ReflectionTestUtils.setField(service, "otpLength", otpLength);
        ReflectionTestUtils.setField(service, "otpTtlSeconds", otpTtlSeconds);
        ReflectionTestUtils.setField(service, "otpMaxAttempts", otpMaxAttempts);
        ReflectionTestUtils.setField(service, "resetTokenTtlSeconds", resetTokenTtlSeconds);
    }

    @Test
    void requestOtp_whenUserMissing_returnsGenericMessageAndDoesNothing() {
        setConfig(6, 300, 5, 900);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.empty());

        ForgotPasswordRequestDTO req = new ForgotPasswordRequestDTO();
        req.setEmail("a@b.com");

        var resp = service.requestOtp(req);
        assertThat(resp.getMessage()).contains("If your account is eligible");

        verifyNoInteractions(redisTemplate, emailSender);
    }

    @Test
    void requestOtp_whenEligible_persistsOtpAndSendsEmail() {
        setConfig(6, 300, 5, 900);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        User user = new User();
        user.setEmail("a@b.com");
        user.setIsActive(true);
        user.setAuthProvider(AuthProvider.LOCAL);

        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        ForgotPasswordRequestDTO req = new ForgotPasswordRequestDTO();
        req.setEmail("a@b.com");

        service.requestOtp(req);

        verify(valueOps, times(2)).set(anyString(), anyString(), any(Duration.class));
        verify(emailSender).send(eq("a@b.com"), eq("Serenity OTP Code"), contains("Your Serenity OTP is"));
    }

    @Test
    void requestOtp_whenEmailSenderFails_cleansRedisKeys() {
        setConfig(6, 300, 5, 900);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        User user = new User();
        user.setEmail("a@b.com");
        user.setIsActive(true);
        user.setAuthProvider(AuthProvider.LOCAL);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        doThrow(new RuntimeException("smtp down"))
                .when(emailSender)
                .send(eq("a@b.com"), anyString(), anyString());

        ForgotPasswordRequestDTO req = new ForgotPasswordRequestDTO();
        req.setEmail("a@b.com");

        assertThatThrownBy(() -> service.requestOtp(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("smtp down");

        verify(redisTemplate).delete(org.mockito.ArgumentMatchers.<List<String>>argThat(
                keys -> keys.contains("pwd:otp:a@b.com") && keys.contains("pwd:otp:attempts:a@b.com")
        ));
    }

    @Test
    void verifyOtp_whenMissingStoredOtp_throws() {
        setConfig(6, 300, 5, 900);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        User user = new User();
        user.setEmail("a@b.com");
        user.setIsActive(true);
        user.setAuthProvider(AuthProvider.LOCAL);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        when(valueOps.get("pwd:otp:a@b.com")).thenReturn(null);

        ForgotPasswordVerifyRequestDTO req = new ForgotPasswordVerifyRequestDTO();
        req.setEmail("a@b.com");
        req.setOtp("123456");

        assertThatThrownBy(() -> service.verifyOtp(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid or expired OTP");
    }

    @Test
    void verifyOtp_whenWrongOtp_incrementsAttemptsAndKeepsTtl() {
        setConfig(6, 300, 5, 900);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        User user = new User();
        user.setEmail("a@b.com");
        user.setIsActive(true);
        user.setAuthProvider(AuthProvider.LOCAL);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        // Stored OTP hash won't match submitted OTP
        when(valueOps.get("pwd:otp:a@b.com")).thenReturn("SOME_OTHER_HASH");
        when(valueOps.get("pwd:otp:attempts:a@b.com")).thenReturn("0");
        when(redisTemplate.getExpire("pwd:otp:a@b.com", TimeUnit.SECONDS)).thenReturn(120L);

        ForgotPasswordVerifyRequestDTO req = new ForgotPasswordVerifyRequestDTO();
        req.setEmail("a@b.com");
        req.setOtp("123456");

        assertThatThrownBy(() -> service.verifyOtp(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid or expired OTP");

        verify(valueOps).set(eq("pwd:otp:attempts:a@b.com"), eq("1"), eq(Duration.ofSeconds(120)));
    }

    @Test
    void verifyOtp_whenTooManyAttempts_deletesKeysAndThrows() {
        setConfig(6, 300, 2, 900);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        User user = new User();
        user.setEmail("a@b.com");
        user.setIsActive(true);
        user.setAuthProvider(AuthProvider.LOCAL);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        when(valueOps.get("pwd:otp:a@b.com")).thenReturn("SOME_OTHER_HASH");
        when(valueOps.get("pwd:otp:attempts:a@b.com")).thenReturn("1");

        ForgotPasswordVerifyRequestDTO req = new ForgotPasswordVerifyRequestDTO();
        req.setEmail("a@b.com");
        req.setOtp("123456");

        assertThatThrownBy(() -> service.verifyOtp(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Too many incorrect OTP attempts");

        verify(redisTemplate).delete(org.mockito.ArgumentMatchers.<List<String>>argThat(
                keys -> keys.contains("pwd:otp:a@b.com") && keys.contains("pwd:otp:attempts:a@b.com")
        ));
    }

    @Test
    void resetPassword_whenTokenMissing_throws() {
        setConfig(6, 300, 5, 900);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("pwd:reset:token")).thenReturn(null);

        ForgotPasswordResetRequestDTO req = new ForgotPasswordResetRequestDTO();
        req.setToken("token");
        req.setNewPassword("pw");

        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("expired or is invalid");
    }

    @Test
    void resetPassword_whenUserNotLocal_throws() {
        setConfig(6, 300, 5, 900);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("pwd:reset:token")).thenReturn("a@b.com");

        User user = new User();
        user.setEmail("a@b.com");
        user.setAuthProvider(AuthProvider.GOOGLE);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));

        ForgotPasswordResetRequestDTO req = new ForgotPasswordResetRequestDTO();
        req.setToken("token");
        req.setNewPassword("pw");

        assertThatThrownBy(() -> service.resetPassword(req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("local accounts");
    }

    @Test
    void resetPassword_whenValid_updatesPasswordAndDeletesToken() {
        setConfig(6, 300, 5, 900);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get("pwd:reset:token")).thenReturn("a@b.com");

        User user = new User();
        user.setEmail("a@b.com");
        user.setAuthProvider(AuthProvider.LOCAL);
        when(userRepository.findByEmail("a@b.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("pw")).thenReturn("ENC");

        ForgotPasswordResetRequestDTO req = new ForgotPasswordResetRequestDTO();
        req.setToken("token");
        req.setNewPassword("pw");

        var resp = service.resetPassword(req);
        assertThat(resp.getMessage()).contains("Password reset successful");

        verify(userRepository).save(user);
        verify(redisTemplate).delete("pwd:reset:token");
        assertThat(user.getPassword()).isEqualTo("ENC");
    }
}

