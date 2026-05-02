package com.serenity.monitoring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Read-only mapping to {@code user_profiles} (same schema as user-service) for avatar URLs.
 */
@Entity
@Table(name = "user_profiles")
@Getter
@Setter
public class UserProfileSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    private String avatar;

    @Column(length = 1000)
    private String bio;

    private Boolean isAnonymous;

    private String preferredLanguage;
}
