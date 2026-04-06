package com.rolling.api.domain.user.repository;

import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserDevice;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-device-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "jwt.access-token-expiry=1800000",
        "jwt.refresh-token-expiry=1209600000",
        "spring.profiles.active=prod",
        "firebase.enabled=false",
        "openmat.status.schedule.enabled=false",
        "tournament.crawler.schedule.enabled=false",
        "cloud.aws.s3.bucket=test-bucket",
        "cloud.aws.credentials.access-key=test-access-key",
        "cloud.aws.credentials.secret-key=test-secret-key",
        "cloud.aws.region.static=ap-northeast-2"
})
class UserDeviceRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserDeviceRepository userDeviceRepository;

    @AfterEach
    void tearDown() {
        userDeviceRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("푸시 발송 대상 조회는 pushNotificationEnabled가 true인 활성 사용자만 포함한다")
    void findPushTargetDevicesByUserIds_includesOnlyPushEnabledActiveUsers() {
        User enabledUser = userRepository.save(createUser("enabled-user", true));
        User disabledUser = userRepository.save(createUser("disabled-user", false));
        User pendingUser = createUser("pending-user", true);
        pendingUser.requestWithdrawal(LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        pendingUser = userRepository.save(pendingUser);

        userDeviceRepository.save(UserDevice.builder()
                .user(enabledUser)
                .fcmToken("token-enabled")
                .platform("ANDROID")
                .build());
        userDeviceRepository.save(UserDevice.builder()
                .user(disabledUser)
                .fcmToken("token-disabled")
                .platform("ANDROID")
                .build());
        userDeviceRepository.save(UserDevice.builder()
                .user(pendingUser)
                .fcmToken("token-pending")
                .platform("ANDROID")
                .build());

        List<UserDevice> result = userDeviceRepository.findPushTargetDevicesByUserIds(
                List.of(enabledUser.getId(), disabledUser.getId(), pendingUser.getId())
        );

        assertThat(result).extracting(UserDevice::getFcmToken)
                .containsExactly("token-enabled");
    }

    private User createUser(String socialId, boolean pushNotificationEnabled) {
        return User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(socialId)
                .email(socialId + "@test.com")
                .beltColor(BeltColor.WHITE)
                .pushNotificationEnabled(pushNotificationEnabled)
                .build();
    }
}
