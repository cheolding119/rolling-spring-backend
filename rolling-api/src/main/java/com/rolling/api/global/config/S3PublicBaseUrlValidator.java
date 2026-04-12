package com.rolling.api.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Profile("prod")
public class S3PublicBaseUrlValidator {

    @Value("${cloud.aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @PostConstruct
    void validate() {
        if (!StringUtils.hasText(publicBaseUrl)) {
            throw new IllegalStateException(
                    "cloud.aws.s3.public-base-url must be set in prod. Configure AWS_S3_PUBLIC_BASE_URL to a public image base URL."
            );
        }
    }
}
