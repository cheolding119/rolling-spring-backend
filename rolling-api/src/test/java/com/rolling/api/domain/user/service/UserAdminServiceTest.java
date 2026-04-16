package com.rolling.api.domain.user.service;

import com.rolling.api.domain.user.dto.UserSanctionCreateRequest;
import com.rolling.api.domain.user.dto.UserSanctionResponse;
import com.rolling.api.domain.user.entity.AccountStatus;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserSanction;
import com.rolling.api.domain.user.entity.UserSanctionType;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.domain.user.repository.UserSanctionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserSanctionRepository userSanctionRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private UserAdminService userAdminService;

    @BeforeEach
    void setUpClock() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-04-14T03:00:00Z"));
    }

    @Test
    @DisplayName("일시정지 제재 생성 시 사용자 상태와 제재 이력이 함께 반영된다")
    void createSanction_tempSuspend_updatesUserAndReturnsResponse() {
        User user = user(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userSanctionRepository.findTopByUser_IdAndReleasedAtIsNullOrderByCreatedAtDesc(7L)).thenReturn(Optional.empty());
        when(userSanctionRepository.save(any(UserSanction.class))).thenAnswer(invocation -> {
            UserSanction sanction = invocation.getArgument(0);
            ReflectionTestUtils.setField(sanction, "id", 100L);
            ReflectionTestUtils.setField(sanction, "createdAt", LocalDateTime.of(2026, 4, 14, 13, 0));
            return sanction;
        });

        UserSanctionCreateRequest request = new UserSanctionCreateRequest();
        ReflectionTestUtils.setField(request, "type", UserSanctionType.TEMP_SUSPEND);
        ReflectionTestUtils.setField(request, "reason", "반복적인 욕설");
        ReflectionTestUtils.setField(request, "memo", "운영 규정 위반");
        ReflectionTestUtils.setField(request, "endsAt", LocalDateTime.of(2026, 4, 20, 0, 0));

        UserSanctionResponse response = userAdminService.createSanction(1L, 7L, request);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(user.getSuspensionUntil()).isEqualTo(LocalDateTime.of(2026, 4, 20, 0, 0));
        assertThat(user.getSanctionReasonSummary()).isEqualTo("반복적인 욕설");
        assertThat(response.getType()).isEqualTo(UserSanctionType.TEMP_SUSPEND);
        assertThat(response.getReason()).isEqualTo("반복적인 욕설");
        assertThat(response.getEndsAt()).isEqualTo(LocalDateTime.of(2026, 4, 20, 0, 0));
    }

    @Test
    @DisplayName("활성 제재 해제 시 사용자 상태가 정상으로 복구된다")
    void releaseSanction_restoresUserState() {
        User user = user(7L);
        user.suspend(LocalDateTime.of(2026, 4, 20, 0, 0), "반복적인 욕설");

        UserSanction sanction = UserSanction.builder()
                .user(user)
                .sanctionType(UserSanctionType.TEMP_SUSPEND)
                .reason("반복적인 욕설")
                .memo("운영 규정 위반")
                .startsAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .endsAt(LocalDateTime.of(2026, 4, 20, 0, 0))
                .createdByUserId(1L)
                .build();
        ReflectionTestUtils.setField(sanction, "id", 100L);
        ReflectionTestUtils.setField(sanction, "createdAt", LocalDateTime.of(2026, 4, 14, 13, 0));

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userSanctionRepository.findByIdAndUser_Id(100L, 7L)).thenReturn(Optional.of(sanction));
        when(userSanctionRepository.findTopByUser_IdAndReleasedAtIsNullOrderByCreatedAtDesc(7L)).thenReturn(Optional.of(sanction));

        UserSanctionResponse response = userAdminService.releaseSanction(1L, 7L, 100L);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getSuspensionUntil()).isNull();
        assertThat(user.getSanctionReasonSummary()).isNull();
        assertThat(response.getReleasedByUserId()).isEqualTo(1L);
        assertThat(response.getReleasedAt()).isNotNull();
    }

    @Test
    @DisplayName("영구정지 요청은 현재 정책에서 허용되지 않는다")
    void createSanction_permanentBanRejected() {
        User user = user(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserSanctionCreateRequest request = new UserSanctionCreateRequest();
        ReflectionTestUtils.setField(request, "type", UserSanctionType.PERMANENT_BAN);
        ReflectionTestUtils.setField(request, "reason", "중대한 운영 위반");

        assertThatThrownBy(() -> userAdminService.createSanction(1L, 7L, request))
                .hasMessage("영구정지는 더 이상 지원하지 않습니다");
    }

    @Test
    @DisplayName("기존 영구정지 이력은 관리자 응답에서 장기 일시정지로 노출된다")
    void findSanctions_legacyPermanentBanNormalizedToTempSuspend() {
        User user = user(7L);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        UserSanction sanction = UserSanction.builder()
                .user(user)
                .sanctionType(UserSanctionType.PERMANENT_BAN)
                .reason("중대한 운영 위반")
                .memo("legacy data")
                .startsAt(LocalDateTime.of(2026, 4, 1, 12, 0))
                .endsAt(null)
                .createdByUserId(1L)
                .build();
        ReflectionTestUtils.setField(sanction, "id", 300L);
        ReflectionTestUtils.setField(sanction, "createdAt", LocalDateTime.of(2026, 4, 1, 12, 0));
        when(userSanctionRepository.findAllByUser_IdOrderByCreatedAtDesc(7L)).thenReturn(java.util.List.of(sanction));

        java.util.List<UserSanctionResponse> responses = userAdminService.findSanctions(7L);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getType()).isEqualTo(UserSanctionType.TEMP_SUSPEND);
        assertThat(responses.get(0).getReason()).isEqualTo("중대한 운영 위반");
    }

    private User user(Long id) {
        User user = User.builder()
                .socialId("social-" + id)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("user-" + id)
                .email("user" + id + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
