package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.traininglog.dto.FriendRequestResponse;
import com.rolling.api.domain.traininglog.dto.FriendSearchResultResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentCreateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendSharingUpdateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendEntrySummaryResponse;
import com.rolling.api.domain.traininglog.entity.FriendRequest;
import com.rolling.api.domain.traininglog.entity.FriendRequestStatus;
import com.rolling.api.domain.traininglog.entity.FriendSearchRelationshipStatus;
import com.rolling.api.domain.traininglog.entity.Friendship;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogComment;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import com.rolling.api.domain.traininglog.entity.TrainingLogLike;
import com.rolling.api.domain.traininglog.entity.TrainingLogVisibility;
import com.rolling.api.domain.traininglog.event.TrainingLogCommentNotificationEvent;
import com.rolling.api.domain.traininglog.repository.FriendRequestRepository;
import com.rolling.api.domain.traininglog.repository.FriendshipRepository;
import com.rolling.api.domain.traininglog.repository.TrainingLogCommentRepository;
import com.rolling.api.domain.traininglog.repository.TrainingLogCountProjection;
import com.rolling.api.domain.traininglog.repository.TrainingLogEntryRepository;
import com.rolling.api.domain.traininglog.repository.TrainingLogLikeRepository;
import com.rolling.api.domain.traininglog.repository.UserTrainingLogShareSettingRepository;
import com.rolling.api.domain.user.entity.AccountStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TrainingLogSocialServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserBlockRepository userBlockRepository;

    @Mock
    private FriendshipRepository friendshipRepository;

    @Mock
    private FriendRequestRepository friendRequestRepository;

    @Mock
    private TrainingLogEntryRepository trainingLogEntryRepository;

    @Mock
    private TrainingLogLikeRepository trainingLogLikeRepository;

    @Mock
    private TrainingLogCommentRepository trainingLogCommentRepository;

    @Mock
    private UserTrainingLogShareSettingRepository userTrainingLogShareSettingRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private TrainingLogSocialService trainingLogSocialService;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-05-23T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        trainingLogSocialService = new TrainingLogSocialService(
                userRepository,
                userBlockRepository,
                friendshipRepository,
                friendRequestRepository,
                trainingLogEntryRepository,
                trainingLogLikeRepository,
                trainingLogCommentRepository,
                userTrainingLogShareSettingRepository,
                applicationEventPublisher,
                clock
        );
    }

    @Test
    @DisplayName("친구 검색은 관계 상태와 보낸 요청 id를 함께 응답한다")
    void searchFriends_returnsRelationshipStates() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(1L)).willReturn(true);
        User friend = createUser(2L, "민준");
        User pendingSent = createUser(3L, "민석");
        User pendingReceived = createUser(4L, "민호");
        User blocked = createUser(5L, "민성");
        given(userRepository.searchFriendCandidates(eq(1L), eq("min"), any())).willReturn(List.of(
                friend,
                pendingSent,
                pendingReceived,
                blocked
        ));
        given(friendshipRepository.findFriendUserIdsByUserId(1L)).willReturn(List.of(2L));
        FriendRequest outgoing = FriendRequest.builder()
                .sender(createUser(1L, "viewer"))
                .receiver(pendingSent)
                .status(FriendRequestStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(outgoing, "id", 88L);
        FriendRequest incoming = FriendRequest.builder()
                .sender(pendingReceived)
                .receiver(createUser(1L, "viewer"))
                .status(FriendRequestStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(incoming, "id", 89L);
        given(friendRequestRepository.findPendingRequestsBetweenUserAndCandidates(1L, List.of(2L, 3L, 4L, 5L)))
                .willReturn(List.of(outgoing, incoming));
        given(userBlockRepository.findBlockedRelationUserIds(1L, List.of(2L, 3L, 4L, 5L))).willReturn(List.of(5L));

        List<FriendSearchResultResponse> response = trainingLogSocialService.searchFriends(1L, "min");

        assertThat(response).extracting(FriendSearchResultResponse::getUserId).containsExactly(2L, 3L, 4L);
        assertThat(response).extracting(FriendSearchResultResponse::getFriendRequestStatus)
                .containsExactly(
                        FriendSearchRelationshipStatus.FRIEND,
                        FriendSearchRelationshipStatus.PENDING_SENT,
                        FriendSearchRelationshipStatus.PENDING_RECEIVED
                );
        assertThat(response.get(1).getOutgoingRequestId()).isEqualTo(88L);
        assertThat(response.get(2).getOutgoingRequestId()).isNull();
    }

    @Test
    @DisplayName("친구 요청 수락은 양방향 friendship row를 생성한다")
    void acceptFriendRequest_createsBidirectionalFriendships() {
        User receiver = createUser(10L, "receiver");
        User sender = createUser(20L, "sender");
        FriendRequest request = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(request, "id", 100L);

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findById(100L)).willReturn(Optional.of(request));
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(10L, 20L)).willReturn(false);
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(20L, 10L)).willReturn(false);
        given(friendshipRepository.existsByUser_IdAndFriendUser_Id(10L, 20L)).willReturn(false);

        trainingLogSocialService.acceptFriendRequest(10L, 100L);

        ArgumentCaptor<List<Friendship>> captor = ArgumentCaptor.forClass(List.class);
        verify(friendshipRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue()).extracting(friendship -> friendship.getUser().getId()).containsExactly(10L, 20L);
        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.ACCEPTED);
        assertThat(request.getRespondedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 12, 0));
    }

    @Test
    @DisplayName("친구 요청 전송은 pending 요청을 저장한다")
    void sendFriendRequest_savesPendingRequest() {
        User sender = createUser(10L, "sender");
        User receiver = createUser(20L, "receiver");
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(sender));
        given(userRepository.findByIdAndIsWithdrawnFalseAndWithdrawalPendingFalseAndAccountStatus(20L, AccountStatus.ACTIVE))
                .willReturn(Optional.of(receiver));
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(10L, 20L)).willReturn(false);
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(20L, 10L)).willReturn(false);
        given(friendshipRepository.existsByUser_IdAndFriendUser_Id(10L, 20L)).willReturn(false);
        given(friendRequestRepository.existsBetweenUsersWithStatus(10L, 20L, FriendRequestStatus.PENDING)).willReturn(false);
        given(friendRequestRepository.save(any(FriendRequest.class))).willAnswer(invocation -> {
            FriendRequest saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 99L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 5, 23, 12, 0));
            return saved;
        });

        FriendRequestResponse response = trainingLogSocialService.sendFriendRequest(10L, 20L);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getSenderUserId()).isEqualTo(10L);
        assertThat(response.getReceiverUserId()).isEqualTo(20L);
        assertThat(response.getStatus()).isEqualTo(FriendRequestStatus.PENDING);
    }

    @Test
    @DisplayName("친구 요청 거절은 상태와 응답 시각을 변경한다")
    void rejectFriendRequest_updatesStatus() {
        User receiver = createUser(10L, "receiver");
        User sender = createUser(20L, "sender");
        FriendRequest request = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(request, "id", 100L);

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(receiver));
        given(friendRequestRepository.findById(100L)).willReturn(Optional.of(request));

        trainingLogSocialService.rejectFriendRequest(10L, 100L);

        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.REJECTED);
        assertThat(request.getRespondedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 12, 0));
    }

    @Test
    @DisplayName("친구 요청 취소는 본인이 보낸 pending 요청만 canceled로 변경한다")
    void cancelFriendRequest_updatesStatus() {
        User sender = createUser(10L, "sender");
        User receiver = createUser(20L, "receiver");
        FriendRequest request = FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build();
        ReflectionTestUtils.setField(request, "id", 100L);

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(sender));
        given(friendRequestRepository.findById(100L)).willReturn(Optional.of(request));

        trainingLogSocialService.cancelFriendRequest(10L, 100L);

        assertThat(request.getStatus()).isEqualTo(FriendRequestStatus.CANCELED);
        assertThat(request.getRespondedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 12, 0));
    }

    @Test
    @DisplayName("친구 삭제는 양방향 friendship row를 함께 삭제한다")
    void deleteFriend_deletesBothDirections() {
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(createUser(10L, "user")));
        given(friendshipRepository.existsByUser_IdAndFriendUser_Id(10L, 20L)).willReturn(true);

        trainingLogSocialService.deleteFriend(10L, 20L);

        verify(friendshipRepository).deleteAllByUser_IdAndFriendUser_Id(10L, 20L);
        verify(friendshipRepository).deleteAllByUser_IdAndFriendUser_Id(20L, 10L);
    }

    @Test
    @DisplayName("친구 피드는 좋아요 여부와 댓글 수를 함께 매핑한다")
    void findFriendFeed_mapsReactionMetrics() {
        User friend = createUser(2L, "friend");
        TrainingLogEntry entry = createEntry(30L, friend, TrainingLogVisibility.FRIENDS);

        given(userRepository.existsByIdAndIsWithdrawnFalse(1L)).willReturn(true);
        given(trainingLogEntryRepository.findFriendFeedEntries(eq(1L), any()))
                .willReturn(new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1));
        given(trainingLogLikeRepository.countByEntryIds(List.of(30L))).willReturn(List.of(countProjection(30L, 2L)));
        given(trainingLogCommentRepository.countActiveByEntryIds(List.of(30L))).willReturn(List.of(countProjection(30L, 3L)));
        given(trainingLogLikeRepository.findLikedEntryIdsByUserIdAndEntryIds(1L, List.of(30L))).willReturn(List.of(30L));

        List<TrainingLogFriendEntrySummaryResponse> content = trainingLogSocialService.findFriendFeed(1L, PageRequest.of(0, 20)).getContent();

        assertThat(content).hasSize(1);
        assertThat(content.get(0).getLikeCount()).isEqualTo(2L);
        assertThat(content.get(0).getCommentCount()).isEqualTo(3L);
        assertThat(content.get(0).isLikedByMe()).isTrue();
    }

    @Test
    @DisplayName("본인 기록에는 좋아요를 누를 수 없다")
    void likeEntry_whenOwnEntry_throwsValidationError() {
        User owner = createUser(10L, "owner");
        TrainingLogEntry entry = createEntry(40L, owner, TrainingLogVisibility.PRIVATE);
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(owner));
        given(trainingLogEntryRepository.findWithUserById(40L)).willReturn(Optional.of(entry));

        assertThatThrownBy(() -> trainingLogSocialService.likeEntry(10L, 40L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("본인 기록에는 좋아요를 누를 수 없습니다");
    }

    @Test
    @DisplayName("이미 좋아요한 기록에 다시 좋아요를 누르면 중복 저장하지 않는다")
    void likeEntry_whenAlreadyLiked_doesNotSaveDuplicate() {
        User viewer = createUser(10L, "viewer");
        User owner = createUser(20L, "owner");
        TrainingLogEntry entry = createEntry(40L, owner, TrainingLogVisibility.FRIENDS);
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(viewer));
        given(trainingLogEntryRepository.findWithUserById(40L)).willReturn(Optional.of(entry));
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(10L, 20L)).willReturn(false);
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(20L, 10L)).willReturn(false);
        given(friendshipRepository.existsByUser_IdAndFriendUser_Id(10L, 20L)).willReturn(true);
        given(userRepository.findByIdAndIsWithdrawnFalseAndWithdrawalPendingFalseAndAccountStatus(20L, AccountStatus.ACTIVE))
                .willReturn(Optional.of(owner));
        given(userTrainingLogShareSettingRepository.existsByUser_IdAndShareWithFriendsTrue(20L)).willReturn(true);
        given(trainingLogLikeRepository.existsByEntry_IdAndUser_Id(40L, 10L)).willReturn(true);

        trainingLogSocialService.likeEntry(10L, 40L);

        verify(trainingLogLikeRepository, never()).save(any(TrainingLogLike.class));
    }

    @Test
    @DisplayName("친구가 아닌 사용자는 FRIENDS 기록 상세를 조회할 수 없다")
    void findFriendEntryDetail_whenNotFriend_throwsForbidden() {
        User owner = createUser(20L, "owner");
        TrainingLogEntry entry = createEntry(40L, owner, TrainingLogVisibility.FRIENDS);
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findWithUserById(40L)).willReturn(Optional.of(entry));
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(10L, 20L)).willReturn(false);
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(20L, 10L)).willReturn(false);
        given(friendshipRepository.existsByUser_IdAndFriendUser_Id(10L, 20L)).willReturn(false);

        assertThatThrownBy(() -> trainingLogSocialService.findFriendEntryDetail(10L, 40L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("친구 기록만 조회할 수 있습니다");
    }

    @Test
    @DisplayName("차단 관계에서는 FRIENDS 기록 상세를 조회할 수 없다")
    void findFriendEntryDetail_whenBlocked_throwsForbidden() {
        User owner = createUser(20L, "owner");
        TrainingLogEntry entry = createEntry(40L, owner, TrainingLogVisibility.FRIENDS);
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findWithUserById(40L)).willReturn(Optional.of(entry));
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(10L, 20L)).willReturn(true);

        assertThatThrownBy(() -> trainingLogSocialService.findFriendEntryDetail(10L, 40L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("차단 관계에서는 친구 기능을 사용할 수 없습니다");
    }

    @Test
    @DisplayName("친구 공개 설정이 꺼져 있으면 친구도 상세 조회할 수 없다")
    void findFriendEntryDetail_whenFriendSharingOff_throwsForbidden() {
        User owner = createUser(20L, "owner");
        TrainingLogEntry entry = createEntry(40L, owner, TrainingLogVisibility.PRIVATE);
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findWithUserById(40L)).willReturn(Optional.of(entry));
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(10L, 20L)).willReturn(false);
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(20L, 10L)).willReturn(false);
        given(friendshipRepository.existsByUser_IdAndFriendUser_Id(10L, 20L)).willReturn(true);
        given(userRepository.findByIdAndIsWithdrawnFalseAndWithdrawalPendingFalseAndAccountStatus(20L, AccountStatus.ACTIVE))
                .willReturn(Optional.of(owner));
        given(userTrainingLogShareSettingRepository.existsByUser_IdAndShareWithFriendsTrue(20L)).willReturn(false);

        assertThatThrownBy(() -> trainingLogSocialService.findFriendEntryDetail(10L, 40L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("친구 공개가 설정된 기록만 조회할 수 있습니다");
    }

    @Test
    @DisplayName("댓글 작성은 기록 작성자에게 댓글 알림 이벤트를 발행한다")
    void createComment_commentPublishesNotificationEvent() {
        User commenter = createUser(10L, "commenter");
        User entryOwner = createUser(20L, "entry-owner");
        TrainingLogEntry entry = createEntry(50L, entryOwner, TrainingLogVisibility.FRIENDS);

        TrainingLogCommentCreateRequest request = new TrainingLogCommentCreateRequest();
        ReflectionTestUtils.setField(request, "content", "  nice work  ");

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(commenter));
        given(trainingLogEntryRepository.findWithUserById(50L)).willReturn(Optional.of(entry));
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(10L, 20L)).willReturn(false);
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(20L, 10L)).willReturn(false);
        given(friendshipRepository.existsByUser_IdAndFriendUser_Id(10L, 20L)).willReturn(true);
        given(userRepository.findByIdAndIsWithdrawnFalseAndWithdrawalPendingFalseAndAccountStatus(20L, AccountStatus.ACTIVE))
                .willReturn(Optional.of(entryOwner));
        given(userTrainingLogShareSettingRepository.existsByUser_IdAndShareWithFriendsTrue(20L)).willReturn(true);
        given(trainingLogCommentRepository.save(any(TrainingLogComment.class))).willAnswer(invocation -> {
            TrainingLogComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 70L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 5, 23, 12, 0));
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.of(2026, 5, 23, 12, 0));
            return saved;
        });

        trainingLogSocialService.createComment(10L, 50L, request);

        ArgumentCaptor<TrainingLogCommentNotificationEvent> eventCaptor = ArgumentCaptor.forClass(TrainingLogCommentNotificationEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().recipientUserId()).isEqualTo(20L);
        assertThat(eventCaptor.getValue().type()).isEqualTo(PushNotificationType.TRAINING_LOG_COMMENT_CREATED);
    }

    @Test
    @DisplayName("대댓글 작성은 상위 댓글 작성자에게 알림 이벤트를 발행한다")
    void createComment_replyPublishesNotificationEvent() {
        User replier = createUser(10L, "reply-user");
        User entryOwner = createUser(20L, "entry-owner");
        User parentAuthor = createUser(30L, "parent-author");
        TrainingLogEntry entry = createEntry(50L, entryOwner, TrainingLogVisibility.FRIENDS);
        TrainingLogComment parentComment = TrainingLogComment.builder()
                .entry(entry)
                .author(parentAuthor)
                .content("parent")
                .build();
        ReflectionTestUtils.setField(parentComment, "id", 60L);
        ReflectionTestUtils.setField(parentComment, "createdAt", LocalDateTime.of(2026, 5, 23, 11, 0));
        ReflectionTestUtils.setField(parentComment, "updatedAt", LocalDateTime.of(2026, 5, 23, 11, 0));

        TrainingLogCommentCreateRequest request = new TrainingLogCommentCreateRequest();
        ReflectionTestUtils.setField(request, "content", "  reply  ");
        ReflectionTestUtils.setField(request, "parentCommentId", 60L);

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(replier));
        given(trainingLogEntryRepository.findWithUserById(50L)).willReturn(Optional.of(entry));
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(10L, 20L)).willReturn(false);
        given(userBlockRepository.existsByUser_IdAndBlockedUser_Id(20L, 10L)).willReturn(false);
        given(friendshipRepository.existsByUser_IdAndFriendUser_Id(10L, 20L)).willReturn(true);
        given(userRepository.findByIdAndIsWithdrawnFalseAndWithdrawalPendingFalseAndAccountStatus(20L, AccountStatus.ACTIVE))
                .willReturn(Optional.of(entryOwner));
        given(userTrainingLogShareSettingRepository.existsByUser_IdAndShareWithFriendsTrue(20L)).willReturn(true);
        given(trainingLogCommentRepository.findById(60L)).willReturn(Optional.of(parentComment));
        given(trainingLogCommentRepository.save(any(TrainingLogComment.class))).willAnswer(invocation -> {
            TrainingLogComment saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 70L);
            ReflectionTestUtils.setField(saved, "createdAt", LocalDateTime.of(2026, 5, 23, 12, 0));
            ReflectionTestUtils.setField(saved, "updatedAt", LocalDateTime.of(2026, 5, 23, 12, 0));
            return saved;
        });

        TrainingLogCommentResponse response = trainingLogSocialService.createComment(10L, 50L, request);

        ArgumentCaptor<TrainingLogCommentNotificationEvent> eventCaptor = ArgumentCaptor.forClass(TrainingLogCommentNotificationEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(response.getParentCommentId()).isEqualTo(60L);
        assertThat(eventCaptor.getValue().recipientUserId()).isEqualTo(30L);
        assertThat(eventCaptor.getValue().type()).isEqualTo(PushNotificationType.TRAINING_LOG_COMMENT_REPLY_CREATED);
    }

    @Test
    @DisplayName("댓글 수정은 본인 댓글 본문을 변경한다")
    void updateComment_updatesOwnComment() {
        User author = createUser(10L, "author");
        TrainingLogEntry entry = createEntry(50L, author, TrainingLogVisibility.PRIVATE);
        TrainingLogComment comment = TrainingLogComment.builder()
                .entry(entry)
                .author(author)
                .content("before")
                .build();
        ReflectionTestUtils.setField(comment, "id", 60L);
        ReflectionTestUtils.setField(comment, "createdAt", LocalDateTime.of(2026, 5, 23, 11, 0));
        ReflectionTestUtils.setField(comment, "updatedAt", LocalDateTime.of(2026, 5, 23, 11, 0));

        com.rolling.api.domain.traininglog.dto.TrainingLogCommentUpdateRequest request =
                new com.rolling.api.domain.traininglog.dto.TrainingLogCommentUpdateRequest();
        ReflectionTestUtils.setField(request, "content", "  after  ");

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(author));
        given(trainingLogCommentRepository.findById(60L)).willReturn(Optional.of(comment));

        TrainingLogCommentResponse response = trainingLogSocialService.updateComment(10L, 60L, request);

        assertThat(comment.getContent()).isEqualTo("after");
        assertThat(response.getContent()).isEqualTo("after");
    }

    @Test
    @DisplayName("기록 작성자는 내 기록의 댓글을 soft delete 할 수 있다")
    void deleteComment_byEntryOwner_softDeletesComment() {
        User entryOwner = createUser(10L, "owner");
        User commenter = createUser(20L, "commenter");
        TrainingLogEntry entry = createEntry(50L, entryOwner, TrainingLogVisibility.PRIVATE);
        TrainingLogComment comment = TrainingLogComment.builder()
                .entry(entry)
                .author(commenter)
                .content("comment")
                .build();
        ReflectionTestUtils.setField(comment, "id", 60L);

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(entryOwner));
        given(trainingLogCommentRepository.findById(60L)).willReturn(Optional.of(comment));

        trainingLogSocialService.deleteComment(10L, false, 60L);

        assertThat(comment.isDeleted()).isTrue();
        assertThat(comment.getContent()).isNull();
        assertThat(comment.getDeletedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 12, 0));
    }

    @Test
    @DisplayName("대댓글에는 다시 대댓글을 달 수 없다")
    void createComment_replyToReply_throwsValidationError() {
        User owner = createUser(10L, "owner");
        TrainingLogEntry entry = createEntry(50L, owner, TrainingLogVisibility.PRIVATE);
        TrainingLogComment parentReply = TrainingLogComment.builder()
                .entry(entry)
                .parentComment(TrainingLogComment.builder().entry(entry).author(owner).content("root").build())
                .author(owner)
                .content("reply")
                .build();
        ReflectionTestUtils.setField(parentReply, "id", 60L);

        TrainingLogCommentCreateRequest request = new TrainingLogCommentCreateRequest();
        ReflectionTestUtils.setField(request, "content", "nested");
        ReflectionTestUtils.setField(request, "parentCommentId", 60L);

        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(owner));
        given(trainingLogEntryRepository.findWithUserById(50L)).willReturn(Optional.of(entry));
        given(trainingLogCommentRepository.findById(60L)).willReturn(Optional.of(parentReply));

        assertThatThrownBy(() -> trainingLogSocialService.createComment(10L, 50L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("대댓글에는 다시 대댓글을 달 수 없습니다");
    }

    @Test
    @DisplayName("삭제된 원댓글은 placeholder로 유지하고 replies를 함께 반환한다")
    void findComments_keepsDeletedParentPlaceholder() {
        User owner = createUser(10L, "owner");
        User replier = createUser(20L, "replier");
        TrainingLogEntry entry = createEntry(50L, owner, TrainingLogVisibility.PRIVATE);
        TrainingLogComment parent = TrainingLogComment.builder()
                .entry(entry)
                .author(owner)
                .content("parent")
                .deleted(true)
                .deletedAt(LocalDateTime.of(2026, 5, 23, 11, 30))
                .build();
        ReflectionTestUtils.setField(parent, "id", 60L);
        ReflectionTestUtils.setField(parent, "content", null);
        ReflectionTestUtils.setField(parent, "createdAt", LocalDateTime.of(2026, 5, 23, 11, 0));
        ReflectionTestUtils.setField(parent, "updatedAt", LocalDateTime.of(2026, 5, 23, 11, 30));

        TrainingLogComment reply = TrainingLogComment.builder()
                .entry(entry)
                .parentComment(parent)
                .author(replier)
                .content("reply")
                .build();
        ReflectionTestUtils.setField(reply, "id", 61L);
        ReflectionTestUtils.setField(reply, "createdAt", LocalDateTime.of(2026, 5, 23, 11, 10));
        ReflectionTestUtils.setField(reply, "updatedAt", LocalDateTime.of(2026, 5, 23, 11, 10));

        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findWithUserById(50L)).willReturn(Optional.of(entry));
        given(trainingLogCommentRepository.findAllByEntry_IdOrderByCreatedAtAscIdAsc(50L)).willReturn(List.of(parent, reply));

        List<TrainingLogCommentResponse> response = trainingLogSocialService.findComments(10L, 50L, false);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).isDeleted()).isTrue();
        assertThat(response.get(0).getContent()).isNull();
        assertThat(response.get(0).getReplies()).hasSize(1);
        assertThat(response.get(0).getReplies().get(0).getContent()).isEqualTo("reply");
    }

    @Test
    @DisplayName("친구 공개 설정 조회는 설정이 없으면 기본 비공개를 반환한다")
    void getFriendSharing_withoutSetting_returnsDefaultPrivate() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(userTrainingLogShareSettingRepository.findByUser_Id(10L)).willReturn(Optional.empty());

        var response = trainingLogSocialService.getFriendSharing(10L);

        assertThat(response.getShareWithFriends()).isFalse();
        assertThat(response.getUpdatedAt()).isNull();
    }

    @Test
    @DisplayName("친구 공개 설정 변경은 새 설정 row를 생성한다")
    void updateFriendSharing_createsSettingWhenMissing() {
        User owner = createUser(10L, "owner");
        TrainingLogFriendSharingUpdateRequest request = new TrainingLogFriendSharingUpdateRequest();
        ReflectionTestUtils.setField(request, "shareWithFriends", true);
        given(userRepository.findByIdAndIsWithdrawnFalse(10L)).willReturn(Optional.of(owner));
        given(userTrainingLogShareSettingRepository.findByUser_Id(10L)).willReturn(Optional.empty());
        given(userTrainingLogShareSettingRepository.save(any())).willAnswer(invocation -> {
            var setting = invocation.getArgument(0, com.rolling.api.domain.traininglog.entity.UserTrainingLogShareSetting.class);
            ReflectionTestUtils.setField(setting, "id", 1L);
            ReflectionTestUtils.setField(setting, "createdAt", LocalDateTime.of(2026, 5, 23, 12, 0));
            ReflectionTestUtils.setField(setting, "updatedAt", LocalDateTime.of(2026, 5, 23, 12, 0));
            return setting;
        });
        given(userTrainingLogShareSettingRepository.saveAndFlush(any())).willAnswer(invocation -> {
            var setting = invocation.getArgument(0, com.rolling.api.domain.traininglog.entity.UserTrainingLogShareSetting.class);
            ReflectionTestUtils.setField(setting, "updatedAt", LocalDateTime.of(2026, 5, 23, 12, 5));
            return setting;
        });

        var response = trainingLogSocialService.updateFriendSharing(10L, request);

        assertThat(response.getShareWithFriends()).isTrue();
        assertThat(response.getUpdatedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 12, 5));
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

    private TrainingLogEntry createEntry(Long id, User owner, TrainingLogVisibility visibility) {
        TrainingLogEntry entry = TrainingLogEntry.builder()
                .user(owner)
                .trainingDate(LocalDate.of(2026, 5, 22))
                .category(TrainingLogCategory.TECHNIQUE)
                .title("shared log")
                .content("shared content")
                .visibility(visibility)
                .build();
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "createdAt", LocalDateTime.of(2026, 5, 22, 10, 0));
        ReflectionTestUtils.setField(entry, "updatedAt", LocalDateTime.of(2026, 5, 22, 10, 0));
        return entry;
    }

    private TrainingLogCountProjection countProjection(Long entryId, Long count) {
        return new TrainingLogCountProjection() {
            @Override
            public Long getEntryId() {
                return entryId;
            }

            @Override
            public Long getCount() {
                return count;
            }
        };
    }
}
