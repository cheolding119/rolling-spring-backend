package com.rolling.api.global.config;

import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("!prod")
@RequiredArgsConstructor
public class MockOpenMatDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final OpenMatRepository openMatRepository;

    @Override
    public void run(String... args) {
        if (openMatRepository.count() > 0) {
            return;
        }

        List<User> users = createMockUsers();
        List<OpenMat> openMats = createMockOpenMats(users);
        openMatRepository.saveAll(openMats);

        log.info("Seeded {} mock open mats", openMats.size());
    }

    private List<User> createMockUsers() {
        List<User> users = new ArrayList<>();
        users.add(getOrCreateUser("host-001", "rolling_host_1", "host1@rolling.dev", "010-1000-0001"));
        users.add(getOrCreateUser("host-002", "rolling_host_2", "host2@rolling.dev", "010-1000-0002"));
        users.add(getOrCreateUser("host-003", "rolling_host_3", "host3@rolling.dev", "010-1000-0003"));
        users.add(getOrCreateUser("host-004", "rolling_host_4", "host4@rolling.dev", "010-1000-0004"));
        users.add(getOrCreateUser("user-001", "rolling_user_1", "user1@rolling.dev", "010-2000-0001"));
        users.add(getOrCreateUser("user-002", "rolling_user_2", "user2@rolling.dev", "010-2000-0002"));
        users.add(getOrCreateUser("user-003", "rolling_user_3", "user3@rolling.dev", "010-2000-0003"));
        users.add(getOrCreateUser("user-004", "rolling_user_4", "user4@rolling.dev", "010-2000-0004"));
        return users;
    }

    private User getOrCreateUser(String socialId, String nickname, String email, String phone) {
        return userRepository.findBySocialIdAndSocialProvider(socialId, SocialProvider.GOOGLE)
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .socialProvider(SocialProvider.GOOGLE)
                                .socialId(socialId)
                                .nickname(nickname)
                                .email(email)
                                .phone(phone)
                                .build()
                ));
    }

    private List<OpenMat> createMockOpenMats(List<User> users) {
        User host1 = users.get(0);
        User host2 = users.get(1);
        User host3 = users.get(2);
        User host4 = users.get(3);
        User user1 = users.get(4);
        User user2 = users.get(5);
        User user3 = users.get(6);
        User user4 = users.get(7);

        List<OpenMat> openMats = new ArrayList<>();

        openMats.add(newOpenMat(
                host1,
                "Morning No-Gi Open Mat",
                "Light rounds and positional sparring",
                LocalDateTime.of(2026, 3, 2, 7, 0),
                LocalDateTime.of(2026, 3, 2, 9, 0),
                "Rolling Gym Gangnam",
                "Seoul Gangnam-gu Teheran-ro 100",
                24,
                "rolling_host_1",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId(), user2.getId())));

        openMats.add(newOpenMat(
                host2,
                "Lunch Drill Session",
                "Beginner-friendly drilling and QnA",
                LocalDateTime.of(2026, 3, 3, 12, 0),
                LocalDateTime.of(2026, 3, 3, 13, 30),
                "Rolling Gym Seongsu",
                "Seoul Seongdong-gu Yeonmujang-gil 45",
                16,
                "rolling_host_2",
                OpenMatStatus.RECRUITING,
                List.of(user2.getId())));

        openMats.add(newOpenMat(
                host3,
                "Evening Gi Open Mat",
                "Competition rounds with timer",
                LocalDateTime.of(2026, 3, 4, 19, 30),
                LocalDateTime.of(2026, 3, 4, 21, 30),
                "Rolling Gym Hongdae",
                "Seoul Mapo-gu Wausan-ro 88",
                20,
                "rolling_host_3",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId(), user3.getId())));

        openMats.add(newOpenMat(
                host4,
                "Weekend Recovery Roll",
                "Low intensity mobility and flow rolls",
                LocalDateTime.of(2026, 3, 7, 10, 0),
                LocalDateTime.of(2026, 3, 7, 12, 0),
                "Rolling Gym Jamsil",
                "Seoul Songpa-gu Olympic-ro 300",
                -1,
                "rolling_host_4",
                OpenMatStatus.RECRUITING,
                List.of(user4.getId())));

        openMats.add(newOpenMat(
                host1,
                "Women Open Mat",
                "Women and beginner-focused open mat",
                LocalDateTime.of(2026, 3, 8, 14, 0),
                LocalDateTime.of(2026, 3, 8, 16, 0),
                "Rolling Gym Mokdong",
                "Seoul Yangcheon-gu Omok-ro 200",
                18,
                "rolling_host_1",
                OpenMatStatus.RECRUITING,
                List.of(user2.getId(), user3.getId(), user4.getId())));

        openMats.add(newOpenMat(
                host2,
                "Competition Camp Sparring",
                "Hard rounds for upcoming tournaments",
                LocalDateTime.of(2026, 3, 10, 20, 0),
                LocalDateTime.of(2026, 3, 10, 22, 0),
                "Rolling Gym Suwon",
                "Gyeonggi Suwon-si Yeongtong-gu 120",
                14,
                "rolling_host_2",
                OpenMatStatus.CLOSED,
                List.of(user1.getId(), user2.getId(), user3.getId(), user4.getId())));

        openMats.add(newOpenMat(
                host3,
                "Open Mat with Guest Coach",
                "Guest coach positional class + rounds",
                LocalDateTime.of(2026, 3, 12, 18, 30),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                "Rolling Gym Incheon",
                "Incheon Namdong-gu Central-ro 70",
                30,
                "rolling_host_3",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId())));

        openMats.add(newOpenMat(
                host4,
                "Early Bird Drilling",
                "Technical drilling before work",
                LocalDateTime.of(2026, 3, 13, 6, 30),
                LocalDateTime.of(2026, 3, 13, 8, 0),
                "Rolling Gym Ilsan",
                "Gyeonggi Goyang-si Ilsandong-gu 15",
                12,
                "rolling_host_4",
                OpenMatStatus.RECRUITING,
                List.of(user3.getId())));

        openMats.add(newOpenMat(
                host1,
                "Sunday Open Sparring",
                "Open rounds all levels welcome",
                LocalDateTime.of(2026, 3, 15, 11, 0),
                LocalDateTime.of(2026, 3, 15, 13, 0),
                "Rolling Gym Bundang",
                "Gyeonggi Seongnam-si Bundang-gu 55",
                25,
                "rolling_host_1",
                OpenMatStatus.RECRUITING,
                List.of(user2.getId(), user4.getId())));

        openMats.add(newOpenMat(
                host2,
                "Past Open Mat Archive",
                "Completed session kept as sample data",
                LocalDateTime.of(2026, 2, 10, 19, 0),
                LocalDateTime.of(2026, 2, 10, 21, 0),
                "Rolling Gym Daegu",
                "Daegu Suseong-gu Dalgubeol-daero 400",
                20,
                "rolling_host_2",
                OpenMatStatus.FINISHED,
                List.of(user1.getId(), user2.getId(), user3.getId())));

        return openMats;
    }

    private OpenMat newOpenMat(
            User host,
            String title,
            String description,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            String locationName,
            String address,
            Integer maxCapacity,
            String hostInstagramId,
            OpenMatStatus status,
            List<Long> participantUserIds
    ) {
        OpenMat openMat = OpenMat.builder()
                .host(host)
                .title(title)
                .description(description)
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .locationName(locationName)
                .address(address)
                .maxCapacity(maxCapacity)
                .hostInstagramId(hostInstagramId)
                .status(status)
                .build();

        for (Long participantUserId : participantUserIds) {
            openMat.addParticipant(participantUserId);
        }
        return openMat;
    }
}
