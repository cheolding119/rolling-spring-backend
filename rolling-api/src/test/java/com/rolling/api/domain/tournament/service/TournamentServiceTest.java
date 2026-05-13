package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.dto.TournamentCreateRequest;
import com.rolling.api.domain.tournament.dto.TournamentPosterUploadUrlRequest;
import com.rolling.api.domain.tournament.dto.TournamentPosterUploadUrlResponse;
import com.rolling.api.domain.tournament.dto.TournamentResponse;
import com.rolling.api.domain.tournament.dto.TournamentUpdateRequest;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.repository.TournamentRepository;
import com.rolling.api.domain.report.dto.ReportCreateRequest;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.security.AdminAccessConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private com.rolling.api.domain.report.service.ReportService reportService;

    @Mock
    private TournamentPosterService tournamentPosterService;

    private final AdminAccessConfig adminAccessConfig = new AdminAccessConfig("1");

    @Test
    @DisplayName("대회 목록 조회 시 접수 가능 항목이 우선 정렬되고 접수 마감 항목은 하단으로 정렬된다")
    void findAll_sortsOpenFirst() {
        Tournament closed = tournament(1L, 10L, TournamentSource.MANUAL, "closed", "2026-03-01", "2026-02-20", "https://apply/closed");
        Tournament openLater = tournament(2L, 10L, TournamentSource.STREET_JIU_JITSU, "open-later", "2026-05-20", "2999-05-10", "https://apply/open-later");
        Tournament openEarly = tournament(3L, 10L, TournamentSource.KOREA_JIU, "open-early", "2026-04-10", "2999-04-01", "https://apply/open-early");
        when(tournamentRepository.findAll()).thenReturn(List.of(closed, openLater, openEarly));

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        Page<TournamentResponse> page = tournamentService.findAll(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getId()).isEqualTo(3L);
        assertThat(page.getContent().get(1).getId()).isEqualTo(2L);
        assertThat(page.getContent().get(2).getId()).isEqualTo(1L);
        assertThat(page.getContent().get(2).isRegistrationClosed()).isTrue();
    }

    @Test
    @DisplayName("대회 목록 조회 시 source 필터를 적용한다")
    void findAll_filtersBySource() {
        Tournament manual = tournament(1L, 10L, TournamentSource.MANUAL, "manual", "2026-04-10", "2999-04-01", "https://apply/manual");
        Tournament street = tournament(2L, 10L, TournamentSource.STREET_JIU_JITSU, "street", "2026-05-10", "2999-05-01", "https://apply/street");
        Tournament legacyManual = tournament(3L, 10L, null, "legacy-manual", "2026-06-10", "2999-06-01", "https://apply/legacy-manual");
        when(tournamentRepository.findAll()).thenReturn(List.of(street, legacyManual, manual));

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        Page<TournamentResponse> page = tournamentService.findAll(PageRequest.of(0, 10), TournamentSource.MANUAL);

        assertThat(page.getContent()).extracting(TournamentResponse::getId).containsExactly(1L, 3L);
        assertThat(page.getContent()).extracting(TournamentResponse::getSource).containsOnly(TournamentSource.MANUAL);
    }

    @Test
    @DisplayName("차단한 사용자가 등록한 대회는 목록에서 제외한다")
    void findAll_excludesBlockedHosts() {
        Tournament blocked = tournament(1L, 10L, TournamentSource.MANUAL, "blocked", "2026-04-10", "2999-04-01", "https://apply/blocked");
        Tournament visible = tournament(2L, 11L, TournamentSource.MANUAL, "visible", "2026-05-10", "2999-05-01", "https://apply/visible");
        when(tournamentRepository.findAll()).thenReturn(List.of(blocked, visible));
        when(userRepository.findBlockedUserIdsByUserId(99L)).thenReturn(List.of(10L));

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        Page<TournamentResponse> page = tournamentService.findAll(PageRequest.of(0, 10), null, 99L);

        assertThat(page.getContent()).extracting(TournamentResponse::getId).containsExactly(2L);
    }

    @Test
    @DisplayName("수동 등록 대회와 크롤링 대회는 같은 응답 필드 구조를 가진다")
    void findAll_mapsManualAndCrawlerWithSameResponseShape() {
        Tournament manual = tournament(1L, 10L, TournamentSource.MANUAL, "manual", "2026-04-10", "2999-04-01", "https://apply/manual");
        Tournament crawled = tournament(2L, null, TournamentSource.KOREA_JIU, "crawled", "2026-05-10", "2999-05-01", "https://apply/crawled");
        when(tournamentRepository.findAll()).thenReturn(List.of(manual, crawled));
        when(tournamentPosterService.resolveDisplayUrl(manual)).thenReturn("https://cdn.rolling.com/manual.jpg");
        when(tournamentPosterService.resolveDisplayUrl(crawled)).thenReturn("https://cdn.rolling.com/crawled.jpg");

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        Page<TournamentResponse> page = tournamentService.findAll(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent()).allSatisfy(response -> {
            assertThat(response.getTitle()).isNotBlank();
            assertThat(response.getOrganizer()).isEqualTo("주최");
            assertThat(response.getPosterUrl()).startsWith("https://cdn.rolling.com/");
            assertThat(response.getCompetitionDate()).isNotNull();
            assertThat(response.getRegistrationDeadline()).isNotNull();
            assertThat(response.getLocation()).isEqualTo("서울");
            assertThat(response.getApplyLink()).startsWith("https://apply/");
            assertThat(response.isRegistrationClosed()).isFalse();
            assertThat(response.getCreatedAt()).isNull();
        });
    }

    @Test
    @DisplayName("대회 생성은 작성자 ID를 저장하고 응답을 반환한다")
    void create_savesTournament() {
        TournamentCreateRequest request = createRequest();
        when(userRepository.existsByIdAndIsWithdrawnFalse(11L)).thenReturn(true);
        when(tournamentPosterService.buildPublicUrl("tournaments/posters/poster-1.jpg"))
                .thenReturn("https://cdn.rolling.com/posters/poster-1.jpg");
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            ReflectionTestUtils.setField(tournament, "id", 99L);
            return tournament;
        });

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        TournamentResponse response = tournamentService.create(11L, request);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getSource()).isEqualTo(TournamentSource.MANUAL);
        assertThat(response.getTitle()).isEqualTo("제5회 롤링컵");
        assertThat(response.getCompetitionDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        verify(tournamentRepository).save(any(Tournament.class));
    }

    @Test
    @DisplayName("대회 생성 시 포스터가 없으면 posterKey와 posterUrl 없이 저장한다")
    void create_allowsNullPosterKey() {
        TournamentCreateRequest request = createRequest();
        ReflectionTestUtils.setField(request, "posterKey", null);
        when(userRepository.existsByIdAndIsWithdrawnFalse(11L)).thenReturn(true);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            ReflectionTestUtils.setField(tournament, "id", 100L);
            return tournament;
        });

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        TournamentResponse response = tournamentService.create(11L, request);

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        assertThat(captor.getValue().getPosterKey()).isNull();
        assertThat(captor.getValue().getPosterUrl()).isNull();
        assertThat(response.getPosterUrl()).isNull();
        assertThat(response.getId()).isEqualTo(100L);
    }

    @Test
    @DisplayName("대회 상세 응답은 posterKey가 있으면 S3 public URL을 우선 사용한다")
    void findById_usesPosterKeyForPosterUrl() {
        Tournament tournament = tournament(77L, 3L, TournamentSource.MANUAL, "detail", "2026-04-15", "2026-04-01", "https://apply/detail");
        when(tournamentRepository.findById(77L)).thenReturn(Optional.of(tournament));
        when(tournamentPosterService.resolveDisplayUrl(tournament))
                .thenReturn("https://cdn.rolling.com/posters/poster.jpg");

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        TournamentResponse response = tournamentService.findById(77L);

        assertThat(response.getPosterUrl()).isEqualTo("https://cdn.rolling.com/posters/poster.jpg");
    }

    @Test
    @DisplayName("차단한 사용자가 등록한 대회 상세는 찾을 수 없다")
    void findById_whenHostBlocked_throwsNotFound() {
        Tournament tournament = tournament(78L, 3L, TournamentSource.MANUAL, "blocked-detail", "2026-04-15", "2026-04-01", "https://apply/blocked-detail");
        when(tournamentRepository.findById(78L)).thenReturn(Optional.of(tournament));
        when(userRepository.findBlockedUserIdsByUserId(99L)).thenReturn(List.of(3L));

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        assertThatThrownBy(() -> tournamentService.findById(78L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("대회를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("작성자가 아닌 일반 사용자는 대회를 수정할 수 없다")
    void update_forbiddenWhenNotOwnerAndNotAdmin() {
        Tournament tournament = tournament(10L, 3L, TournamentSource.MANUAL, "sample", "2026-04-15", "2026-04-01", "https://apply/sample");
        when(tournamentRepository.findById(10L)).thenReturn(Optional.of(tournament));

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        TournamentUpdateRequest request = new TournamentUpdateRequest();

        assertThatThrownBy(() -> tournamentService.update(2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("작성자 또는 관리자만 수정할 수 있습니다");
    }

    @Test
    @DisplayName("관리자는 작성자가 아니어도 대회를 수정할 수 있다")
    void update_allowsAdmin() {
        Tournament tournament = tournament(11L, 3L, TournamentSource.MANUAL, "old-title", "2026-04-15", "2026-04-01", "https://apply/sample");
        when(tournamentRepository.findById(11L)).thenReturn(Optional.of(tournament));

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        TournamentUpdateRequest request = new TournamentUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "새 제목");

        TournamentResponse response = tournamentService.update(1L, 11L, request);

        assertThat(response.getTitle()).isEqualTo("새 제목");
        verify(tournamentRepository).findById(11L);
    }

    @Test
    @DisplayName("작성자는 대회를 삭제할 수 있다")
    void delete_byOwner() {
        Tournament tournament = tournament(20L, 7L, TournamentSource.MANUAL, "sample", "2026-04-15", "2026-04-01", "https://apply/sample");
        when(tournamentRepository.findById(20L)).thenReturn(Optional.of(tournament));

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        tournamentService.delete(7L, 20L);

        verify(tournamentRepository).delete(tournament);
    }

    @Test
    @DisplayName("수정 시 접수 마감일이 대회일보다 늦으면 실패한다")
    void update_failsWhenDeadlineAfterCompetitionDate() {
        Tournament tournament = tournament(30L, 3L, TournamentSource.MANUAL, "sample", "2026-04-15", "2026-04-01", "https://apply/sample");
        when(tournamentRepository.findById(30L)).thenReturn(Optional.of(tournament));

        TournamentUpdateRequest request = new TournamentUpdateRequest();
        ReflectionTestUtils.setField(request, "competitionDate", LocalDate.of(2026, 4, 15));
        ReflectionTestUtils.setField(request, "registrationDeadline", LocalDate.of(2026, 4, 20));

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        assertThatThrownBy(() -> tournamentService.update(3L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("접수 마감일은 대회일보다 이후일 수 없습니다");
    }

    @Test
    @DisplayName("대회 신고는 공통 신고 저장 로직을 재사용한다")
    void report_reusesCommonReportService() {
        Tournament tournament = tournament(40L, 8L, TournamentSource.MANUAL, "report-target", "2026-04-15", "2026-04-01", "https://apply/report-target");
        when(tournamentRepository.findById(40L)).thenReturn(Optional.of(tournament));

        TournamentService tournamentService = new TournamentService(
                tournamentRepository,
                userRepository,
                reportService,
                tournamentPosterService,
                adminAccessConfig);

        ReportCreateRequest request = new ReportCreateRequest();
        ReflectionTestUtils.setField(request, "reason", com.rolling.api.domain.report.entity.ReportReason.SPAM);

        tournamentService.report(9L, 40L, request);

        org.mockito.Mockito.verify(reportService).createReport(
                9L,
                com.rolling.api.domain.report.entity.ReportTargetType.TOURNAMENT,
                40L,
                8L,
                com.rolling.api.domain.report.entity.ReportReason.SPAM,
                null
        );
    }

    private TournamentCreateRequest createRequest() {
        TournamentCreateRequest request = new TournamentCreateRequest();
        ReflectionTestUtils.setField(request, "title", "제5회 롤링컵");
        ReflectionTestUtils.setField(request, "organizer", "롤링 주짓수");
        ReflectionTestUtils.setField(request, "posterKey", "tournaments/posters/poster-1.jpg");
        ReflectionTestUtils.setField(request, "competitionDate", LocalDate.of(2026, 4, 15));
        ReflectionTestUtils.setField(request, "registrationDeadline", LocalDate.of(2026, 4, 1));
        ReflectionTestUtils.setField(request, "location", "서울 올림픽공원 체조경기장");
        ReflectionTestUtils.setField(request, "applyLink", "https://forms.google.com/...");
        return request;
    }

    private Tournament tournament(Long id,
                                  Long hostUserId,
                                  TournamentSource source,
                                  String title,
                                  String competitionDate,
                                  String registrationDeadline,
                                  String applyLink) {
        Tournament tournament = Tournament.builder()
                .hostUserId(hostUserId)
                .source(source)
                .title(title)
                .organizer("주최")
                .posterUrl("https://cdn.rolling.com/poster.jpg")
                .posterKey("tournaments/posters/poster.jpg")
                .competitionDate(competitionDate)
                .registrationDeadline(registrationDeadline)
                .location("서울")
                .applyLink(applyLink)
                .build();
        ReflectionTestUtils.setField(tournament, "id", id);
        return tournament;
    }
}
