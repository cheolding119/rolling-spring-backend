package com.rolling.api.domain.seminar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "세미나 신청 취소 요청")
public class SeminarCancelApplicationRequest {

    private String cancelReason;
}
