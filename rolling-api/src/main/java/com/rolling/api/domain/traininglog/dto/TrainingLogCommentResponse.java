package com.rolling.api.domain.traininglog.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TrainingLogCommentResponse {

    private Long id;
    private Long entryId;
    private Long parentCommentId;
    private Long authorUserId;
    private String authorNickname;
    private String content;
    private boolean deleted;
    private boolean editableByMe;
    private boolean deletableByMe;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TrainingLogCommentResponse> replies;
}
