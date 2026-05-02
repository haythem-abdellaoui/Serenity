package com.example.appointment.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserLookupSnippet {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
}
