package com.rolling.api.global.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class AdminAccessConfig {

    private final Set<Long> adminUserIds;

    public AdminAccessConfig(@Value("${admin.user-ids:}") String adminUserIdsProperty) {
        this.adminUserIds = parseAdminUserIds(adminUserIdsProperty);
    }

    public boolean isAdmin(Long userId) {
        return userId != null && adminUserIds.contains(userId);
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
}
