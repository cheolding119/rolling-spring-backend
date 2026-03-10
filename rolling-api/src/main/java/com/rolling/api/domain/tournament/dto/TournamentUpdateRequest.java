package com.rolling.api.domain.tournament.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Schema(description = "대회 수정 요청")
public class TournamentUpdateRequest {

    @Schema(description = "대회 제목", example = "제5회 롤링컵")
    private String title;

    @Schema(description = "주최사", example = "롤링 주짓수")
    private String organizer;

    @Schema(description = "포스터 이미지 URL", example = "https://cdn.rolling.com/posters/1.jpg")
    private String posterUrl;

    @Schema(description = "대회 개최일", example = "2026-04-15")
    private LocalDate competitionDate;

    @Schema(description = "접수 마감일", example = "2026-04-01")
    private LocalDate registrationDeadline;

    @Schema(description = "개최 장소", example = "서울 올림픽공원 체조경기장")
    private String location;

    @Schema(description = "외부 접수 링크", example = "https://forms.google.com/...")
    private String applyLink;

}
