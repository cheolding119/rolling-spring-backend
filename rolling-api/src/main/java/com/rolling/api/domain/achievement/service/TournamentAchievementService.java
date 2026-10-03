package com.rolling.api.domain.achievement.service;

import com.rolling.api.domain.achievement.dto.*;
import com.rolling.api.domain.achievement.entity.*;
import com.rolling.api.domain.achievement.repository.TournamentAchievementRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.EnumMap;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentAchievementService {
    private final TournamentAchievementRepository repository;
    private final UserRepository userRepository;
    private final Clock clock;

    public Page<TournamentAchievementResponse> list(Long userId, int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw BusinessException.badRequest("page는 0 이상, size는 1~100이어야 합니다");
        }
        return repository.findAllByUser_Id(userId, PageRequest.of(page, size,
                        Sort.by(Sort.Direction.DESC, "competitionDate", "id")))
                .map(TournamentAchievementResponse::from);
    }

    public TournamentAchievementSummaryResponse summary(Long userId) {
        var counts = new EnumMap<AchievementResult, Long>(AchievementResult.class);
        repository.countResults(userId).forEach(row -> counts.put(row.getResult(), row.getTotal()));
        long gold = counts.getOrDefault(AchievementResult.GOLD, 0L);
        long silver = counts.getOrDefault(AchievementResult.SILVER, 0L);
        long bronze = counts.getOrDefault(AchievementResult.BRONZE, 0L);
        long participation = counts.getOrDefault(AchievementResult.PARTICIPATION, 0L);
        long medals = gold + silver + bronze;
        return new TournamentAchievementSummaryResponse(medals + participation, gold, silver, bronze,
                medals, participation);
    }

    public TournamentAchievementResponse get(Long userId, Long id) {
        return TournamentAchievementResponse.from(findOwned(userId, id));
    }

    @Transactional
    public TournamentAchievementResponse create(Long userId, TournamentAchievementSaveRequest request) {
        validate(request);
        var user = userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));
        var entry = new TournamentAchievement(user);
        apply(entry, request);
        return TournamentAchievementResponse.from(repository.saveAndFlush(entry));
    }

    @Transactional
    public TournamentAchievementResponse update(Long userId, Long id, TournamentAchievementSaveRequest request) {
        var entry = findOwned(userId, id);
        validate(request);
        apply(entry, request);
        repository.flush();
        return TournamentAchievementResponse.from(entry);
    }

    @Transactional
    public void delete(Long userId, Long id) {
        repository.delete(findOwned(userId, id));
    }

    private TournamentAchievement findOwned(Long userId, Long id) {
        return repository.findByIdAndUser_Id(id, userId)
                .orElseThrow(() -> BusinessException.notFound("대회 성적 기록을 찾을 수 없습니다"));
    }

    private void validate(TournamentAchievementSaveRequest request) {
        if (request.competitionDate().isAfter(LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))))) {
            throw BusinessException.badRequest("대회 개최일은 미래일 수 없습니다");
        }
        if (request.divisionType() == DivisionType.ABSOLUTE && request.weightClass() != null) {
            throw BusinessException.badRequest("오픈급 기록에는 체급을 입력할 수 없습니다");
        }
        if (request.resultUrl() != null) {
            try {
                URI uri = URI.create(request.resultUrl());
                if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null
                        || uri.getUserInfo() != null) {
                    throw new IllegalArgumentException();
                }
            } catch (IllegalArgumentException e) {
                throw BusinessException.badRequest("결과 링크는 유효한 HTTPS URL이어야 합니다");
            }
        }
    }

    private void apply(TournamentAchievement entry, TournamentAchievementSaveRequest request) {
        entry.updateDetails(request.tournamentName(), request.competitionDate(), request.result(),
                request.uniformType(), request.beltColor(), request.ageDivision(), request.divisionType(),
                request.weightClass(), request.organizer(), request.resultUrl(), request.memo());
    }
}
