package com.rolling.api.global.config;

import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
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
                "아침 노기 오픈매트",
                "가벼운 스파링과 포지셔널 훈련",
                LocalDateTime.of(2026, 3, 2, 7, 0),
                LocalDateTime.of(2026, 3, 2, 9, 0),
                "롤링짐 강남",
                "서울 강남구 테헤란로 100",
                Region.SEOUL,
                24,
                "rolling_host_1",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId(), user2.getId())));

        openMats.add(newOpenMat(
                host2,
                "점심 드릴 세션",
                "초보자 친화적인 드릴 및 질의응답",
                LocalDateTime.of(2026, 3, 3, 12, 0),
                LocalDateTime.of(2026, 3, 3, 13, 30),
                "롤링짐 성수",
                "서울 성동구 연무장길 45",
                Region.SEOUL,
                16,
                "rolling_host_2",
                OpenMatStatus.RECRUITING,
                List.of(user2.getId())));

        openMats.add(newOpenMat(
                host3,
                "저녁 기 오픈매트",
                "타이머를 사용한 대회 방식 스파링",
                LocalDateTime.of(2026, 3, 4, 19, 30),
                LocalDateTime.of(2026, 3, 4, 21, 30),
                "롤링짐 홍대",
                "서울 마포구 와우산로 88",
                Region.SEOUL,
                20,
                "rolling_host_3",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId(), user3.getId())));

        openMats.add(newOpenMat(
                host4,
                "주말 회복 롤링",
                "저강도 유연성 및 플로우 롤",
                LocalDateTime.of(2026, 3, 7, 10, 0),
                LocalDateTime.of(2026, 3, 7, 12, 0),
                "롤링짐 잠실",
                "서울 송파구 올림픽로 300",
                Region.SEOUL,
                -1,
                "rolling_host_4",
                OpenMatStatus.RECRUITING,
                List.of(user4.getId())));

        openMats.add(newOpenMat(
                host1,
                "여성 오픈매트",
                "여성 및 초보자 중심 오픈매트",
                LocalDateTime.of(2026, 3, 8, 14, 0),
                LocalDateTime.of(2026, 3, 8, 16, 0),
                "롤링짐 목동",
                "서울 양천구 오목로 200",
                Region.SEOUL,
                18,
                "rolling_host_1",
                OpenMatStatus.RECRUITING,
                List.of(user2.getId(), user3.getId(), user4.getId())));

        openMats.add(newOpenMat(
                host2,
                "대회 캠프 스파링",
                "다가오는 대회를 위한 강도 높은 스파링",
                LocalDateTime.of(2026, 3, 10, 20, 0),
                LocalDateTime.of(2026, 3, 10, 22, 0),
                "롤링짐 수원",
                "경기 수원시 영통구 120",
                Region.GYEONGGI,
                14,
                "rolling_host_2",
                OpenMatStatus.CLOSED,
                List.of(user1.getId(), user2.getId(), user3.getId(), user4.getId())));

        openMats.add(newOpenMat(
                host3,
                "게스트 코치 오픈매트",
                "게스트 코치의 포지셔널 수업 및 스파링",
                LocalDateTime.of(2026, 3, 12, 18, 30),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                "롤링짐 인천",
                "인천 남동구 센트럴로 70",
                Region.INCHEON,
                30,
                "rolling_host_3",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId())));

        openMats.add(newOpenMat(
                host4,
                "얼리버드 드릴 세션",
                "출근 전 기술 드릴 훈련",
                LocalDateTime.of(2026, 3, 13, 6, 30),
                LocalDateTime.of(2026, 3, 13, 8, 0),
                "롤링짐 일산",
                "경기 고양시 일산동구 15",
                Region.GYEONGGI,
                12,
                "rolling_host_4",
                OpenMatStatus.RECRUITING,
                List.of(user3.getId())));

        openMats.add(newOpenMat(
                host1,
                "일요일 오픈 스파링",
                "모든 레벨 환영, 자유 스파링",
                LocalDateTime.of(2026, 3, 15, 11, 0),
                LocalDateTime.of(2026, 3, 15, 13, 0),
                "롤링짐 분당",
                "경기 성남시 분당구 55",
                Region.GYEONGGI,
                25,
                "rolling_host_1",
                OpenMatStatus.RECRUITING,
                List.of(user2.getId(), user4.getId())));

        openMats.add(newOpenMat(
                host2,
                "지난 오픈매트 기록",
                "완료된 세션 샘플 데이터",
                LocalDateTime.of(2026, 2, 10, 19, 0),
                LocalDateTime.of(2026, 2, 10, 21, 0),
                "롤링짐 대구",
                "대구 수성구 달구벌대로 400",
                Region.DAEGU,
                20,
                "rolling_host_2",
                OpenMatStatus.FINISHED,
                List.of(user1.getId(), user2.getId(), user3.getId())));

        openMats.add(newOpenMat(
                host3,
                "노기 기초 야간 세션",
                "가드 리텐션과 스윕 중심, 전 레벨 환영",
                LocalDateTime.of(2026, 3, 17, 19, 0),
                LocalDateTime.of(2026, 3, 17, 21, 0),
                "롤링짐 신촌",
                "서울 서대문구 신촌로 58",
                Region.SEOUL,
                20,
                "rolling_host_3",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId(), user4.getId())));

        openMats.add(newOpenMat(
                host4,
                "블루벨트 이상 스파링",
                "중급 이상, 라이브 스파링만 진행",
                LocalDateTime.of(2026, 3, 18, 20, 0),
                LocalDateTime.of(2026, 3, 18, 22, 0),
                "롤링짐 영등포",
                "서울 영등포구 여의나루로 22",
                Region.SEOUL,
                16,
                "rolling_host_4",
                OpenMatStatus.RECRUITING,
                List.of(user2.getId(), user3.getId())));

        openMats.add(newOpenMat(
                host1,
                "기 압박 패싱 클리닉",
                "기 그립을 활용한 강력한 가드 패싱 집중 훈련",
                LocalDateTime.of(2026, 3, 21, 10, 0),
                LocalDateTime.of(2026, 3, 21, 12, 0),
                "롤링짐 노원",
                "서울 노원구 동일로 500",
                Region.SEOUL,
                18,
                "rolling_host_1",
                OpenMatStatus.RECRUITING,
                List.of(user3.getId())));

        openMats.add(newOpenMat(
                host2,
                "심야 롤링 세션",
                "야간 근무자를 위한 심야 오픈매트",
                LocalDateTime.of(2026, 3, 22, 23, 0),
                LocalDateTime.of(2026, 3, 23, 1, 0),
                "롤링짐 이태원",
                "서울 용산구 이태원로 177",
                Region.SEOUL,
                10,
                "rolling_host_2",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId())));

        openMats.add(newOpenMat(
                host3,
                "어린이·부모 오픈매트",
                "가족 친화적 오픈매트, 아이와 부모가 함께",
                LocalDateTime.of(2026, 3, 28, 14, 0),
                LocalDateTime.of(2026, 3, 28, 16, 0),
                "롤링짐 마포",
                "서울 마포구 공덕로 10",
                Region.SEOUL,
                -1,
                "rolling_host_3",
                OpenMatStatus.RECRUITING,
                List.of(user2.getId(), user4.getId())));

        openMats.add(newOpenMat(
                host4,
                "타관 교류 오픈매트",
                "타관 수련생을 위한 교류 오픈매트",
                LocalDateTime.of(2026, 4, 5, 11, 0),
                LocalDateTime.of(2026, 4, 5, 13, 30),
                "롤링짐 부산 해운대",
                "부산 해운대구 해운대로 210",
                Region.BUSAN,
                30,
                "rolling_host_4",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId(), user2.getId(), user3.getId())));

        openMats.add(newOpenMat(
                host1,
                "서브미션 온리 대회 준비",
                "다가오는 대회를 위한 서브미션 온리 준비",
                LocalDateTime.of(2026, 4, 11, 9, 0),
                LocalDateTime.of(2026, 4, 11, 12, 0),
                "롤링짐 대전",
                "대전 서구 둔산로 90",
                Region.DAEJEON,
                24,
                "rolling_host_1",
                OpenMatStatus.RECRUITING,
                List.of(user4.getId())));

        openMats.add(newOpenMat(
                host2,
                "가드 플레이어 특별 세션",
                "백 포지션과 가드 플레이 중심 오픈매트",
                LocalDateTime.of(2026, 4, 19, 16, 0),
                LocalDateTime.of(2026, 4, 19, 18, 0),
                "롤링짐 광명",
                "경기 광명시 하안로 30",
                Region.GYEONGGI,
                14,
                "rolling_host_2",
                OpenMatStatus.RECRUITING,
                List.of(user1.getId(), user3.getId())));

        openMats.add(newOpenMat(
                host3,
                "시즌 마무리 오픈매트",
                "시즌 종료 기념 소셜 오픈매트",
                LocalDateTime.of(2026, 2, 1, 13, 0),
                LocalDateTime.of(2026, 2, 1, 15, 0),
                "롤링짐 광주",
                "광주 북구 충장로 60",
                Region.GWANGJU,
                22,
                "rolling_host_3",
                OpenMatStatus.FINISHED,
                List.of(user1.getId(), user2.getId(), user3.getId(), user4.getId())));

        openMats.add(newOpenMat(
                host4,
                "풀하우스 오픈매트",
                "모든 자리 마감, 치열한 경쟁 분위기",
                LocalDateTime.of(2026, 2, 15, 18, 0),
                LocalDateTime.of(2026, 2, 15, 20, 0),
                "롤링짐 수서",
                "서울 강남구 수서동 145",
                Region.SEOUL,
                12,
                "rolling_host_4",
                OpenMatStatus.CLOSED,
                List.of(user1.getId(), user2.getId(), user3.getId(), user4.getId())));

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
            Region region,
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
                .region(region)
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
