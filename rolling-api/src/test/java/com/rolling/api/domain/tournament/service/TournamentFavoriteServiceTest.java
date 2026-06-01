package com.rolling.api.domain.tournament.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.tournament.dto.TournamentFavoriteReminderDispatchResult;
import com.rolling.api.domain.tournament.dto.TournamentFavoriteReminderUpdateRequest;
import com.rolling.api.domain.tournament.dto.TournamentFavoriteResponse;
import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentFavorite;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.repository.TournamentFavoriteRepository;
import com.rolling.api.domain.tournament.repository.TournamentRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentFavoriteServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private TournamentFavoriteRepository tournamentFavoriteRepository;

    @Mock
    private TournamentRepository tournamentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TournamentPosterService tournamentPosterService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Test
    @DisplayName("찜한 대회 리마인드는 미래 시각이면 저장된다")
    void updateReminder_enablesReminderForFutureSchedule() {
        Tournament tournament = tournament(10L, "2026-06-20");
        User user = user(2L);
        TournamentFavorite favorite = favorite(user, tournament);
        TournamentFavoriteReminderUpdateRequest request = new TournamentFavoriteReminderUpdateRequest();
        ReflectionTestUtils.setField(request, "notificationEnabled", true);
        ReflectionTestUtils.setField(request, "remindDate", LocalDate.of(2026, 6, 19));
        ReflectionTestUtils.setField(request, "remindTime", LocalTime.of(9, 0));

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(user));
        when(tournamentFavoriteRepository.findByUser_IdAndTournament_Id(2L, 10L)).thenReturn(Optional.of(favorite));
        when(tournamentRepository.findById(10L)).thenReturn(Optional.of(tournament));
        when(tournamentPosterService.resolveDisplayUrl(tournament)).thenReturn("https://cdn.rolling.com/posters/10.jpg");

        TournamentFavoriteResponse response = service().updateReminder(2L, 10L, request);

        assertThat(favorite.getNotificationEnabled()).isTrue();
        assertThat(favorite.getRemindDate()).isEqualTo(LocalDate.of(2026, 6, 19));
        assertThat(favorite.getRemindTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(favorite.getScheduledAt()).isEqualTo(LocalDateTime.of(2026, 6, 19, 9, 0));
        assertThat(response.isNotificationEnabled()).isTrue();
        assertThat(response.getTournamentId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("과거 시각 리마인드는 저장할 수 없다")
    void updateReminder_rejectsPastSchedule() {
        Tournament tournament = tournament(10L, "2026-06-20");
        User user = user(2L);
        TournamentFavorite favorite = favorite(user, tournament);
        TournamentFavoriteReminderUpdateRequest request = new TournamentFavoriteReminderUpdateRequest();
        ReflectionTestUtils.setField(request, "notificationEnabled", true);
        ReflectionTestUtils.setField(request, "remindDate", LocalDate.of(2026, 6, 1));
        ReflectionTestUtils.setField(request, "remindTime", LocalTime.of(8, 59));

        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(user));
        when(tournamentFavoriteRepository.findByUser_IdAndTournament_Id(2L, 10L)).thenReturn(Optional.of(favorite));
        when(tournamentRepository.findById(10L)).thenReturn(Optional.of(tournament));

        assertThatThrownBy(() -> service().updateReminder(2L, 10L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("알림 시각은 현재 시각 이후여야 합니다");
    }

    @Test
    @DisplayName("리마인드 발송은 Notification 저장이 성공하면 푸시 실패와 무관하게 sentAt을 남긴다")
    void dispatchDueReminders_marksSentWhenPushFails() {
        Tournament tournament = tournament(10L, "2026-06-20");
        User user = user(2L);
        TournamentFavorite favorite = favorite(user, tournament);
        favorite.enableReminder(
                LocalDate.of(2026, 6, 1),
                LocalTime.of(8, 30),
                LocalDateTime.of(2026, 6, 1, 8, 30)
        );

        when(tournamentFavoriteRepository.findDueReminderTargets(LocalDateTime.of(2026, 6, 1, 9, 0)))
                .thenReturn(List.of(favorite));
        doThrow(new RuntimeException("push failed"))
                .when(pushNotificationService)
                .sendToUsers(eq(List.of(2L)), any(PushNotificationCommand.class));

        TournamentFavoriteReminderDispatchResult result = service().dispatchDueReminders();

        ArgumentCaptor<PushNotificationCommand> commandCaptor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        verify(notificationService).saveNotificationsForUsers(eq(List.of(2L)), commandCaptor.capture());
        verify(pushNotificationService).sendToUsers(eq(List.of(2L)), any(PushNotificationCommand.class));
        assertThat(commandCaptor.getValue().type().name()).isEqualTo("TOURNAMENT_FAVORITE_REMINDER");
        assertThat(commandCaptor.getValue().targetId()).isEqualTo(10L);
        assertThat(commandCaptor.getValue().data()).containsEntry("route", "/tournament/detail");
        assertThat(result.scannedCount()).isEqualTo(1);
        assertThat(result.sentCount()).isEqualTo(1);
        assertThat(result.disabledCount()).isEqualTo(0);
        assertThat(result.skippedCount()).isEqualTo(0);
        assertThat(favorite.getSentAt()).isEqualTo(LocalDateTime.of(2026, 6, 1, 9, 0));
    }

    @Test
    @DisplayName("마감 기준을 벗어난 리마인드는 발송하지 않고 비활성화한다")
    void dispatchDueReminders_disablesUndeliverableReminder() {
        Tournament tournament = tournament(10L, "2026-05-31");
        User user = user(2L);
        TournamentFavorite favorite = favorite(user, tournament);
        favorite.enableReminder(
                LocalDate.of(2026, 6, 1),
                LocalTime.of(8, 30),
                LocalDateTime.of(2026, 6, 1, 8, 30)
        );

        when(tournamentFavoriteRepository.findDueReminderTargets(LocalDateTime.of(2026, 6, 1, 9, 0)))
                .thenReturn(List.of(favorite));

        TournamentFavoriteReminderDispatchResult result = service().dispatchDueReminders();

        verify(notificationService, never()).saveNotificationsForUsers(any(), any());
        verify(pushNotificationService, never()).sendToUsers(any(), any());
        assertThat(result.scannedCount()).isEqualTo(1);
        assertThat(result.sentCount()).isEqualTo(0);
        assertThat(result.disabledCount()).isEqualTo(1);
        assertThat(favorite.getNotificationEnabled()).isFalse();
        assertThat(favorite.getScheduledAt()).isNull();
        assertThat(favorite.getSentAt()).isNull();
    }

    @Test
    @DisplayName("대회 마감일이 앞당겨지면 기존 pending reminder를 비활성화한다")
    void disableInvalidRemindersForTournament_disablesReminderAfterDeadlineChanged() {
        Tournament tournament = tournament(10L, "2026-06-10");
        User user = user(2L);
        TournamentFavorite favorite = favorite(user, tournament);
        favorite.enableReminder(
                LocalDate.of(2026, 6, 12),
                LocalTime.of(9, 0),
                LocalDateTime.of(2026, 6, 12, 9, 0)
        );

        when(tournamentFavoriteRepository.findAllByTournament_IdAndNotificationEnabledTrue(10L))
                .thenReturn(List.of(favorite));

        service().disableInvalidRemindersForTournament(10L);

        assertThat(favorite.getNotificationEnabled()).isFalse();
        assertThat(favorite.getRemindDate()).isNull();
        assertThat(favorite.getRemindTime()).isNull();
        assertThat(favorite.getScheduledAt()).isNull();
        assertThat(favorite.getSentAt()).isNull();
    }

    private TournamentFavoriteService service() {
        Clock clock = Clock.fixed(Instant.parse("2026-06-01T00:00:00Z"), SEOUL_ZONE);
        return new TournamentFavoriteService(
                tournamentFavoriteRepository,
                tournamentRepository,
                userRepository,
                tournamentPosterService,
                notificationService,
                pushNotificationService,
                clock
        );
    }

    private Tournament tournament(Long id, String registrationDeadline) {
        Tournament tournament = Tournament.builder()
                .hostUserId(99L)
                .source(TournamentSource.MANUAL)
                .title("제5회 롤링컵")
                .organizer("Rolling")
                .posterUrl("https://cdn.rolling.com/posters/10.jpg")
                .competitionDate("2026-06-30")
                .registrationDeadline(registrationDeadline)
                .location("서울")
                .region(Region.SEOUL)
                .applyLink("https://example.com/apply")
                .build();
        ReflectionTestUtils.setField(tournament, "id", id);
        return tournament;
    }

    private User user(Long id) {
        User user = User.builder()
                .socialId("social-" + id)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("user-" + id)
                .email("user" + id + "@example.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private TournamentFavorite favorite(User user, Tournament tournament) {
        TournamentFavorite favorite = TournamentFavorite.builder()
                .user(user)
                .tournament(tournament)
                .build();
        ReflectionTestUtils.setField(favorite, "id", 100L);
        return favorite;
    }
}
