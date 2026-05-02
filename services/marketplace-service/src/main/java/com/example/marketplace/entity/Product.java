package com.example.marketplace.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "article_products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductType type;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Boolean active;

    @Column(length = 500)
    private String imageUrl;

    @Column
    private Boolean previewable;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private PreviewContentType previewType;

    @Column(length = 1000)
    private String previewUrl;

    @Column(length = 1000)
    private String contentUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // Cascade delete relationships - allows deleting products even if they have orders/reviews/wishlist entries
    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<MarketplaceOrderItem> orderItems;

    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<ProductReview> reviews;

    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Wishlist> wishlistEntries;

    @PrePersist
    void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.active == null) {
            this.active = true;
        }
        if (this.previewable == null) {
            this.previewable = false;
        }
    }
}

