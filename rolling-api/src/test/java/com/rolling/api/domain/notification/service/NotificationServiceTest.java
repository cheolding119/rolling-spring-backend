package com.rolling.api.domain.notification.service;

import com.rolling.api.domain.notification.dto.NotificationResponse;
import com.rolling.api.domain.notification.entity.Notification;
import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.repository.NotificationRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-17T06:30:00Z"), SEOUL_ZONE);
        notificationService = new NotificationService(notificationRepository, userRepository, fixedClock);
    }

    @Test
    @DisplayName("알림함 저장은 새 트랜잭션으로 실행한다")
    void saveNotificationsForUsers_usesRequiresNewTransaction() throws NoSuchMethodException {
        Transactional transactional = NotificationService.class
                .getMethod("saveNotificationsForUsers", java.util.Collection.class, PushNotificationCommand.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }

    @Test
    @DisplayName("대상 사용자별 알림함 레코드를 저장한다")
    void saveNotificationsForUsers_savesInboxEntries() {
        User firstUser = createUser(2L, "user-2");
        User secondUser = createUser(3L, "user-3");
        PushNotificationCommand command = new PushNotificationCommand(
                PushNotificationType.OPEN_MAT_UPDATED,
                "오픈매트 일정이 변경되었습니다",
                "주말 오픈매트 오픈매트의 일정 또는 장소가 변경되었습니다.",
                11L,
                Map.of("route", "/openmat/detail")
        );

        when(userRepository.findAllByIdInAndIsWithdrawnFalse(List.of(2L, 3L)))
                .thenReturn(List.of(firstUser, secondUser));

        notificationService.saveNotificationsForUsers(java.util.Arrays.asList(2L, 2L, null, 3L), command);

        ArgumentCaptor<List<Notification>> captor = ArgumentCaptor.forClass(List.class);
        verify(notificationRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue())
                .extracting(notification -> notification.getUser().getId())
                .containsExactly(2L, 3L);
        assertThat(captor.getValue())
                .extracting(Notification::getType)
                .containsOnly(PushNotificationType.OPEN_MAT_UPDATED);
        assertThat(captor.getValue())
                .extracting(Notification::getTargetId)
                .containsOnly(11L);
        assertThat(captor.getValue())
                .extracting(Notification::getRoute)
                .containsOnly("/openmat/detail");
    }

    @Test
    @DisplayName("알림 목록 조회는 createdAt 내림차순 기본 정렬로 응답을 매핑한다")
    void findNotifications_returnsMappedPage() {
        User user = createUser(5L, "user-5");
        Notification notification = Notification.builder()
                .user(user)
                .type(PushNotificationType.OPEN_MAT_DELETED)
                .targetId(99L)
                .route("/openmat")
                .title("오픈매트가 취소되었습니다")
                .body("평일 오픈매트 오픈매트가 삭제되었습니다.")
                .build();
        ReflectionTestUtils.setField(notification, "id", 100L);
        ReflectionTestUtils.setField(notification, "createdAt", LocalDateTime.of(2026, 3, 17, 15, 30));

        when(userRepository.existsByIdAndIsWithdrawnFalse(5L)).thenReturn(true);
        when(notificationRepository.findAllByUser_Id(eq(5L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        Page<NotificationResponse> response = notificationService.findNotifications(5L, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(notificationRepository).findAllByUser_Id(eq(5L), pageableCaptor.capture());

        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getType()).isEqualTo("OPEN_MAT_DELETED");
        assertThat(response.getContent().get(0).getRoute()).isEqualTo("/openmat");
    }

    @Test
    @DisplayName("알림 읽음 처리 시 readAt을 기록한다")
    void markAsRead_setsReadAt() {
        Notification notification = Notification.builder()
                .user(createUser(7L, "user-7"))
                .type(PushNotificationType.OPEN_MAT_UPDATED)
                .targetId(31L)
                .route("/openmat/detail")
                .title("오픈매트 일정이 변경되었습니다")
                .body("테스트")
                .build();

        when(userRepository.existsByIdAndIsWithdrawnFalse(7L)).thenReturn(true);
        when(notificationRepository.findByIdAndUser_Id(200L, 7L)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(7L, 200L);

        assertThat(notification.getReadAt()).isEqualTo(LocalDateTime.of(2026, 3, 17, 15, 30));
    }

    @Test
    @DisplayName("다른 사용자의 알림은 읽음 처리할 수 없다")
    void markAsRead_whenNotificationNotOwned_throwsNotFound() {
        when(userRepository.existsByIdAndIsWithdrawnFalse(8L)).thenReturn(true);
        when(notificationRepository.findByIdAndUser_Id(201L, 8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(8L, 201L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("알림을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("알림함 저장은 route가 없으면 실패한다")
    void saveNotificationsForUsers_withoutRoute_throwsValidationError() {
        PushNotificationCommand command = new PushNotificationCommand(
                PushNotificationType.OPEN_MAT_UPDATED,
                "오픈매트 일정이 변경되었습니다",
                "본문",
                11L,
                Map.of()
        );

        assertThatThrownBy(() -> notificationService.saveNotificationsForUsers(List.of(2L), command))
                .isInstanceOf(BusinessException.class)
                .hasMessage("알림 route는 비어 있을 수 없습니다");
    }

    @Test
    @DisplayName("알림함 저장은 targetId가 없으면 실패한다")
    void saveNotificationsForUsers_withoutTargetId_throwsValidationError() {
        PushNotificationCommand command = new PushNotificationCommand(
                PushNotificationType.OPEN_MAT_UPDATED,
                "오픈매트 일정이 변경되었습니다",
                "본문",
                null,
                Map.of("route", "/openmat/detail")
        );

        assertThatThrownBy(() -> notificationService.saveNotificationsForUsers(List.of(2L), command))
                .isInstanceOf(BusinessException.class)
                .hasMessage("알림 targetId는 비어 있을 수 없습니다");
    }

    private User createUser(Long id, String socialId) {
        User user = User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(socialId)
                .email(socialId + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}






