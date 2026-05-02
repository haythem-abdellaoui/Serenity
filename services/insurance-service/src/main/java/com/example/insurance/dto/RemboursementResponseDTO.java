package com.example.insurance.dto;

import lombok.*;

import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RemboursementResponseDTO {

    private Long id;
    private Double montant;
    private Date date;
    private String statut;
    private Long claimId;
}
