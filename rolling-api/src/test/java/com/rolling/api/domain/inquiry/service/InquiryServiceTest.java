package com.rolling.api.domain.inquiry.service;

import com.rolling.api.domain.inquiry.dto.InquiryAnswerRequest;
import com.rolling.api.domain.inquiry.dto.InquiryCreateRequest;
import com.rolling.api.domain.inquiry.dto.InquiryResponse;
import com.rolling.api.domain.inquiry.dto.InquiryStatusUpdateRequest;
import com.rolling.api.domain.inquiry.entity.Inquiry;
import com.rolling.api.domain.inquiry.entity.InquiryStatus;
import com.rolling.api.domain.inquiry.repository.InquiryRepository;
import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
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

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private InquiryRepository inquiryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationService notificationService;

    private InquiryService inquiryService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-20T03:00:00Z"), SEOUL_ZONE);
        inquiryService = new InquiryService(inquiryRepository, userRepository, notificationService, fixedClock);
    }

    @Test
    @DisplayName("문의 생성 시 RECEIVED 상태로 저장한다")
    void create_savesInquiryWithReceivedStatus() {
        User user = createUser(10L, "user-10", "rolling-user");
        InquiryCreateRequest request = new InquiryCreateRequest();
        ReflectionTestUtils.setField(request, "title", "알림이 오지 않습니다");
        ReflectionTestUtils.setField(request, "content", "수정 알림이 오지 않습니다.");

        when(userRepository.findByIdAndIsWithdrawnFalse(10L)).thenReturn(Optional.of(user));
        when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(invocation -> {
            Inquiry inquiry = invocation.getArgument(0);
            ReflectionTestUtils.setField(inquiry, "id", 100L);
            ReflectionTestUtils.setField(inquiry, "createdAt", LocalDateTime.of(2026, 3, 20, 12, 0));
            ReflectionTestUtils.setField(inquiry, "updatedAt", LocalDateTime.of(2026, 3, 20, 12, 0));
            return inquiry;
        });

        InquiryResponse response = inquiryService.create(10L, request);

        ArgumentCaptor<Inquiry> captor = ArgumentCaptor.forClass(Inquiry.class);
        verify(inquiryRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(InquiryStatus.RECEIVED);
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getStatus()).isEqualTo(InquiryStatus.RECEIVED);
    }

    @Test
    @DisplayName("내 문의 목록 조회는 createdAt 내림차순 기본 정렬을 적용한다")
    void findMyInquiries_appliesDefaultSort() {
        Inquiry inquiry = createInquiry(21L, createUser(10L, "user-10", "rolling-user"), InquiryStatus.IN_REVIEW);
        when(userRepository.existsByIdAndIsWithdrawnFalse(10L)).thenReturn(true);
        when(inquiryRepository.findAllByUser_Id(eq(10L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(inquiry)));

        Page<InquiryResponse> response = inquiryService.findMyInquiries(10L, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(inquiryRepository).findAllByUser_Id(eq(10L), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(InquiryStatus.IN_REVIEW);
    }

    @Test
    @DisplayName("본인 문의 상세는 사용자 소유 범위에서만 조회한다")
    void findMyInquiry_returnsOwnedInquiry() {
        Inquiry inquiry = createInquiry(31L, createUser(10L, "user-10", "rolling-user"), InquiryStatus.RECEIVED);
        when(userRepository.existsByIdAndIsWithdrawnFalse(10L)).thenReturn(true);
        when(inquiryRepository.findByIdAndUser_Id(31L, 10L)).thenReturn(Optional.of(inquiry));

        InquiryResponse response = inquiryService.findMyInquiry(10L, 31L);

        assertThat(response.getId()).isEqualTo(31L);
        assertThat(response.getUserId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("관리자 답변 저장 시 상태를 ANSWERED로 바꾸고 알림함을 저장한다")
    void answer_updatesInquiryAndSavesNotification() {
        User user = createUser(10L, "user-10", "rolling-user");
        Inquiry inquiry = createInquiry(41L, user, InquiryStatus.IN_REVIEW);
        InquiryAnswerRequest request = new InquiryAnswerRequest();
        ReflectionTestUtils.setField(request, "answerContent", "기기 토큰을 다시 등록해 주세요.");

        when(inquiryRepository.findById(41L)).thenReturn(Optional.of(inquiry));

        InquiryResponse response = inquiryService.answer(1L, 41L, request);

        ArgumentCaptor<PushNotificationCommand> commandCaptor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        verify(notificationService).saveNotificationsForUsers(eq(List.of(10L)), commandCaptor.capture());
        assertThat(response.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
        assertThat(response.getAnsweredByUserId()).isEqualTo(1L);
        assertThat(response.getAnsweredAt()).isEqualTo(LocalDateTime.of(2026, 3, 20, 12, 0));
        assertThat(commandCaptor.getValue().type()).isEqualTo(PushNotificationType.INQUIRY_ANSWERED);
        assertThat(commandCaptor.getValue().targetId()).isEqualTo(41L);
        assertThat(commandCaptor.getValue().data()).containsEntry("route", InquiryService.INQUIRY_DETAIL_ROUTE);
    }

    @Test
    @DisplayName("이미 ANSWERED 상태인 문의의 답변 수정은 추가 알림을 보내지 않는다")
    void answer_whenAlreadyAnswered_doesNotSendDuplicateNotification() {
        Inquiry inquiry = createInquiry(42L, createUser(10L, "user-10", "rolling-user"), InquiryStatus.ANSWERED);
        inquiry.answer("기존 답변", 1L, LocalDateTime.of(2026, 3, 19, 18, 0));
        InquiryAnswerRequest request = new InquiryAnswerRequest();
        ReflectionTestUtils.setField(request, "answerContent", "수정된 답변");

        when(inquiryRepository.findById(42L)).thenReturn(Optional.of(inquiry));

        InquiryResponse response = inquiryService.answer(1L, 42L, request);

        verify(notificationService, never()).saveNotificationsForUsers(any(), any());
        assertThat(response.getAnswerContent()).isEqualTo("수정된 답변");
    }

    @Test
    @DisplayName("답변 없는 문의는 ANSWERED 상태로 변경할 수 없다")
    void updateStatus_toAnsweredWithoutAnswer_throwsValidationError() {
        Inquiry inquiry = createInquiry(51L, createUser(10L, "user-10", "rolling-user"), InquiryStatus.RECEIVED);
        InquiryStatusUpdateRequest request = new InquiryStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", InquiryStatus.ANSWERED);

        when(inquiryRepository.findById(51L)).thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> inquiryService.updateStatus(51L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("ANSWERED 상태로 변경하려면 답변이 필요합니다");
    }

    @Test
    @DisplayName("답변이 저장된 문의는 ANSWERED 외 상태로 되돌릴 수 없다")
    void updateStatus_fromAnsweredToInReview_throwsValidationError() {
        Inquiry inquiry = createInquiry(52L, createUser(10L, "user-10", "rolling-user"), InquiryStatus.ANSWERED);
        inquiry.answer("답변 완료", 1L, LocalDateTime.of(2026, 3, 20, 11, 0));
        InquiryStatusUpdateRequest request = new InquiryStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", InquiryStatus.IN_REVIEW);

        when(inquiryRepository.findById(52L)).thenReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> inquiryService.updateStatus(52L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("답변이 저장된 문의는 ANSWERED 외 상태로 변경할 수 없습니다");
    }

    @Test
    @DisplayName("답변 없는 문의는 RECEIVED에서 IN_REVIEW로 상태 변경할 수 있다")
    void updateStatus_toInReview_succeeds() {
        Inquiry inquiry = createInquiry(53L, createUser(10L, "user-10", "rolling-user"), InquiryStatus.RECEIVED);
        InquiryStatusUpdateRequest request = new InquiryStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", InquiryStatus.IN_REVIEW);

        when(inquiryRepository.findById(53L)).thenReturn(Optional.of(inquiry));

        InquiryResponse response = inquiryService.updateStatus(53L, request);

        assertThat(response.getStatus()).isEqualTo(InquiryStatus.IN_REVIEW);
    }

    private User createUser(Long id, String socialId, String nickname) {
        User user = User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(nickname)
                .email(socialId + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Inquiry createInquiry(Long id, User user, InquiryStatus status) {
        Inquiry inquiry = Inquiry.builder()
                .user(user)
                .title("알림이 오지 않습니다")
                .content("수정 알림이 오지 않습니다.")
                .status(status)
                .build();
        ReflectionTestUtils.setField(inquiry, "id", id);
        ReflectionTestUtils.setField(inquiry, "createdAt", LocalDateTime.of(2026, 3, 20, 9, 0));
        ReflectionTestUtils.setField(inquiry, "updatedAt", LocalDateTime.of(2026, 3, 20, 9, 0));
        return inquiry;
    }
}
