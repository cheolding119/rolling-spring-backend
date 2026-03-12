package com.rolling.api.domain.tournament.model;

import com.rolling.api.domain.tournament.entity.TournamentSource;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TournamentModel {
    private TournamentSource source;
    private String title;
    private String organizer;
    private String posterUrl;
    private String competitionDate;
    private String registrationDeadline;
    private String location;
    private String applyLink;
}