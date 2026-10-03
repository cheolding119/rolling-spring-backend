package com.rolling.api.domain.achievement.entity;

import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tournament_achievements")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TournamentAchievement extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 255)
    private String tournamentName;

    @Column(nullable = false)
    private LocalDate competitionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AchievementResult result;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private UniformType uniformType;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private BeltColor beltColor;

    @Column(length = 100)
    private String ageDivision;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private DivisionType divisionType;

    @Column(length = 100)
    private String weightClass;

    @Column(length = 255)
    private String organizer;

    @Column(length = 1000)
    private String resultUrl;

    @Column(length = 500)
    private String memo;

    public TournamentAchievement(User user) {
        this.user = user;
    }

    public void updateDetails(String tournamentName, LocalDate competitionDate, AchievementResult result,
                              UniformType uniformType, BeltColor beltColor, String ageDivision,
                              DivisionType divisionType, String weightClass, String organizer,
                              String resultUrl, String memo) {
        this.tournamentName = tournamentName;
        this.competitionDate = competitionDate;
        this.result = result;
        this.uniformType = uniformType;
        this.beltColor = beltColor;
        this.ageDivision = ageDivision;
        this.divisionType = divisionType;
        this.weightClass = weightClass;
        this.organizer = organizer;
        this.resultUrl = resultUrl;
        this.memo = memo;
    }
}
