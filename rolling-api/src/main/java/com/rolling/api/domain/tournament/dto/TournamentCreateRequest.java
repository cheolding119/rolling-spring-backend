package com.rolling.api.domain.tournament.dto;

import com.rolling.api.domain.openmat.entity.Region;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "대회 생성 요청")
public class TournamentCreateRequest {

    @NotBlank(message = "대회 제목은 필수입니다")
    @Schema(description = "대회 제목", example = "제5회 롤링컵")
    private String title;

    @Schema(description = "주최사", example = "롤링 주짓수")
    private String organizer;

    @Schema(description = "S3 포스터 object key", example = "tournaments/posters/uuid.jpg", nullable = true)
    private String posterKey;

    @NotNull(message = "대회 개최일은 필수입니다")
    @Schema(description = "대회 개최일", example = "2026-04-15")
    private LocalDate competitionDate;

    @NotNull(message = "접수 마감일은 필수입니다")
    @Schema(description = "접수 마감일", example = "2026-04-01")
    private LocalDate registrationDeadline;

    @Schema(description = "개최 장소", example = "서울 올림픽공원 체조경기장")
    private String location;

    @Schema(description = "지역", example = "SEOUL", nullable = true)
    private Region region;

    @NotBlank(message = "외부 접수 링크는 필수입니다")
    @Schema(description = "외부 접수 링크", example = "https://forms.google.com/...")
    private String applyLink;

}
