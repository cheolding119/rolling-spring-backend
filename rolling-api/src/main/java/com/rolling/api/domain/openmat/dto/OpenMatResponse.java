package com.rolling.api.domain.openmat.dto;

import com.rolling.api.domain.openmat.entity.OpenMat;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class OpenMatResponse {

    private Long id;
    private String title;
    private String description;
    private String region;
    private String locationName;
    private String address;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private Integer currentParticipants;
    private Integer maxCapacity;
    private String hostInstagramId;
    private LocalDateTime createdAt;

    public static OpenMatResponse from(OpenMat openMat) {
        return OpenMatResponse.builder()
                .id(openMat.getId())
                .title(openMat.getTitle())
                .description(openMat.getDescription())
                .region(openMat.getRegion())
                .locationName(openMat.getLocationName())
                .address(openMat.getAddress())
                .startDateTime(openMat.getStartDateTime())
                .endDateTime(openMat.getEndDateTime())
                .currentParticipants(openMat.getCurrentParticipants())
                .maxCapacity(openMat.getMaxCapacity())
                .hostInstagramId(openMat.getHostInstagramId())
                .createdAt(openMat.getCreatedAt())
                .build();
    }
}
