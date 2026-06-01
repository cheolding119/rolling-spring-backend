package com.rolling.api.domain.tournament.util;

import com.rolling.api.domain.tournament.entity.Tournament;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TournamentOrdering {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    public static Comparator<Tournament> byRegistrationAvailability() {
        return Comparator.comparing(TournamentOrdering::isRegistrationClosed)
                .thenComparing(TournamentOrdering::registrationDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(TournamentOrdering::competitionDate, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Tournament::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    public static boolean isRegistrationClosed(Tournament tournament) {
        LocalDate registrationDeadline = registrationDeadline(tournament);
        return registrationDeadline != null && LocalDate.now(SEOUL_ZONE).isAfter(registrationDeadline);
    }

    private static LocalDate registrationDeadline(Tournament tournament) {
        return tournament == null ? null : TournamentDateUtils.parse(tournament.getRegistrationDeadline());
    }

    private static LocalDate competitionDate(Tournament tournament) {
        return tournament == null ? null : TournamentDateUtils.parse(tournament.getCompetitionDate());
    }
}
