package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.dto.TournamentCreateRequest;
import com.rolling.api.domain.tournament.dto.TournamentResponse;
import com.rolling.api.domain.tournament.dto.TournamentUpdateRequest;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.repository.TournamentRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private UserRepository userRepository;

    @Test
    @DisplayName("대회 목록 조회는 레포지토리 정렬 결과를 그대로 응답으로 매핑한다")
    void findAll_mapsRepositoryPage() {
        Tournament closed = tournament(1L, 10L, TournamentSource.MANUAL, "closed", "2026-03-01", "2026-02-20", "https://apply/closed");
        Tournament openLater = tournament(2L, 10L, TournamentSource.STREET_JIU_JITSU, "open-later", "2026-05-20", "2999-05-10", "https://apply/open-later");
        Tournament openEarly = tournament(3L, 10L, TournamentSource.KOREA_JIU, "open-early", "2026-04-10", "2999-04-01", "https://apply/open-early");
        PageRequest pageable = PageRequest.of(0, 10);

        when(tournamentRepository.searchVisible(eq(null), eq(false), eq(null), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(openEarly, openLater, closed), pageable, 3));

        TournamentService tournamentService = new TournamentService(tournamentRepository, userRepository);

        Page<TournamentResponse> page = tournamentService.findAll(pageable);

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getId()).isEqualTo(3L);
        assertThat(page.getContent().get(1).getId()).isEqualTo(2L);
        assertThat(page.getContent().get(2).getId()).isEqualTo(1L);
        assertThat(page.getContent().get(2).isRegistrationClosed()).isTrue();

        ArgumentCaptor<String> todayCaptor = ArgumentCaptor.forClass(String.class);
        verify(tournamentRepository).searchVisible(eq(null), eq(false), eq(null), todayCaptor.capture(), eq(pageable));
        assertThat(todayCaptor.getValue()).isEqualTo(LocalDate.now(SEOUL_ZONE).toString());
    }

    @Test
    @DisplayName("대회 목록 조회 시 source 필터와 검색어를 레포지토리에 전달하고 legacy MANUAL도 포함한다")
    void findAll_filtersBySourceAndKeyword() {
        Tournament manual = tournament(1L, 10L, TournamentSource.MANUAL, "manual", "2026-04-10", "2999-04-01", "https://apply/manual");
        Tournament legacyManual = tournament(3L, 10L, null, "legacy-manual", "2026-06-10", "2999-06-01", "https://apply/legacy-manual");
        PageRequest pageable = PageRequest.of(0, 10);

        when(tournamentRepository.searchVisible(eq(TournamentSource.MANUAL), eq(true), eq("안산 오픈"), any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(manual, legacyManual), pageable, 2));

        TournamentService tournamentService = new TournamentService(tournamentRepository, userRepository);

        Page<TournamentResponse> page = tournamentService.findAll(pageable, TournamentSource.MANUAL, "  안산   오픈 ");

        assertThat(page.getContent()).extracting(TournamentResponse::getId).containsExactly(1L, 3L);
        assertThat(page.getContent()).extracting(TournamentResponse::getSource).containsOnly(TournamentSource.MANUAL);
        verify(tournamentRepository).searchVisible(eq(TournamentSource.MANUAL), eq(true), eq("안산 오픈"), any(), eq(pageable));
    }

    @Test
    @DisplayName("대회 생성은 작성자 ID를 저장하고 응답을 반환한다")
    void create_savesTournament() {
        TournamentCreateRequest request = createRequest();
        when(userRepository.existsByIdAndIsWithdrawnFalse(11L)).thenReturn(true);
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament tournament = invocation.getArgument(0);
            ReflectionTestUtils.setField(tournament, "id", 99L);
            return tournament;
        });

        TournamentService tournamentService = new TournamentService(tournamentRepository, userRepository);

        TournamentResponse response = tournamentService.create(11L, request);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getSource()).isEqualTo(TournamentSource.MANUAL);
        assertThat(response.getTitle()).isEqualTo("제5회 롤링컵");
        assertThat(response.getCompetitionDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        verify(tournamentRepository).save(any(Tournament.class));
    }

    @Test
    @DisplayName("작성자가 아닌 사용자는 대회를 수정할 수 없다")
    void update_forbiddenWhenNotOwner() {
        Tournament tournament = tournament(10L, 1L, TournamentSource.MANUAL, "sample", "2026-04-15", "2026-04-01", "https://apply/sample");
        when(tournamentRepository.findById(10L)).thenReturn(Optional.of(tournament));

        TournamentService tournamentService = new TournamentService(tournamentRepository, userRepository);

        TournamentUpdateRequest request = new TournamentUpdateRequest();

        assertThatThrownBy(() -> tournamentService.update(2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("작성자만 수정/삭제할 수 있습니다");
    }

    @Test
    @DisplayName("작성자는 대회를 삭제할 수 있다")
    void delete_byOwner() {
        Tournament tournament = tournament(20L, 7L, TournamentSource.MANUAL, "sample", "2026-04-15", "2026-04-01", "https://apply/sample");
        when(tournamentRepository.findById(20L)).thenReturn(Optional.of(tournament));

        TournamentService tournamentService = new TournamentService(tournamentRepository, userRepository);

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

        TournamentService tournamentService = new TournamentService(tournamentRepository, userRepository);

        assertThatThrownBy(() -> tournamentService.update(3L, 30L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("접수 마감일은 대회일보다 이후일 수 없습니다");
    }

    private TournamentCreateRequest createRequest() {
        TournamentCreateRequest request = new TournamentCreateRequest();
        ReflectionTestUtils.setField(request, "title", "제5회 롤링컵");
        ReflectionTestUtils.setField(request, "organizer", "롤링 주짓수");
        ReflectionTestUtils.setField(request, "posterUrl", "https://cdn.rolling.com/posters/1.jpg");
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
                .competitionDate(competitionDate)
                .registrationDeadline(registrationDeadline)
                .location("서울")
                .applyLink(applyLink)
                .build();
        ReflectionTestUtils.setField(tournament, "id", id);
        return tournament;
    }
}