package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.traininglog.dto.FriendRequestResponse;
import com.rolling.api.domain.traininglog.dto.FriendResponse;
import com.rolling.api.domain.traininglog.dto.FriendSearchResultResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentCreateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentUpdateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendEntryDetailResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendEntrySummaryResponse;
import com.rolling.api.domain.traininglog.entity.FriendRequest;
import com.rolling.api.domain.traininglog.entity.FriendRequestStatus;
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
import com.rolling.api.domain.user.entity.AccountStatus;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserBlockRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrainingLogSocialService {

    private static final int FRIEND_SEARCH_LIMIT = 20;
    private static final int FRIEND_SEARCH_FETCH_SIZE = 100;
    private static final Sort DEFAULT_FEED_SORT = Sort.by("trainingDate").descending()
            .and(Sort.by("createdAt").descending())
            .and(Sort.by("id").descending());

    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final FriendshipRepository friendshipRepository;
    private final FriendRequestRepository friendRequestRepository;
    private final TrainingLogEntryRepository trainingLogEntryRepository;
    private final TrainingLogLikeRepository trainingLogLikeRepository;
    private final TrainingLogCommentRepository trainingLogCommentRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<FriendSearchResultResponse> searchFriends(Long userId, String query) {
        requireActiveUser(userId);
        String normalizedQuery = normalizeSearchQuery(query);

        List<User> candidates = userRepository.searchFriendCandidates(
                userId,
                normalizedQuery,
                PageRequest.of(0, FRIEND_SEARCH_FETCH_SIZE)
        );
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Long> candidateIds = candidates.stream()
                .map(User::getId)
                .toList();
        Set<Long> friendUserIds = new HashSet<>(friendshipRepository.findFriendUserIdsByUserId(userId));
        Set<Long> pendingUserIds = new HashSet<>(friendRequestRepository.findPendingRelatedUserIds(userId));
        Set<Long> blockedRelationUserIds = new HashSet<>(userBlockRepository.findBlockedRelationUserIds(userId, candidateIds));

        return candidates.stream()
                .filter(candidate -> !friendUserIds.contains(candidate.getId()))
                .filter(candidate -> !pendingUserIds.contains(candidate.getId()))
                .filter(candidate -> !blockedRelationUserIds.contains(candidate.getId()))
                .limit(FRIEND_SEARCH_LIMIT)
                .map(FriendSearchResultResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendResponse> findFriends(Long userId) {
        requireActiveUser(userId);
        return friendshipRepository.findAllByUser_IdOrderByFriendedAtDesc(userId).stream()
                .map(FriendResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> findReceivedRequests(Long userId) {
        requireActiveUser(userId);
        return friendRequestRepository.findAllByReceiver_IdAndStatusOrderByCreatedAtDesc(userId, FriendRequestStatus.PENDING).stream()
                .map(FriendRequestResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendRequestResponse> findSentRequests(Long userId) {
        requireActiveUser(userId);
        return friendRequestRepository.findAllBySender_IdAndStatusOrderByCreatedAtDesc(userId, FriendRequestStatus.PENDING).stream()
                .map(FriendRequestResponse::from)
                .toList();
    }

    @Transactional
    public FriendRequestResponse sendFriendRequest(Long userId, Long targetUserId) {
        User sender = getActiveUser(userId);
        User receiver = getRequestableUser(targetUserId);

        validateNotSelf(userId, targetUserId);
        ensureNotBlocked(userId, targetUserId);
        ensureNotAlreadyFriends(userId, targetUserId);
        ensureNoPendingRequest(userId, targetUserId);

        FriendRequest saved = friendRequestRepository.save(FriendRequest.builder()
                .sender(sender)
                .receiver(receiver)
                .status(FriendRequestStatus.PENDING)
                .build());
        return FriendRequestResponse.from(saved);
    }

    @Transactional
    public FriendResponse acceptFriendRequest(Long userId, Long requestId) {
        User receiver = getActiveUser(userId);
        FriendRequest request = getFriendRequest(requestId);

        if (!request.getReceiver().getId().equals(userId)) {
            throw BusinessException.forbidden("본인이 받은 친구 요청만 수락할 수 있습니다");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw BusinessException.badRequest("대기 중인 친구 요청만 수락할 수 있습니다");
        }

        Long senderUserId = request.getSender().getId();
        ensureNotBlocked(userId, senderUserId);
        ensureNotAlreadyFriends(userId, senderUserId);

        LocalDateTime now = LocalDateTime.now(clock);
        request.accept(now);
        friendshipRepository.saveAll(List.of(
                Friendship.builder().user(receiver).friendUser(request.getSender()).friendedAt(now).build(),
                Friendship.builder().user(request.getSender()).friendUser(receiver).friendedAt(now).build()
        ));
        return FriendResponse.from(request.getSender(), now);
    }

    @Transactional
    public void rejectFriendRequest(Long userId, Long requestId) {
        getActiveUser(userId);
        FriendRequest request = getFriendRequest(requestId);

        if (!request.getReceiver().getId().equals(userId)) {
            throw BusinessException.forbidden("본인이 받은 친구 요청만 거절할 수 있습니다");
        }
        if (request.getStatus() != FriendRequestStatus.PENDING) {
            throw BusinessException.badRequest("대기 중인 친구 요청만 거절할 수 있습니다");
        }
        request.reject(LocalDateTime.now(clock));
    }

    @Transactional
    public void deleteFriend(Long userId, Long friendUserId) {
        getActiveUser(userId);
        if (!friendshipRepository.existsByUser_IdAndFriendUser_Id(userId, friendUserId)) {
            throw BusinessException.notFound("친구 관계를 찾을 수 없습니다");
        }

        friendshipRepository.deleteAllByUser_IdAndFriendUser_Id(userId, friendUserId);
        friendshipRepository.deleteAllByUser_IdAndFriendUser_Id(friendUserId, userId);
    }

    @Transactional(readOnly = true)
    public Page<TrainingLogFriendEntrySummaryResponse> findFriendFeed(Long userId, Pageable pageable) {
        requireActiveUser(userId);
        Page<TrainingLogEntry> entries = trainingLogEntryRepository.findFriendFeedEntries(
                userId,
                TrainingLogVisibility.FRIENDS,
                normalizeFeedPageable(pageable)
        );
        return mapSummaryPage(entries, userId);
    }

    @Transactional(readOnly = true)
    public Page<TrainingLogFriendEntrySummaryResponse> findFriendEntries(
            Long userId,
            Long friendUserId,
            LocalDate dateFrom,
            LocalDate dateTo,
            Pageable pageable
    ) {
        requireActiveUser(userId);
        validateDateRange(dateFrom, dateTo);
        ensureFriendAccessible(userId, friendUserId);

        Page<TrainingLogEntry> entries = trainingLogEntryRepository.findFriendEntriesByAuthorId(
                friendUserId,
                TrainingLogVisibility.FRIENDS,
                dateFrom,
                dateTo,
                normalizeFeedPageable(pageable)
        );
        return mapSummaryPage(entries, userId);
    }

    @Transactional(readOnly = true)
    public TrainingLogFriendEntryDetailResponse findFriendEntryDetail(Long userId, Long entryId) {
        requireActiveUser(userId);
        TrainingLogEntry entry = loadAccessibleEntry(userId, entryId);
        Map<Long, Long> likeCounts = toCountMap(trainingLogLikeRepository.countByEntryIds(List.of(entry.getId())));
        Map<Long, Long> commentCounts = toCountMap(trainingLogCommentRepository.countActiveByEntryIds(List.of(entry.getId())));
        boolean likedByMe = trainingLogLikeRepository.existsByEntry_IdAndUser_Id(entry.getId(), userId);

        return toFriendDetailResponse(
                entry,
                likeCounts.getOrDefault(entry.getId(), 0L),
                commentCounts.getOrDefault(entry.getId(), 0L),
                likedByMe,
                true
        );
    }

    @Transactional
    public void likeEntry(Long userId, Long entryId) {
        User user = getActiveUser(userId);
        TrainingLogEntry entry = loadAccessibleEntry(userId, entryId);
        if (entry.getUser().getId().equals(userId)) {
            throw BusinessException.badRequest("본인 기록에는 좋아요를 누를 수 없습니다");
        }
        if (trainingLogLikeRepository.existsByEntry_IdAndUser_Id(entryId, userId)) {
            return;
        }

        trainingLogLikeRepository.save(TrainingLogLike.builder()
                .entry(entry)
                .user(user)
                .build());
    }

    @Transactional
    public void unlikeEntry(Long userId, Long entryId) {
        TrainingLogEntry entry = loadAccessibleEntry(userId, entryId);
        if (entry.getUser().getId().equals(userId)) {
            throw BusinessException.badRequest("본인 기록에는 좋아요를 누를 수 없습니다");
        }

        trainingLogLikeRepository.findByEntry_IdAndUser_Id(entryId, userId)
                .ifPresent(trainingLogLikeRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<TrainingLogCommentResponse> findComments(Long userId, Long entryId, boolean isAdmin) {
        requireActiveUser(userId);
        TrainingLogEntry entry = loadAccessibleEntry(userId, entryId);
        return buildCommentTree(trainingLogCommentRepository.findAllByEntry_IdOrderByCreatedAtAscIdAsc(entryId), userId, isAdmin, entry.getUser().getId());
    }

    @Transactional
    public TrainingLogCommentResponse createComment(Long userId, Long entryId, TrainingLogCommentCreateRequest request) {
        User author = getActiveUser(userId);
        TrainingLogEntry entry = loadAccessibleEntry(userId, entryId);
        TrainingLogComment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = getComment(request.getParentCommentId());
            validateReplyParent(entryId, parentComment);
        }

        TrainingLogComment saved = trainingLogCommentRepository.save(TrainingLogComment.builder()
                .entry(entry)
                .parentComment(parentComment)
                .author(author)
                .content(normalizeCommentContent(request.getContent()))
                .build());

        publishCommentNotificationIfNeeded(saved, entry, parentComment, author);
        return toCommentResponse(saved, userId, false, entry.getUser().getId(), List.of());
    }

    @Transactional
    public TrainingLogCommentResponse updateComment(Long userId, Long commentId, TrainingLogCommentUpdateRequest request) {
        getActiveUser(userId);
        TrainingLogComment comment = getComment(commentId);
        if (!comment.getAuthor().getId().equals(userId)) {
            throw BusinessException.forbidden("본인 댓글만 수정할 수 있습니다");
        }
        if (comment.isDeleted()) {
            throw BusinessException.badRequest("삭제된 댓글은 수정할 수 없습니다");
        }

        comment.updateContent(normalizeCommentContent(request.getContent()));
        return toCommentResponse(comment, userId, false, comment.getEntry().getUser().getId(), List.of());
    }

    @Transactional
    public void deleteComment(Long userId, boolean isAdmin, Long commentId) {
        getActiveUser(userId);
        TrainingLogComment comment = getComment(commentId);
        boolean entryOwner = comment.getEntry().getUser().getId().equals(userId);
        boolean commentOwner = comment.getAuthor().getId().equals(userId);

        if (!isAdmin && !entryOwner && !commentOwner) {
            throw BusinessException.forbidden("삭제 권한이 없는 댓글입니다");
        }
        if (comment.isDeleted()) {
            return;
        }
        comment.softDelete(LocalDateTime.now(clock));
    }

    private Page<TrainingLogFriendEntrySummaryResponse> mapSummaryPage(Page<TrainingLogEntry> entries, Long viewerUserId) {
        List<Long> entryIds = entries.getContent().stream()
                .map(TrainingLogEntry::getId)
                .toList();
        Map<Long, Long> likeCounts = toCountMap(trainingLogLikeRepository.countByEntryIds(entryIds));
        Map<Long, Long> commentCounts = toCountMap(trainingLogCommentRepository.countActiveByEntryIds(entryIds));
        Set<Long> likedEntryIds = new HashSet<>(entryIds.isEmpty()
                ? List.<Long>of()
                : trainingLogLikeRepository.findLikedEntryIdsByUserIdAndEntryIds(viewerUserId, entryIds));

        return entries.map(entry -> TrainingLogFriendEntrySummaryResponse.builder()
                .id(entry.getId())
                .authorUserId(entry.getUser().getId())
                .authorNickname(entry.getUser().getNickname())
                .trainingDate(entry.getTrainingDate())
                .category(entry.getCategory())
                .color(entry.getColor())
                .title(entry.getTitle())
                .content(entry.getContent())
                .likeCount(likeCounts.getOrDefault(entry.getId(), 0L))
                .commentCount(commentCounts.getOrDefault(entry.getId(), 0L))
                .likedByMe(likedEntryIds.contains(entry.getId()))
                .createdAt(entry.getCreatedAt())
                .build());
    }

    private TrainingLogFriendEntryDetailResponse toFriendDetailResponse(
            TrainingLogEntry entry,
            long likeCount,
            long commentCount,
            boolean likedByMe,
            boolean commentableByMe
    ) {
        List<String> imageUrls = readImageUrls(entry.getImageUrlsJson(), entry.getImageUrl());
        return TrainingLogFriendEntryDetailResponse.builder()
                .id(entry.getId())
                .authorUserId(entry.getUser().getId())
                .authorNickname(entry.getUser().getNickname())
                .trainingDate(entry.getTrainingDate())
                .visibility(entry.getVisibility())
                .category(entry.getCategory())
                .color(entry.getColor())
                .trainingIntensity(entry.getTrainingIntensity())
                .gymAttendance(entry.getGymAttendance())
                .condition(entry.getCondition())
                .trainingMinutes(entry.getTrainingMinutes())
                .title(entry.getTitle())
                .content(entry.getContent())
                .checklist(TrainingLogJsonCodec.readChecklist(entry.getChecklistJson()))
                .hashtags(TrainingLogJsonCodec.readStringList(entry.getHashtagsJson()))
                .imageUrls(imageUrls)
                .externalLinks(TrainingLogJsonCodec.readExternalLinks(entry.getExternalLinksJson()))
                .likeCount(likeCount)
                .commentCount(commentCount)
                .likedByMe(likedByMe)
                .commentableByMe(commentableByMe)
                .createdAt(entry.getCreatedAt())
                .updatedAt(entry.getUpdatedAt())
                .build();
    }

    private List<TrainingLogCommentResponse> buildCommentTree(
            List<TrainingLogComment> comments,
            Long viewerUserId,
            boolean isAdmin,
            Long entryOwnerId
    ) {
        Map<Long, List<TrainingLogComment>> repliesByParentId = comments.stream()
                .filter(TrainingLogComment::isReply)
                .collect(Collectors.groupingBy(
                        comment -> comment.getParentComment().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return comments.stream()
                .filter(comment -> !comment.isReply())
                .map(comment -> toCommentResponse(
                        comment,
                        viewerUserId,
                        isAdmin,
                        entryOwnerId,
                        repliesByParentId.getOrDefault(comment.getId(), List.of()).stream()
                                .map(reply -> toCommentResponse(reply, viewerUserId, isAdmin, entryOwnerId, List.of()))
                                .toList()
                ))
                .toList();
    }

    private TrainingLogCommentResponse toCommentResponse(
            TrainingLogComment comment,
            Long viewerUserId,
            boolean isAdmin,
            Long entryOwnerId,
            List<TrainingLogCommentResponse> replies
    ) {
        boolean editableByMe = !comment.isDeleted() && comment.getAuthor().getId().equals(viewerUserId);
        boolean deletableByMe = !comment.isDeleted() && (
                isAdmin
                        || comment.getAuthor().getId().equals(viewerUserId)
                        || entryOwnerId.equals(viewerUserId)
        );

        return TrainingLogCommentResponse.builder()
                .id(comment.getId())
                .entryId(comment.getEntry().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .authorUserId(comment.getAuthor().getId())
                .authorNickname(comment.getAuthor().getNickname())
                .content(comment.isDeleted() ? null : comment.getContent())
                .deleted(comment.isDeleted())
                .editableByMe(editableByMe)
                .deletableByMe(deletableByMe)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .replies(replies)
                .build();
    }

    private void publishCommentNotificationIfNeeded(
            TrainingLogComment savedComment,
            TrainingLogEntry entry,
            TrainingLogComment parentComment,
            User author
    ) {
        Long recipientUserId = parentComment == null ? entry.getUser().getId() : parentComment.getAuthor().getId();
        if (recipientUserId.equals(author.getId())) {
            return;
        }

        PushNotificationType type = parentComment == null
                ? PushNotificationType.TRAINING_LOG_COMMENT_CREATED
                : PushNotificationType.TRAINING_LOG_COMMENT_REPLY_CREATED;

        applicationEventPublisher.publishEvent(new TrainingLogCommentNotificationEvent(
                entry.getId(),
                recipientUserId,
                author.getId(),
                author.getNickname(),
                entry.getTitle(),
                type
        ));
    }

    private void validateReplyParent(Long entryId, TrainingLogComment parentComment) {
        if (!parentComment.getEntry().getId().equals(entryId)) {
            throw BusinessException.badRequest("parentCommentId는 같은 훈련일지의 댓글이어야 합니다");
        }
        if (parentComment.isReply()) {
            throw BusinessException.badRequest("대댓글에는 다시 대댓글을 달 수 없습니다");
        }
        if (parentComment.isDeleted()) {
            throw BusinessException.badRequest("삭제된 댓글에는 답글을 달 수 없습니다");
        }
    }

    private Map<Long, Long> toCountMap(Collection<TrainingLogCountProjection> counts) {
        Map<Long, Long> result = new HashMap<>();
        for (TrainingLogCountProjection count : counts) {
            result.put(count.getEntryId(), count.getCount());
        }
        return result;
    }

    private TrainingLogEntry loadAccessibleEntry(Long viewerUserId, Long entryId) {
        TrainingLogEntry entry = trainingLogEntryRepository.findWithUserById(entryId)
                .orElseThrow(() -> BusinessException.notFound("훈련 기록을 찾을 수 없습니다"));
        Long ownerUserId = entry.getUser().getId();
        if (ownerUserId.equals(viewerUserId)) {
            return entry;
        }
        if (!isVisibleAuthor(entry.getUser())) {
            throw BusinessException.notFound("훈련 기록을 찾을 수 없습니다");
        }
        if (entry.getVisibility() != TrainingLogVisibility.FRIENDS) {
            throw BusinessException.forbidden("친구만 조회할 수 있는 기록입니다");
        }
        ensureFriendAccessible(viewerUserId, ownerUserId);
        return entry;
    }

    private void ensureFriendAccessible(Long viewerUserId, Long friendUserId) {
        if (viewerUserId.equals(friendUserId)) {
            return;
        }
        if (isBlockedBetween(viewerUserId, friendUserId)) {
            throw BusinessException.forbidden("차단 관계에서는 친구 기능을 사용할 수 없습니다");
        }
        if (!friendshipRepository.existsByUser_IdAndFriendUser_Id(viewerUserId, friendUserId)) {
            throw BusinessException.forbidden("친구 기록만 조회할 수 있습니다");
        }
        if (!isVisibleAuthor(getRequestableUser(friendUserId))) {
            throw BusinessException.notFound("사용자를 찾을 수 없습니다");
        }
    }

    private boolean isBlockedBetween(Long firstUserId, Long secondUserId) {
        return userBlockRepository.existsByUser_IdAndBlockedUser_Id(firstUserId, secondUserId)
                || userBlockRepository.existsByUser_IdAndBlockedUser_Id(secondUserId, firstUserId);
    }

    private void ensureNotBlocked(Long firstUserId, Long secondUserId) {
        if (isBlockedBetween(firstUserId, secondUserId)) {
            throw BusinessException.forbidden("차단 관계에서는 친구 기능을 사용할 수 없습니다");
        }
    }

    private void ensureNotAlreadyFriends(Long userId, Long otherUserId) {
        if (friendshipRepository.existsByUser_IdAndFriendUser_Id(userId, otherUserId)) {
            throw BusinessException.badRequest("이미 친구입니다");
        }
    }

    private void ensureNoPendingRequest(Long firstUserId, Long secondUserId) {
        if (friendRequestRepository.existsBetweenUsersWithStatus(firstUserId, secondUserId, FriendRequestStatus.PENDING)) {
            throw BusinessException.badRequest("이미 대기 중인 친구 요청이 있습니다");
        }
    }

    private void validateNotSelf(Long userId, Long otherUserId) {
        if (userId.equals(otherUserId)) {
            throw BusinessException.badRequest("자기 자신에게는 친구 요청을 보낼 수 없습니다");
        }
    }

    private String normalizeSearchQuery(String query) {
        if (!StringUtils.hasText(query)) {
            throw BusinessException.badRequest("q는 2자 이상이어야 합니다");
        }
        String normalized = query.trim();
        if (normalized.length() < 2) {
            throw BusinessException.badRequest("q는 2자 이상이어야 합니다");
        }
        return normalized;
    }

    private String normalizeCommentContent(String content) {
        if (!StringUtils.hasText(content)) {
            throw BusinessException.badRequest("content는 1자 이상 1000자 이하여야 합니다");
        }
        String normalized = content.trim();
        if (normalized.isEmpty() || normalized.length() > 1000) {
            throw BusinessException.badRequest("content는 1자 이상 1000자 이하여야 합니다");
        }
        return normalized;
    }

    private List<String> readImageUrls(String imageUrlsJson, String imageUrl) {
        List<String> imageUrls = TrainingLogJsonCodec.readStringList(imageUrlsJson);
        if (!imageUrls.isEmpty()) {
            return imageUrls;
        }
        if (StringUtils.hasText(imageUrl)) {
            return List.of(imageUrl);
        }
        return List.of();
    }

    private Pageable normalizeFeedPageable(Pageable pageable) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), DEFAULT_FEED_SORT);
    }

    private void validateDateRange(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw BusinessException.badRequest("dateFrom은 dateTo보다 늦을 수 없습니다");
        }
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));
    }

    private User getRequestableUser(Long userId) {
        return userRepository.findByIdAndIsWithdrawnFalseAndWithdrawalPendingFalseAndAccountStatus(userId, AccountStatus.ACTIVE)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));
    }

    private void requireActiveUser(Long userId) {
        if (!userRepository.existsByIdAndIsWithdrawnFalse(userId)) {
            throw BusinessException.notFound("사용자를 찾을 수 없습니다");
        }
    }

    private boolean isVisibleAuthor(User user) {
        return !user.getIsWithdrawn()
                && !user.getWithdrawalPending()
                && user.getAccountStatus() == AccountStatus.ACTIVE;
    }

    private FriendRequest getFriendRequest(Long requestId) {
        return friendRequestRepository.findById(requestId)
                .orElseThrow(() -> BusinessException.notFound("친구 요청을 찾을 수 없습니다"));
    }

    private TrainingLogComment getComment(Long commentId) {
        return trainingLogCommentRepository.findById(commentId)
                .orElseThrow(() -> BusinessException.notFound("댓글을 찾을 수 없습니다"));
    }
}
