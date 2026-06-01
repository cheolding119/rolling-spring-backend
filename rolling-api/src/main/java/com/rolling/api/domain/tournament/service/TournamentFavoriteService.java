package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.tournament.dto.TournamentFavoriteReminderDispatchResult;
import com.rolling.api.domain.tournament.dto.TournamentFavoriteReminderUpdateRequest;
import com.rolling.api.domain.tournament.dto.TournamentFavoriteResponse;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentFavorite;
import com.rolling.api.domain.tournament.repository.TournamentFavoriteRepository;
import com.rolling.api.domain.tournament.repository.TournamentRepository;
import com.rolling.api.domain.tournament.util.TournamentDateUtils;
import com.rolling.api.domain.tournament.util.TournamentOrdering;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TournamentFavoriteService {

    private static final String TOURNAMENT_DETAIL_ROUTE = "/tournament/detail";
    private static final LocalTime REGISTRATION_DEADLINE_CUTOFF_TIME = LocalTime.of(23, 59, 59);

    private final TournamentFavoriteRepository tournamentFavoriteRepository;
    private final TournamentRepository tournamentRepository;
    private final UserRepository userRepository;
    private final TournamentPosterService tournamentPosterService;
    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;
    private final Clock clock;

    @Transactional
    public TournamentFavoriteResponse addFavorite(Long userId, Long tournamentId) {
        User user = requireActiveUser(userId);
        Tournament tournament = requireVisibleTournament(tournamentId, userId);

        TournamentFavorite favorite = tournamentFavoriteRepository.findByUser_IdAndTournament_Id(userId, tournamentId)
                .orElseGet(() -> tournamentFavoriteRepository.save(TournamentFavorite.builder()
                        .user(user)
                        .tournament(tournament)
                        .build()));

        return toResponse(favorite);
    }

    @Transactional
    public void removeFavorite(Long userId, Long tournamentId) {
        requireActiveUser(userId);
        TournamentFavorite favorite = tournamentFavoriteRepository.findByUser_IdAndTournament_Id(userId, tournamentId)
                .orElseThrow(() -> BusinessException.notFound("찜한 대회를 찾을 수 없습니다"));

        tournamentFavoriteRepository.delete(favorite);
    }

    @Transactional(readOnly = true)
    public Page<TournamentFavoriteResponse> findFavorites(Long userId, Pageable pageable) {
        requireActiveUser(userId);
        List<Long> blockedUserIds = getBlockedUserIds(userId);

        List<TournamentFavorite> sortedFavorites = tournamentFavoriteRepository.findAllByUser_Id(userId).stream()
                .filter(favorite -> !isBlockedHostTournament(favorite.getTournament(), blockedUserIds))
                .sorted(Comparator.comparing(TournamentFavorite::getTournament, TournamentOrdering.byRegistrationAvailability()))
                .toList();

        int total = sortedFavorites.size();
        int start = (int) pageable.getOffset();
        if (start >= total) {
            return new PageImpl<>(List.of(), pageable, total);
        }
        int end = Math.min(start + pageable.getPageSize(), total);

        List<TournamentFavoriteResponse> content = sortedFavorites.subList(start, end).stream()
                .map(this::toResponse)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    @Transactional
    public TournamentFavoriteResponse updateReminder(Long userId, Long tournamentId, TournamentFavoriteReminderUpdateRequest request) {
        requireActiveUser(userId);
        if (request.getNotificationEnabled() == null) {
            throw BusinessException.badRequest("알림 활성화 여부는 필수입니다");
        }

        TournamentFavorite favorite = requireFavorite(userId, tournamentId);
        requireVisibleTournament(tournamentId, userId);

        if (!request.getNotificationEnabled()) {
            favorite.disableReminder();
            return toResponse(favorite);
        }

        if (request.getRemindDate() == null || request.getRemindTime() == null) {
            throw BusinessException.badRequest("알림 날짜와 시간은 필수입니다");
        }

        LocalDateTime scheduledAt = LocalDateTime.of(request.getRemindDate(), request.getRemindTime());
        validateReminderSchedule(favorite.getTournament(), scheduledAt);

        favorite.enableReminder(request.getRemindDate(), request.getRemindTime(), scheduledAt);
        return toResponse(favorite);
    }

    @Transactional
    public void disableInvalidRemindersForTournament(Long tournamentId) {
        List<TournamentFavorite> favorites = tournamentFavoriteRepository.findAllByTournament_IdAndNotificationEnabledTrue(tournamentId);
        for (TournamentFavorite favorite : favorites) {
            LocalDateTime scheduledAt = favorite.getScheduledAt();
            if (scheduledAt == null || isAfterInternalDeadline(favorite.getTournament(), scheduledAt)) {
                favorite.disableReminder();
            }
        }
    }

    @Transactional
    public void removeFavoritesByTournament(Long tournamentId) {
        tournamentFavoriteRepository.deleteAllByTournament_Id(tournamentId);
    }

    @Transactional
    public TournamentFavoriteReminderDispatchResult dispatchDueReminders() {
        LocalDateTime now = LocalDateTime.now(clock);
        List<TournamentFavorite> dueFavorites = tournamentFavoriteRepository.findDueReminderTargets(now);

        int sentCount = 0;
        int disabledCount = 0;
        int skippedCount = 0;

        for (TournamentFavorite favorite : dueFavorites) {
            if (!isReminderDeliverable(favorite, now)) {
                favorite.disableReminder();
                disabledCount++;
                continue;
            }

            PushNotificationCommand command = reminderCommand(favorite);
            List<Long> userIds = List.of(favorite.getUser().getId());

            try {
                notificationService.saveNotificationsForUsers(userIds, command);
            } catch (RuntimeException exception) {
                skippedCount++;
                log.warn("Failed to persist tournament favorite reminder. tournamentId={}, userId={}",
                        favorite.getTournament().getId(),
                        favorite.getUser().getId(),
                        exception);
                continue;
            }

            try {
                pushNotificationService.sendToUsers(userIds, command);
            } catch (RuntimeException exception) {
                log.warn("Failed to send tournament favorite reminder push. tournamentId={}, userId={}",
                        favorite.getTournament().getId(),
                        favorite.getUser().getId(),
                        exception);
            }

            favorite.markAsSent(now);
            sentCount++;
        }

        return new TournamentFavoriteReminderDispatchResult(dueFavorites.size(), sentCount, disabledCount, skippedCount);
    }

    private TournamentFavorite requireFavorite(Long userId, Long tournamentId) {
        return tournamentFavoriteRepository.findByUser_IdAndTournament_Id(userId, tournamentId)
                .orElseThrow(() -> BusinessException.badRequest("찜한 대회만 알림을 설정할 수 있습니다"));
    }

    private User requireActiveUser(Long userId) {
        return userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));
    }

    private Tournament requireVisibleTournament(Long tournamentId, Long viewerUserId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> BusinessException.notFound("대회를 찾을 수 없습니다"));

        if (isBlockedHostTournament(tournament, getBlockedUserIds(viewerUserId))) {
            throw BusinessException.notFound("대회를 찾을 수 없습니다");
        }
        return tournament;
    }

    private void validateReminderSchedule(Tournament tournament, LocalDateTime scheduledAt) {
        if (scheduledAt == null) {
            throw BusinessException.badRequest("알림 시각이 유효하지 않습니다");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        if (!scheduledAt.isAfter(now)) {
            throw BusinessException.badRequest("알림 시각은 현재 시각 이후여야 합니다");
        }

        if (isAfterInternalDeadline(tournament, scheduledAt)) {
            throw BusinessException.badRequest("알림 시각은 접수 마감일 이전이어야 합니다");
        }
    }

    private boolean isReminderDeliverable(TournamentFavorite favorite, LocalDateTime now) {
        if (!Boolean.TRUE.equals(favorite.getNotificationEnabled())
                || favorite.getScheduledAt() == null
                || favorite.getSentAt() != null) {
            return false;
        }

        LocalDate registrationDeadline = TournamentDateUtils.parse(favorite.getTournament().getRegistrationDeadline());
        if (registrationDeadline == null) {
            return false;
        }

        LocalDateTime internalDeadline = registrationDeadline.atTime(REGISTRATION_DEADLINE_CUTOFF_TIME);
        return !favorite.getScheduledAt().isAfter(now) && !favorite.getScheduledAt().isAfter(internalDeadline);
    }

    private boolean isAfterInternalDeadline(Tournament tournament, LocalDateTime scheduledAt) {
        LocalDate registrationDeadline = TournamentDateUtils.parse(tournament.getRegistrationDeadline());
        if (registrationDeadline == null) {
            throw BusinessException.badRequest("접수 마감일이 없는 대회는 알림을 설정할 수 없습니다");
        }

        LocalDateTime internalDeadline = registrationDeadline.atTime(REGISTRATION_DEADLINE_CUTOFF_TIME);
        return scheduledAt.isAfter(internalDeadline);
    }

    private TournamentFavoriteResponse toResponse(TournamentFavorite favorite) {
        return TournamentFavoriteResponse.from(
                favorite,
                tournamentPosterService.resolveDisplayUrl(favorite.getTournament())
        );
    }

    private PushNotificationCommand reminderCommand(TournamentFavorite favorite) {
        Tournament tournament = favorite.getTournament();

        return new PushNotificationCommand(
                PushNotificationType.TOURNAMENT_FAVORITE_REMINDER,
                "대회 접수 마감 알림",
                tournament.getTitle() + " 대회 접수 마감이 다가오고 있습니다.",
                tournament.getId(),
                Map.of("route", TOURNAMENT_DETAIL_ROUTE)
        );
    }

    private List<Long> getBlockedUserIds(Long viewerUserId) {
        if (viewerUserId == null) {
            return List.of();
        }
        List<Long> blockedUserIds = userRepository.findBlockedUserIdsByUserId(viewerUserId);
        return blockedUserIds != null ? blockedUserIds : List.of();
    }

    private boolean isBlockedHostTournament(Tournament tournament, List<Long> blockedUserIds) {
        return tournament.getHostUserId() != null && blockedUserIds.contains(tournament.getHostUserId());
    }
}
