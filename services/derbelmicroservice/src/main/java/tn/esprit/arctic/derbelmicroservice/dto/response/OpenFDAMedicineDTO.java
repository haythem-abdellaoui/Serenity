package tn.esprit.arctic.derbelmicroservice.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenFDAMedicineDTO {
    private String name;
    private String description;
    private String sideEffects;
}
