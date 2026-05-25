package com.rolling.api.domain.traininglog.controller;

import com.rolling.api.domain.traininglog.dto.FriendRequestResponse;
import com.rolling.api.domain.traininglog.dto.FriendResponse;
import com.rolling.api.domain.traininglog.dto.FriendSearchResultResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentCreateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentReportRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentUpdateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendEntryDetailResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendSharingResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendSharingUpdateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendEntrySummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntrySummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarResponse;
import com.rolling.api.domain.traininglog.service.TrainingLogSocialService;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TrainingLogSocialController {

    private final TrainingLogSocialService trainingLogSocialService;

    @GetMapping("/friends/search")
    public ResponseEntity<ApiResponse<List<FriendSearchResultResponse>>> searchFriends(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("q") String query
    ) {
        return ResponseEntity.ok(ApiResponse.success(trainingLogSocialService.searchFriends(requireUserId(principal), query)));
    }

    @GetMapping("/friends")
    public ResponseEntity<ApiResponse<List<FriendResponse>>> listFriends(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(trainingLogSocialService.findFriends(requireUserId(principal))));
    }

    @GetMapping("/friends/requests/received")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> receivedRequests(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(trainingLogSocialService.findReceivedRequests(requireUserId(principal))));
    }

    @GetMapping("/friends/requests/sent")
    public ResponseEntity<ApiResponse<List<FriendRequestResponse>>> sentRequests(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(trainingLogSocialService.findSentRequests(requireUserId(principal))));
    }

    @PostMapping("/friends/requests/{targetUserId}")
    public ResponseEntity<ApiResponse<FriendRequestResponse>> sendFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long targetUserId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.sendFriendRequest(requireUserId(principal), targetUserId)
        ));
    }

    @PostMapping("/friends/requests/{requestId}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long requestId
    ) {
        trainingLogSocialService.cancelFriendRequest(requireUserId(principal), requestId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/friends/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<FriendResponse>> acceptFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long requestId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.acceptFriendRequest(requireUserId(principal), requestId)
        ));
    }

    @PostMapping("/friends/requests/{requestId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectFriendRequest(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long requestId
    ) {
        trainingLogSocialService.rejectFriendRequest(requireUserId(principal), requestId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/friends/{friendUserId}")
    public ResponseEntity<ApiResponse<Void>> deleteFriend(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long friendUserId
    ) {
        trainingLogSocialService.deleteFriend(requireUserId(principal), friendUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/training-logs/me/friend-sharing")
    public ResponseEntity<ApiResponse<TrainingLogFriendSharingResponse>> getFriendSharing(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.getFriendSharing(requireUserId(principal))
        ));
    }

    @PatchMapping("/training-logs/me/friend-sharing")
    public ResponseEntity<ApiResponse<TrainingLogFriendSharingResponse>> updateFriendSharing(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TrainingLogFriendSharingUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.updateFriendSharing(requireUserId(principal), request)
        ));
    }

    @GetMapping("/training-logs/friends")
    public ResponseEntity<ApiResponse<Page<TrainingLogFriendEntrySummaryResponse>>> friendFeed(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = {"trainingDate", "createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.findFriendFeed(requireUserId(principal), pageable)
        ));
    }

    @GetMapping("/training-logs/friends/{friendUserId}/calendar")
    public ResponseEntity<ApiResponse<TrainingLogMonthlyCalendarResponse>> friendCalendar(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long friendUserId,
            @RequestParam int year,
            @RequestParam int month
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.getFriendMonthlyCalendar(requireUserId(principal), friendUserId, year, month)
        ));
    }

    @GetMapping(value = "/training-logs/friends/{friendUserId}/entries", params = "date")
    public ResponseEntity<ApiResponse<List<TrainingLogEntrySummaryResponse>>> friendEntriesByDate(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long friendUserId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.findFriendEntriesByDate(requireUserId(principal), friendUserId, date)
        ));
    }

    @GetMapping("/training-logs/friends/entries/{entryId}")
    public ResponseEntity<ApiResponse<TrainingLogFriendEntryDetailResponse>> friendEntryDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long entryId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.findFriendEntryDetail(requireUserId(principal), entryId)
        ));
    }

    @PostMapping("/training-logs/entries/{entryId}/like")
    public ResponseEntity<ApiResponse<Void>> likeEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long entryId
    ) {
        trainingLogSocialService.likeEntry(requireUserId(principal), entryId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/training-logs/entries/{entryId}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeEntry(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long entryId
    ) {
        trainingLogSocialService.unlikeEntry(requireUserId(principal), entryId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/training-logs/entries/{entryId}/comments")
    public ResponseEntity<ApiResponse<List<TrainingLogCommentResponse>>> comments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long entryId
    ) {
        boolean isAdmin = principal != null && principal.isAdmin();
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.findComments(requireUserId(principal), entryId, isAdmin)
        ));
    }

    @PostMapping("/training-logs/entries/{entryId}/comments")
    public ResponseEntity<ApiResponse<TrainingLogCommentResponse>> createComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long entryId,
            @Valid @RequestBody TrainingLogCommentCreateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.createComment(requireUserId(principal), entryId, request)
        ));
    }

    @PatchMapping("/training-logs/comments/{commentId}")
    public ResponseEntity<ApiResponse<TrainingLogCommentResponse>> updateComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody TrainingLogCommentUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.updateComment(requireUserId(principal), commentId, request)
        ));
    }

    @PostMapping("/training-logs/comments/{commentId}/report")
    public ResponseEntity<ApiResponse<Void>> reportComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody TrainingLogCommentReportRequest request
    ) {
        trainingLogSocialService.reportComment(requireUserId(principal), commentId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/training-logs/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId
    ) {
        trainingLogSocialService.deleteComment(requireUserId(principal), principal != null && principal.isAdmin(), commentId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
