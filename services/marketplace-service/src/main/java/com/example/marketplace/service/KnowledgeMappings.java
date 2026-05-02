package com.example.marketplace.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KnowledgeMappings {

    public enum ProductCategory {
        STRESS_RELIEF,
        SLEEP_SUPPORT,
        MINDFULNESS,
        THERAPY_TOOLS
    }

    private KnowledgeMappings() {
        // Static utility class
    }

    public static Map<String, List<ProductCategory>> getKeywordMappings() {
        Map<String, List<ProductCategory>> mappings = new HashMap<>();
        
        // STRESS_RELIEF mappings
        mappings.put("anxiety", Arrays.asList(ProductCategory.STRESS_RELIEF));
        mappings.put("panic", Arrays.asList(ProductCategory.STRESS_RELIEF));
        mappings.put("stress", Arrays.asList(ProductCategory.STRESS_RELIEF));
        
        // SLEEP_SUPPORT mappings
        mappings.put("insomnia", Arrays.asList(ProductCategory.SLEEP_SUPPORT));
        mappings.put("sleep", Arrays.asList(ProductCategory.SLEEP_SUPPORT));
        mappings.put("sleeplessness", Arrays.asList(ProductCategory.SLEEP_SUPPORT));
        
        // MINDFULNESS mappings
        mappings.put("depression", Arrays.asList(ProductCategory.MINDFULNESS));
        mappings.put("mood", Arrays.asList(ProductCategory.MINDFULNESS));
        mappings.put("sadness", Arrays.asList(ProductCategory.MINDFULNESS));
        
        // THERAPY_TOOLS mappings
        mappings.put("focus", Arrays.asList(ProductCategory.THERAPY_TOOLS));
        mappings.put("concentration", Arrays.asList(ProductCategory.THERAPY_TOOLS));
        mappings.put("adhd", Arrays.asList(ProductCategory.THERAPY_TOOLS));
        
        return mappings;
    }

    public static Map<String, List<ProductCategory>> getMedicationMappings() {
        Map<String, List<ProductCategory>> mappings = new HashMap<>();
        
        // MINDFULNESS mappings
        mappings.put("sertraline", Arrays.asList(ProductCategory.MINDFULNESS));
        mappings.put("lexapro", Arrays.asList(ProductCategory.MINDFULNESS));
        mappings.put("fluoxetine", Arrays.asList(ProductCategory.MINDFULNESS));
        
        // SLEEP_SUPPORT mappings
        mappings.put("zolpidem", Arrays.asList(ProductCategory.SLEEP_SUPPORT));
        mappings.put("ambien", Arrays.asList(ProductCategory.SLEEP_SUPPORT));
        
        // STRESS_RELIEF mappings
        mappings.put("lorazepam", Arrays.asList(ProductCategory.STRESS_RELIEF));
        mappings.put("xanax", Arrays.asList(ProductCategory.STRESS_RELIEF));
        mappings.put("alprazolam", Arrays.asList(ProductCategory.STRESS_RELIEF));
        
        return mappings;
    }

    public static ProductCategory mapKeywordToCategory(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return null;
        }
        
        String lowerKeyword = keyword.toLowerCase();
        Map<String, List<ProductCategory>> mappings = getKeywordMappings();
        
        List<ProductCategory> categories = mappings.get(lowerKeyword);
        return categories != null && !categories.isEmpty() ? categories.get(0) : null;
    }

    public static ProductCategory mapMedicationToCategory(String medicationName) {
        if (medicationName == null || medicationName.trim().isEmpty()) {
            return null;
        }
        
        String lowerMed = medicationName.toLowerCase();
        Map<String, List<ProductCategory>> mappings = getMedicationMappings();
        
        List<ProductCategory> categories = mappings.get(lowerMed);
        return categories != null && !categories.isEmpty() ? categories.get(0) : null;
    }
}
