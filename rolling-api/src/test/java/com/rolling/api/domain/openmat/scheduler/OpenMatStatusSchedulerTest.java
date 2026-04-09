package com.rolling.api.domain.openmat.scheduler;

import com.rolling.api.domain.openmat.service.OpenMatService;
import com.rolling.api.global.alert.OperationalAlertPublisher;
import com.rolling.api.global.monitoring.ScheduledTaskSnapshot;
import com.rolling.api.global.monitoring.ScheduledTaskTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenMatStatusSchedulerTest {

    @Mock
    private OpenMatService openMatService;

    @Mock
    private ScheduledTaskTracker scheduledTaskTracker;

    @Mock
    private OperationalAlertPublisher operationalAlertPublisher;

    @Test
    @DisplayName("오픈매트 상태 스케줄러는 만료된 오픈매트 동기화 서비스를 호출한다")
    void syncExpiredStatuses_callsOpenMatService() {
        OpenMatStatusScheduler scheduler = new OpenMatStatusScheduler(openMatService, scheduledTaskTracker, operationalAlertPublisher);
        when(openMatService.syncExpiredOpenMats()).thenReturn(2);

        scheduler.syncExpiredStatuses();

        verify(openMatService).syncExpiredOpenMats();
    }

    @Test
    @DisplayName("오픈매트 상태 스케줄러는 예외가 발생해도 예외를 전파하지 않는다")
    void syncExpiredStatuses_whenServiceFails_doesNotThrow() {
        OpenMatStatusScheduler scheduler = new OpenMatStatusScheduler(openMatService, scheduledTaskTracker, operationalAlertPublisher);
        when(openMatService.syncExpiredOpenMats()).thenThrow(new RuntimeException("sync failed"));
        when(scheduledTaskTracker.snapshot("openMatStatusSync")).thenReturn(
                new ScheduledTaskSnapshot("openMatStatusSync", "FAILED", false, null, null, null, null, null, null, 1)
        );

        assertThatCode(scheduler::syncExpiredStatuses).doesNotThrowAnyException();

        verify(operationalAlertPublisher).publishSchedulerFailure(
                org.mockito.ArgumentMatchers.eq("openMatStatusSync"),
                org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.any(RuntimeException.class)
        );
    }
}
