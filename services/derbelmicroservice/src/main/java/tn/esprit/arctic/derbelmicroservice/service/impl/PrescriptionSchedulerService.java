package tn.esprit.arctic.derbelmicroservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.arctic.derbelmicroservice.entity.Prescription;
import tn.esprit.arctic.derbelmicroservice.entity.PrescriptionItem;
import tn.esprit.arctic.derbelmicroservice.repository.PrescriptionRepository;

import java.time.LocalDate;
import java.util.List;

/**
 * Scheduler qui vérifie automatiquement les prescriptions actives.
 * Si tous les médicaments d'une prescription ont dépassé leur date de fin,
 * le statut est automatiquement mis à jour en "COMPLETED".
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PrescriptionSchedulerService {

    private final PrescriptionRepository prescriptionRepository;

    
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void autoCompletePrescriptions() {
        log.info("⏰ [SCHEDULER] Vérification des prescriptions expirées...");

        List<Prescription> activePrescriptions = prescriptionRepository.findByStatusWithItems("ACTIVE");
        int completedCount = 0;

        for (Prescription prescription : activePrescriptions) {
            if (prescription.getItems().isEmpty()) {
                continue;
            }

            // Trouver la date de fin la plus lointaine parmi tous les items
            LocalDate latestEndDate = prescription.getItems().stream()
                    .map(PrescriptionItem::getEndDate)
                    .filter(date -> date != null)
                    .max(LocalDate::compareTo)
                    .orElse(null);

            // Si tous les items ont une date de fin ET elle est dépassée → COMPLETED
            if (latestEndDate != null && latestEndDate.isBefore(LocalDate.now())) {
                prescription.setStatus("COMPLETED");
                prescriptionRepository.save(prescription);
                completedCount++;
                log.info("✅ [SCHEDULER] Clôture automatique de la prescription ID: {} (date fin: {})",
                        prescription.getId(), latestEndDate);
            }
        }

        if (completedCount > 0) {
            log.info("📊 [SCHEDULER] {} prescription(s) clôturée(s) automatiquement.", completedCount);
        } else {
            log.info("📊 [SCHEDULER] Aucune prescription à clôturer.");
        }
    }
}
