package com.rolling.api.domain.traininglog.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "feature.training-card")
public record TrainingCardFeatureProperties(
        boolean enabled
) {

    public static final boolean DEFAULT_ENABLED = true;

    public TrainingCardFeatureProperties {
        // record canonical constructor
    }

    public TrainingCardFeatureProperties() {
        this(DEFAULT_ENABLED);
    }
}
