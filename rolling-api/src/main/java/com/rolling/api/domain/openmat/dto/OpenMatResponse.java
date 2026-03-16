package com.rolling.api.domain.openmat.dto;

import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

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
    private Region region;
    private Integer maxCapacity;
    private Integer currentParticipants;
    private OpenMatStatus status;
    private Boolean reported;
    private Long hostId;
    private String hostNickname;
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
                .region(openMat.getRegion())
                .maxCapacity(openMat.getMaxCapacity())
                .currentParticipants(openMat.getParticipantUids().size())
                .status(openMat.getStatus())
                .reported(openMat.isReported())
                .hostId(openMat.getHost().getId())
                .hostNickname(openMat.getHost().getNickname())
                .hostInstagramId(openMat.getHostInstagramId())
                .createdAt(openMat.getCreatedAt())
                .build();
    }
}
