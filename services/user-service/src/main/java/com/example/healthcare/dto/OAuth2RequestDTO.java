package com.example.healthcare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OAuth2RequestDTO {

    @NotBlank(message = "Token is required")
    private String token;
}
