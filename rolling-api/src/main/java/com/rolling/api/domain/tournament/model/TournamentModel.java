package com.rolling.api.domain.tournament.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TournamentModel {
    private String title;
    private String organizer;
    private String posterUrl;
    private String competitionDate;
    private String registrationDeadline;
    private String location;
    private String applyLink;
}
