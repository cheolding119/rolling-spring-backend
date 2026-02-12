package com.rolling.api.domain.openmat.controller;

import com.rolling.api.domain.openmat.dto.OpenMatCreateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.service.OpenMatService;
import com.rolling.api.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/open-mats")
@RequiredArgsConstructor
public class OpenMatController {

    private final OpenMatService openMatService;

    @PostMapping
    public ResponseEntity<ApiResponse<OpenMatResponse>> create(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @RequestHeader(value = "user_id", required = false) Long userId,
            @Valid @RequestBody OpenMatCreateRequest request) {
        Long hostId = userId != null ? userId : xUserId;
        if (hostId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "user_id 헤더가 필요합니다");
        }
        OpenMatResponse response = openMatService.create(hostId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<OpenMatResponse>>> list(Pageable pageable) {
        Page<OpenMatResponse> response = openMatService.findAll(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
