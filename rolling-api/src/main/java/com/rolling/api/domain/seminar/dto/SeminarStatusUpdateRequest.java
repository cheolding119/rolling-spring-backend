package com.rolling.api.domain.seminar.dto;

import com.rolling.api.domain.seminar.entity.SeminarStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "세미나 모집 상태 변경 요청")
public class SeminarStatusUpdateRequest {

    @NotNull(message = "상태는 필수입니다")
    private SeminarStatus status;

    private String reason;
}
