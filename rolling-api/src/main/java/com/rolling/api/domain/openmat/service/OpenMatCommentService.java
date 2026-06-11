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
import com.rolling.api.domain.openmat.event.OpenMatCommentNotificationEvent;
import com.rolling.api.domain.openmat.repository.OpenMatCommentReportRepository;
import com.rolling.api.domain.openmat.repository.OpenMatCommentRepository;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
import com.rolling.api.domain.report.dto.ReportStatusUpdateRequest;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserBlockRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.page.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpenMatCommentService {

    private static final long COMMENT_REPORT_BLOCK_THRESHOLD = 3L;
    private static final Sort DEFAULT_ADMIN_SORT = Sort.by(Sort.Direction.DESC, "createdAt")
            .and(Sort.by(Sort.Direction.DESC, "id"));
    private static final Set<String> COMMENT_REPORT_ADMIN_ALLOWED_SORTS =
            Set.of("createdAt", "updatedAt", "status", "processedAt", "id");

    private final OpenMatRepository openMatRepository;
    private final OpenMatCommentRepository openMatCommentRepository;
    private final OpenMatCommentReportRepository openMatCommentReportRepository;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<OpenMatCommentResponse> findComments(Long viewerUserId, boolean isAdmin, Long openMatId) {
        OpenMat openMat = loadAccessibleOpenMat(openMatId, viewerUserId, isAdmin);
        List<OpenMatComment> comments = openMatCommentRepository.findAllByOpenMat_IdOrderByCreatedAtAscIdAsc(openMat.getId());
        Set<Long> blockedRelationUserIds = isAdmin ? Set.of() : findBlockedRelationUserIds(viewerUserId, comments);
        return buildCommentTree(comments, viewerUserId, isAdmin, openMat.getHost().getId(), blockedRelationUserIds);
    }

    @Transactional
    public OpenMatCommentResponse createComment(Long userId, Long openMatId, OpenMatCommentCreateRequest request) {
        User author = getActiveUser(userId);
        OpenMat openMat = loadWritableOpenMat(openMatId, userId, false);

        OpenMatComment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = getCommentForUpdate(request.getParentCommentId());
            validateReplyParent(openMatId, parentComment);
            ensureNotBlocked(userId, parentComment.getAuthor().getId(), "차단 관계에서는 댓글을 작성할 수 없습니다");
        }

        OpenMatComment saved = openMatCommentRepository.save(OpenMatComment.builder()
                .openMat(openMat)
                .parentComment(parentComment)
                .author(author)
                .content(normalizeCommentContent(request.getContent()))
                .build());

        publishCommentNotificationIfNeeded(openMat, parentComment, author);
        return toCommentResponse(saved, userId, false, openMat.getHost().getId(), List.of());
    }

    @Transactional
    public OpenMatCommentResponse updateComment(Long userId, Long commentId, OpenMatCommentUpdateRequest request) {
        getActiveUser(userId);
        OpenMatComment comment = getCommentForUpdate(commentId);
        loadAccessibleOpenMat(comment.getOpenMat().getId(), userId, false);

        if (!comment.getAuthor().getId().equals(userId)) {
            throw BusinessException.forbidden("본인 댓글만 수정할 수 있습니다");
        }
        if (comment.isDeleted()) {
            throw BusinessException.badRequest("삭제된 댓글은 수정할 수 없습니다");
        }

        ensureNotBlocked(userId, comment.getOpenMat().getHost().getId(), "차단 관계에서는 댓글을 수정할 수 없습니다");
        if (comment.isReply()) {
            ensureNotBlocked(userId, comment.getParentComment().getAuthor().getId(), "차단 관계에서는 댓글을 수정할 수 없습니다");
        }

        comment.updateContent(normalizeCommentContent(request.getContent()));
        return toCommentResponse(comment, userId, false, comment.getOpenMat().getHost().getId(), List.of());
    }

    @Transactional
    public void deleteComment(Long userId, boolean isAdmin, Long commentId) {
        getActiveUser(userId);
        OpenMatComment comment = getCommentForUpdate(commentId);
        loadAccessibleOpenMat(comment.getOpenMat().getId(), userId, isAdmin);

        boolean hostOwner = comment.getOpenMat().getHost().getId().equals(userId);
        boolean commentOwner = comment.getAuthor().getId().equals(userId);

        if (!isAdmin && !hostOwner && !commentOwner) {
            throw BusinessException.forbidden("삭제 권한이 없는 댓글입니다");
        }
        if (comment.isDeleted()) {
            return;
        }

        comment.softDelete(LocalDateTime.now(clock));
    }

    @Transactional
    public void reportComment(Long userId, Long commentId, OpenMatCommentReportRequest request) {
        User reporter = getActiveUser(userId);
        OpenMatComment comment = getCommentForReport(commentId);
        loadWritableOpenMat(comment.getOpenMat().getId(), userId, false);
        ensureCommentVisibleToViewer(userId, comment);

        if (comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException("SELF_REPORT_NOT_ALLOWED", "자신의 댓글은 신고할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        validateReportRequest(request);
        if (openMatCommentReportRepository.existsByComment_IdAndReporter_Id(commentId, userId)) {
            throw alreadyReported();
        }

        openMatCommentReportRepository.save(OpenMatCommentReport.builder()
                .comment(comment)
                .reporter(reporter)
                .reason(request.getReason())
                .customReason(normalizeCustomReason(request.getReason(), request.getCustomReason()))
                .status(ReportStatus.RECEIVED)
                .build());

        comment.incrementReportCount();
        if (comment.getReportCount() >= COMMENT_REPORT_BLOCK_THRESHOLD) {
            softDeleteReportedComment(comment);
        }
    }

    @Transactional(readOnly = true)
    public Page<OpenMatCommentReportAdminResponse> findCommentReportsForAdmin(ReportStatus status, Pageable pageable) {
        Pageable normalizedPageable = normalizeAdminPageable(pageable);
        Page<OpenMatCommentReport> reports = status == null
                ? openMatCommentReportRepository.findAll(normalizedPageable)
                : openMatCommentReportRepository.findAllByStatus(status, normalizedPageable);
        return reports.map(OpenMatCommentReportAdminResponse::from);
    }

    @Transactional(readOnly = true)
    public OpenMatCommentReportAdminResponse findCommentReportForAdmin(Long reportId) {
        return OpenMatCommentReportAdminResponse.from(loadCommentReportForAdmin(reportId));
    }

    @Transactional
    public OpenMatCommentReportAdminResponse updateCommentReportStatus(
            Long adminUserId,
            Long reportId,
            ReportStatusUpdateRequest request
    ) {
        OpenMatCommentReport report = loadCommentReportForAdmin(reportId);
        report.updateStatus(
                requireStatus(request.getStatus()),
                adminUserId,
                LocalDateTime.now(clock),
                normalizeFreeText(request.getProcessingMemo()),
                normalizeFreeText(request.getFinalAction())
        );
        return OpenMatCommentReportAdminResponse.from(report);
    }

    private List<OpenMatCommentResponse> buildCommentTree(
            List<OpenMatComment> comments,
            Long viewerUserId,
            boolean isAdmin,
            Long hostUserId,
            Set<Long> blockedRelationUserIds
    ) {
        Map<Long, List<OpenMatComment>> repliesByParentId = comments.stream()
                .filter(OpenMatComment::isReply)
                .collect(Collectors.groupingBy(
                        comment -> comment.getParentComment().getId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        return comments.stream()
                .filter(comment -> !comment.isReply())
                .filter(comment -> isCommentVisible(comment, blockedRelationUserIds))
                .map(comment -> toCommentResponse(
                        comment,
                        viewerUserId,
                        isAdmin,
                        hostUserId,
                        repliesByParentId.getOrDefault(comment.getId(), List.of()).stream()
                                .filter(reply -> isCommentVisible(reply, blockedRelationUserIds))
                                .map(reply -> toCommentResponse(reply, viewerUserId, isAdmin, hostUserId, List.of()))
                                .toList()
                ))
                .toList();
    }

    private OpenMatCommentResponse toCommentResponse(
            OpenMatComment comment,
            Long viewerUserId,
            boolean isAdmin,
            Long hostUserId,
            List<OpenMatCommentResponse> replies
    ) {
        boolean editableByMe = !comment.isDeleted() && Objects.equals(comment.getAuthor().getId(), viewerUserId);
        boolean deletableByMe = !comment.isDeleted() && viewerUserId != null && (
                isAdmin
                        || Objects.equals(comment.getAuthor().getId(), viewerUserId)
                        || Objects.equals(hostUserId, viewerUserId)
        );

        return OpenMatCommentResponse.builder()
                .id(comment.getId())
                .openMatId(comment.getOpenMat().getId())
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

    private void validateReplyParent(Long openMatId, OpenMatComment parentComment) {
        if (!parentComment.getOpenMat().getId().equals(openMatId)) {
            throw BusinessException.badRequest("parentCommentId는 같은 오픈매트의 댓글이어야 합니다");
        }
        if (parentComment.isReply()) {
            throw BusinessException.badRequest("대댓글에는 다시 대댓글을 달 수 없습니다");
        }
        if (parentComment.isDeleted()) {
            throw BusinessException.badRequest("삭제된 댓글에는 답글을 달 수 없습니다");
        }
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

    private User getActiveUser(Long userId) {
        return userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));
    }

    private OpenMatComment getCommentForUpdate(Long commentId) {
        return openMatCommentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> BusinessException.notFound("댓글을 찾을 수 없습니다"));
    }

    private OpenMatComment getCommentForReport(Long commentId) {
        OpenMatComment comment = openMatCommentRepository.findByIdForUpdate(commentId)
                .orElseThrow(() -> BusinessException.notFound("댓글을 찾을 수 없습니다"));
        if (comment.isDeleted()) {
            throw BusinessException.notFound("댓글을 찾을 수 없습니다");
        }
        return comment;
    }

    private OpenMat loadWritableOpenMat(Long openMatId, Long requesterUserId, boolean requesterIsAdmin) {
        OpenMat openMat = loadAccessibleOpenMat(openMatId, requesterUserId, requesterIsAdmin);
        if (Boolean.TRUE.equals(openMat.getIsHidden())) {
            throw BusinessException.badRequest("삭제된 오픈매트에는 댓글을 작성할 수 없습니다");
        }
        return openMat;
    }

    private OpenMat loadAccessibleOpenMat(Long openMatId, Long requesterUserId, boolean requesterIsAdmin) {
        OpenMat openMat = openMatRepository.findByIdAndIsHiddenFalse(openMatId)
                .orElseGet(() -> openMatRepository.findById(openMatId)
                        .orElseThrow(() -> BusinessException.notFound("오픈매트를 찾을 수 없습니다")));

        if (!requesterIsAdmin) {
            validateNotBlockedHost(requesterUserId, openMat.getHost().getId());
        }

        if (Boolean.TRUE.equals(openMat.getIsHidden()) && !canViewDeletedOpenMat(openMat, requesterUserId, requesterIsAdmin)) {
            throw BusinessException.notFound("오픈매트를 찾을 수 없습니다");
        }
        return openMat;
    }

    private void validateNotBlockedHost(Long requesterUserId, Long hostUserId) {
        if (requesterUserId == null) {
            return;
        }
        if (isBlockedBetween(requesterUserId, hostUserId)) {
            throw BusinessException.notFound("오픈매트를 찾을 수 없습니다");
        }
    }

    private void publishCommentNotificationIfNeeded(
            OpenMat openMat,
            OpenMatComment parentComment,
            User author
    ) {
        Long recipientUserId = parentComment == null ? openMat.getHost().getId() : parentComment.getAuthor().getId();
        if (recipientUserId.equals(author.getId())) {
            return;
        }
        if (isBlockedBetween(recipientUserId, author.getId())) {
            return;
        }

        PushNotificationType type = parentComment == null
                ? PushNotificationType.OPEN_MAT_COMMENT_CREATED
                : PushNotificationType.OPEN_MAT_COMMENT_REPLY_CREATED;

        applicationEventPublisher.publishEvent(new OpenMatCommentNotificationEvent(
                openMat.getId(),
                recipientUserId,
                author.getId(),
                author.getNickname(),
                openMat.getTitle(),
                type
        ));
    }

    private void softDeleteReportedComment(OpenMatComment comment) {
        LocalDateTime deletedAt = LocalDateTime.now(clock);
        comment.softDelete(deletedAt);
        if (comment.isReply()) {
            return;
        }

        for (OpenMatComment reply : openMatCommentRepository.findAllByParentComment_Id(comment.getId())) {
            if (!reply.isDeleted()) {
                reply.softDelete(deletedAt);
            }
        }
    }

    private Set<Long> findBlockedRelationUserIds(Long viewerUserId, List<OpenMatComment> comments) {
        if (viewerUserId == null || comments.isEmpty()) {
            return Set.of();
        }

        List<Long> candidateIds = comments.stream()
                .flatMap(comment -> comment.isReply()
                        ? java.util.stream.Stream.of(comment.getAuthor().getId(), comment.getParentComment().getAuthor().getId())
                        : java.util.stream.Stream.of(comment.getAuthor().getId()))
                .distinct()
                .toList();
        if (candidateIds.isEmpty()) {
            return Set.of();
        }
        List<Long> blockedRelationUserIds = userBlockRepository.findBlockedRelationUserIds(viewerUserId, candidateIds);
        return blockedRelationUserIds == null ? Set.of() : new HashSet<>(blockedRelationUserIds);
    }

    private boolean isCommentVisible(OpenMatComment comment, Set<Long> blockedRelationUserIds) {
        if (blockedRelationUserIds.contains(comment.getAuthor().getId())) {
            return false;
        }
        return !comment.isReply() || !blockedRelationUserIds.contains(comment.getParentComment().getAuthor().getId());
    }

    private void ensureCommentVisibleToViewer(Long viewerUserId, OpenMatComment comment) {
        if (!isCommentVisible(comment, findBlockedRelationUserIds(viewerUserId, List.of(comment)))) {
            throw BusinessException.forbidden("차단 관계에서는 댓글을 신고할 수 없습니다");
        }
    }

    private void validateReportRequest(OpenMatCommentReportRequest request) {
        if (request == null || request.getReason() == null) {
            throw BusinessException.badRequest("신고 사유는 필수입니다");
        }
    }

    private String normalizeCustomReason(ReportReason reason, String customReason) {
        if (reason != ReportReason.OTHER) {
            return null;
        }
        if (!StringUtils.hasText(customReason)) {
            throw BusinessException.badRequest("기타 신고 사유를 입력해주세요");
        }
        String normalized = customReason.trim();
        if (normalized.isEmpty() || normalized.length() > 500) {
            throw BusinessException.badRequest("기타 신고 사유를 입력해주세요");
        }
        return normalized;
    }

    private ReportStatus requireStatus(ReportStatus status) {
        if (status == null) {
            throw BusinessException.badRequest("신고 상태는 필수입니다");
        }
        return status;
    }

    private String normalizeFreeText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private OpenMatCommentReport loadCommentReportForAdmin(Long reportId) {
        return openMatCommentReportRepository.findById(reportId)
                .orElseThrow(() -> BusinessException.notFound("신고를 찾을 수 없습니다"));
    }

    private Pageable normalizeAdminPageable(Pageable pageable) {
        return PageableUtils.normalize(pageable, DEFAULT_ADMIN_SORT, COMMENT_REPORT_ADMIN_ALLOWED_SORTS, 20, 100);
    }

    private boolean isBlockedBetween(Long firstUserId, Long secondUserId) {
        if (firstUserId == null || secondUserId == null || firstUserId.equals(secondUserId)) {
            return false;
        }
        return userBlockRepository.existsByUser_IdAndBlockedUser_Id(firstUserId, secondUserId)
                || userBlockRepository.existsByUser_IdAndBlockedUser_Id(secondUserId, firstUserId);
    }

    private void ensureNotBlocked(Long firstUserId, Long secondUserId, String message) {
        if (isBlockedBetween(firstUserId, secondUserId)) {
            throw BusinessException.forbidden(message);
        }
    }

    private boolean canViewDeletedOpenMat(OpenMat openMat, Long requesterUserId, boolean requesterIsAdmin) {
        if (requesterIsAdmin) {
            return true;
        }
        if (requesterUserId == null) {
            return false;
        }
        if (Objects.equals(openMat.getHost().getId(), requesterUserId)) {
            return true;
        }
        if (openMat.isParticipant(requesterUserId)) {
            return true;
        }
        return notificationRepository.existsByUser_IdAndTypeAndTargetId(
                requesterUserId,
                PushNotificationType.OPEN_MAT_DELETED,
                openMat.getId()
        );
    }

    private BusinessException alreadyReported() {
        return new BusinessException("ALREADY_REPORTED", "이미 신고한 대상입니다", HttpStatus.BAD_REQUEST);
    }
}
