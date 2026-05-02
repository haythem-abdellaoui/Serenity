package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineResponseDTO {

    private Long id;
    private String name;
    private String description;
    private String sideEffects;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
