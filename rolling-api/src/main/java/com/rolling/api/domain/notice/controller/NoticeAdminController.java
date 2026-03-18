package com.rolling.api.domain.notice.controller;

import com.rolling.api.domain.notice.dto.NoticeCreateRequest;
import com.rolling.api.domain.notice.dto.NoticeResponse;
import com.rolling.api.domain.notice.dto.NoticeUpdateRequest;
import com.rolling.api.domain.notice.service.NoticeService;
import com.rolling.api.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notice", description = "공지사항 운영 API")
@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeAdminController {

    private final NoticeService noticeService;

    @Operation(summary = "공지사항 생성", description = "운영자가 공지사항을 생성합니다.")
    @Parameter(name = "X-Crawler-Admin-Key", in = ParameterIn.HEADER, required = true, description = "운영 API 인증 키")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<NoticeResponse>> create(@Valid @RequestBody NoticeCreateRequest request) {
        NoticeResponse response = noticeService.create(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공지사항 수정", description = "운영자가 공지사항을 수정합니다.")
    @Parameter(name = "X-Crawler-Admin-Key", in = ParameterIn.HEADER, required = true, description = "운영 API 인증 키")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항 없음")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeResponse>> update(@PathVariable Long id, @Valid @RequestBody NoticeUpdateRequest request) {
        NoticeResponse response = noticeService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "공지사항 삭제", description = "운영자가 공지사항을 hard delete 합니다.")
    @Parameter(name = "X-Crawler-Admin-Key", in = ParameterIn.HEADER, required = true, description = "운영 API 인증 키")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
