package com.example.pharmacy.repository;

import com.example.pharmacy.entity.Pharmacy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PharmacyRepository extends JpaRepository<Pharmacy, Long> {
    Optional<Pharmacy> findByOwnerUserId(Long ownerUserId);
    List<Pharmacy> findAllByOrderByNameAsc();
    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);

    @Query("""
        select p from Pharmacy p
        where p.latitude is not null
          and p.longitude is not null
        order by p.name asc
        """)
    List<Pharmacy> findAllWithCoordinates();
}
