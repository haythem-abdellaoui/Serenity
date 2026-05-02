package com.example.marketplace.service;

import com.example.marketplace.dto.CouponDTO;

import java.math.BigDecimal;

public interface CouponService {
    CouponDTO validateCoupon(String code, BigDecimal orderAmount);
    void applyCoupon(String code);
    CouponDTO getCouponByCode(String code);
}
