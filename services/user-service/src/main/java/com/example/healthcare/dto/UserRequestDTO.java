package com.example.healthcare.dto;

import com.example.healthcare.entity.InsuranceCompany;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequestDTO {

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    private String password;

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Pattern(regexp = "^$|^\\d{8}$", message = "Phone must be exactly 8 digits")
    private String phone;

    @Past(message = "Date of birth must be in the past")
    private Date dateOfBirth;

    private InsuranceCompany insuranceCompany;

    private String role;
}
