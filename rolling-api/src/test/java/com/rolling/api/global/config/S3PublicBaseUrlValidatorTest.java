package com.rolling.api.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class S3PublicBaseUrlValidatorTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(S3PublicBaseUrlValidator.class);

    @Test
    @DisplayName("prod 프로필에서 public base URL이 없으면 애플리케이션 시작이 실패한다")
    void failsInProdWhenPublicBaseUrlIsMissing() {
        contextRunner
                .withPropertyValues("spring.profiles.active=prod")
                .run(context -> {
                    context.assertThat().hasFailed();
                    assertThat(context.getStartupFailure())
                            .isInstanceOf(org.springframework.beans.factory.BeanCreationException.class);
                    assertThat(context.getStartupFailure().getCause())
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("AWS_S3_PUBLIC_BASE_URL");
                });
    }

    @Test
    @DisplayName("public base URL이 있으면 validator가 통과한다")
    void passesWhenPublicBaseUrlIsConfigured() {
        contextRunner
                .withPropertyValues(
                        "spring.profiles.active=prod",
                        "cloud.aws.s3.public-base-url=https://cdn.rolling.com"
                )
                .run(context -> context.assertThat().hasNotFailed());
    }
}
