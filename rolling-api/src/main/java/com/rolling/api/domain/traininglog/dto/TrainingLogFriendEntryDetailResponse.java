package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.traininglog.entity.TrainingLogVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class TrainingLogFriendEntryDetailResponse {

    private Long id;
    private Long authorUserId;
    private String authorNickname;
    private LocalDate trainingDate;
    private TrainingLogVisibility visibility;
    private TrainingLogCategory category;
    private TrainingLogColor color;
    private Integer trainingIntensity;
    private Boolean gymAttendance;
    private Integer condition;
    private Integer trainingMinutes;
    private String title;
    private String content;
    private List<TrainingLogChecklistItem> checklist;
    private List<String> hashtags;
    private List<String> imageUrls;
    private List<TrainingLogExternalLink> externalLinks;
    private long likeCount;
    private long commentCount;
    private boolean likedByMe;
    private boolean commentableByMe;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
