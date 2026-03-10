package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.dto.TournamentCreateRequest;
import com.rolling.api.domain.tournament.dto.TournamentResponse;
import com.rolling.api.domain.tournament.dto.TournamentUpdateRequest;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.repository.TournamentRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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

    @Test
    @DisplayName("대회 목록 조회 시 접수 가능 항목이 우선 정렬되고 접수 마감 항목은 하단으로 정렬된다")
    void findAll_sortsOpenFirst() {
        Tournament closed = tournament(1L, 10L, "closed", "2026-03-01", "2026-02-20", "https://apply/closed");
        Tournament openLater = tournament(2L, 10L, "open-later", "2026-05-20", "2999-05-10", "https://apply/open-later");
        Tournament openEarly = tournament(3L, 10L, "open-early", "2026-04-10", "2999-04-01", "https://apply/open-early");
        when(tournamentRepository.findAll()).thenReturn(List.of(closed, openLater, openEarly));

        TournamentService tournamentService = new TournamentService(tournamentRepository, userRepository);

        Page<TournamentResponse> page = tournamentService.findAll(PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(3);
        assertThat(page.getContent().get(0).getId()).isEqualTo(3L);
        assertThat(page.getContent().get(1).getId()).isEqualTo(2L);
        assertThat(page.getContent().get(2).getId()).isEqualTo(1L);
        assertThat(page.getContent().get(2).isRegistrationClosed()).isTrue();
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
        assertThat(response.getTitle()).isEqualTo("제5회 롤링컵");
        assertThat(response.getCompetitionDate()).isEqualTo(LocalDate.of(2026, 4, 15));
        verify(tournamentRepository).save(any(Tournament.class));
    }

    @Test
    @DisplayName("작성자가 아닌 사용자는 대회를 수정할 수 없다")
    void update_forbiddenWhenNotOwner() {
        Tournament tournament = tournament(10L, 1L, "sample", "2026-04-15", "2026-04-01", "https://apply/sample");
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
        Tournament tournament = tournament(20L, 7L, "sample", "2026-04-15", "2026-04-01", "https://apply/sample");
        when(tournamentRepository.findById(20L)).thenReturn(Optional.of(tournament));

        TournamentService tournamentService = new TournamentService(tournamentRepository, userRepository);

        tournamentService.delete(7L, 20L);

        verify(tournamentRepository).delete(tournament);
    }

    @Test
    @DisplayName("수정 시 접수 마감일이 대회일보다 늦으면 실패한다")
    void update_failsWhenDeadlineAfterCompetitionDate() {
        Tournament tournament = tournament(30L, 3L, "sample", "2026-04-15", "2026-04-01", "https://apply/sample");
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
                                  String title,
                                  String competitionDate,
                                  String registrationDeadline,
                                  String applyLink) {
        Tournament tournament = Tournament.builder()
                .hostUserId(hostUserId)
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
