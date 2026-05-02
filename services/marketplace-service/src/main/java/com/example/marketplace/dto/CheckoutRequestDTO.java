package com.example.marketplace.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CheckoutRequestDTO {

    @Valid
    @NotEmpty
    private List<CheckoutItemDTO> items;

    @NotBlank
    @Size(max = 500)
    private String shippingAddress;

    @Size(max = 1000)
    private String customerNote;
}
