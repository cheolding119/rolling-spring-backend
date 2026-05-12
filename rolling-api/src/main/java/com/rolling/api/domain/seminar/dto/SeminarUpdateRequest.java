package com.rolling.api.domain.seminar.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.rolling.api.domain.openmat.entity.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Schema(description = "세미나 수정 요청")
public class SeminarUpdateRequest {

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

    private Region region;
    private Integer maxCapacity;

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

    @JsonIgnore
    public boolean isEmpty() {
        return title == null
                && description == null
                && mainImageUrl == null
                && instructorName == null
                && instructorBio == null
                && curriculum == null
                && targetAudience == null
                && preparation == null
                && contactInfo == null
                && hostInstagramId == null
                && startDateTime == null
                && endDateTime == null
                && applicationStartDateTime == null
                && applicationEndDateTime == null
                && locationName == null
                && address == null
                && !hasLatitudeField()
                && !hasLongitudeField()
                && region == null
                && maxCapacity == null
                && price == null
                && paymentGuide == null
                && refundPolicy == null;
    }
}
