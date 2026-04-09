package com.rolling.api.global.alert;

import com.rolling.api.global.monitoring.ExternalDependenciesHealthIndicator;
import com.rolling.api.global.monitoring.SchedulerHealthIndicator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.health.contributor.Health;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StartupHealthAlertRunnerTest {

    @Test
    @DisplayName("startup health 중 하나라도 DOWN이면 Slack alert publisher를 호출한다")
    void run_whenAnyHealthDown_publishesAlert() throws Exception {
        ExternalDependenciesHealthIndicator externalDependenciesHealthIndicator = mock(ExternalDependenciesHealthIndicator.class);
        SchedulerHealthIndicator schedulerHealthIndicator = mock(SchedulerHealthIndicator.class);
        OperationalAlertPublisher operationalAlertPublisher = mock(OperationalAlertPublisher.class);
        StartupHealthAlertRunner runner = new StartupHealthAlertRunner(
                externalDependenciesHealthIndicator,
                schedulerHealthIndicator,
                operationalAlertPublisher
        );

        when(externalDependenciesHealthIndicator.health()).thenReturn(Health.down().withDetail("firebase", "DOWN").build());
        when(schedulerHealthIndicator.health()).thenReturn(Health.up().build());

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(operationalAlertPublisher).publishStartupHealthDown(org.mockito.ArgumentMatchers.anyMap());
    }

    @Test
    @DisplayName("startup health가 모두 UP이면 Slack alert publisher를 호출하지 않는다")
    void run_whenAllHealthUp_doesNotPublishAlert() throws Exception {
        ExternalDependenciesHealthIndicator externalDependenciesHealthIndicator = mock(ExternalDependenciesHealthIndicator.class);
        SchedulerHealthIndicator schedulerHealthIndicator = mock(SchedulerHealthIndicator.class);
        OperationalAlertPublisher operationalAlertPublisher = mock(OperationalAlertPublisher.class);
        StartupHealthAlertRunner runner = new StartupHealthAlertRunner(
                externalDependenciesHealthIndicator,
                schedulerHealthIndicator,
                operationalAlertPublisher
        );

        when(externalDependenciesHealthIndicator.health()).thenReturn(Health.up().build());
        when(schedulerHealthIndicator.health()).thenReturn(Health.up().build());

        runner.run(new DefaultApplicationArguments(new String[0]));

        verify(operationalAlertPublisher, never()).publishStartupHealthDown(org.mockito.ArgumentMatchers.anyMap());
    }
}
