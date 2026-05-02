package com.example.healthcare.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class LookupIdsRequest {

    @NotNull
    @Size(max = 100)
    private List<Long> ids;
}
