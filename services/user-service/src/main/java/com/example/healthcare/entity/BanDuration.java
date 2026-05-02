package com.example.healthcare.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

public enum BanDuration {
    ONE_DAY,
    THREE_DAYS,
    ONE_WEEK,
    ONE_MONTH,
    PERMANENT;

    @JsonCreator
    public static BanDuration fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Ban duration is required");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT).replace("-", "_");
        return switch (normalized) {
            case "ONE_DAY", "1_DAY", "1D", "DAY", "1DAY" -> ONE_DAY;
            case "THREE_DAYS", "3_DAYS", "3D", "THREE_DAY", "3DAY" -> THREE_DAYS;
            case "ONE_WEEK", "1_WEEK", "WEEK", "1W", "1WEEK" -> ONE_WEEK;
            case "ONE_MONTH", "1_MONTH", "MONTH", "1M", "1MONTH" -> ONE_MONTH;
            case "PERMANENT", "PERMA", "PERM", "FOREVER" -> PERMANENT;
            default -> throw new IllegalArgumentException("Invalid ban duration value: " + value);
        };
    }

    @JsonValue
    public String toJsonValue() {
        return name();
    }

    public Date resolveBannedUntil(Date from) {
        Instant start = from.toInstant();
        return switch (this) {
            case ONE_DAY -> Date.from(start.plus(1, ChronoUnit.DAYS));
            case THREE_DAYS -> Date.from(start.plus(3, ChronoUnit.DAYS));
            case ONE_WEEK -> Date.from(start.plus(7, ChronoUnit.DAYS));
            case ONE_MONTH -> Date.from(start.plus(30, ChronoUnit.DAYS));
            case PERMANENT -> null;
        };
    }

    public boolean isPermanent() {
        return this == PERMANENT;
    }
}
