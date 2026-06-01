package com.rolling.api.domain.tournament.dto;

import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentFavorite;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.util.TournamentDateUtils;
import com.rolling.api.domain.tournament.util.TournamentOrdering;
import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Getter
@Builder
public class TournamentFavoriteResponse {

    private Long tournamentId;
    private TournamentSource source;
    private String title;
    private String organizer;
    private String posterUrl;
    private LocalDate competitionDate;
    private LocalDate registrationDeadline;
    private String location;
    private Region region;
    private String applyLink;
    private boolean registrationClosed;
    private boolean notificationEnabled;
    private LocalDate remindDate;
    private LocalTime remindTime;
    private LocalDateTime favoritedAt;

    public static TournamentFavoriteResponse from(TournamentFavorite favorite, String posterUrl) {
        Tournament tournament = favorite.getTournament();

        return TournamentFavoriteResponse.builder()
                .tournamentId(tournament.getId())
                .source(resolveSource(tournament.getSource()))
                .title(tournament.getTitle())
                .organizer(tournament.getOrganizer())
                .posterUrl(StringUtils.hasText(posterUrl) ? posterUrl : tournament.getPosterUrl())
                .competitionDate(TournamentDateUtils.parse(tournament.getCompetitionDate()))
                .registrationDeadline(TournamentDateUtils.parse(tournament.getRegistrationDeadline()))
                .location(tournament.getLocation())
                .region(tournament.getRegion())
                .applyLink(tournament.getApplyLink())
                .registrationClosed(TournamentOrdering.isRegistrationClosed(tournament))
                .notificationEnabled(Boolean.TRUE.equals(favorite.getNotificationEnabled()))
                .remindDate(favorite.getRemindDate())
                .remindTime(favorite.getRemindTime())
                .favoritedAt(favorite.getCreatedAt())
                .build();
    }

    private static TournamentSource resolveSource(TournamentSource source) {
        return source == null ? TournamentSource.MANUAL : source;
    }
}
