package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.dto.TournamentCreateRequest;
import com.rolling.api.domain.tournament.dto.TournamentResponse;
import com.rolling.api.domain.tournament.dto.TournamentUpdateRequest;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.repository.TournamentRepository;
import com.rolling.api.domain.tournament.util.TournamentDateUtils;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<TournamentResponse> findAll(Pageable pageable) {
        return findAll(pageable, null, null);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> findAll(Pageable pageable, TournamentSource source) {
        return findAll(pageable, source, null);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> findAll(Pageable pageable, TournamentSource source, String keyword) {
        boolean includeLegacyManual = source == TournamentSource.MANUAL;
        String normalizedKeyword = normalizeKeyword(keyword);
        String todayIso = LocalDate.now(SEOUL_ZONE).toString();

        return tournamentRepository.searchVisible(source, includeLegacyManual, normalizedKeyword, todayIso, pageable)
                .map(TournamentResponse::from);
    }

    @Transactional(readOnly = true)
    public TournamentResponse findById(Long id) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("대회를 찾을 수 없습니다"));
        return TournamentResponse.from(tournament);
    }

    @Transactional
    public TournamentResponse create(Long userId, TournamentCreateRequest request) {
        validateActiveUser(userId);

        LocalDate competitionDate = request.getCompetitionDate();
        LocalDate registrationDeadline = request.getRegistrationDeadline();
        validateDatePolicy(competitionDate, registrationDeadline);

        Tournament tournament = Tournament.builder()
                .hostUserId(userId)
                .source(TournamentSource.MANUAL)
                .title(requireText(request.getTitle(), "대회 제목은 필수입니다"))
                .organizer(optionalText(request.getOrganizer()))
                .posterUrl(requireText(request.getPosterUrl(), "포스터 URL은 필수입니다"))
                .competitionDate(TournamentDateUtils.toIsoString(competitionDate))
                .registrationDeadline(TournamentDateUtils.toIsoString(registrationDeadline))
                .location(optionalText(request.getLocation()))
                .applyLink(requireText(request.getApplyLink(), "외부 접수 링크는 필수입니다"))
                .build();

        Tournament saved = tournamentRepository.save(tournament);
        return TournamentResponse.from(saved);
    }

    @Transactional
    public TournamentResponse update(Long userId, Long tournamentId, TournamentUpdateRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> BusinessException.notFound("대회를 찾을 수 없습니다"));
        validateOwner(tournament, userId);
        validateAtLeastOneField(request);
        tournament.assignSourceIfAbsent(TournamentSource.MANUAL);

        LocalDate effectiveCompetitionDate = request.getCompetitionDate() != null
                ? request.getCompetitionDate()
                : TournamentDateUtils.parse(tournament.getCompetitionDate());
        LocalDate effectiveRegistrationDeadline = request.getRegistrationDeadline() != null
                ? request.getRegistrationDeadline()
                : TournamentDateUtils.parse(tournament.getRegistrationDeadline());
        validateDatePolicy(effectiveCompetitionDate, effectiveRegistrationDeadline);

        String effectiveTitle = request.getTitle() != null
                ? requireText(request.getTitle(), "대회 제목은 비어 있을 수 없습니다")
                : tournament.getTitle();
        String effectiveOrganizer = request.getOrganizer() != null
                ? optionalText(request.getOrganizer())
                : tournament.getOrganizer();
        String effectivePosterUrl = request.getPosterUrl() != null
                ? requireText(request.getPosterUrl(), "포스터 URL은 비어 있을 수 없습니다")
                : tournament.getPosterUrl();
        String effectiveLocation = request.getLocation() != null
                ? optionalText(request.getLocation())
                : tournament.getLocation();
        String effectiveApplyLink = request.getApplyLink() != null
                ? requireText(request.getApplyLink(), "외부 접수 링크는 비어 있을 수 없습니다")
                : tournament.getApplyLink();

        tournament.updateByOwner(
                effectiveTitle,
                effectiveOrganizer,
                effectivePosterUrl,
                TournamentDateUtils.toIsoString(effectiveCompetitionDate),
                TournamentDateUtils.toIsoString(effectiveRegistrationDeadline),
                effectiveLocation,
                effectiveApplyLink
        );

        return TournamentResponse.from(tournament);
    }

    @Transactional
    public void delete(Long userId, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> BusinessException.notFound("대회를 찾을 수 없습니다"));
        validateOwner(tournament, userId);
        tournamentRepository.delete(tournament);
    }

    private void validateActiveUser(Long userId) {
        if (!userRepository.existsByIdAndIsWithdrawnFalse(userId)) {
            throw BusinessException.notFound("사용자를 찾을 수 없습니다");
        }
    }

    private void validateOwner(Tournament tournament, Long userId) {
        if (tournament.getHostUserId() == null || !tournament.getHostUserId().equals(userId)) {
            throw BusinessException.forbidden("작성자만 수정/삭제할 수 있습니다");
        }
    }

    private void validateAtLeastOneField(TournamentUpdateRequest request) {
        if (request.getTitle() == null
                && request.getOrganizer() == null
                && request.getPosterUrl() == null
                && request.getCompetitionDate() == null
                && request.getRegistrationDeadline() == null
                && request.getLocation() == null
                && request.getApplyLink() == null) {
            throw BusinessException.badRequest("수정할 필드가 없습니다");
        }
    }

    private void validateDatePolicy(LocalDate competitionDate, LocalDate registrationDeadline) {
        if (competitionDate == null || registrationDeadline == null) {
            throw BusinessException.badRequest("대회일/접수 마감일이 유효하지 않습니다");
        }
        if (registrationDeadline.isAfter(competitionDate)) {
            throw BusinessException.badRequest("접수 마감일은 대회일보다 이후일 수 없습니다");
        }
    }

    private String requireText(String value, String message) {
        String normalized = optionalText(value);
        if (normalized == null) {
            throw BusinessException.badRequest(message);
        }
        return normalized;
    }

    private String normalizeKeyword(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replaceAll("\\s+", " ");
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}