package com.rolling.api.global.alert;

import com.rolling.api.global.monitoring.ExternalDependenciesHealthIndicator;
import com.rolling.api.global.monitoring.SchedulerHealthIndicator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class StartupHealthAlertRunner implements ApplicationRunner {

    private final ExternalDependenciesHealthIndicator externalDependenciesHealthIndicator;
    private final SchedulerHealthIndicator schedulerHealthIndicator;
    private final OperationalAlertPublisher operationalAlertPublisher;

    @Override
    public void run(ApplicationArguments args) {
        Health externalHealth = externalDependenciesHealthIndicator.health();
        Health schedulerHealth = schedulerHealthIndicator.health();

        if (!"DOWN".equals(externalHealth.getStatus().getCode())
                && !"DOWN".equals(schedulerHealth.getStatus().getCode())) {
            return;
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("externalDependenciesStatus", externalHealth.getStatus().getCode());
        details.put("schedulerStatus", schedulerHealth.getStatus().getCode());
        details.put("externalDependencies", externalHealth.getDetails());
        details.put("scheduler", schedulerHealth.getDetails());

        operationalAlertPublisher.publishStartupHealthDown(details);
    }
}
