package tn.esprit.arctic.derbelmicroservice.service;

import tn.esprit.arctic.derbelmicroservice.dto.response.PrescriptionResponseDTO;

public interface IPdfGeneratorService {
    byte[] generatePrescriptionPdf(PrescriptionResponseDTO prescription) throws Exception;
}
