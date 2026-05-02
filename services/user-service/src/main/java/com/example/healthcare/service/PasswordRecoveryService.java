package com.example.healthcare.service;

import com.example.healthcare.dto.ForgotPasswordRequestDTO;
import com.example.healthcare.dto.ForgotPasswordResetRequestDTO;
import com.example.healthcare.dto.ForgotPasswordVerifyRequestDTO;
import com.example.healthcare.dto.ForgotPasswordVerifyResponseDTO;
import com.example.healthcare.dto.MessageResponseDTO;

public interface PasswordRecoveryService {
    MessageResponseDTO requestOtp(ForgotPasswordRequestDTO request);
    ForgotPasswordVerifyResponseDTO verifyOtp(ForgotPasswordVerifyRequestDTO request);
    MessageResponseDTO resetPassword(ForgotPasswordResetRequestDTO request);
}
