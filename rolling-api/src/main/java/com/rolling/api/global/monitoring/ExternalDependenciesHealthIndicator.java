package com.rolling.api.global.monitoring;

import com.google.firebase.FirebaseApp;
import com.rolling.api.global.config.FirebaseProperties;
import com.rolling.api.infra.google.GoogleClient;
import com.rolling.api.infra.kakao.KakaoClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component("externalDependencies")
public class ExternalDependenciesHealthIndicator implements HealthIndicator {

    private final FirebaseProperties firebaseProperties;
    private final ObjectProvider<FirebaseApp> firebaseAppProvider;
    private final ObjectProvider<S3Client> s3ClientProvider;
    private final ObjectProvider<GoogleClient> googleClientProvider;
    private final ObjectProvider<KakaoClient> kakaoClientProvider;
    private final String awsBucketName;
    private final List<String> crawlerUrls;

    public ExternalDependenciesHealthIndicator(
            FirebaseProperties firebaseProperties,
            ObjectProvider<FirebaseApp> firebaseAppProvider,
            ObjectProvider<S3Client> s3ClientProvider,
            ObjectProvider<GoogleClient> googleClientProvider,
            ObjectProvider<KakaoClient> kakaoClientProvider,
            @Value("${cloud.aws.s3.bucket:}") String awsBucketName,
            @Value("${tournament.crawler.street-jiujitsu.list-page-urls:}") List<String> crawlerUrls
    ) {
        this.firebaseProperties = firebaseProperties;
        this.firebaseAppProvider = firebaseAppProvider;
        this.s3ClientProvider = s3ClientProvider;
        this.googleClientProvider = googleClientProvider;
        this.kakaoClientProvider = kakaoClientProvider;
        this.awsBucketName = awsBucketName;
        this.crawlerUrls = crawlerUrls;
    }

    @Override
    public Health health() {
        Map<String, Object> firebase = firebaseDetail();
        Map<String, Object> s3 = s3Detail();
        Map<String, Object> social = socialDetail();
        Map<String, Object> crawler = crawlerDetail();

        boolean down = isDown(firebase) || isDown(s3) || isDown(social) || isDown(crawler);

        return (down ? Health.down() : Health.up())
                .withDetail("firebase", firebase)
                .withDetail("s3", s3)
                .withDetail("socialLogin", social)
                .withDetail("crawler", crawler)
                .build();
    }

    private Map<String, Object> firebaseDetail() {
        Map<String, Object> detail = new LinkedHashMap<>();
        boolean enabled = firebaseProperties.enabled();
        boolean initialized = firebaseAppProvider.getIfAvailable() != null;
        detail.put("enabled", enabled);
        detail.put("projectIdConfigured", StringUtils.hasText(firebaseProperties.projectId()));
        detail.put("credentialsPathConfigured", StringUtils.hasText(firebaseProperties.credentialsPath()));
        detail.put("firebaseAppInitialized", initialized);
        detail.put("status", !enabled || initialized ? "UP" : "DOWN");
        return detail;
    }

    private Map<String, Object> s3Detail() {
        Map<String, Object> detail = new LinkedHashMap<>();
        boolean initialized = s3ClientProvider.getIfAvailable() != null;
        boolean bucketConfigured = StringUtils.hasText(awsBucketName);
        detail.put("bucketConfigured", bucketConfigured);
        detail.put("clientInitialized", initialized);
        detail.put("status", bucketConfigured && initialized ? "UP" : "DOWN");
        return detail;
    }

    private Map<String, Object> socialDetail() {
        Map<String, Object> detail = new LinkedHashMap<>();
        boolean googleReady = googleClientProvider.getIfAvailable() != null;
        boolean kakaoReady = kakaoClientProvider.getIfAvailable() != null;
        detail.put("googleClientInitialized", googleReady);
        detail.put("kakaoClientInitialized", kakaoReady);
        detail.put("status", googleReady && kakaoReady ? "UP" : "DOWN");
        return detail;
    }

    private Map<String, Object> crawlerDetail() {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("streetJiuJitsuUrlCount", crawlerUrls.size());
        detail.put("status", crawlerUrls.isEmpty() ? "DOWN" : "UP");
        return detail;
    }

    private boolean isDown(Map<String, Object> detail) {
        return "DOWN".equals(detail.get("status"));
    }
}

