package com.rolling.api.domain.tournament.entity;

import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "tournaments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tournaments_apply_link", columnNames = "apply_link"),
                @UniqueConstraint(name = "uk_tournaments_title_competition_date", columnNames = {"title", "competition_date"})
        },
        indexes = {
                @Index(name = "idx_tournaments_competition_date", columnList = "competition_date")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tournament extends BaseTimeEntity {

    // 대회 고유 식별자 (PK)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 대회 등록(작성) 유저 ID
    @Column(name = "host_user_id")
    private Long hostUserId;

    // 대회 등록 출처
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private TournamentSource source;

    // 대회 제목
    @Column(nullable = false)
    private String title;

    // 주최사 정보
    private String organizer;

    // 대회 포스터 이미지 URL
    @Column(name = "poster_url", length = 1000)
    private String posterUrl;

    @Column(name = "poster_key", length = 1000)
    private String posterKey;

    // 대회 개최일 (YYYY-MM-DD)
    @Column(name = "competition_date", nullable = false)
    private String competitionDate;

    // 접수 마감일 (YYYY-MM-DD)
    @Column(name = "registration_deadline")
    private String registrationDeadline;

    // 대회 개최 장소
    @Column
    private String location;

    // 지역 필터용 분류
    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private Region region;

    // 외부 접수 링크
    @Column(name = "apply_link", nullable = false, length = 512)
    private String applyLink;

    @Builder
    public Tournament(Long hostUserId,
                      TournamentSource source,
                      String title,
                      String organizer,
                      String posterUrl,
                      String posterKey,
                      String competitionDate,
                      String registrationDeadline,
                      String location,
                      Region region,
                      String applyLink) {
        this.hostUserId = hostUserId;
        this.source = source;
        this.title = title;
        this.organizer = organizer;
        this.posterUrl = posterUrl;
        this.posterKey = posterKey;
        this.competitionDate = competitionDate;
        this.registrationDeadline = registrationDeadline;
        this.location = location;
        this.region = region;
        this.applyLink = applyLink;
    }

    public void assignSourceIfAbsent(TournamentSource source) {
        if (this.source == null) {
            this.source = source;
        }
    }

    public void updateFromCrawler(String title,
                                  String organizer,
                                  String posterUrl,
                                  String competitionDate,
                                  String registrationDeadline,
                                  String location,
                                  String applyLink) {
        this.title = title;
        this.organizer = organizer;
        this.posterUrl = posterUrl;
        this.competitionDate = competitionDate;
        this.registrationDeadline = registrationDeadline;
        this.location = location;
        this.applyLink = applyLink;
    }

    public void updateByOwner(String title,
                              String organizer,
                              String posterUrl,
                              String competitionDate,
                              String registrationDeadline,
                              String location,
                              Region region,
                              String applyLink) {
        this.title = title;
        this.organizer = organizer;
        this.posterUrl = posterUrl;
        this.competitionDate = competitionDate;
        this.registrationDeadline = registrationDeadline;
        this.location = location;
        this.region = region;
        this.applyLink = applyLink;
    }
}
