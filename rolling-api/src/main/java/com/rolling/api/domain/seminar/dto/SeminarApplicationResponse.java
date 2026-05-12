package com.rolling.api.domain.seminar.dto;

import com.rolling.api.domain.seminar.entity.SeminarApplication;
import com.rolling.api.domain.seminar.entity.SeminarApplicationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SeminarApplicationResponse {

    private Long id;
    private Long seminarId;
    private String seminarTitle;
    private Long userId;
    private String nickname;
    private String affiliation;
    private String beltColor;
    private SeminarApplicationStatus status;
    private String cancelReason;
    private LocalDateTime appliedAt;
    private LocalDateTime canceledAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SeminarApplicationResponse from(SeminarApplication application) {
        return SeminarApplicationResponse.builder()
                .id(application.getId())
                .seminarId(application.getSeminar().getId())
                .seminarTitle(application.getSeminar().getTitle())
                .userId(application.getUser().getId())
                .nickname(application.getUser().getNickname())
                .affiliation(application.getUser().getAffiliation())
                .beltColor(application.getUser().getBeltColor().name())
                .status(application.getStatus())
                .cancelReason(application.getCancelReason())
                .appliedAt(application.getAppliedAt())
                .canceledAt(application.getCanceledAt())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }
}
