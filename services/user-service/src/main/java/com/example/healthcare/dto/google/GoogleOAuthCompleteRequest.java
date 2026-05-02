package com.example.healthcare.dto.google;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleOAuthCompleteRequest {

    @NotBlank
    private String code;

    @NotBlank
    private String state;
}
