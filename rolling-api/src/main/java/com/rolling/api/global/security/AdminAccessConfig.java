package com.rolling.api.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AdminAccessConfig {

    private final Set<Long> adminUserIds;
    private final String tournamentCrawlerAdminKey;

    public AdminAccessConfig(String adminUserIdsProperty) {
        this(adminUserIdsProperty, "");
    }

    @Autowired
    public AdminAccessConfig(
            @Value("${admin.user-ids:}") String adminUserIdsProperty,
            @Value("${tournament.crawler.admin-key:}") String tournamentCrawlerAdminKey
    ) {
        this.adminUserIds = parseAdminUserIds(adminUserIdsProperty);
        this.tournamentCrawlerAdminKey = normalizeSecret(tournamentCrawlerAdminKey);
    }

    public boolean isAdmin(Long userId) {
        return userId != null && adminUserIds.contains(userId);
    }

    public boolean matchesCrawlerAdminKey(String value) {
        String normalized = normalizeSecret(value);
        return tournamentCrawlerAdminKey != null && tournamentCrawlerAdminKey.equals(normalized);
    }

    private Set<Long> parseAdminUserIds(String adminUserIdsProperty) {
        if (adminUserIdsProperty == null || adminUserIdsProperty.isBlank()) {
            return Set.of();
        }

        try {
            return Arrays.stream(adminUserIdsProperty.split(","))
                    .map(String::trim)
                    .filter(value -> !value.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toUnmodifiableSet());
        } catch (NumberFormatException e) {
            throw new IllegalStateException("admin.user-ids must be a comma-separated list of numeric user IDs", e);
        }
    }

    private String normalizeSecret(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
