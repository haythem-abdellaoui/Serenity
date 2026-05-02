package com.example.pharmacy.repository;

import com.example.pharmacy.entity.MedicineStockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface MedicineStockItemRepository extends JpaRepository<MedicineStockItem, Long> {

    @Query("""
        select m from MedicineStockItem m
        where m.archived = false
          and m.pharmacy.id in :pharmacyIds
          and lower(m.medicineName) in :medicineNames
        """)
    List<MedicineStockItem> findActiveByPharmacyIdsAndMedicineNames(
        @Param("pharmacyIds") Set<Long> pharmacyIds,
        @Param("medicineNames") Set<String> medicineNames
    );

    @Query("""
        select m from MedicineStockItem m
        join m.pharmacy p
        where p.ownerUserId = :ownerUserId
          and m.archived = :includeArchived
          and (:query is null or lower(m.medicineName) like lower(concat('%', :query, '%')))
        order by m.updatedAt desc
        """)
    List<MedicineStockItem> findByOwnerUserIdWithSearch(
        @Param("ownerUserId") Long ownerUserId,
        @Param("includeArchived") boolean includeArchived,
        @Param("query") String query
    );

    Optional<MedicineStockItem> findByIdAndPharmacyOwnerUserId(Long id, Long ownerUserId);

    void deleteByPharmacyId(Long pharmacyId);
}
