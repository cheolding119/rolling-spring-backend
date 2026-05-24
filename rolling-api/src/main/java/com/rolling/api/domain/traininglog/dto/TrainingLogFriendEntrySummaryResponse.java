package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class TrainingLogFriendEntrySummaryResponse {

    private Long id;
    private Long authorUserId;
    private String authorNickname;
    private LocalDate trainingDate;
    private TrainingLogCategory category;
    private TrainingLogColor color;
    private String title;
    private String content;
    private long likeCount;
    private long commentCount;
    private boolean likedByMe;
    private LocalDateTime createdAt;
}
