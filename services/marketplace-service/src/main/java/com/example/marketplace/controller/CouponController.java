package com.example.marketplace.controller;

import com.example.marketplace.dto.CouponDTO;
import com.example.marketplace.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping({"/api/articles/coupons", "/api/marketplace/coupons"})
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @GetMapping("/validate")
    public ResponseEntity<CouponDTO> validateCoupon(
            @RequestParam String code,
            @RequestParam BigDecimal orderAmount) {
        CouponDTO coupon = couponService.validateCoupon(code, orderAmount);
        return ResponseEntity.ok(coupon);
    }

    @PostMapping("/apply")
    public ResponseEntity<Void> applyCoupon(@RequestParam String code) {
        couponService.applyCoupon(code);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{code}")
    public ResponseEntity<CouponDTO> getCoupon(@PathVariable String code) {
        CouponDTO coupon = couponService.getCouponByCode(code);
        return ResponseEntity.ok(coupon);
    }
}
