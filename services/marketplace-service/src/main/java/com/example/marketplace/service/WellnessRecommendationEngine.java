package com.example.marketplace.service;

import com.example.marketplace.clients.InsuranceServiceClient;
import com.example.marketplace.clients.PharmacyServiceClient;
import com.example.marketplace.dto.*;
import com.example.marketplace.repository.ProductRepository;
import com.example.marketplace.entity.Product;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WellnessRecommendationEngine {

    private final InsuranceServiceClient insuranceServiceClient;
    private final PharmacyServiceClient pharmacyServiceClient;
    private final ProductRepository productRepository;

        public RecommendationResponseDTO getQuizBasedRecommendations(QuizRecommendationRequestDTO request) {
        int anxietyNeed = toScale(request.getAnxietyLevel());
        int stressNeed = toScale(request.getStressLevel());
        int sleepNeed = toScale(request.getSleepNeed());
        int focusNeed = Math.max(10, 100 - (anxietyNeed + stressNeed) / 2);
        int sensoryNeed = Math.min(100, Math.round((anxietyNeed * 0.6f) + (stressNeed * 0.4f)));

        List<ProductRecommendationItemDTO> recommendations = productRepository.findByActiveTrueOrderByCreatedAtDesc()
            .stream()
            .map(product -> toQuizRecommendation(product, anxietyNeed, stressNeed, sleepNeed, focusNeed, sensoryNeed))
            .sorted((a, b) -> Integer.compare(b.getConfidence(), a.getConfidence()))
            .limit(8)
            .toList();

        return RecommendationResponseDTO.builder()
            .recommendations(recommendations)
            .reasoning("Based on your 3-answer wellness scan, these products best fit your current needs.")
            .totalRecommendations(recommendations.size())
            .generatedAt(LocalDateTime.now())
            .build();
        }

    @Transactional(readOnly = true)
    @Cacheable(value = "recommendations", key = "#userId")
    public RecommendationResponseDTO getPersonalizedRecommendations(Long userId, String jwtToken) {
        try {
            log.info("Generating personalized recommendations for user {}", userId);

            // Step 1 & 2: Fetch approved claims and accepted prescriptions
            List<InsuranceClaimDTO> approvedClaims = insuranceServiceClient.getUserApprovedClaims(userId, jwtToken);
            List<PrescriptionDTO> acceptedPrescriptions = pharmacyServiceClient.getUserAcceptedPrescriptions(userId, jwtToken);

            // Step 3 & 4: Extract keywords and medication names
            Set<String> keywords = extractKeywordsFromClaims(approvedClaims);
            Set<String> medications = extractMedicationsFromPrescriptions(acceptedPrescriptions);

            log.info("Extracted {} keywords and {} medications for user {}", 
                keywords.size(), medications.size(), userId);

            // Step 5: Map to product categories
            Set<KnowledgeMappings.ProductCategory> categories = new HashSet<>();
            
            for (String keyword : keywords) {
                KnowledgeMappings.ProductCategory category = KnowledgeMappings.mapKeywordToCategory(keyword);
                if (category != null) {
                    categories.add(category);
                }
            }
            
            for (String medication : medications) {
                KnowledgeMappings.ProductCategory category = KnowledgeMappings.mapMedicationToCategory(medication);
                if (category != null) {
                    categories.add(category);
                }
            }

            // Step 6: Get top 4 products for each category
            List<ProductRecommendationItemDTO> recommendations = getTopProductsForCategories(categories, keywords, medications);

            // Step 7: Build response
            String reasoning = buildReasoningMessage(approvedClaims, acceptedPrescriptions);
            
            RecommendationResponseDTO response = RecommendationResponseDTO.builder()
                    .recommendations(recommendations)
                    .reasoning(reasoning)
                    .totalRecommendations(recommendations.size())
                    .generatedAt(LocalDateTime.now())
                    .build();

            log.info("Generated {} recommendations for user {}", recommendations.size(), userId);
            return response;

        } catch (Exception e) {
            log.error("Error generating recommendations for user {}: {}", userId, e.getMessage(), e);
            return RecommendationResponseDTO.builder()
                    .recommendations(new ArrayList<>())
                    .reasoning("Unable to generate recommendations at this time")
                    .totalRecommendations(0)
                    .generatedAt(LocalDateTime.now())
                    .build();
        }
    }

    private Set<String> extractKeywordsFromClaims(List<InsuranceClaimDTO> claims) {
        Set<String> keywords = new HashSet<>();
        
        String[] keywordTerms = {
            "anxiety", "panic", "stress", 
            "insomnia", "sleep", "sleeplessness",
            "depression", "mood", "sadness",
            "focus", "concentration", "adhd"
        };
        
        for (InsuranceClaimDTO claim : claims) {
            if (claim.getDescription() != null && !claim.getDescription().isEmpty()) {
                String description = claim.getDescription().toLowerCase();
                for (String term : keywordTerms) {
                    if (description.contains(term)) {
                        keywords.add(term);
                    }
                }
            }
        }
        
        return keywords;
    }

    private Set<String> extractMedicationsFromPrescriptions(List<PrescriptionDTO> prescriptions) {
        Set<String> medications = new HashSet<>();
        
        for (PrescriptionDTO prescription : prescriptions) {
            // Add main medication name
            if (prescription.getMedicationName() != null && !prescription.getMedicationName().isEmpty()) {
                medications.add(prescription.getMedicationName());
            }
            
            // Add medication lines if present
            if (prescription.getMedicationLines() != null) {
                for (MedicationLineDTO line : prescription.getMedicationLines()) {
                    if (line.getMedicationName() != null && !line.getMedicationName().isEmpty()) {
                        medications.add(line.getMedicationName());
                    }
                }
            }
        }
        
        return medications;
    }

    private List<ProductRecommendationItemDTO> getTopProductsForCategories(
            Set<KnowledgeMappings.ProductCategory> categories, 
            Set<String> keywords, 
            Set<String> medications) {
        
        Map<Long, ProductRecommendationItemDTO> recommendedProductsMap = new LinkedHashMap<>();
        
        for (KnowledgeMappings.ProductCategory category : categories) {
            String categoryStr = category.toString();
            
            try {
                // Fetch products for this category (limit to top 4)
                List<Product> products = productRepository.findByActiveTrueAndCategoryOrderByCreatedAtDesc(
                        com.example.marketplace.entity.ProductCategory.valueOf(categoryStr)
                );
                
                products.stream()
                        .limit(4)
                        .forEach(product -> {
                            if (!recommendedProductsMap.containsKey(product.getId())) {
                                int confidence = calculateConfidence(product, keywords, medications, categoryStr);
                                
                                ProductRecommendationItemDTO item = ProductRecommendationItemDTO.builder()
                                        .productId(product.getId())
                                        .productName(product.getName())
                                        .category(categoryStr)
                                        .reason(buildProductReason(categoryStr, keywords, medications))
                                        .confidence(confidence)
                                        .build();
                                
                                recommendedProductsMap.put(product.getId(), item);
                            }
                        });
            } catch (IllegalArgumentException e) {
                log.warn("Category {} not found in Product entity: {}", categoryStr, e.getMessage());
            }
        }
        
        return recommendedProductsMap.values().stream()
                .sorted((a, b) -> Integer.compare(b.getConfidence(), a.getConfidence()))
                .collect(Collectors.toList());
    }

    private int calculateConfidence(Product product, Set<String> keywords, Set<String> medications, String category) {
        int confidence = 60;
        
        // Boost confidence based on keyword/medication matches
        if (keywords.size() > 0 || medications.size() > 0) {
            confidence = Math.min(95, confidence + (keywords.size() * 5) + (medications.size() * 5));
        }
        
        return confidence;
    }

    private String buildProductReason(String category, Set<String> keywords, Set<String> medications) {
        List<String> reasons = new ArrayList<>();
        
        if (!keywords.isEmpty()) {
            reasons.add("based on your health conditions (" + String.join(", ", keywords) + ")");
        }
        
        if (!medications.isEmpty()) {
            reasons.add("based on your current medications (" + String.join(", ", medications) + ")");
        }
        
        if (reasons.isEmpty()) {
            reasons.add("recommended for " + category.toLowerCase().replace("_", " "));
        }
        
        return "Recommended " + String.join(" and ", reasons);
    }

    private String buildReasoningMessage(List<InsuranceClaimDTO> claims, List<PrescriptionDTO> prescriptions) {
        StringBuilder reasoning = new StringBuilder();
        reasoning.append("Based on your ");
        
        List<String> parts = new ArrayList<>();
        if (!claims.isEmpty()) {
            parts.add(claims.size() + " approved health claim(s)");
        }
        if (!prescriptions.isEmpty()) {
            parts.add(prescriptions.size() + " active prescription(s)");
        }
        
        reasoning.append(String.join(" and ", parts));
        reasoning.append(", we've curated these personalized wellness products for you.");
        
        return reasoning.toString();
    }

    private ProductRecommendationItemDTO toQuizRecommendation(
            Product product,
            int anxietyNeed,
            int stressNeed,
            int sleepNeed,
            int focusNeed,
            int sensoryNeed
    ) {
        int[] productSignal = productSignal(product);
        int confidence = computeMatchScore(productSignal, anxietyNeed, stressNeed, sleepNeed, focusNeed, sensoryNeed);

        return ProductRecommendationItemDTO.builder()
                .productId(product.getId())
                .productName(product.getName())
                .category(product.getCategory().name())
                .reason(buildQuizReason(productSignal, anxietyNeed, stressNeed, sleepNeed, focusNeed, sensoryNeed))
                .confidence(confidence)
                .build();
    }

    private int[] productSignal(Product product) {
        int anxiety = 55;
        int stress = 55;
        int sleep = 45;
        int focus = 45;
        int sensory = 35;

        switch (product.getCategory()) {
            case STRESS_RELIEF -> {
                anxiety = 76;
                stress = 92;
                sleep = 45;
                focus = 42;
                sensory = 68;
            }
            case SLEEP_SUPPORT -> {
                anxiety = 64;
                stress = 58;
                sleep = 95;
                focus = 34;
                sensory = 56;
            }
            case MINDFULNESS -> {
                anxiety = 84;
                stress = 77;
                sleep = 66;
                focus = 58;
                sensory = 43;
            }
            case THERAPY_TOOLS -> {
                anxiety = 58;
                stress = 62;
                sleep = 36;
                focus = 91;
                sensory = 80;
            }
            case SELF_CARE -> {
                anxiety = 66;
                stress = 72;
                sleep = 60;
                focus = 53;
                sensory = 48;
            }
            case EDUCATION -> {
                anxiety = 38;
                stress = 47;
                sleep = 32;
                focus = 72;
                sensory = 24;
            }
        }

        String text = ((product.getName() == null ? "" : product.getName()) + " "
                + (product.getDescription() == null ? "" : product.getDescription())).toLowerCase(Locale.ROOT);

        if (text.contains("fidget") || text.contains("sensory")) {
            sensory = Math.min(100, sensory + 20);
            stress = Math.min(100, stress + 8);
        }
        if (text.contains("sleep") || text.contains("insomnia") || text.contains("night")) {
            sleep = Math.min(100, sleep + 18);
        }
        if (text.contains("focus") || text.contains("concentration")) {
            focus = Math.min(100, focus + 16);
        }
        if (text.contains("anxiety") || text.contains("calm") || text.contains("soothing")) {
            anxiety = Math.min(100, anxiety + 14);
        }

        return new int[] {anxiety, stress, sleep, focus, sensory};
    }

    private int computeMatchScore(int[] productSignal, int anxietyNeed, int stressNeed, int sleepNeed, int focusNeed, int sensoryNeed) {
        int[] userNeed = new int[] {anxietyNeed, stressNeed, sleepNeed, focusNeed, sensoryNeed};
        long weightedSignal = 0L;
        long weightedMax = 0L;

        for (int i = 0; i < productSignal.length; i++) {
            weightedSignal += (long) productSignal[i] * userNeed[i];
            weightedMax += (long) 100 * userNeed[i];
        }

        if (weightedMax == 0) {
            return 50;
        }

        return (int) Math.round((weightedSignal * 100.0) / weightedMax);
    }

    private String buildQuizReason(int[] productSignal, int anxietyNeed, int stressNeed, int sleepNeed, int focusNeed, int sensoryNeed) {
        Map<String, Integer> priorities = new LinkedHashMap<>();
        priorities.put("anxiety soothing", anxietyNeed * productSignal[0]);
        priorities.put("stress relief", stressNeed * productSignal[1]);
        priorities.put("sleep support", sleepNeed * productSignal[2]);
        priorities.put("focus support", focusNeed * productSignal[3]);
        priorities.put("sensory calming", sensoryNeed * productSignal[4]);

        List<String> topReasons = priorities.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(2)
                .map(Map.Entry::getKey)
                .toList();

        return "Strong fit for " + String.join(" + ", topReasons);
    }

    private int toScale(Integer answer) {
        if (answer == null) {
            return 50;
        }
        return Math.max(1, Math.min(5, answer)) * 20;
    }
}
