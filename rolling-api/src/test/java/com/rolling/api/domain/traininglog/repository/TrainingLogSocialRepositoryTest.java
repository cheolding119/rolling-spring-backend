package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.Friendship;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import com.rolling.api.domain.traininglog.entity.TrainingLogVisibility;
import com.rolling.api.domain.traininglog.entity.UserTrainingLogShareSetting;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:training-log-social-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
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
class TrainingLogSocialRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TrainingLogEntryRepository trainingLogEntryRepository;

    @Autowired
    private FriendshipRepository friendshipRepository;

    @Autowired
    private UserTrainingLogShareSettingRepository userTrainingLogShareSettingRepository;

    @Test
    @DisplayName("친구 피드 쿼리는 친구 공개 설정이 켜진 친구의 기록만 반환한다")
    void findFriendFeedEntries_returnsOnlySharedFriendEntries() {
        User viewer = userRepository.save(createUser("viewer"));
        User friend = userRepository.save(createUser("friend"));
        User blockedFriend = userRepository.save(createUser("blocked-friend"));
        User privateFriend = userRepository.save(createUser("private-friend"));

        friendshipRepository.save(Friendship.builder().user(viewer).friendUser(friend).friendedAt(LocalDateTime.now()).build());
        friendshipRepository.save(Friendship.builder().user(friend).friendUser(viewer).friendedAt(LocalDateTime.now()).build());
        friendshipRepository.save(Friendship.builder().user(viewer).friendUser(blockedFriend).friendedAt(LocalDateTime.now()).build());
        friendshipRepository.save(Friendship.builder().user(blockedFriend).friendUser(viewer).friendedAt(LocalDateTime.now()).build());
        friendshipRepository.save(Friendship.builder().user(viewer).friendUser(privateFriend).friendedAt(LocalDateTime.now()).build());
        friendshipRepository.save(Friendship.builder().user(privateFriend).friendUser(viewer).friendedAt(LocalDateTime.now()).build());

        viewer.blockUser(blockedFriend);
        userRepository.save(viewer);

        userTrainingLogShareSettingRepository.save(UserTrainingLogShareSetting.builder().user(friend).shareWithFriends(true).build());
        userTrainingLogShareSettingRepository.save(UserTrainingLogShareSetting.builder().user(blockedFriend).shareWithFriends(true).build());
        userTrainingLogShareSettingRepository.save(UserTrainingLogShareSetting.builder().user(privateFriend).shareWithFriends(false).build());

        trainingLogEntryRepository.save(createEntry(friend, TrainingLogVisibility.FRIENDS, "shared"));
        trainingLogEntryRepository.save(createEntry(friend, TrainingLogVisibility.PRIVATE, "private"));
        trainingLogEntryRepository.save(createEntry(blockedFriend, TrainingLogVisibility.FRIENDS, "blocked"));
        trainingLogEntryRepository.save(createEntry(privateFriend, TrainingLogVisibility.FRIENDS, "hidden-by-setting"));

        Page<TrainingLogEntry> response = trainingLogEntryRepository.findFriendFeedEntries(
                viewer.getId(),
                PageRequest.of(0, 20)
        );

        assertThat(response.getContent()).hasSize(2);
        assertThat(response.getContent()).extracting(TrainingLogEntry::getTitle)
                .containsExactlyInAnyOrder("shared", "private");
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

    private TrainingLogEntry createEntry(User owner, TrainingLogVisibility visibility, String title) {
        return TrainingLogEntry.builder()
                .user(owner)
                .trainingDate(LocalDate.of(2026, 5, 22))
                .category(TrainingLogCategory.TECHNIQUE)
                .title(title)
                .content(title + " content")
                .visibility(visibility)
                .build();
    }
}
