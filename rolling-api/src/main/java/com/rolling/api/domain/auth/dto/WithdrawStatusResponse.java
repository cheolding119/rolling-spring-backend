package com.rolling.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "회원 탈퇴 예약 상태 응답")
public class WithdrawStatusResponse {

    @Schema(description = "탈퇴 예약 상태", example = "true")
    private boolean withdrawalPending;

    @Schema(description = "탈퇴 예정 시각 (Asia/Seoul 기준)", example = "2026-03-02T21:00:00")
    private LocalDateTime scheduledAt;
}
