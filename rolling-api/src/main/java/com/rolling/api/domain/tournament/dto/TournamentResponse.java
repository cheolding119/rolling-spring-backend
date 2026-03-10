package com.rolling.api.domain.tournament.dto;

import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.util.TournamentDateUtils;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
@Builder
public class TournamentResponse {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private Long id;
    private String title;
    private String organizer;
    private String posterUrl;
    private LocalDate competitionDate;
    private LocalDate registrationDeadline;
    private String location;
    private String applyLink;
    private boolean registrationClosed;
    private LocalDateTime createdAt;

    public static TournamentResponse from(Tournament tournament) {
        LocalDate competitionDate = TournamentDateUtils.parse(tournament.getCompetitionDate());
        LocalDate registrationDeadline = TournamentDateUtils.parse(tournament.getRegistrationDeadline());
        boolean registrationClosed = registrationDeadline != null && LocalDate.now(SEOUL_ZONE).isAfter(registrationDeadline);

        return TournamentResponse.builder()
                .id(tournament.getId())
                .title(tournament.getTitle())
                .organizer(tournament.getOrganizer())
                .posterUrl(tournament.getPosterUrl())
                .competitionDate(competitionDate)
                .registrationDeadline(registrationDeadline)
                .location(tournament.getLocation())
                .applyLink(tournament.getApplyLink())
                .registrationClosed(registrationClosed)
                .createdAt(tournament.getCreatedAt())
                .build();
    }
}
