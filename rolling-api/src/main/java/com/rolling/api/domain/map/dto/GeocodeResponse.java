package com.rolling.api.domain.map.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@Schema(description = "주소 좌표 변환 응답")
public class GeocodeResponse {

    @Schema(description = "좌표 변환 기준 주소", example = "경남 창녕군 창녕읍 종로 2")
    private String address;

    @Schema(description = "위도", example = "35.5412345")
    private BigDecimal latitude;

    @Schema(description = "경도", example = "128.4912345")
    private BigDecimal longitude;
}
