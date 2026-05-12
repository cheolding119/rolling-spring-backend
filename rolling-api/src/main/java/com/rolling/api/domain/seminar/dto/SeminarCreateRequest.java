package com.rolling.api.domain.seminar.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.rolling.api.domain.openmat.entity.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Schema(description = "세미나 생성 요청")
public class SeminarCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "설명은 필수입니다")
    private String description;

    private String mainImageUrl;

    @NotBlank(message = "강사명은 필수입니다")
    private String instructorName;

    private String instructorBio;
    private String curriculum;
    private String targetAudience;
    private String preparation;
    private String contactInfo;
    private String hostInstagramId;

    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startDateTime;

    @NotNull(message = "종료 시간은 필수입니다")
    private LocalDateTime endDateTime;

    private LocalDateTime applicationStartDateTime;
    private LocalDateTime applicationEndDateTime;

    @NotBlank(message = "장소명은 필수입니다")
    private String locationName;

    @NotBlank(message = "주소는 필수입니다")
    private String address;

    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    private BigDecimal latitude;

    @JsonIgnore
    private boolean latitudeFieldPresent;

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    private BigDecimal longitude;

    @JsonIgnore
    private boolean longitudeFieldPresent;

    @NotNull(message = "지역은 필수입니다")
    private Region region;

    @NotNull(message = "최대 정원은 필수입니다")
    private Integer maxCapacity;

    @NotNull(message = "참가비는 필수입니다")
    @Min(value = 0, message = "참가비는 0 이상이어야 합니다")
    private Integer price;

    private String paymentGuide;
    private String refundPolicy;

    @JsonSetter("latitude")
    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
        this.latitudeFieldPresent = true;
    }

    @JsonSetter("longitude")
    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
        this.longitudeFieldPresent = true;
    }

    @JsonIgnore
    public boolean hasLatitudeField() {
        return latitudeFieldPresent || latitude != null;
    }

    @JsonIgnore
    public boolean hasLongitudeField() {
        return longitudeFieldPresent || longitude != null;
    }
}
