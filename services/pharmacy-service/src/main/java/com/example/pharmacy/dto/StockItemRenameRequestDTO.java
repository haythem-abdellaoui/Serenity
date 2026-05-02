 package com.example.pharmacy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockItemRenameRequestDTO {

    @NotBlank
    @Size(min = 2, max = 80)
    private String medicineName;
}
