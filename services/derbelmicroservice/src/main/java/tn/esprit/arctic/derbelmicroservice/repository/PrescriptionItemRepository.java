package tn.esprit.arctic.derbelmicroservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import tn.esprit.arctic.derbelmicroservice.entity.PrescriptionItem;

import java.util.List;

@Repository
public interface PrescriptionItemRepository extends JpaRepository<PrescriptionItem, Long> {

    List<PrescriptionItem> findByPrescriptionId(Long prescriptionId);

    @Query("SELECT pi FROM PrescriptionItem pi WHERE pi.medicine.id = :medicineId")
    List<PrescriptionItem> findByMedicineId(@Param("medicineId") Long medicineId);

    // ── AI Recommendation Statistics ──

    // Total items recommended by AI
    long countByIsAiRecommended(Boolean isAiRecommended);

    // Top AI-recommended medicines (name + count)
    @Query("SELECT pi.medicine.name, COUNT(pi) FROM PrescriptionItem pi " +
           "WHERE pi.isAiRecommended = true " +
           "GROUP BY pi.medicine.name ORDER BY COUNT(pi) DESC")
    List<Object[]> findTopAiRecommendedMedicines();
}
