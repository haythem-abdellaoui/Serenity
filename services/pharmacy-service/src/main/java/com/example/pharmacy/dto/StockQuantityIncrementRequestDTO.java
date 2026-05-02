package com.example.pharmacy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockQuantityIncrementRequestDTO {

    @NotNull(message = "Increment value is required")
    @Min(value = 1, message = "Increment value must be at least 1")
    private Integer incrementBy;
}
