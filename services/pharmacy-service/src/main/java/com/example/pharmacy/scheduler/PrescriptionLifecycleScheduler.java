package com.example.pharmacy.scheduler;

import com.example.pharmacy.entity.PharmacyPrescription;
import com.example.pharmacy.entity.PrescriptionStatus;
import com.example.pharmacy.repository.PharmacyPrescriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PrescriptionLifecycleScheduler {

    private final PharmacyPrescriptionRepository pharmacyPrescriptionRepository;

    // Runs every day at 03:00.
    @Scheduled(cron = "0 0 3 * * *", zone = "Africa/Tunis")
    @Transactional
    public void expireOldReadyForPickupPrescriptions() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(72);
        List<PharmacyPrescription> rows = pharmacyPrescriptionRepository
            .findByStatusAndReadyAtBefore(PrescriptionStatus.READY_FOR_PICKUP, cutoff);

        for (PharmacyPrescription row : rows) {
            row.setStatus(PrescriptionStatus.EXPIRED);
        }

        pharmacyPrescriptionRepository.saveAll(rows);
        log.info("Prescription scheduler expired {} rows", rows.size());
    }
}
