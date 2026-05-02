package com.example.pharmacy.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "stock_consumption_events",
    indexes = {
        @Index(name = "idx_stock_consumption_pharmacy_medicine_event", columnList = "pharmacy_id, medicine_name, event_at"),
        @Index(name = "idx_stock_consumption_source", columnList = "source_prescription_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockConsumptionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "pharmacy_id", nullable = false)
    private Pharmacy pharmacy;

    @Column(name = "medicine_name", nullable = false)
    private String medicineName;

    @Column(name = "consumed_qty", nullable = false)
    private Integer consumedQty;

    @Column(name = "event_at", nullable = false)
    private LocalDateTime eventAt;

    @Column(name = "source_prescription_id", nullable = false)
    private Long sourcePrescriptionId;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
