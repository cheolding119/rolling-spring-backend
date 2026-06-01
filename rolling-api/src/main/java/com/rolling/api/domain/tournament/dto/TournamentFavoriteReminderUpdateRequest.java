package com.rolling.api.domain.tournament.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Schema(description = "찜한 대회 리마인드 설정 요청")
public class TournamentFavoriteReminderUpdateRequest {

    @Schema(description = "알림 활성화 여부", example = "true")
    private Boolean notificationEnabled;

    @Schema(description = "알림 날짜", example = "2026-06-13", nullable = true)
    private LocalDate remindDate;

    @Schema(description = "알림 시간", example = "09:00", nullable = true)
    private LocalTime remindTime;
}
