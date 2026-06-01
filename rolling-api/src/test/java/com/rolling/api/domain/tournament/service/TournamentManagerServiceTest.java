package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.crawler.HeroesOfJiuJitsuCrawler;
import com.rolling.api.domain.tournament.crawler.KoreaJiuCrawler;
import com.rolling.api.domain.tournament.crawler.StreetJiuJitsuCrawler;
import com.rolling.api.domain.tournament.crawler.TournamentCrawler;
import com.rolling.api.domain.tournament.dto.TournamentCrawlResult;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.model.TournamentModel;
import com.rolling.api.domain.tournament.repository.TournamentFavoriteRepository;
import com.rolling.api.domain.tournament.repository.TournamentRepository;
import com.rolling.api.infra.s3.S3Uploader;
import com.rolling.api.global.exception.BusinessException;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class TournamentManagerServiceTest {

    @Mock
    private TournamentCrawler failingCrawler;

    @Mock
    private TournamentCrawler successCrawler;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private TournamentFavoriteRepository tournamentFavoriteRepository;

    @Mock
    private S3Uploader s3Uploader;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        lenient().when(s3Uploader.uploadImageFromUrl(anyString())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(successCrawler.getSource()).thenReturn(TournamentSource.STREET_JIU_JITSU);
    }

    @Test
    @DisplayName("특정 크롤러가 실패해도 다음 크롤러를 계속 실행한다")
    void crawlAll_continueWhenOneCrawlerFails() {
        TournamentModel model = new TournamentModel();
        model.setTitle("sample");

        when(failingCrawler.crawlAll()).thenThrow(new RuntimeException("crawler failed"));
        when(successCrawler.getSource()).thenReturn(TournamentSource.STREET_JIU_JITSU);
        when(successCrawler.crawlAll()).thenReturn(List.of(model));

        TournamentManagerService managerService =
                managerService(failingCrawler, successCrawler);

        List<TournamentModel> result = managerService.crawlAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("sample");
        assertThat(result.get(0).getSource()).isEqualTo(TournamentSource.STREET_JIU_JITSU);
        verify(failingCrawler).crawlAll();
        verify(successCrawler).crawlAll();
    }

    @Test
    @DisplayName("applyLink가 일치하면 기존 대회를 업데이트한다")
    void crawlAndSaveAll_updatesByApplyLink() {
        TournamentModel model = validModel(
                "스트릿 주짓수 154 전주 오픈",
                "2099-03-07",
                "전주대학교 체육관",
                "https://www.street-jiujitsu.com/tournament-154"
        );

        Tournament existing = Tournament.builder()
                .title("오래된 제목")
                .organizer("오래된 주최")
                .posterUrl("https://old-poster")
                .competitionDate("2025년 1월 1일")
                .registrationDeadline("2024년 12월 1일")
                .location("오래된 장소")
                .applyLink(model.getApplyLink())
                .build();
        ReflectionTestUtils.setField(existing, "id", 1L);

        when(successCrawler.crawlAll()).thenReturn(List.of(model));
        when(tournamentRepository.findByApplyLink(model.getApplyLink())).thenReturn(Optional.of(existing));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentManagerService managerService =
                managerService(successCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveAll();

        assertThat(result.getCrawledCount()).isEqualTo(1);
        assertThat(result.getCreatedCount()).isEqualTo(0);
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(result.getSkippedCount()).isEqualTo(0);
        assertThat(existing.getSource()).isEqualTo(TournamentSource.STREET_JIU_JITSU);
        assertThat(existing.getTitle()).isEqualTo(model.getTitle());
        assertThat(existing.getPosterUrl()).isEqualTo(model.getPosterUrl());
        assertThat(existing.getCompetitionDate()).isEqualTo(model.getCompetitionDate());
        verify(tournamentRepository).findByApplyLink(model.getApplyLink());
        verify(tournamentRepository, never()).findByTitleAndCompetitionDate(any(), any());
        verify(tournamentRepository).save(existing);
    }

    @Test
    @DisplayName("applyLink가 없을 때 title+competitionDate로 기존 대회를 업데이트한다")
    void crawlAndSaveAll_updatesByTitleAndCompetitionDate() {
        TournamentModel model = validModel(
                "스트릿 주짓수 160 서울 오픈",
                "2099-05-10",
                "잠실 실내체육관",
                "https://www.street-jiujitsu.com/tournament-160"
        );

        Tournament existing = Tournament.builder()
                .title(model.getTitle())
                .organizer("기존 주최사")
                .posterUrl("https://existing-poster")
                .competitionDate(model.getCompetitionDate())
                .registrationDeadline("2026년 4월 30일")
                .location("기존 장소")
                .applyLink("https://www.street-jiujitsu.com/old-link")
                .build();
        ReflectionTestUtils.setField(existing, "id", 2L);

        when(successCrawler.crawlAll()).thenReturn(List.of(model));
        when(tournamentRepository.findByApplyLink(model.getApplyLink())).thenReturn(Optional.empty());
        when(tournamentRepository.findByTitleAndCompetitionDate(model.getTitle(), model.getCompetitionDate()))
                .thenReturn(Optional.of(existing));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentManagerService managerService =
                managerService(successCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveAll();

        assertThat(result.getCreatedCount()).isEqualTo(0);
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(existing.getSource()).isEqualTo(TournamentSource.STREET_JIU_JITSU);
        assertThat(existing.getApplyLink()).isEqualTo(model.getApplyLink());
        verify(tournamentRepository).findByApplyLink(model.getApplyLink());
        verify(tournamentRepository).findByTitleAndCompetitionDate(model.getTitle(), model.getCompetitionDate());
    }

    @Test
    @DisplayName("필수값이 누락된 크롤링 데이터는 저장하지 않고 스킵한다")
    void crawlAndSaveAll_skipsInvalidTournamentModel() {
        TournamentModel invalid = new TournamentModel();
        invalid.setSource(TournamentSource.STREET_JIU_JITSU);
        invalid.setApplyLink("https://www.street-jiujitsu.com/invalid");
        invalid.setCompetitionDate("2099-03-07");
        invalid.setLocation("전주대학교 체육관");

        when(successCrawler.crawlAll()).thenReturn(List.of(invalid));

        TournamentManagerService managerService =
                managerService(successCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveAll();

        assertThat(result.getCrawledCount()).isEqualTo(1);
        assertThat(result.getCreatedCount()).isEqualTo(0);
        assertThat(result.getUpdatedCount()).isEqualTo(0);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        verify(tournamentRepository, never()).save(any());
        assertThat(meterRegistry.get("rolling_tournament_crawl_items_total")
                .tag("source", "street_jiu_jitsu")
                .tag("result", "skipped")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("필드 길이가 DB 스키마를 초과하면 저장하지 않고 스킵한다")
    void crawlAndSaveAll_skipsWhenFieldLengthOverflow() {
        TournamentModel invalid = validModel(
                "스트릿 주짓수 171 오픈",
                "2099-07-18",
                "서울시 강남구 체육관",
                "https://www.street-jiujitsu.com/" + "a".repeat(520)
        );

        when(successCrawler.crawlAll()).thenReturn(List.of(invalid));

        TournamentManagerService managerService =
                managerService(successCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveAll();

        assertThat(result.getSkippedCount()).isEqualTo(1);
        verify(tournamentRepository, never()).save(any());
    }

    @Test
    @DisplayName("중복이 없으면 신규 대회를 생성한다")
    void crawlAndSaveAll_createsWhenNoDuplicateExists() {
        TournamentModel model = validModel(
                "스트릿 주짓수 170 부산 오픈",
                "2099-07-11",
                "부산 벡스코",
                "https://www.street-jiujitsu.com/tournament-170"
        );
        AtomicReference<Tournament> savedTournament = new AtomicReference<>();

        when(successCrawler.crawlAll()).thenReturn(List.of(model));
        when(tournamentRepository.findByApplyLink(model.getApplyLink())).thenReturn(Optional.empty());
        when(tournamentRepository.findByTitleAndCompetitionDate(model.getTitle(), model.getCompetitionDate()))
                .thenReturn(Optional.empty());
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            savedTournament.set(tournament);
            return tournament;
        });

        TournamentManagerService managerService =
                managerService(successCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveAll();

        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(result.getUpdatedCount()).isEqualTo(0);
        assertThat(savedTournament.get()).isNotNull();
        assertThat(savedTournament.get().getSource()).isEqualTo(TournamentSource.STREET_JIU_JITSU);
        verify(tournamentRepository).findByApplyLink(model.getApplyLink());
        verify(tournamentRepository).findByTitleAndCompetitionDate(eq(model.getTitle()), eq(model.getCompetitionDate()));
        verify(tournamentRepository).save(any(Tournament.class));
        assertThat(meterRegistry.get("rolling_tournament_crawl_items_total")
                .tag("source", "street_jiu_jitsu")
                .tag("result", "created")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("rolling_tournament_crawl_duration_seconds")
                .tag("source", "street_jiu_jitsu")
                .timer()
                .count()).isEqualTo(1L);
    }

    @Test
    @DisplayName("포스터 업로드가 성공하면 S3 URL을 저장한다")
    void crawlAndSaveAll_usesUploadedPosterUrl() {
        TournamentModel model = validModel(
                "스트릿 주짓수 173 인천 오픈",
                "2099-08-08",
                "인천 남동체육관",
                "https://www.street-jiujitsu.com/tournament-173"
        );
        AtomicReference<Tournament> savedTournament = new AtomicReference<>();
        String uploadedPosterUrl = "https://rolling-jiujitsu-bucket.s3.ap-northeast-2.amazonaws.com/posters/uploaded.webp";

        when(successCrawler.crawlAll()).thenReturn(List.of(model));
        when(s3Uploader.uploadImageFromUrl(model.getPosterUrl())).thenReturn(uploadedPosterUrl);
        when(tournamentRepository.findByApplyLink(model.getApplyLink())).thenReturn(Optional.empty());
        when(tournamentRepository.findByTitleAndCompetitionDate(model.getTitle(), model.getCompetitionDate()))
                .thenReturn(Optional.empty());
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            savedTournament.set(tournament);
            return tournament;
        });

        TournamentManagerService managerService = managerService(successCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveAll();

        assertThat(result.getCreatedCount()).isEqualTo(1);
        assertThat(savedTournament.get()).isNotNull();
        assertThat(savedTournament.get().getPosterUrl()).isEqualTo(uploadedPosterUrl);
        verify(s3Uploader).uploadImageFromUrl("https://static.example.com/poster.jpg");
    }

    @Test
    @DisplayName("크롤링 데이터의 포스터가 null이면 기존 포스터를 유지한다")
    void crawlAndSaveAll_keepsExistingPosterWhenCrawledPosterIsNull() {
        TournamentModel model = validModel(
                "스포트라이트 접수중 대회",
                "2099-09-01",
                "서울 체육관",
                "https://spotlite.co.kr/jiujitsu/415"
        );
        model.setSource(TournamentSource.SPOTLITE);
        model.setPosterUrl(null);

        Tournament existing = Tournament.builder()
                .source(TournamentSource.SPOTLITE)
                .title("기존 제목")
                .organizer("기존 주최")
                .posterUrl("https://rolling.example.com/default-existing.jpg")
                .competitionDate("2099-08-01")
                .registrationDeadline("2099-07-01")
                .location("기존 장소")
                .applyLink(model.getApplyLink())
                .build();
        ReflectionTestUtils.setField(existing, "id", 20L);

        when(successCrawler.getSource()).thenReturn(TournamentSource.SPOTLITE);
        when(successCrawler.crawlAll()).thenReturn(List.of(model));
        when(tournamentRepository.findByApplyLink(model.getApplyLink())).thenReturn(Optional.of(existing));
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentManagerService managerService = managerService(successCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveAll();

        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(existing.getPosterUrl()).isEqualTo("https://rolling.example.com/default-existing.jpg");
        verify(s3Uploader, never()).uploadImageFromUrl(anyString());
    }

    @Test
    @DisplayName("대회일이 오늘보다 과거면 저장하지 않고 스킵한다")
    void crawlAndSaveAll_skipsPastCompetitionDate() {
        TournamentModel past = validModel(
                "스트릿 주짓수 지난 대회",
                LocalDate.now().minusDays(1).toString(),
                "서울시 체육관",
                "https://www.street-jiujitsu.com/past-event"
        );

        when(successCrawler.crawlAll()).thenReturn(List.of(past));

        TournamentManagerService managerService =
                managerService(successCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveAll();

        assertThat(result.getCrawledCount()).isEqualTo(1);
        assertThat(result.getCreatedCount()).isEqualTo(0);
        assertThat(result.getUpdatedCount()).isEqualTo(0);
        assertThat(result.getSkippedCount()).isEqualTo(1);
        verify(tournamentRepository, never()).save(any());
    }

    @Test
    @DisplayName("크롤링 시작 전에 접수 마감일이 지난 대회는 DB에서 삭제한다")
    void crawlAndSaveAll_deletesExpiredByRegistrationDeadline() {
        Tournament expired = Tournament.builder()
                .source(TournamentSource.STREET_JIU_JITSU)
                .title("expired")
                .organizer("org")
                .competitionDate(LocalDate.now().plusDays(2).toString())
                .registrationDeadline(LocalDate.now().minusDays(1).toString())
                .location("서울")
                .applyLink("https://expired")
                .build();
        ReflectionTestUtils.setField(expired, "id", 10L);

        Tournament active = Tournament.builder()
                .source(TournamentSource.KOREA_JIU)
                .title("active")
                .organizer("org")
                .competitionDate(LocalDate.now().plusDays(3).toString())
                .registrationDeadline(LocalDate.now().plusDays(1).toString())
                .location("서울")
                .applyLink("https://active")
                .build();
        ReflectionTestUtils.setField(active, "id", 11L);

        when(tournamentRepository.findAll()).thenReturn(List.of(expired, active));
        when(successCrawler.crawlAll()).thenReturn(List.of());

        TournamentManagerService managerService =
                managerService(successCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveAll();

        assertThat(result.getCrawledCount()).isEqualTo(0);
        verify(tournamentFavoriteRepository).deleteAllByTournament_IdIn(List.of(10L));
        verify(tournamentRepository).deleteAllByIdInBatch(List.of(10L));
    }

    @Test
    @DisplayName("source가 STREET_JIU_JITSU면 스트릿 주짓수 크롤러만 실행한다")
    void crawlAndSaveBySource_runsOnlyStreetCrawler() {
        StreetJiuJitsuCrawler streetCrawler = mock(StreetJiuJitsuCrawler.class);
        KoreaJiuCrawler koreaJiuCrawler = mock(KoreaJiuCrawler.class);
        TournamentModel model = validModel(
                "스트릿 주짓수 172 오픈",
                "2099-08-01",
                "서울 체육관",
                "https://www.street-jiujitsu.com/tournament-172"
        );

        when(streetCrawler.getSource()).thenReturn(TournamentSource.STREET_JIU_JITSU);
        when(streetCrawler.crawlAll()).thenReturn(List.of(model));
        when(tournamentRepository.findByApplyLink(model.getApplyLink())).thenReturn(Optional.empty());
        when(tournamentRepository.findByTitleAndCompetitionDate(model.getTitle(), model.getCompetitionDate()))
                .thenReturn(Optional.empty());
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentManagerService managerService =
                managerService(streetCrawler, koreaJiuCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveBySource(TournamentSource.STREET_JIU_JITSU);

        assertThat(result.getCrawledCount()).isEqualTo(1);
        verify(streetCrawler).crawlAll();
        verify(koreaJiuCrawler, never()).crawlAll();
    }

    @Test
    @DisplayName("source가 HEROES_OF_JIU_JITSU면 히어로즈 오브 주짓수 크롤러만 실행한다")
    void crawlAndSaveBySource_runsOnlyHeroesCrawler() {
        HeroesOfJiuJitsuCrawler heroesCrawler = mock(HeroesOfJiuJitsuCrawler.class);
        StreetJiuJitsuCrawler streetCrawler = mock(StreetJiuJitsuCrawler.class);
        TournamentModel model = validModel(
                "2026 히어로즈 오브 주짓수 리그 시즌16(대구)",
                "2099-04-05",
                "대구 시민체육관",
                "https://www.heroesofsports.kr/events/application/240"
        );
        model.setOrganizer(null);
        model.setPosterUrl(null);

        when(heroesCrawler.getSource()).thenReturn(TournamentSource.HEROES_OF_JIU_JITSU);
        when(streetCrawler.getSource()).thenReturn(TournamentSource.STREET_JIU_JITSU);
        when(heroesCrawler.crawlAll()).thenReturn(List.of(model));
        when(tournamentRepository.findByApplyLink(model.getApplyLink())).thenReturn(Optional.empty());
        when(tournamentRepository.findByTitleAndCompetitionDate(model.getTitle(), model.getCompetitionDate()))
                .thenReturn(Optional.empty());
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TournamentManagerService managerService =
                managerService(streetCrawler, heroesCrawler);

        TournamentCrawlResult result = managerService.crawlAndSaveBySource(TournamentSource.HEROES_OF_JIU_JITSU);

        assertThat(result.getCrawledCount()).isEqualTo(1);
        verify(heroesCrawler).crawlAll();
        verify(streetCrawler, never()).crawlAll();
    }

    @Test
    @DisplayName("해당 source의 크롤러가 없으면 예외를 던진다")
    void crawlAndSaveBySource_throwsWhenCrawlerMissing() {
        when(successCrawler.getSource()).thenReturn(TournamentSource.STREET_JIU_JITSU);
        TournamentManagerService managerService =
                managerService(successCrawler);

        assertThatThrownBy(() -> managerService.crawlAndSaveBySource(TournamentSource.KOREA_JIU))
                .isInstanceOf(BusinessException.class)
                .hasMessage("지원하지 않는 대회 크롤러입니다: KOREA_JIU");
    }

    private TournamentManagerService managerService(TournamentCrawler... crawlers) {
        TournamentManagerService managerService = new TournamentManagerService(
                List.of(crawlers),
                tournamentRepository,
                tournamentFavoriteRepository,
                s3Uploader
        );
        ReflectionTestUtils.setField(managerService, "meterRegistry", meterRegistry);
        return managerService;
    }
    private TournamentModel validModel(String title, String competitionDate, String location, String applyLink) {
        TournamentModel model = new TournamentModel();
        model.setSource(TournamentSource.STREET_JIU_JITSU);
        model.setTitle(title);
        model.setOrganizer("스트릿 주짓수");
        model.setPosterUrl("https://static.example.com/poster.jpg");
        model.setCompetitionDate(competitionDate);
        model.setRegistrationDeadline("2099-02-27");
        model.setLocation(location);
        model.setApplyLink(applyLink);
        return model;
    }
}
