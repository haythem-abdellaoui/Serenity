package com.example.pharmacy.repository;

import com.example.pharmacy.entity.StockConsumptionEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockConsumptionEventRepository extends JpaRepository<StockConsumptionEvent, Long> {
}
