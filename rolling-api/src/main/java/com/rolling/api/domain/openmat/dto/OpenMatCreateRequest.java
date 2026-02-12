package com.rolling.api.domain.openmat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class OpenMatCreateRequest {

    @NotBlank(message = "제목은 필수입니다")
    private String title;

    @NotBlank(message = "설명은 필수입니다")
    private String description;

    @NotNull(message = "시작 시간은 필수입니다")
    private LocalDateTime startDateTime;

    @NotNull(message = "종료 시간은 필수입니다")
    private LocalDateTime endDateTime;

    @NotBlank(message = "장소명은 필수입니다")
    private String locationName;

    @NotBlank(message = "주소는 필수입니다")
    private String address;

    @NotNull(message = "최대 정원은 필수입니다")
    private Integer maxCapacity;

    private String hostInstagramId;
}
