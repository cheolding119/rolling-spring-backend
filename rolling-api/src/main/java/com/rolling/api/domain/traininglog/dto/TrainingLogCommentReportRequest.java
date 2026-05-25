package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class TrainingLogCommentReportRequest {

    @NotNull(message = "reason은 필수입니다")
    private ReportReason reason;

    @Size(max = 500, message = "customReason은 500자 이하여야 합니다")
    private String customReason;
}
