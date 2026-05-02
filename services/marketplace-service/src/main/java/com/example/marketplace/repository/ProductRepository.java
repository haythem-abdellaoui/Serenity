package com.example.marketplace.repository;

import com.example.marketplace.entity.Product;
import com.example.marketplace.entity.ProductCategory;
import com.example.marketplace.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByOrderByCreatedAtDesc();

    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    List<Product> findByActiveTrueAndNameContainingIgnoreCaseOrderByCreatedAtDesc(String name);

    List<Product> findByActiveTrueAndCategoryAndTypeOrderByCreatedAtDesc(ProductCategory category, ProductType type);

    List<Product> findByActiveTrueAndCategoryOrderByCreatedAtDesc(ProductCategory category);

    List<Product> findByActiveTrueAndTypeOrderByCreatedAtDesc(ProductType type);
}
