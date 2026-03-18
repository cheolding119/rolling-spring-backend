package com.rolling.api.domain.notice.controller;

import com.rolling.api.domain.notice.dto.NoticeListItemResponse;
import com.rolling.api.domain.notice.dto.NoticeResponse;
import com.rolling.api.domain.notice.service.NoticeService;
import com.rolling.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notice", description = "공지사항 API")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 목록 조회", description = "공지사항 목록을 최신순으로 페이징 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<NoticeListItemResponse>>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<NoticeListItemResponse> response = noticeService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공지사항 상세 조회", description = "공지사항 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeResponse>> findById(@PathVariable Long id) {
        NoticeResponse response = noticeService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
