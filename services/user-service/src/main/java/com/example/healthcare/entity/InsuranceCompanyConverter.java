package com.example.healthcare.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class InsuranceCompanyConverter implements AttributeConverter<InsuranceCompany, String> {

    @Override
    public String convertToDatabaseColumn(InsuranceCompany attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public InsuranceCompany convertToEntityAttribute(String dbData) {
        return InsuranceCompany.fromJson(dbData);
    }
}
