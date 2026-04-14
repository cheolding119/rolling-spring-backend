package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.tournament.dto.TournamentCreateRequest;
import com.rolling.api.domain.tournament.dto.TournamentPosterUploadUrlRequest;
import com.rolling.api.domain.tournament.dto.TournamentPosterUploadUrlResponse;
import com.rolling.api.domain.tournament.dto.TournamentResponse;
import com.rolling.api.domain.tournament.dto.TournamentUpdateRequest;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.repository.TournamentRepository;
import com.rolling.api.domain.tournament.util.TournamentDateUtils;
import com.rolling.api.domain.report.dto.ReportCreateRequest;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.service.ReportService;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.security.AdminAccessConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final ReportService reportService;
    private final TournamentPosterService tournamentPosterService;
    private final AdminAccessConfig adminAccessConfig;

    @Transactional(readOnly = true)
    public Page<TournamentResponse> findAll(Pageable pageable) {
        return findAll(pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> findAll(Pageable pageable, TournamentSource source) {
        return findAll(pageable, source, null);
    }

    @Transactional(readOnly = true)
    public Page<TournamentResponse> findAll(Pageable pageable, TournamentSource source, Long viewerUserId) {
        List<Long> blockedUserIds = getBlockedUserIds(viewerUserId);
        List<TournamentResponse> sorted = tournamentRepository.findAll().stream()
                .filter(tournament -> matchesSource(tournament, source))
                .filter(tournament -> !isBlockedHostTournament(tournament, blockedUserIds))
                .map(this::toResponse)
                .sorted(
                        Comparator.comparing(TournamentResponse::isRegistrationClosed)
                                .thenComparing(TournamentResponse::getRegistrationDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(TournamentResponse::getCompetitionDate, Comparator.nullsLast(Comparator.naturalOrder()))
                                .thenComparing(TournamentResponse::getId, Comparator.nullsLast(Comparator.naturalOrder()))
                )
                .toList();

        int total = sorted.size();
        int start = (int) pageable.getOffset();
        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        int end = Math.min(start + pageable.getPageSize(), total);
        return new PageImpl<>(sorted.subList(start, end), pageable, total);
    }

    @Transactional(readOnly = true)
    public TournamentResponse findById(Long id) {
        return findById(id, null);
    }

    @Transactional(readOnly = true)
    public TournamentResponse findById(Long id, Long viewerUserId) {
        Tournament tournament = tournamentRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("대회를 찾을 수 없습니다"));
        if (isBlockedHostTournament(tournament, getBlockedUserIds(viewerUserId))) {
            throw BusinessException.notFound("대회를 찾을 수 없습니다");
        }
        return toResponse(tournament);
    }

    @Transactional
    public TournamentResponse create(Long userId, TournamentCreateRequest request) {
        validateActiveUser(userId);

        LocalDate competitionDate = request.getCompetitionDate();
        LocalDate registrationDeadline = request.getRegistrationDeadline();
        validateDatePolicy(competitionDate, registrationDeadline);

        String posterKey = requireText(request.getPosterKey(), "포스터 key는 필수입니다");
        String posterUrl = tournamentPosterService.buildPublicUrl(posterKey);

        Tournament tournament = Tournament.builder()
                .hostUserId(userId)
                .source(TournamentSource.MANUAL)
                .title(requireText(request.getTitle(), "대회 제목은 필수입니다"))
                .organizer(optionalText(request.getOrganizer()))
                .posterUrl(posterUrl)
                .posterKey(posterKey)
                .competitionDate(TournamentDateUtils.toIsoString(competitionDate))
                .registrationDeadline(TournamentDateUtils.toIsoString(registrationDeadline))
                .location(optionalText(request.getLocation()))
                .applyLink(requireText(request.getApplyLink(), "외부 접수 링크는 필수입니다"))
                .build();

        Tournament saved = tournamentRepository.save(tournament);
        return toResponse(saved);
    }

    @Transactional
    public TournamentPosterUploadUrlResponse createPosterUploadUrl(TournamentPosterUploadUrlRequest request) {
        return tournamentPosterService.createUploadUrl(request);
    }

    @Transactional
    public TournamentResponse update(Long userId, Long tournamentId, TournamentUpdateRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> BusinessException.notFound("대회를 찾을 수 없습니다"));
        validateCanModify(tournament, userId);
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

        return toResponse(tournament);
    }

    @Transactional
    public void delete(Long userId, Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> BusinessException.notFound("대회를 찾을 수 없습니다"));
        validateOwner(tournament, userId);
        tournamentRepository.delete(tournament);
    }

    @Transactional
    public void report(Long userId, Long tournamentId, ReportCreateRequest request) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> BusinessException.notFound("대회를 찾을 수 없습니다"));

        reportService.createReport(
                userId,
                ReportTargetType.TOURNAMENT,
                tournamentId,
                tournament.getHostUserId(),
                request.getReason(),
                request.getCustomReason()
        );
    }

    private void validateActiveUser(Long userId) {
        if (!userRepository.existsByIdAndIsWithdrawnFalse(userId)) {
            throw BusinessException.notFound("사용자를 찾을 수 없습니다");
        }
    }

    private void validateCanModify(Tournament tournament, Long userId) {
        if (isOwner(tournament, userId) || adminAccessConfig.isAdmin(userId)) {
            return;
        }
        throw BusinessException.forbidden("작성자 또는 관리자만 수정할 수 있습니다");
    }

    private void validateOwner(Tournament tournament, Long userId) {
        if (!isOwner(tournament, userId)) {
            throw BusinessException.forbidden("작성자만 수정/삭제할 수 있습니다");
        }
    }

    private boolean isOwner(Tournament tournament, Long userId) {
        return tournament.getHostUserId() != null && tournament.getHostUserId().equals(userId);
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

    private boolean matchesSource(Tournament tournament, TournamentSource source) {
        return source == null || resolveSource(tournament) == source;
    }

    private boolean isBlockedHostTournament(Tournament tournament, List<Long> blockedUserIds) {
        return tournament.getHostUserId() != null && blockedUserIds.contains(tournament.getHostUserId());
    }

    private List<Long> getBlockedUserIds(Long viewerUserId) {
        if (viewerUserId == null) {
            return List.of();
        }
        List<Long> blockedUserIds = userRepository.findBlockedUserIdsByUserId(viewerUserId);
        return blockedUserIds != null ? blockedUserIds : List.of();
    }

    private TournamentSource resolveSource(Tournament tournament) {
        return tournament.getSource() == null ? TournamentSource.MANUAL : tournament.getSource();
    }

    private TournamentResponse toResponse(Tournament tournament) {
        return TournamentResponse.from(tournament, tournamentPosterService.resolveDisplayUrl(tournament));
    }

    private String requireText(String value, String message) {
        String normalized = optionalText(value);
        if (normalized == null) {
            throw BusinessException.badRequest(message);
        }
        return normalized;
    }

    private String optionalText(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
