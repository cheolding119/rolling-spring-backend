package com.rolling.api.domain.seminar.dto;

import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.seminar.entity.Seminar;
import com.rolling.api.domain.seminar.entity.SeminarApplicationStatus;
import com.rolling.api.domain.seminar.entity.SeminarStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SeminarResponse {

    private Long id;
    private String title;
    private String description;
    private String mainImageUrl;
    private String instructorName;
    private String instructorBio;
    private String curriculum;
    private String targetAudience;
    private String preparation;
    private String contactInfo;
    private String hostInstagramId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private LocalDateTime applicationStartDateTime;
    private LocalDateTime applicationEndDateTime;
    private String locationName;
    private String address;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Region region;
    private Integer maxCapacity;
    private Integer appliedCount;
    private Integer remainingCapacity;
    private Integer price;
    private String paymentGuide;
    private String refundPolicy;
    private SeminarStatus status;
    private SeminarApplicationStatus myApplicationStatus;
    private Long hostId;
    private String hostNickname;
    private Boolean reported;
    private Boolean deleted;
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static SeminarResponse from(Seminar seminar) {
        return from(seminar, 0, null, false);
    }

    public static SeminarResponse from(Seminar seminar,
                                       int appliedCount,
                                       SeminarApplicationStatus myApplicationStatus,
                                       boolean reported) {
        int remaining = seminar.remainingCapacity(appliedCount);
        return SeminarResponse.builder()
                .id(seminar.getId())
                .title(seminar.getTitle())
                .description(seminar.getDescription())
                .mainImageUrl(seminar.getMainImageUrl())
                .instructorName(seminar.getInstructorName())
                .instructorBio(seminar.getInstructorBio())
                .curriculum(seminar.getCurriculum())
                .targetAudience(seminar.getTargetAudience())
                .preparation(seminar.getPreparation())
                .contactInfo(seminar.getContactInfo())
                .hostInstagramId(seminar.getHostInstagramId())
                .startDateTime(seminar.getStartDateTime())
                .endDateTime(seminar.getEndDateTime())
                .applicationStartDateTime(seminar.getApplicationStartDateTime())
                .applicationEndDateTime(seminar.getApplicationEndDateTime())
                .locationName(seminar.getLocationName())
                .address(seminar.getAddress())
                .latitude(seminar.getLatitude())
                .longitude(seminar.getLongitude())
                .region(seminar.getRegion())
                .maxCapacity(seminar.getMaxCapacity())
                .appliedCount(appliedCount)
                .remainingCapacity(remaining == -1 ? null : remaining)
                .price(seminar.getPrice())
                .paymentGuide(seminar.getPaymentGuide())
                .refundPolicy(seminar.getRefundPolicy())
                .status(seminar.getStatus())
                .myApplicationStatus(myApplicationStatus)
                .hostId(seminar.getHost().getId())
                .hostNickname(seminar.getHost().getNickname())
                .reported(reported)
                .deleted(Boolean.TRUE.equals(seminar.getIsHidden()))
                .deletedAt(seminar.getDeletedAt())
                .createdAt(seminar.getCreatedAt())
                .updatedAt(seminar.getUpdatedAt())
                .build();
    }
}
