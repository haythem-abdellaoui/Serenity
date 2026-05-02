package com.example.healthcare.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum InsuranceCompany {
    INSURANCE_1("Insurance 1"),
    INSURANCE_2("Insurance 2"),
    INSURANCE_3("Insurance 3"),
    INSURANCE_4("Insurance 4"),
    INSURANCE_5("Insurance 5");

    private final String label;

    InsuranceCompany(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static InsuranceCompany fromJson(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(values())
                .filter(c -> c.name().equalsIgnoreCase(value) || c.label.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid insurance company: " + value));
    }
}
