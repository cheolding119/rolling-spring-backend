package com.rolling.api.domain.openmat.dto;

import com.rolling.api.domain.openmat.entity.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Schema(description = "오픈매트 수정 요청")
public class OpenMatUpdateRequest {

    @Schema(description = "오픈매트 제목", example = "주말 오픈매트")
    private String title;

    @Schema(description = "오픈매트 설명", example = "자유롭게 참가 가능한 주말 오픈매트입니다.")
    private String description;

    @Schema(description = "시작 일시", example = "2026-03-01T10:00:00")
    private LocalDateTime startDateTime;

    @Schema(description = "종료 일시", example = "2026-03-01T12:00:00")
    private LocalDateTime endDateTime;

    @Schema(description = "장소명", example = "롤링 주짓수 아카데미")
    private String locationName;

    @Schema(description = "주소", example = "서울시 강남구 역삼동 123-45")
    private String address;

    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    @Schema(description = "위도. null이면 기존 좌표를 유지", example = "37.5012345", nullable = true)
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    @Schema(description = "경도. null이면 기존 좌표를 유지", example = "127.0398765", nullable = true)
    private BigDecimal longitude;

    @Schema(description = "지역", example = "SEOUL")
    private Region region;

    @Schema(description = "최대 참가 인원", example = "20")
    private Integer maxCapacity;

    @Schema(description = "호스트 인스타그램 ID", example = "rolling_bjj")
    private String hostInstagramId;
}
