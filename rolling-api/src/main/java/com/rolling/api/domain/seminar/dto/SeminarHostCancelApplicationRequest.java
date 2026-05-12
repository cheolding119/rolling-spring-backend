package com.rolling.api.domain.seminar.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "세미나 호스트 참가자 강제 취소 요청")
public class SeminarHostCancelApplicationRequest {

    private String cancelReason;
}
