package com.rolling.api.domain.notification.config;

import com.google.firebase.messaging.FirebaseMessaging;
import com.rolling.api.domain.notification.service.FcmPushNotificationService;
import com.rolling.api.domain.notification.service.NoOpPushNotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Slf4j
@Configuration
@EnableConfigurationProperties(PushNotificationProperties.class)
public class PushNotificationConfig {

    @Bean
    public PushNotificationService pushNotificationService(
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider,
            UserDeviceRepository userDeviceRepository,
            ApplicationContext applicationContext,
            Environment environment,
            PushNotificationProperties pushNotificationProperties
    ) {
        FirebaseMessaging firebaseMessaging = firebaseMessagingProvider.getIfAvailable();
        boolean firebaseEnabled = environment.getProperty("firebase.enabled", Boolean.class, false);
        String[] firebaseMessagingBeans = applicationContext.getBeanNamesForType(FirebaseMessaging.class);

        if (firebaseMessaging != null) {
            log.info(
                    "Using FcmPushNotificationService. firebaseEnabled={}, firebaseMessagingBeans={}, androidChannelId={}",
                    firebaseEnabled,
                    Arrays.toString(firebaseMessagingBeans),
                    pushNotificationProperties.androidChannelId()
            );
            return new FcmPushNotificationService(
                    firebaseMessaging,
                    userDeviceRepository,
                    pushNotificationProperties.androidChannelId()
            );
        }

        String reason = "firebaseEnabled=" + firebaseEnabled
                + ", firebaseMessagingBeans=" + Arrays.toString(firebaseMessagingBeans);
        log.warn("Using NoOpPushNotificationService. {}", reason);
        return new NoOpPushNotificationService(reason);
    }

    @Bean
    public ApplicationRunner pushNotificationDiagnostics(
            PushNotificationService pushNotificationService,
            ApplicationContext applicationContext,
            ObjectProvider<FirebaseMessaging> firebaseMessagingProvider,
            Environment environment,
            PushNotificationProperties pushNotificationProperties
    ) {
        return args -> log.info(
                "Push notification diagnostics. pushServiceClass={}, firebaseEnabled={}, firebaseMessagingPresent={}, androidChannelId={}, pushServiceBeans={}",
                pushNotificationService.getClass().getName(),
                environment.getProperty("firebase.enabled", Boolean.class, false),
                firebaseMessagingProvider.getIfAvailable() != null,
                pushNotificationProperties.androidChannelId(),
                Arrays.toString(applicationContext.getBeanNamesForType(PushNotificationService.class))
        );
    }
}
