package com.example.healthcare.controller;

import com.example.healthcare.dto.AuthResponseDTO;
import com.example.healthcare.dto.ForgotPasswordRequestDTO;
import com.example.healthcare.dto.ForgotPasswordResetRequestDTO;
import com.example.healthcare.dto.ForgotPasswordVerifyRequestDTO;
import com.example.healthcare.dto.ForgotPasswordVerifyResponseDTO;
import com.example.healthcare.dto.LoginRequestDTO;
import com.example.healthcare.dto.MessageResponseDTO;
import com.example.healthcare.dto.OAuth2RequestDTO;
import com.example.healthcare.dto.UserRequestDTO;
import com.example.healthcare.service.OAuth2AuthService;
import com.example.healthcare.service.PasswordRecoveryService;
import com.example.healthcare.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final OAuth2AuthService oAuth2AuthService;
    private final PasswordRecoveryService passwordRecoveryService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@Valid @RequestBody UserRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.registerUser(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(userService.login(request));
    }

    @PostMapping("/oauth2/google")
    public ResponseEntity<AuthResponseDTO> loginWithGoogle(@Valid @RequestBody OAuth2RequestDTO request) {
        return ResponseEntity.ok(oAuth2AuthService.loginWithGoogle(request.getToken()));
    }

    @PostMapping("/oauth2/facebook")
    public ResponseEntity<AuthResponseDTO> loginWithFacebook(@Valid @RequestBody OAuth2RequestDTO request) {
        return ResponseEntity.ok(oAuth2AuthService.loginWithFacebook(request.getToken()));
    }

    @PostMapping("/password/forgot/request")
    public ResponseEntity<MessageResponseDTO> requestForgotPasswordOtp(@Valid @RequestBody ForgotPasswordRequestDTO request) {
        return ResponseEntity.ok(passwordRecoveryService.requestOtp(request));
    }

    @PostMapping("/password/forgot/verify")
    public ResponseEntity<ForgotPasswordVerifyResponseDTO> verifyForgotPasswordOtp(
            @Valid @RequestBody ForgotPasswordVerifyRequestDTO request) {
        return ResponseEntity.ok(passwordRecoveryService.verifyOtp(request));
    }

    @PostMapping("/password/forgot/reset")
    public ResponseEntity<MessageResponseDTO> resetForgottenPassword(@Valid @RequestBody ForgotPasswordResetRequestDTO request) {
        return ResponseEntity.ok(passwordRecoveryService.resetPassword(request));
    }
}
