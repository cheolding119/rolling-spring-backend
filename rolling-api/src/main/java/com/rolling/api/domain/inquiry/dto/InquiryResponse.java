package com.rolling.api.domain.inquiry.dto;

import com.rolling.api.domain.inquiry.entity.Inquiry;
import com.rolling.api.domain.inquiry.entity.InquiryStatus;
import com.rolling.api.domain.inquiry.entity.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "문의 응답")
public class InquiryResponse {

    @Schema(description = "문의 ID", example = "1")
    private Long id;

    @Schema(description = "작성자 사용자 ID", example = "10")
    private Long userId;

    @Schema(description = "작성자 닉네임", example = "rolling_user")
    private String userNickname;

    @Schema(description = "문의 제목", example = "알림이 오지 않습니다")
    private String title;

    @Schema(description = "문의 본문")
    private String content;

    @Schema(description = "문의 유형", example = "NOTIFICATION")
    private InquiryType type;

    @Schema(description = "문의 상태", example = "RECEIVED")
    private InquiryStatus status;

    @Schema(description = "운영자 답변")
    private String answerContent;

    @Schema(description = "답변한 관리자 사용자 ID", example = "1")
    private Long answeredByUserId;

    @Schema(description = "답변 시각")
    private LocalDateTime answeredAt;

    @Schema(description = "문의 생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "문의 수정 시각")
    private LocalDateTime updatedAt;

    public static InquiryResponse from(Inquiry inquiry) {
        return InquiryResponse.builder()
                .id(inquiry.getId())
                .userId(inquiry.getUser().getId())
                .userNickname(inquiry.getUser().getNickname())
                .title(inquiry.getTitle())
                .content(inquiry.getContent())
                .type(inquiry.getType())
                .status(inquiry.getStatus())
                .answerContent(inquiry.getAnswerContent())
                .answeredByUserId(inquiry.getAnsweredByUserId())
                .answeredAt(inquiry.getAnsweredAt())
                .createdAt(inquiry.getCreatedAt())
                .updatedAt(inquiry.getUpdatedAt())
                .build();
    }
}
