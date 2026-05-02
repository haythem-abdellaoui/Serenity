package com.example.marketplace.service.impl;

import com.example.marketplace.dto.CouponDTO;
import com.example.marketplace.entity.Coupon;
import com.example.marketplace.exception.ResourceNotFoundException;
import com.example.marketplace.repository.CouponRepository;
import com.example.marketplace.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    @Override
    @Transactional(readOnly = true)
    public CouponDTO validateCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));

        if (!coupon.isValid()) {
            throw new IllegalArgumentException("Coupon is no longer valid");
        }

        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Order amount must be at least " + coupon.getMinOrderAmount());
        }

        BigDecimal discount = coupon.calculateDiscount(orderAmount);
        return mapToDTO(coupon, discount);
    }

    @Override
    public void applyCoupon(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));

        if (!coupon.isValid()) {
            throw new IllegalArgumentException("Coupon is no longer valid");
        }

        coupon.setUsageCount(coupon.getUsageCount() + 1);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional(readOnly = true)
    public CouponDTO getCouponByCode(String code) {
        Coupon coupon = couponRepository.findByCode(code.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found: " + code));
        return mapToDTO(coupon, BigDecimal.ZERO);
    }

    private CouponDTO mapToDTO(Coupon coupon, BigDecimal discountAmount) {
        return CouponDTO.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountPercentage(coupon.getDiscountPercentage())
                .minOrderAmount(coupon.getMinOrderAmount())
                .discountAmount(discountAmount)
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .isValid(coupon.isValid())
                .build();
    }
}
