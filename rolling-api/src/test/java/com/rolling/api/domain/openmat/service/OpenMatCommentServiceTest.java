package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.repository.NotificationRepository;
import com.rolling.api.domain.openmat.dto.OpenMatCommentCreateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatCommentReportAdminResponse;
import com.rolling.api.domain.openmat.dto.OpenMatCommentReportRequest;
import com.rolling.api.domain.openmat.dto.OpenMatCommentResponse;
import com.rolling.api.domain.openmat.dto.OpenMatCommentUpdateRequest;
import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatComment;
import com.rolling.api.domain.openmat.entity.OpenMatCommentReport;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.openmat.event.OpenMatCommentNotificationEvent;
import com.rolling.api.domain.openmat.repository.OpenMatCommentReportRepository;
import com.rolling.api.domain.openmat.repository.OpenMatCommentRepository;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
import com.rolling.api.domain.report.dto.ReportStatusUpdateRequest;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserBlockRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenMatCommentServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private OpenMatRepository openMatRepository;

    @Mock
    private OpenMatCommentRepository openMatCommentRepository;

    @Mock
    private OpenMatCommentReportRepository openMatCommentReportRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private OpenMatCommentService openMatCommentService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-06-11T12:00:00Z"), SEOUL_ZONE);
        openMatCommentService = new OpenMatCommentService(
                openMatRepository,
                openMatCommentRepository,
                openMatCommentReportRepository,
                userRepository,
                userBlockRepository,
                notificationRepository,
                applicationEventPublisher,
                fixedClock
        );
        lenient().when(userBlockRepository.existsByUser_IdAndBlockedUser_Id(anyLong(), anyLong())).thenReturn(false);
    }

    @Test
    @DisplayName("오픈매트 댓글 목록 조회는 원댓글과 1단계 대댓글 트리를 반환한다")
    void findComments_returnsCommentTree() {
        User host = createUser(1L, "host");
        OpenMat openMat = createOpenMat(10L, host);
        OpenMatComment parent = createComment(100L, openMat, null, createUser(2L, "commenter"), "원댓글");
        OpenMatComment reply = createComment(101L, openMat, parent, createUser(3L, "replier"), "대댓글");

        when(openMatRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(openMat));
        when(openMatCommentRepository.findAllByOpenMat_IdOrderByCreatedAtAscIdAsc(10L)).thenReturn(List.of(parent, reply));

        List<OpenMatCommentResponse> response = openMatCommentService.findComments(null, false, 10L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(100L);
        assertThat(response.get(0).getReplies()).hasSize(1);
        assertThat(response.get(0).getReplies().get(0).getId()).isEqualTo(101L);
        assertThat(response.get(0).isEditableByMe()).isFalse();
    }

    @Test
    @DisplayName("오픈매트 댓글 목록 조회는 차단 관계 원댓글과 하위 대댓글을 함께 숨긴다")
    void findComments_hidesBlockedCommentTree() {
        User viewer = createUser(9L, "viewer");
        User host = createUser(1L, "host");
        User blockedAuthor = createUser(2L, "blocked");
        OpenMat openMat = createOpenMat(10L, host);
        OpenMatComment blockedParent = createComment(100L, openMat, null, blockedAuthor, "원댓글");
        OpenMatComment blockedReply = createComment(101L, openMat, blockedParent, createUser(3L, "reply"), "대댓글");
        OpenMatComment visibleParent = createComment(102L, openMat, null, createUser(4L, "visible"), "보이는 댓글");

        when(openMatRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(openMat));
        when(openMatCommentRepository.findAllByOpenMat_IdOrderByCreatedAtAscIdAsc(10L))
                .thenReturn(List.of(blockedParent, blockedReply, visibleParent));
        when(userBlockRepository.findBlockedRelationUserIds(9L, List.of(2L, 3L, 4L))).thenReturn(List.of(2L));

        List<OpenMatCommentResponse> response = openMatCommentService.findComments(viewer.getId(), false, 10L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(102L);
    }

    @Test
    @DisplayName("오픈매트 댓글 작성은 대댓글의 대댓글을 허용하지 않는다")
    void createComment_replyToReply_throwsValidationError() {
        User author = createUser(2L, "viewer");
        User host = createUser(1L, "host");
        OpenMat openMat = createOpenMat(10L, host);
        OpenMatComment parent = createComment(100L, openMat, null, createUser(3L, "parent"), "원댓글");
        OpenMatComment reply = createComment(101L, openMat, parent, createUser(4L, "reply"), "대댓글");
        OpenMatCommentCreateRequest request = new OpenMatCommentCreateRequest();
        ReflectionTestUtils.setField(request, "content", "답글");
        ReflectionTestUtils.setField(request, "parentCommentId", 101L);

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(author));
        when(openMatRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(openMat));
        when(openMatCommentRepository.findByIdForUpdate(101L)).thenReturn(Optional.of(reply));

        assertThatThrownBy(() -> openMatCommentService.createComment(2L, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception).hasMessage("대댓글에는 다시 대댓글을 달 수 없습니다");
                });
        verify(openMatCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("오픈매트 대댓글 작성은 상위 댓글 작성자와 차단 관계면 거부된다")
    void createComment_whenBlockedWithParentAuthor_forbidden() {
        User author = createUser(2L, "viewer");
        User host = createUser(1L, "host");
        User parentAuthor = createUser(3L, "parent");
        OpenMat openMat = createOpenMat(10L, host);
        OpenMatComment parent = createComment(100L, openMat, null, parentAuthor, "원댓글");
        OpenMatCommentCreateRequest request = new OpenMatCommentCreateRequest();
        ReflectionTestUtils.setField(request, "content", "답글");
        ReflectionTestUtils.setField(request, "parentCommentId", 100L);

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(author));
        when(openMatRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(openMat));
        when(openMatCommentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(parent));
        when(userBlockRepository.existsByUser_IdAndBlockedUser_Id(2L, 3L)).thenReturn(true);

        assertThatThrownBy(() -> openMatCommentService.createComment(2L, 10L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("FORBIDDEN");
                    assertThat(exception).hasMessage("차단 관계에서는 댓글을 작성할 수 없습니다");
                });
        verify(openMatCommentRepository, never()).save(any());
    }

    @Test
    @DisplayName("오픈매트 새 원댓글 작성은 호스트 알림 이벤트를 발행한다")
    void createComment_rootComment_publishesNotificationEvent() {
        User author = createUser(2L, "commenter");
        User host = createUser(1L, "host");
        OpenMat openMat = createOpenMat(10L, host);
        OpenMatCommentCreateRequest request = new OpenMatCommentCreateRequest();
        OpenMatComment savedComment = createComment(100L, openMat, null, author, "질문 있습니다");
        ReflectionTestUtils.setField(request, "content", "질문 있습니다");

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(author));
        when(openMatRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(openMat));
        when(openMatCommentRepository.save(any(OpenMatComment.class))).thenReturn(savedComment);

        openMatCommentService.createComment(2L, 10L, request);

        ArgumentCaptor<OpenMatCommentNotificationEvent> captor = ArgumentCaptor.forClass(OpenMatCommentNotificationEvent.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        OpenMatCommentNotificationEvent event = captor.getValue();
        assertThat(event.recipientUserId()).isEqualTo(1L);
        assertThat(event.type()).isEqualTo(PushNotificationType.OPEN_MAT_COMMENT_CREATED);
        assertThat(event.openMatTitle()).isEqualTo("오픈매트");
    }

    @Test
    @DisplayName("내 댓글에 내가 대댓글을 달면 자기 알림은 만들지 않는다")
    void createComment_replyToOwnComment_skipsSelfNotification() {
        User author = createUser(2L, "commenter");
        User host = createUser(1L, "host");
        OpenMat openMat = createOpenMat(10L, host);
        OpenMatComment parent = createComment(100L, openMat, null, author, "원댓글");
        OpenMatComment reply = createComment(101L, openMat, parent, author, "셀프 답글");
        OpenMatCommentCreateRequest request = new OpenMatCommentCreateRequest();
        ReflectionTestUtils.setField(request, "content", "셀프 답글");
        ReflectionTestUtils.setField(request, "parentCommentId", 100L);

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(author));
        when(openMatRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(openMat));
        when(openMatCommentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(parent));
        when(openMatCommentRepository.save(any(OpenMatComment.class))).thenReturn(reply);

        openMatCommentService.createComment(2L, 10L, request);

        verify(applicationEventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("오픈매트 호스트는 자신의 오픈매트 댓글을 삭제할 수 있다")
    void deleteComment_byHost_softDeletesComment() {
        User host = createUser(1L, "host");
        OpenMat openMat = createOpenMat(10L, host);
        OpenMatComment comment = createComment(100L, openMat, null, createUser(2L, "commenter"), "원댓글");

        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(host));
        when(openMatCommentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(comment));
        when(openMatRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(openMat));

        openMatCommentService.deleteComment(1L, false, 100L);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getContent()).isNull();
    }

    @Test
    @DisplayName("오픈매트 댓글 삭제는 작성자와 호스트와 관리자 외 사용자에게 거부된다")
    void deleteComment_byUnauthorizedUser_forbidden() {
        User host = createUser(1L, "host");
        User otherUser = createUser(9L, "other");
        OpenMat openMat = createOpenMat(10L, host);
        OpenMatComment comment = createComment(100L, openMat, null, createUser(2L, "commenter"), "원댓글");

        when(userRepository.findByIdAndIsWithdrawnFalse(9L)).thenReturn(Optional.of(otherUser));
        when(openMatCommentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(comment));
        when(openMatRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(openMat));

        assertThatThrownBy(() -> openMatCommentService.deleteComment(9L, false, 100L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("FORBIDDEN");
                    assertThat(exception).hasMessage("삭제 권한이 없는 댓글입니다");
                });
    }

    @Test
    @DisplayName("오픈매트 댓글 수정은 작성자 본인만 가능하다")
    void updateComment_byNonAuthor_forbidden() {
        User host = createUser(1L, "host");
        User otherUser = createUser(9L, "other");
        OpenMat openMat = createOpenMat(10L, host);
        OpenMatComment comment = createComment(100L, openMat, null, createUser(2L, "commenter"), "원댓글");
        OpenMatCommentUpdateRequest request = new OpenMatCommentUpdateRequest();
        ReflectionTestUtils.setField(request, "content", "수정");

        when(userRepository.findByIdAndIsWithdrawnFalse(9L)).thenReturn(Optional.of(otherUser));
        when(openMatCommentRepository.findByIdForUpdate(100L)).thenReturn(Optional.of(comment));
        when(openMatRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(openMat));

        assertThatThrownBy(() -> openMatCommentService.updateComment(9L, 100L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("FORBIDDEN");
                    assertThat(exception).hasMessage("본인 댓글만 수정할 수 있습니다");
                });
    }

    @Test
    @DisplayName("오픈매트 댓글 신고는 자기 신고를 막는다")
    void reportComment_selfReport_throwsBadRequest() {
        User author = createUser(10L, "author");
        OpenMat openMat = createOpenMat(50L, createUser(1L, "host"));
        OpenMatComment comment = createComment(60L, openMat, null, author, "target");
        OpenMatCommentReportRequest request = new OpenMatCommentReportRequest();
        ReflectionTestUtils.setField(request, "reason", ReportReason.SPAM);

        when(userRepository.findByIdAndIsWithdrawnFalse(10L)).thenReturn(Optional.of(author));
        when(openMatCommentRepository.findByIdForUpdate(60L)).thenReturn(Optional.of(comment));
        when(openMatRepository.findByIdAndIsHiddenFalse(50L)).thenReturn(Optional.of(openMat));

        assertThatThrownBy(() -> openMatCommentService.reportComment(10L, 60L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("SELF_REPORT_NOT_ALLOWED");
    }

    @Test
    @DisplayName("오픈매트 댓글 신고는 중복 신고를 막는다")
    void reportComment_whenAlreadyReported_throwsBadRequest() {
        User reporter = createUser(10L, "reporter");
        User author = createUser(20L, "author");
        OpenMat openMat = createOpenMat(50L, createUser(1L, "host"));
        OpenMatComment comment = createComment(60L, openMat, null, author, "target");
        OpenMatCommentReportRequest request = new OpenMatCommentReportRequest();
        ReflectionTestUtils.setField(request, "reason", ReportReason.SPAM);

        when(userRepository.findByIdAndIsWithdrawnFalse(10L)).thenReturn(Optional.of(reporter));
        when(openMatCommentRepository.findByIdForUpdate(60L)).thenReturn(Optional.of(comment));
        when(openMatRepository.findByIdAndIsHiddenFalse(50L)).thenReturn(Optional.of(openMat));
        when(openMatCommentReportRepository.existsByComment_IdAndReporter_Id(60L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> openMatCommentService.reportComment(10L, 60L, request))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("ALREADY_REPORTED");
    }

    @Test
    @DisplayName("오픈매트 OTHER 댓글 신고는 customReason이 필요하다")
    void reportComment_whenOtherWithoutCustomReason_throwsBadRequest() {
        User reporter = createUser(10L, "reporter");
        User author = createUser(20L, "author");
        OpenMat openMat = createOpenMat(50L, createUser(1L, "host"));
        OpenMatComment comment = createComment(60L, openMat, null, author, "target");
        OpenMatCommentReportRequest request = new OpenMatCommentReportRequest();
        ReflectionTestUtils.setField(request, "reason", ReportReason.OTHER);

        when(userRepository.findByIdAndIsWithdrawnFalse(10L)).thenReturn(Optional.of(reporter));
        when(openMatCommentRepository.findByIdForUpdate(60L)).thenReturn(Optional.of(comment));
        when(openMatRepository.findByIdAndIsHiddenFalse(50L)).thenReturn(Optional.of(openMat));
        when(openMatCommentReportRepository.existsByComment_IdAndReporter_Id(60L, 10L)).thenReturn(false);

        assertThatThrownBy(() -> openMatCommentService.reportComment(10L, 60L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("기타 신고 사유를 입력해주세요");
    }

    @Test
    @DisplayName("오픈매트 댓글 신고가 3회 누적되면 원댓글과 대댓글을 함께 soft delete 한다")
    void reportComment_whenThresholdReached_softDeletesParentAndReplies() {
        User reporter = createUser(10L, "reporter");
        User author = createUser(20L, "author");
        User replyAuthor = createUser(30L, "reply");
        OpenMat openMat = createOpenMat(50L, createUser(1L, "host"));
        OpenMatComment parent = createComment(60L, openMat, null, author, "target");
        ReflectionTestUtils.setField(parent, "reportCount", 2L);
        OpenMatComment reply = createComment(61L, openMat, parent, replyAuthor, "reply");
        OpenMatCommentReportRequest request = new OpenMatCommentReportRequest();
        ReflectionTestUtils.setField(request, "reason", ReportReason.SPAM);

        when(userRepository.findByIdAndIsWithdrawnFalse(10L)).thenReturn(Optional.of(reporter));
        when(openMatCommentRepository.findByIdForUpdate(60L)).thenReturn(Optional.of(parent));
        when(openMatRepository.findByIdAndIsHiddenFalse(50L)).thenReturn(Optional.of(openMat));
        when(openMatCommentReportRepository.existsByComment_IdAndReporter_Id(60L, 10L)).thenReturn(false);
        when(openMatCommentRepository.findAllByParentComment_Id(60L)).thenReturn(List.of(reply));

        openMatCommentService.reportComment(10L, 60L, request);

        assertThat(parent.getReportCount()).isEqualTo(3L);
        assertThat(parent.isDeleted()).isTrue();
        assertThat(reply.isDeleted()).isTrue();
        assertThat(parent.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 6, 11, 21, 0));
        assertThat(reply.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 6, 11, 21, 0));
        ArgumentCaptor<OpenMatCommentReport> reportCaptor = ArgumentCaptor.forClass(OpenMatCommentReport.class);
        verify(openMatCommentReportRepository).save(reportCaptor.capture());
        assertThat(reportCaptor.getValue().getStatus()).isEqualTo(ReportStatus.RECEIVED);
    }

    @Test
    @DisplayName("관리자 댓글 신고 목록 조회는 오픈매트와 신고자 정보를 함께 반환한다")
    void findCommentReportsForAdmin_returnsAdminResponses() {
        User host = createUser(20L, "host");
        User commentAuthor = createUser(21L, "commenter");
        User reporter = createUser(22L, "reporter");
        OpenMat openMat = createOpenMat(50L, host);
        OpenMatComment comment = createComment(60L, openMat, null, commentAuthor, "신고 대상");
        OpenMatCommentReport report = OpenMatCommentReport.builder()
                .comment(comment)
                .reporter(reporter)
                .reason(ReportReason.SPAM)
                .status(ReportStatus.RECEIVED)
                .build();
        ReflectionTestUtils.setField(report, "id", 70L);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 6, 11, 20, 0));
        ReflectionTestUtils.setField(report, "updatedAt", LocalDateTime.of(2026, 6, 11, 20, 0));

        when(openMatCommentReportRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report), PageRequest.of(0, 20), 1));

        var response = openMatCommentService.findCommentReportsForAdmin(
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt", "id"))
        );

        assertThat(response.getContent()).hasSize(1);
        OpenMatCommentReportAdminResponse item = response.getContent().get(0);
        assertThat(item.getOpenMatId()).isEqualTo(50L);
        assertThat(item.getOpenMatTitle()).isEqualTo("오픈매트");
        assertThat(item.getCommentAuthorNickname()).isEqualTo("commenter");
        assertThat(item.getReporterNickname()).isEqualTo("reporter");
    }

    @Test
    @DisplayName("관리자 댓글 신고 상태 변경은 처리자와 메모를 기록한다")
    void updateCommentReportStatus_updatesMetadata() {
        User host = createUser(20L, "host");
        User commentAuthor = createUser(21L, "commenter");
        User reporter = createUser(22L, "reporter");
        OpenMat openMat = createOpenMat(50L, host);
        OpenMatComment comment = createComment(60L, openMat, null, commentAuthor, "신고 대상");
        OpenMatCommentReport report = OpenMatCommentReport.builder()
                .comment(comment)
                .reporter(reporter)
                .reason(ReportReason.SPAM)
                .status(ReportStatus.RECEIVED)
                .build();
        ReflectionTestUtils.setField(report, "id", 70L);

        ReportStatusUpdateRequest request = new ReportStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", ReportStatus.RESOLVED);
        ReflectionTestUtils.setField(request, "processingMemo", "  조치 완료  ");
        ReflectionTestUtils.setField(request, "finalAction", "  CONTENT_HIDDEN  ");

        when(openMatCommentReportRepository.findById(70L)).thenReturn(Optional.of(report));

        OpenMatCommentReportAdminResponse response = openMatCommentService.updateCommentReportStatus(1L, 70L, request);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(response.getProcessedByUserId()).isEqualTo(1L);
        assertThat(response.getProcessingMemo()).isEqualTo("조치 완료");
        assertThat(response.getFinalAction()).isEqualTo("CONTENT_HIDDEN");
        assertThat(response.getProcessedAt()).isEqualTo(LocalDateTime.of(2026, 6, 11, 21, 0));
    }

    private User createUser(Long id, String nickname) {
        User user = User.builder()
                .socialId("social-" + id)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(nickname)
                .email(nickname + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private OpenMat createOpenMat(Long id, User host) {
        OpenMat openMat = OpenMat.builder()
                .host(host)
                .title("오픈매트")
                .description("설명")
                .startDateTime(LocalDateTime.of(2026, 6, 12, 19, 0))
                .endDateTime(LocalDateTime.of(2026, 6, 12, 21, 0))
                .locationName("Rolling")
                .address("서울")
                .region(Region.SEOUL)
                .maxCapacity(10)
                .status(OpenMatStatus.RECRUITING)
                .build();
        ReflectionTestUtils.setField(openMat, "id", id);
        ReflectionTestUtils.setField(openMat, "createdAt", LocalDateTime.of(2026, 6, 11, 18, 0));
        ReflectionTestUtils.setField(openMat, "updatedAt", LocalDateTime.of(2026, 6, 11, 18, 0));
        return openMat;
    }

    private OpenMatComment createComment(Long id, OpenMat openMat, OpenMatComment parent, User author, String content) {
        OpenMatComment comment = OpenMatComment.builder()
                .openMat(openMat)
                .parentComment(parent)
                .author(author)
                .content(content)
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.of(2026, 6, 11, 18, 10));
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.of(2026, 6, 11, 18, 10));
        return comment;
    }
}
