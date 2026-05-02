package com.example.marketplace.config;

import com.example.marketplace.entity.Product;
import com.example.marketplace.entity.ProductCategory;
import com.example.marketplace.entity.PreviewContentType;
import com.example.marketplace.entity.ProductType;
import com.example.marketplace.repository.ProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Configuration
public class SeedDataConfig {

    private static final Set<String> LEGACY_SEED_NAMES = Set.of(
            "Mindfulness Journal",
            "Breathing Companion Audio Pack",
            "Sleep Reset Toolkit",
            "Cognitive Reframing Mini Course",
            "Sensory Fidget Focus Kit",
            "Anxiety Soothing Weighted Wrap"
    );

        private static final String VIDEO_UPGRADE_PRODUCT_NAME = "Calm Mind Basics: 10-Minute Mindfulness Course";

    @Bean
    CommandLineRunner seedMarketplaceProducts(ProductRepository productRepository) {
        return args -> {
            List<Product> existingProducts = productRepository.findAll();
            if (!existingProducts.isEmpty()) {
                if (isLegacySeedDataset(existingProducts)) {
                    // Replace the old baseline once so local environments receive the updated article catalog.
                    productRepository.deleteAllInBatch();
                } else if (shouldUpgradeVideoPreview(existingProducts)) {
                    existingProducts.stream()
                            .filter(p -> VIDEO_UPGRADE_PRODUCT_NAME.equals(p.getName()))
                            .findFirst()
                            .ifPresent(product -> {
                                product.setPreviewType(PreviewContentType.VIDEO);
                                product.setPreviewUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
                                product.setContentUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4");
                                productRepository.save(product);
                            });
                    return;
                } else {
                    return;
                }
            }

                List<Product> products = List.of(
                    Product.builder()
                        .name("Quiet Start: Morning Anxiety Reset Podcast")
                        .description("A gentle 12-minute audio session for people who wake up with racing thoughts. It blends grounding cues, compassionate self-talk, and a simple breathing rhythm to help you begin the day with steadier energy.")
                        .category(ProductCategory.STRESS_RELIEF)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("18.90"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.AUDIO)
                        .previewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3")
                        .contentUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3")
                        .imageUrl("https://images.unsplash.com/photo-1518611012118-696072aa579a")
                        .build(),
                    Product.builder()
                        .name("Panic Wave Companion: 5-Step Audio Guide")
                        .description("A calm voice-guided companion designed for moments of anxiety spikes. You are walked through five practical steps to slow physical symptoms and return to a sense of safety.")
                        .category(ProductCategory.STRESS_RELIEF)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("16.50"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.AUDIO)
                        .previewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3")
                        .contentUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3")
                        .imageUrl("https://images.unsplash.com/photo-1506126613408-eca07ce68773")
                        .build(),
                    Product.builder()
                        .name("Breathe Between Meetings: Stress Pause Series")
                        .description("Short guided pauses for busy workdays. These audio breaks help lower tension between tasks, improve emotional balance, and prevent stress from building across the day.")
                        .category(ProductCategory.STRESS_RELIEF)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("14.90"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.AUDIO)
                        .previewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3")
                        .contentUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-11.mp3")
                        .imageUrl("https://images.unsplash.com/photo-1512290923902-8a9f81dc236c")
                        .build(),

                    Product.builder()
                        .name("Sleep Ladder: Wind-Down Audio Journey")
                        .description("A soft evening sequence with body relaxation, slower breath pacing, and gentle narration. Created for people who feel mentally active at bedtime and need a calmer transition to sleep.")
                        .category(ProductCategory.SLEEP_SUPPORT)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("19.40"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.AUDIO)
                        .previewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3")
                        .contentUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-12.mp3")
                        .imageUrl("https://images.unsplash.com/photo-1455642305367-68834aeeaefe")
                        .build(),
                    Product.builder()
                        .name("Night Unwind Notebook: 7 Sleep Reflections")
                        .description("A short reading workbook to empty mental load before bed. Each page includes one prompt, one practical action, and one reassurance to close the day with less overthinking.")
                        .category(ProductCategory.SLEEP_SUPPORT)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("12.90"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.BOOK)
                        .previewUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                        .contentUrl("https://www.africau.edu/images/default/sample.pdf")
                        .imageUrl("https://images.unsplash.com/photo-1505693416388-ac5ce068fe85")
                        .build(),

                    Product.builder()
                        .name("Deep Work Reset: Focus Recovery Reading")
                        .description("A practical mini-book for attention recovery during noisy days. It offers realistic routines, distraction boundaries, and compassionate pacing for people who feel mentally scattered.")
                        .category(ProductCategory.THERAPY_TOOLS)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("15.70"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.BOOK)
                        .previewUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                        .contentUrl("https://www.africau.edu/images/default/sample.pdf")
                        .imageUrl("https://images.unsplash.com/photo-1434030216411-0b793f4b4173")
                        .build(),
                    Product.builder()
                        .name("Micro-Routines For Mental Clarity")
                        .description("A guided exercise collection designed for productivity slumps. It helps you restart with small, achievable steps without pushing into guilt or perfectionism.")
                        .category(ProductCategory.THERAPY_TOOLS)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("13.60"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.AUDIO)
                        .previewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3")
                        .contentUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-13.mp3")
                        .imageUrl("https://images.unsplash.com/photo-1484480974693-6ca0a78fb36b")
                        .build(),

                    Product.builder()
                        .name("Burnout Recovery: Gentle Boundaries Handbook")
                        .description("A short and human-centered handbook for people who feel emotionally exhausted. It introduces boundary scripts, realistic recovery pacing, and permission to rest without losing structure.")
                        .category(ProductCategory.EDUCATION)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("17.20"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.BOOK)
                        .previewUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                        .contentUrl("https://www.africau.edu/images/default/sample.pdf")
                        .imageUrl("https://images.unsplash.com/photo-1521737604893-d14cc237f11d")
                        .build(),
                    Product.builder()
                        .name("After-Work Decompression Audio")
                        .description("An evening guided exercise to release workplace pressure and emotional fatigue. Built to help you separate work stress from personal time and recover with steadier breathing.")
                        .category(ProductCategory.SELF_CARE)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("14.30"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.AUDIO)
                        .previewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3")
                        .contentUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-14.mp3")
                        .imageUrl("https://images.unsplash.com/photo-1499209974431-9dddcece7f88")
                        .build(),

                    Product.builder()
                        .name("Low Mood Support: Tiny Daily Wins")
                        .description("A supportive reading guide for mild low-mood periods. It focuses on small momentum, emotional self-kindness, and simple actions that are achievable on lower-energy days.")
                        .category(ProductCategory.MINDFULNESS)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("12.40"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.BOOK)
                        .previewUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                        .contentUrl("https://www.africau.edu/images/default/sample.pdf")
                        .imageUrl("https://images.unsplash.com/photo-1493836512294-502baa1986e2")
                        .build(),
                    Product.builder()
                        .name("Mood Lift Walk: Audio Companion")
                        .description("A podcast-style outdoor companion that pairs mindful attention with gentle encouragement. Useful when motivation is low and you need a caring push toward movement.")
                        .category(ProductCategory.MINDFULNESS)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("13.20"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.AUDIO)
                        .previewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3")
                        .contentUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-15.mp3")
                        .imageUrl("https://images.unsplash.com/photo-1469474968028-56623f02e42e")
                        .build(),

                    Product.builder()
                        .name("Calm Mind Basics: 10-Minute Mindfulness Course")
                        .description("A concise beginner-friendly course for emotional steadiness. It combines practical mindfulness habits with clear guidance so users can build confidence at their own pace.")
                        .category(ProductCategory.MINDFULNESS)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("16.80"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.VIDEO)
                        .previewUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                        .contentUrl("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
                        .imageUrl("https://images.unsplash.com/photo-1474418397713-7ede21d49118")
                        .build(),
                    Product.builder()
                        .name("Weekend Reset For Emotional Balance")
                        .description("A restorative audio program for general well-being. It blends reflection prompts, gratitude practice, and gentle planning so the next week feels manageable and calmer.")
                        .category(ProductCategory.SELF_CARE)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("11.90"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.AUDIO)
                        .previewUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3")
                        .contentUrl("https://www.soundhelix.com/examples/mp3/SoundHelix-Song-16.mp3")
                        .imageUrl("https://images.unsplash.com/photo-1506126613408-eca07ce68773")
                        .build(),

                    Product.builder()
                        .name("Compassionate Self-Talk Crash Course")
                        .description("A short educational article series that teaches how to replace harsh inner dialogue with balanced, realistic language during stress, mistakes, and uncertainty.")
                        .category(ProductCategory.EDUCATION)
                        .type(ProductType.DIGITAL)
                        .price(new BigDecimal("15.20"))
                        .active(true)
                        .previewable(true)
                        .previewType(PreviewContentType.BOOK)
                        .previewUrl("https://www.w3.org/WAI/ER/tests/xhtml/testfiles/resources/pdf/dummy.pdf")
                        .contentUrl("https://www.africau.edu/images/default/sample.pdf")
                        .imageUrl("https://images.unsplash.com/photo-1521737604893-d14cc237f11d")
                        .build()
                );

            productRepository.saveAll(products);
        };
    }

    private boolean isLegacySeedDataset(List<Product> existingProducts) {
        if (existingProducts.size() != LEGACY_SEED_NAMES.size()) {
            return false;
        }

        Set<String> existingNames = existingProducts.stream()
                .map(Product::getName)
                .collect(Collectors.toSet());
        return existingNames.containsAll(LEGACY_SEED_NAMES);
    }

    private boolean shouldUpgradeVideoPreview(List<Product> existingProducts) {
        if (existingProducts.size() != 14) {
            return false;
        }

        boolean hasTarget = existingProducts.stream()
                .anyMatch(p -> VIDEO_UPGRADE_PRODUCT_NAME.equals(p.getName()));
        if (!hasTarget) {
            return false;
        }

        boolean hasAnyVideoPreview = existingProducts.stream()
                .anyMatch(p -> p.getPreviewType() == PreviewContentType.VIDEO);
        return !hasAnyVideoPreview;
    }
}
