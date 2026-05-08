package com.rolling.api.domain.community.service;

import com.rolling.api.domain.community.dto.CommunityCommentCreateRequest;
import com.rolling.api.domain.community.dto.CommunityCommentResponse;
import com.rolling.api.domain.community.dto.CommunityCommentUpdateRequest;
import com.rolling.api.domain.community.dto.CommunityPostCreateRequest;
import com.rolling.api.domain.community.dto.CommunityPostDetailResponse;
import com.rolling.api.domain.community.dto.CommunityPostImageResponse;
import com.rolling.api.domain.community.dto.CommunityPostSummaryResponse;
import com.rolling.api.domain.community.dto.CommunityPostUpdateRequest;
import com.rolling.api.domain.community.dto.CommunityReportRequest;
import com.rolling.api.domain.community.entity.CommunityComment;
import com.rolling.api.domain.community.entity.CommunityCommentReport;
import com.rolling.api.domain.community.entity.CommunityCommentStatus;
import com.rolling.api.domain.community.entity.CommunityPost;
import com.rolling.api.domain.community.entity.CommunityPostCategory;
import com.rolling.api.domain.community.entity.CommunityPostImage;
import com.rolling.api.domain.community.entity.CommunityPostLike;
import com.rolling.api.domain.community.entity.CommunityPostReport;
import com.rolling.api.domain.community.entity.CommunityPostStatus;
import com.rolling.api.domain.community.repository.CommunityCommentReportRepository;
import com.rolling.api.domain.community.repository.CommunityCommentRepository;
import com.rolling.api.domain.community.repository.CommunityPostLikeRepository;
import com.rolling.api.domain.community.repository.CommunityPostReportRepository;
import com.rolling.api.domain.community.repository.CommunityPostRepository;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private static final int MAX_IMAGE_COUNT = 5;
    private static final int MAX_POST_IMAGE_URL_LENGTH = 1000;
    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = List.of("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg", "avif", "ico");

    private final UserRepository userRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final CommunityPostReportRepository communityPostReportRepository;
    private final CommunityCommentReportRepository communityCommentReportRepository;
    private final Clock clock;

    @Value("${cloud.aws.s3.public-base-url:}")
    private String publicBaseUrl;

    @Transactional(readOnly = true)
    public Page<CommunityPostSummaryResponse> findPosts(Long viewerUserId,
                                                        CommunityPostCategory category,
                                                        String keyword,
                                                        Pageable pageable) {
        Pageable resolvedPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").descending().and(Sort.by("id").descending()));

        String normalizedKeyword = normalizeOptionalSearchText(keyword);
        return communityPostRepository.searchVisible(viewerUserId, category, normalizedKeyword, resolvedPageable)
                .map(CommunityPostSummaryResponse::from);
    }

    @Transactional
    public CommunityPostDetailResponse findPost(Long viewerUserId, boolean isAdmin, Long postId) {
        CommunityPost post = loadVisiblePost(viewerUserId, isAdmin, postId);
        post.incrementViewCount();
        return buildPostDetailResponse(post, viewerUserId, isAdmin);
    }

    @Transactional
    public CommunityPostDetailResponse createPost(Long userId, CommunityPostCreateRequest request) {
        User author = getActiveUser(userId);
        ensureCommunityNickname(author);

        CommunityPost post = CommunityPost.builder()
                .author(author)
                .category(request.getCategory())
                .title(normalizeRequiredText(request.getTitle(), 2, 80, "title은 2자 이상 80자 이하여야 합니다"))
                .content(normalizeRequiredText(request.getContent(), 10, 5000, "content는 10자 이상 5000자 이하여야 합니다"))
                .build();
        post.replaceImages(buildImages(request.getImageUrls()));

        CommunityPost saved = communityPostRepository.save(post);
        return buildPostDetailResponse(saved, userId, false);
    }

    @Transactional
    public CommunityPostDetailResponse updatePost(Long userId, boolean isAdmin, Long postId, CommunityPostUpdateRequest request) {
        User user = getActiveUser(userId);
        CommunityPost post = loadPostForEdit(user.getId(), isAdmin, postId);
        ensureCanEdit(user.getId(), isAdmin, post);
        ensureAtLeastOneField(request.getCategory(), request.getTitle(), request.getContent(), request.getImageUrls());

        post.update(
                request.getCategory(),
                normalizeOptionalText(request.getTitle(), 2, 80, "title은 2자 이상 80자 이하여야 합니다"),
                normalizeOptionalText(request.getContent(), 10, 5000, "content는 10자 이상 5000자 이하여야 합니다")
        );
        if (request.getImageUrls() != null) {
            post.replaceImages(buildImages(request.getImageUrls()));
        }

        return buildPostDetailResponse(post, userId, isAdmin);
    }

    @Transactional
    public void deletePost(Long userId, boolean isAdmin, Long postId) {
        User user = getActiveUser(userId);
        CommunityPost post = loadPostForEdit(user.getId(), isAdmin, postId);
        ensureCanEdit(user.getId(), isAdmin, post);
        post.delete(now());
    }

    @Transactional(readOnly = true)
    public Page<CommunityCommentResponse> findComments(Long viewerUserId,
                                                       boolean isAdmin,
                                                       Long postId,
                                                       Pageable pageable) {
        loadVisiblePost(viewerUserId, isAdmin, postId);

        Pageable resolvedPageable = pageable.getSort().isSorted()
                ? pageable
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("createdAt").ascending().and(Sort.by("id").ascending()));

        Long effectiveViewerUserId = isAdmin ? null : viewerUserId;
        return communityCommentRepository.findVisibleByPostId(postId, effectiveViewerUserId, resolvedPageable)
                .map(CommunityCommentResponse::from);
    }

    @Transactional
    public CommunityCommentResponse createComment(Long userId, Long postId, CommunityCommentCreateRequest request) {
        User author = getActiveUser(userId);
        ensureCommunityNickname(author);

        CommunityPost post = loadVisiblePost(userId, false, postId);
        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .author(author)
                .content(normalizeRequiredText(request.getContent(), 1, 1000, "content는 1자 이상 1000자 이하여야 합니다"))
                .build();

        post.incrementCommentCount();
        CommunityComment saved = communityCommentRepository.save(comment);
        return CommunityCommentResponse.from(saved);
    }

    @Transactional
    public CommunityCommentResponse updateComment(Long userId, boolean isAdmin, Long commentId, CommunityCommentUpdateRequest request) {
        User user = getActiveUser(userId);
        CommunityComment comment = loadCommentForEdit(commentId);
        ensureCanEdit(user.getId(), isAdmin, comment);

        comment.updateContent(normalizeRequiredText(request.getContent(), 1, 1000, "content는 1자 이상 1000자 이하여야 합니다"));
        return CommunityCommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long userId, boolean isAdmin, Long commentId) {
        User user = getActiveUser(userId);
        CommunityComment comment = loadCommentForEdit(commentId);
        ensureCanEdit(user.getId(), isAdmin, comment);

        comment.delete(now());
        comment.getPost().decrementCommentCount();
    }

    @Transactional
    public void likePost(Long userId, Long postId) {
        User user = getActiveUser(userId);
        CommunityPost post = loadVisiblePost(userId, false, postId);

        if (communityPostLikeRepository.existsByPost_IdAndUser_Id(postId, userId)) {
            return;
        }

        CommunityPostLike like = CommunityPostLike.builder()
                .post(post)
                .user(user)
                .build();
        communityPostLikeRepository.save(like);
        post.incrementLikeCount();
    }

    @Transactional
    public void unlikePost(Long userId, Long postId) {
        CommunityPost post = loadVisiblePost(userId, false, postId);
        communityPostLikeRepository.findByPost_IdAndUser_Id(postId, userId)
                .ifPresent(like -> {
                    communityPostLikeRepository.delete(like);
                    post.decrementLikeCount();
                });
    }

    @Transactional
    public void reportPost(Long userId, Long postId, CommunityReportRequest request) {
        User reporter = getActiveUser(userId);
        CommunityPost post = loadVisiblePost(userId, false, postId);

        if (post.getAuthor().getId().equals(userId)) {
            throw new BusinessException("SELF_REPORT_NOT_ALLOWED", "자신이 작성한 게시글은 신고할 수 없습니다", HttpStatus.BAD_REQUEST);
        }
        validateReportRequest(request);
        if (communityPostReportRepository.existsByPost_IdAndReporter_Id(postId, userId)) {
            throw alreadyReported();
        }

        CommunityPostReport report = CommunityPostReport.builder()
                .post(post)
                .reporter(reporter)
                .reason(request.getReason())
                .customReason(normalizeCustomReason(request.getReason(), request.getCustomReason()))
                .status(ReportStatus.RECEIVED)
                .build();
        communityPostReportRepository.save(report);
        post.incrementReportCount();
    }

    @Transactional
    public void reportComment(Long userId, Long commentId, CommunityReportRequest request) {
        User reporter = getActiveUser(userId);
        CommunityComment comment = loadVisibleComment(commentId);

        if (comment.getAuthor().getId().equals(userId)) {
            throw new BusinessException("SELF_REPORT_NOT_ALLOWED", "자신의 댓글은 신고할 수 없습니다", HttpStatus.BAD_REQUEST);
        }
        validateReportRequest(request);
        if (communityCommentReportRepository.existsByComment_IdAndReporter_Id(commentId, userId)) {
            throw alreadyReported();
        }

        CommunityCommentReport report = CommunityCommentReport.builder()
                .comment(comment)
                .reporter(reporter)
                .reason(request.getReason())
                .customReason(normalizeCustomReason(request.getReason(), request.getCustomReason()))
                .status(ReportStatus.RECEIVED)
                .build();
        communityCommentReportRepository.save(report);
        comment.incrementReportCount();
    }

    private CommunityPostDetailResponse buildPostDetailResponse(CommunityPost post, Long viewerUserId, boolean isAdmin) {
        boolean likedByMe = viewerUserId != null && communityPostLikeRepository.existsByPost_IdAndUser_Id(post.getId(), viewerUserId);
        boolean editableByMe = isAdmin || isAuthor(viewerUserId, post);
        return CommunityPostDetailResponse.from(post, likedByMe, editableByMe, toImageResponses(post));
    }

    private List<CommunityPostImageResponse> toImageResponses(CommunityPost post) {
        return post.getImages().stream()
                .sorted(Comparator.comparing(CommunityPostImage::getSortOrder))
                .map(CommunityPostImageResponse::from)
                .toList();
    }

    private List<CommunityPostImage> buildImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return List.of();
        }
        if (imageUrls.size() > MAX_IMAGE_COUNT) {
            throw BusinessException.badRequest("imageUrls는 최대 5장까지 허용합니다");
        }

        List<CommunityPostImage> images = new ArrayList<>();
        for (int i = 0; i < imageUrls.size(); i++) {
            String normalized = normalizeImageUrl(imageUrls.get(i));
            images.add(CommunityPostImage.builder()
                    .imageUrl(normalized)
                    .sortOrder(i)
                    .build());
        }
        return images;
    }

    private String normalizeImageUrl(String value) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest("imageUrl은 비어 있을 수 없습니다");
        }

        String normalized = value.trim();
        if (normalized.length() > MAX_POST_IMAGE_URL_LENGTH) {
            throw BusinessException.badRequest("imageUrl은 1000자 이하여야 합니다");
        }
        if (!normalized.startsWith("https://") && !normalized.startsWith("http://")) {
            throw BusinessException.badRequest("imageUrl은 http 또는 https URL이어야 합니다");
        }
        if (StringUtils.hasText(publicBaseUrl) && !normalized.startsWith(publicBaseUrl.trim())) {
            throw BusinessException.badRequest("허용된 이미지 URL만 사용할 수 있습니다");
        }

        String lower = normalized.toLowerCase();
        boolean allowed = ALLOWED_IMAGE_EXTENSIONS.stream().anyMatch(ext -> lower.endsWith("." + ext));
        if (!allowed) {
            throw BusinessException.badRequest("허용되지 않은 이미지 확장자입니다");
        }

        return normalized;
    }

    private CommunityPost loadVisiblePost(Long viewerUserId, boolean isAdmin, Long postId) {
        if (isAdmin) {
            return communityPostRepository.findById(postId)
                    .filter(post -> post.getStatus() == CommunityPostStatus.ACTIVE)
                    .filter(post -> post.getDeletedAt() == null)
                    .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다"));
        }

        return communityPostRepository.findVisibleById(postId, viewerUserId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다"));
    }

    private CommunityPost loadPostForEdit(Long viewerUserId, boolean isAdmin, Long postId) {
        if (isAdmin) {
            return communityPostRepository.findById(postId)
                    .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다"));
        }

        return communityPostRepository.findVisibleById(postId, viewerUserId)
                .orElseThrow(() -> BusinessException.notFound("게시글을 찾을 수 없습니다"));
    }

    private CommunityComment loadVisibleComment(Long commentId) {
        CommunityComment comment = communityCommentRepository.findById(commentId)
                .orElseThrow(() -> BusinessException.notFound("댓글을 찾을 수 없습니다"));
        if (comment.getStatus() != CommunityCommentStatus.ACTIVE || comment.getDeletedAt() != null) {
            throw BusinessException.notFound("댓글을 찾을 수 없습니다");
        }
        if (comment.getPost().getStatus() != CommunityPostStatus.ACTIVE || comment.getPost().getDeletedAt() != null) {
            throw BusinessException.notFound("댓글을 찾을 수 없습니다");
        }
        return comment;
    }

    private CommunityComment loadCommentForEdit(Long commentId) {
        return loadVisibleComment(commentId);
    }

    private User getActiveUser(Long userId) {
        return userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));
    }

    private void ensureCommunityNickname(User user) {
        if (!StringUtils.hasText(user.getCommunityNickname())) {
            throw new BusinessException("COMMUNITY_NICKNAME_REQUIRED", "커뮤니티 닉네임을 먼저 설정해 주세요", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isAuthor(Long viewerUserId, CommunityPost post) {
        return viewerUserId != null && viewerUserId.equals(post.getAuthor().getId());
    }

    private boolean isAuthor(Long viewerUserId, CommunityComment comment) {
        return viewerUserId != null && viewerUserId.equals(comment.getAuthor().getId());
    }

    private void ensureCanEdit(Long viewerUserId, boolean isAdmin, CommunityPost post) {
        if (isAdmin || isAuthor(viewerUserId, post)) {
            return;
        }
        throw BusinessException.forbidden("게시글을 수정하거나 삭제할 권한이 없습니다");
    }

    private void ensureCanEdit(Long viewerUserId, boolean isAdmin, CommunityComment comment) {
        if (isAdmin || isAuthor(viewerUserId, comment)) {
            return;
        }
        throw BusinessException.forbidden("댓글을 수정하거나 삭제할 권한이 없습니다");
    }

    private void ensureAtLeastOneField(Object... fields) {
        for (Object field : fields) {
            if (field != null) {
                return;
            }
        }
        throw BusinessException.badRequest("수정할 필드가 없습니다");
    }

    private String normalizeRequiredText(String value, int minLength, int maxLength, String message) {
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(message);
        }
        String normalized = value.trim();
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw BusinessException.badRequest(message);
        }
        return normalized;
    }

    private String normalizeOptionalText(String value, int minLength, int maxLength, String message) {
        if (value == null) {
            return null;
        }
        if (!StringUtils.hasText(value)) {
            throw BusinessException.badRequest(message);
        }
        String normalized = value.trim();
        if (normalized.length() < minLength || normalized.length() > maxLength) {
            throw BusinessException.badRequest(message);
        }
        return normalized;
    }

    private String normalizeOptionalSearchText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private void validateReportRequest(CommunityReportRequest request) {
        if (request == null || request.getReason() == null) {
            throw BusinessException.badRequest("신고 사유는 필수입니다");
        }
    }

    private String normalizeCustomReason(ReportReason reason, String customReason) {
        if (reason != ReportReason.OTHER) {
            return null;
        }

        String normalized = normalizeOptionalText(customReason, 1, 500, "기타 신고 사유를 입력해주세요");
        if (normalized == null) {
            throw BusinessException.badRequest("기타 신고 사유를 입력해주세요");
        }
        return normalized;
    }

    private BusinessException alreadyReported() {
        return new BusinessException("ALREADY_REPORTED", "이미 신고한 대상입니다", HttpStatus.BAD_REQUEST);
    }
    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
