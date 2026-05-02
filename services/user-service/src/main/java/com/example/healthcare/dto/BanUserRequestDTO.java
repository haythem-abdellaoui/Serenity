package com.example.healthcare.dto;

import com.example.healthcare.entity.BanDuration;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BanUserRequestDTO {

    @NotNull(message = "Ban duration is required")
    private BanDuration duration;
}
