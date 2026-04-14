package com.rolling.api.domain.user.repository;

import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserBlock;
import com.rolling.api.domain.user.repository.UserBlockRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:user-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
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
        "cloud.aws.s3.public-base-url=https://cdn.test.com",
        "cloud.aws.credentials.access-key=test-access-key",
        "cloud.aws.credentials.secret-key=test-secret-key",
        "cloud.aws.region.static=ap-northeast-2"
})
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserBlockRepository userBlockRepository;

    @Test
    @DisplayName("차단 사용자 ID 조회는 탈퇴하지 않은 사용자만 반환한다")
    void findBlockedUserIdsByUserId_returnsOnlyActiveBlockedUsers() {
        User viewer = userRepository.save(createUser("viewer-user"));
        User activeBlockedUser = userRepository.save(createUser("active-blocked-user"));
        User withdrawnBlockedUser = userRepository.save(createUser("withdrawn-blocked-user"));

        viewer.blockUser(activeBlockedUser);
        viewer.blockUser(withdrawnBlockedUser);
        userRepository.save(viewer);

        withdrawnBlockedUser.requestWithdrawal(LocalDateTime.now(), LocalDateTime.now().plusDays(1));
        withdrawnBlockedUser.withdraw();
        userRepository.save(withdrawnBlockedUser);

        List<Long> blockedUserIds = userRepository.findBlockedUserIdsByUserId(viewer.getId());

        assertThat(blockedUserIds).containsExactly(activeBlockedUser.getId());

        UserBlock storedBlock = userBlockRepository.findByUser_IdAndBlockedUser_Id(viewer.getId(), activeBlockedUser.getId())
                .orElseThrow();
        assertThat(storedBlock.getBlockedAt()).isNotNull();
    }

    private User createUser(String socialId) {
        return User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(socialId)
                .email(socialId + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
    }
}
