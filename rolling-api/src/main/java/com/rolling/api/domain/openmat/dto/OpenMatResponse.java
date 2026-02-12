package com.rolling.api.domain.openmat.dto;

import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OpenMatResponse {

    private Long id;
    private String title;
    private String description;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String locationName;
    private String address;
    private List<Long> participantUids;
    private Integer maxCapacity;
    private OpenMatStatus status;
    private Integer reportCount;
    private Boolean isHidden;
    private String hostInstagramId;
    private LocalDateTime createdAt;

    public static OpenMatResponse from(OpenMat openMat) {
        return OpenMatResponse.builder()
                .id(openMat.getId())
                .title(openMat.getTitle())
                .description(openMat.getDescription())
                .startDateTime(openMat.getStartDateTime())
                .endDateTime(openMat.getEndDateTime())
                .locationName(openMat.getLocationName())
                .address(openMat.getAddress())
                .participantUids(openMat.getParticipantUids())
                .maxCapacity(openMat.getMaxCapacity())
                .status(openMat.getStatus())
                .reportCount(openMat.getReportCount())
                .isHidden(openMat.getIsHidden())
                .hostInstagramId(openMat.getHostInstagramId())
                .createdAt(openMat.getCreatedAt())
                .build();
    }
}
