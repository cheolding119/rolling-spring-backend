package com.rolling.api.global.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Slf4j
@Configuration
@EnableConfigurationProperties(FirebaseProperties.class)
public class FirebaseAdminConfig {

    private static final String FIREBASE_APP_NAME = "rolling-api";

    @Bean
    @ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
    public FirebaseApp firebaseApp(FirebaseProperties firebaseProperties) throws IOException {
        Optional<FirebaseApp> existingApp = FirebaseApp.getApps().stream()
                .filter(app -> FIREBASE_APP_NAME.equals(app.getName()))
                .findFirst();

        if (existingApp.isPresent()) {
            return existingApp.get();
        }

        FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                .setCredentials(resolveCredentials(firebaseProperties));

        if (StringUtils.hasText(firebaseProperties.projectId())) {
            optionsBuilder.setProjectId(firebaseProperties.projectId().trim());
        }

        FirebaseApp firebaseApp = FirebaseApp.initializeApp(optionsBuilder.build(), FIREBASE_APP_NAME);
        log.info("Initialized FirebaseApp. projectId={}", firebaseProperties.projectId());
        return firebaseApp;
    }

    @Bean
    @ConditionalOnProperty(prefix = "firebase", name = "enabled", havingValue = "true")
    public FirebaseMessaging firebaseMessaging(FirebaseApp firebaseApp) {
        return FirebaseMessaging.getInstance(firebaseApp);
    }

    private GoogleCredentials resolveCredentials(FirebaseProperties firebaseProperties) throws IOException {
        if (StringUtils.hasText(firebaseProperties.credentialsPath())) {
            Path credentialsPath = Path.of(firebaseProperties.credentialsPath().trim());
            try (InputStream inputStream = Files.newInputStream(credentialsPath)) {
                return GoogleCredentials.fromStream(inputStream);
            }
        }

        return GoogleCredentials.getApplicationDefault();
    }
}
