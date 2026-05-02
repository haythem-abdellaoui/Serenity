package com.example.healthcare.dto;

import com.example.healthcare.entity.InsuranceCompany;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileUpdateDTO {

    @Size(min = 2, max = 50)
    private String firstName;

    @Size(min = 2, max = 50)
    private String lastName;

    @Pattern(regexp = "^$|^\\d{8}$", message = "Phone must be exactly 8 digits")
    private String phone;

    private Date dateOfBirth;

    private InsuranceCompany insuranceCompany;

    @Size(max = 1000)
    private String bio;

    private String avatar;

    private String preferredLanguage;

    private Boolean isAnonymous;
}
