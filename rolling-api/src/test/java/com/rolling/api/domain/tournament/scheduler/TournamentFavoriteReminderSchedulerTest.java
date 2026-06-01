package com.rolling.api.domain.tournament.scheduler;

import com.rolling.api.domain.tournament.dto.TournamentFavoriteReminderDispatchResult;
import com.rolling.api.domain.tournament.service.TournamentFavoriteService;
import com.rolling.api.global.alert.OperationalAlertPublisher;
import com.rolling.api.global.monitoring.ScheduledTaskSnapshot;
import com.rolling.api.global.monitoring.ScheduledTaskTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TournamentFavoriteReminderSchedulerTest {

    @Mock
    private TournamentFavoriteService tournamentFavoriteService;

    @Mock
    private ScheduledTaskTracker scheduledTaskTracker;

    @Mock
    private OperationalAlertPublisher operationalAlertPublisher;

    @Test
    @DisplayName("대회 찜 리마인드 스케줄러는 발송 서비스를 호출한다")
    void dispatchDueReminders_callsFavoriteService() {
        TournamentFavoriteReminderScheduler scheduler = new TournamentFavoriteReminderScheduler(
                tournamentFavoriteService,
                scheduledTaskTracker,
                operationalAlertPublisher
        );
        when(tournamentFavoriteService.dispatchDueReminders())
                .thenReturn(new TournamentFavoriteReminderDispatchResult(3, 2, 1, 0));

        scheduler.dispatchDueReminders();

        verify(tournamentFavoriteService).dispatchDueReminders();
    }

    @Test
    @DisplayName("대회 찜 리마인드 스케줄러는 예외가 발생해도 예외를 전파하지 않는다")
    void dispatchDueReminders_whenServiceFails_doesNotThrow() {
        TournamentFavoriteReminderScheduler scheduler = new TournamentFavoriteReminderScheduler(
                tournamentFavoriteService,
                scheduledTaskTracker,
                operationalAlertPublisher
        );
        when(tournamentFavoriteService.dispatchDueReminders()).thenThrow(new RuntimeException("dispatch failed"));
        when(scheduledTaskTracker.snapshot("tournamentFavoriteReminder")).thenReturn(
                new ScheduledTaskSnapshot("tournamentFavoriteReminder", "FAILED", false, null, null, null, null, null, null, 1)
        );

        assertThatCode(scheduler::dispatchDueReminders).doesNotThrowAnyException();

        verify(operationalAlertPublisher).publishSchedulerFailure(
                eq("tournamentFavoriteReminder"),
                isNull(),
                any(RuntimeException.class)
        );
    }
}
