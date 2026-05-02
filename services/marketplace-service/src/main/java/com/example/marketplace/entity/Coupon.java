package com.example.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "article_coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private Integer discountPercentage; // 0-100

    @Column(nullable = false)
    private BigDecimal minOrderAmount; // Minimum order amount to apply coupon

    @Column(nullable = false)
    private Integer maxUsageCount;

    @Column(nullable = false)
    private Integer usageCount;

    @Column(nullable = false)
    private LocalDateTime validFrom;

    @Column(nullable = false)
    private LocalDateTime validUntil;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.usageCount == null) {
            this.usageCount = 0;
        }
        if (this.active == null) {
            this.active = true;
        }
    }

    public Boolean isValid() {
        LocalDateTime now = LocalDateTime.now();
        return this.active &&
                now.isAfter(this.validFrom) &&
                now.isBefore(this.validUntil) &&
                this.usageCount < this.maxUsageCount;
    }

    public BigDecimal calculateDiscount(BigDecimal orderAmount) {
        if (!isValid() || orderAmount.compareTo(this.minOrderAmount) < 0) {
            return BigDecimal.ZERO;
        }
        return orderAmount.multiply(BigDecimal.valueOf(this.discountPercentage))
                .divide(BigDecimal.valueOf(100));
    }
}
