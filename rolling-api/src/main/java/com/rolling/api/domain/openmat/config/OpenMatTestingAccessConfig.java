package com.rolling.api.domain.openmat.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenMatTestingAccessConfig {

    private final boolean allowUnauthenticatedUpdate;

    public OpenMatTestingAccessConfig(
            @Value("${openmat.testing.allow-unauthenticated-update:false}") boolean allowUnauthenticatedUpdate
    ) {
        this.allowUnauthenticatedUpdate = allowUnauthenticatedUpdate;
    }

    public boolean isAllowUnauthenticatedUpdate() {
        return allowUnauthenticatedUpdate;
    }
}
