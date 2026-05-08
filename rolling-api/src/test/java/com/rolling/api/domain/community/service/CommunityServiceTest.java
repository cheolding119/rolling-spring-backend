package com.rolling.api.domain.community.service;

import com.rolling.api.domain.community.dto.CommunityCommentCreateRequest;
import com.rolling.api.domain.community.dto.CommunityCommentResponse;
import com.rolling.api.domain.community.dto.CommunityPostCreateRequest;
import com.rolling.api.domain.community.dto.CommunityPostDetailResponse;
import com.rolling.api.domain.community.dto.CommunityReportRequest;
import com.rolling.api.domain.community.dto.CommunityPostSummaryResponse;
import com.rolling.api.domain.community.entity.CommunityComment;
import com.rolling.api.domain.community.entity.CommunityPost;
import com.rolling.api.domain.community.entity.CommunityPostCategory;
import com.rolling.api.domain.community.entity.CommunityPostStatus;
import com.rolling.api.domain.community.repository.CommunityCommentReportRepository;
import com.rolling.api.domain.community.repository.CommunityCommentRepository;
import com.rolling.api.domain.community.repository.CommunityPostLikeRepository;
import com.rolling.api.domain.community.repository.CommunityPostReportRepository;
import com.rolling.api.domain.community.repository.CommunityPostRepository;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommunityServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CommunityPostRepository communityPostRepository;

    @Mock
    private CommunityCommentRepository communityCommentRepository;

    @Mock
    private CommunityPostLikeRepository communityPostLikeRepository;

    @Mock
    private CommunityPostReportRepository communityPostReportRepository;

    @Mock
    private CommunityCommentReportRepository communityCommentReportRepository;

    @Mock
    private Clock clock;

    @InjectMocks
    private CommunityService communityService;

    @BeforeEach
    void setUp() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-05-08T03:00:00Z"));
    }

    @Test
    @DisplayName("게시글 목록 조회는 작성자 커뮤니티 닉네임을 포함한다")
    void findPosts_includesAuthorCommunityNickname() {
        User author = createUser(1L, "social-1", "open-mat", "rolling-community");
        CommunityPost post = createPost(10L, author, CommunityPostCategory.TECHNIQUE_QNA, "암바 방어 질문", "내용입니다", 3L, 5L);
        Page<CommunityPost> page = new PageImpl<>(List.of(post), PageRequest.of(0, 20), 1);

        when(communityPostRepository.searchVisible(isNull(), eq(CommunityPostCategory.TECHNIQUE_QNA), eq("암바"), any()))
                .thenReturn(page);

        Page<CommunityPostSummaryResponse> response = communityService.findPosts(null, CommunityPostCategory.TECHNIQUE_QNA, "암바", PageRequest.of(0, 20));

        assertThat(response).hasSize(1);
        assertThat(response.getContent().get(0).getAuthorNickname()).isEqualTo("rolling-community");
        assertThat(response.getContent().get(0).getCommentCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("게시글 상세 조회는 조회 수를 증가시킨다")
    void findPost_incrementsViewCount() {
        User author = createUser(1L, "social-2", "open-mat", "rolling-community");
        CommunityPost post = createPost(11L, author, CommunityPostCategory.FREE, "제목", "본문입니다", 2L, 1L);

        when(communityPostRepository.findVisibleById(11L, 1L)).thenReturn(Optional.of(post));

        CommunityPostDetailResponse response = communityService.findPost(1L, false, 11L);

        assertThat(post.getViewCount()).isEqualTo(3L);
        assertThat(response.getEditableByMe()).isTrue();
        assertThat(response.getAuthorNickname()).isEqualTo("rolling-community");
    }

    @Test
    @DisplayName("커뮤니티 닉네임이 없으면 게시글 작성이 차단된다")
    void createPost_requiresCommunityNickname() {
        User author = createUser(2L, "social-3", "open-mat", null);
        CommunityPostCreateRequest request = new CommunityPostCreateRequest();
        ReflectionTestUtils.setField(request, "category", CommunityPostCategory.FREE);
        ReflectionTestUtils.setField(request, "title", "제목");
        ReflectionTestUtils.setField(request, "content", "충분히 긴 본문입니다");

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(author));

        assertThatThrownBy(() -> communityService.createPost(2L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(exception -> assertThat(((BusinessException) exception).getCode()).isEqualTo("COMMUNITY_NICKNAME_REQUIRED"))
                .hasMessage("커뮤니티 닉네임을 먼저 설정해 주세요");
    }

    @Test
    @DisplayName("이미지를 포함한 게시글 작성 시 thumbnailUrl과 이미지 목록이 반영된다")
    void createPost_withImages_setsThumbnailAndImages() {
        User author = createUser(2L, "social-3b", "open-mat", "rolling-community");
        CommunityPostCreateRequest request = new CommunityPostCreateRequest();
        ReflectionTestUtils.setField(request, "category", CommunityPostCategory.FREE);
        ReflectionTestUtils.setField(request, "title", "제목");
        ReflectionTestUtils.setField(request, "content", "충분히 긴 본문입니다");
        ReflectionTestUtils.setField(request, "imageUrls", List.of("https://cdn.rolling.com/community/1.jpg"));

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(author));
        when(communityPostRepository.save(any(CommunityPost.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommunityPostDetailResponse response = communityService.createPost(2L, request);

        assertThat(response.getThumbnailUrl()).isEqualTo("https://cdn.rolling.com/community/1.jpg");
        assertThat(response.getImages()).hasSize(1);
        assertThat(response.getImages().get(0).getImageUrl()).isEqualTo("https://cdn.rolling.com/community/1.jpg");
    }

    @Test
    @DisplayName("댓글 작성 시 게시글 commentCount를 증가시킨다")
    void createComment_incrementsPostCommentCount() {
        User author = createUser(3L, "social-4", "open-mat", "rolling-community");
        CommunityPost post = createPost(12L, author, CommunityPostCategory.FREE, "제목", "본문입니다", 2L, 0L);
        CommunityCommentCreateRequest request = new CommunityCommentCreateRequest();
        ReflectionTestUtils.setField(request, "content", "좋은 의견입니다");

        when(userRepository.findByIdAndIsWithdrawnFalse(3L)).thenReturn(Optional.of(author));
        when(communityPostRepository.findVisibleById(12L, 3L)).thenReturn(Optional.of(post));
        when(communityCommentRepository.save(any(CommunityComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CommunityCommentResponse response = communityService.createComment(3L, 12L, request);

        assertThat(post.getCommentCount()).isEqualTo(1L);
        assertThat(response.getAuthorNickname()).isEqualTo("rolling-community");
        assertThat(response.getContent()).isEqualTo("좋은 의견입니다");
    }

    @Test
    @DisplayName("댓글 삭제 시 게시글 commentCount를 감소시킨다")
    void deleteComment_decrementsPostCommentCount() {
        User author = createUser(4L, "social-5", "open-mat", "rolling-community");
        CommunityPost post = createPost(13L, author, CommunityPostCategory.FREE, "제목", "본문입니다", 2L, 1L);
        CommunityComment comment = createComment(21L, post, author, "테스트 댓글");

        when(userRepository.findByIdAndIsWithdrawnFalse(4L)).thenReturn(Optional.of(author));
        when(communityCommentRepository.findById(21L)).thenReturn(Optional.of(comment));

        communityService.deleteComment(4L, false, 21L);

        assertThat(post.getCommentCount()).isEqualTo(0L);
        assertThat(comment.getStatus()).isEqualTo(com.rolling.api.domain.community.entity.CommunityCommentStatus.DELETED);
    }

    @Test
    @DisplayName("게시글 좋아요는 중복 요청에도 한 번만 증가한다")
    void likePost_isIdempotent() {
        User author = createUser(5L, "social-6", "open-mat", "rolling-community");
        CommunityPost post = createPost(14L, author, CommunityPostCategory.FREE, "제목", "본문입니다", 2L, 1L);

        when(userRepository.findByIdAndIsWithdrawnFalse(5L)).thenReturn(Optional.of(author));
        when(communityPostRepository.findVisibleById(14L, 5L)).thenReturn(Optional.of(post));
        when(communityPostLikeRepository.existsByPost_IdAndUser_Id(14L, 5L)).thenReturn(false, true);
        when(communityPostLikeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        communityService.likePost(5L, 14L);
        communityService.likePost(5L, 14L);

        assertThat(post.getLikeCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("게시글 신고는 신고 수를 증가시키고 중복 신고를 막는다")
    void reportPost_incrementsReportCountAndRejectsDuplicate() {
        User reporter = createUser(6L, "social-7", "reporter", "reporter-community");
        User author = createUser(7L, "social-8", "author", "author-community");
        CommunityPost post = createPost(15L, author, CommunityPostCategory.FREE, "제목", "본문입니다", 2L, 1L);
        CommunityReportRequest request = new CommunityReportRequest();
        ReflectionTestUtils.setField(request, "reason", ReportReason.SPAM);

        when(userRepository.findByIdAndIsWithdrawnFalse(6L)).thenReturn(Optional.of(reporter));
        when(communityPostRepository.findVisibleById(15L, 6L)).thenReturn(Optional.of(post));
        when(communityPostReportRepository.existsByPost_IdAndReporter_Id(15L, 6L)).thenReturn(false, true);

        communityService.reportPost(6L, 15L, request);

        assertThat(post.getReportCount()).isEqualTo(1L);
        assertThatThrownBy(() -> communityService.reportPost(6L, 15L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("이미 신고한 대상입니다");
    }

    @Test
    @DisplayName("댓글 신고는 신고 수를 증가시킨다")
    void reportComment_incrementsReportCount() {
        User reporter = createUser(8L, "social-9", "reporter", "reporter-community");
        User author = createUser(9L, "social-10", "author", "author-community");
        CommunityPost post = createPost(16L, author, CommunityPostCategory.FREE, "제목", "본문입니다", 2L, 1L);
        CommunityComment comment = createComment(22L, post, author, "테스트 댓글");
        CommunityReportRequest request = new CommunityReportRequest();
        ReflectionTestUtils.setField(request, "reason", ReportReason.INAPPROPRIATE);

        when(userRepository.findByIdAndIsWithdrawnFalse(8L)).thenReturn(Optional.of(reporter));
        when(communityCommentRepository.findById(22L)).thenReturn(Optional.of(comment));
        when(communityCommentReportRepository.existsByComment_IdAndReporter_Id(22L, 8L)).thenReturn(false);

        communityService.reportComment(8L, 22L, request);

        assertThat(comment.getReportCount()).isEqualTo(1L);
    }

    private User createUser(Long id, String socialId, String nickname, String communityNickname) {
        User user = User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(nickname)
                .email(socialId + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        ReflectionTestUtils.setField(user, "communityNickname", communityNickname);
        return user;
    }

    private CommunityPost createPost(Long id,
                                     User author,
                                     CommunityPostCategory category,
                                     String title,
                                     String content,
                                     Long viewCount,
                                     Long commentCount) {
        CommunityPost post = CommunityPost.builder()
                .author(author)
                .category(category)
                .title(title)
                .content(content)
                .viewCount(viewCount)
                .commentCount(commentCount)
                .status(CommunityPostStatus.ACTIVE)
                .build();
        ReflectionTestUtils.setField(post, "id", id);
        return post;
    }

    private CommunityComment createComment(Long id, CommunityPost post, User author, String content) {
        CommunityComment comment = CommunityComment.builder()
                .post(post)
                .author(author)
                .content(content)
                .build();
        ReflectionTestUtils.setField(comment, "id", id);
        return comment;
    }
}
