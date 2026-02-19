package com.rolling.api.domain.openmat.dto;

import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private Integer currentParticipants;
    private OpenMatStatus status;
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
                .participantUids(new ArrayList<>(openMat.getParticipantUids()))
                .maxCapacity(openMat.getMaxCapacity())
                .currentParticipants(openMat.getParticipantUids().size())
                .status(openMat.getStatus())
                .hostId(openMat.getHost().getId())
                .hostNickname(openMat.getHost().getNickname())
                .hostInstagramId(openMat.getHostInstagramId())
                .createdAt(openMat.getCreatedAt())
                .build();
    }
}