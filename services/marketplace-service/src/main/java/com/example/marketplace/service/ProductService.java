package com.example.marketplace.service;

import com.example.marketplace.dto.ProductResponseDTO;
import com.example.marketplace.dto.ProductUpsertRequestDTO;
import com.example.marketplace.entity.ProductCategory;
import com.example.marketplace.entity.ProductType;

import java.util.List;

public interface ProductService {

    List<ProductResponseDTO> getProducts(String query, ProductCategory category, ProductType type);

    List<ProductResponseDTO> getAllProductsForAdmin();

    ProductResponseDTO getProductById(Long id);

    ProductResponseDTO createProduct(ProductUpsertRequestDTO request);

    ProductResponseDTO updateProduct(Long id, ProductUpsertRequestDTO request);

    void deleteProduct(Long id);
}
