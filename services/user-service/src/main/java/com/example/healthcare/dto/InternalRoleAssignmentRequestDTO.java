package com.example.healthcare.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternalRoleAssignmentRequestDTO {

    @NotBlank(message = "Role is required")
    private String role;
}
