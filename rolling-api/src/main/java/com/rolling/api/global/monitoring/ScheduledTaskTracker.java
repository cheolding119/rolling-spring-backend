package com.rolling.api.global.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ScheduledTaskTracker {

    private final Map<String, MutableTaskState> states = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> runningGauges = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> lastSuccessGauges = new ConcurrentHashMap<>();
    private MeterRegistry meterRegistry;

    @Autowired(required = false)
    void setMeterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordStart(String taskName) {
        MutableTaskState state = states.computeIfAbsent(taskName, MutableTaskState::new);
        synchronized (state) {
            state.running = true;
            state.state = "RUNNING";
            state.lastStartedAt = Instant.now();
            state.executionCount++;
            state.lastError = null;
        }
        setRunning(taskName, true);
    }

    public void recordSuccess(String taskName, String summary) {
        MutableTaskState state = states.computeIfAbsent(taskName, MutableTaskState::new);
        Duration duration;
        Instant now;
        synchronized (state) {
            now = Instant.now();
            duration = durationBetween(state.lastStartedAt, now);
            state.running = false;
            state.state = "SUCCESS";
            state.lastFinishedAt = now;
            state.lastSucceededAt = now;
            state.lastSummary = summary;
            state.lastError = null;
        }
        setRunning(taskName, false);
        incrementExecutionCounter(taskName, "success");
        recordDuration(taskName, duration);
        setLastSuccessEpoch(taskName, now.getEpochSecond());
    }

    public void recordFailure(String taskName, Exception exception) {
        MutableTaskState state = states.computeIfAbsent(taskName, MutableTaskState::new);
        Duration duration;
        synchronized (state) {
            Instant now = Instant.now();
            duration = durationBetween(state.lastStartedAt, now);
            state.running = false;
            state.state = "FAILED";
            state.lastFinishedAt = now;
            state.lastFailedAt = now;
            state.lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
        setRunning(taskName, false);
        incrementExecutionCounter(taskName, "failure");
        recordDuration(taskName, duration);
    }

    public ScheduledTaskSnapshot snapshot(String taskName) {
        MutableTaskState state = states.computeIfAbsent(taskName, MutableTaskState::new);
        synchronized (state) {
            return new ScheduledTaskSnapshot(
                    state.taskName,
                    state.state,
                    state.running,
                    state.lastStartedAt,
                    state.lastFinishedAt,
                    state.lastSucceededAt,
                    state.lastFailedAt,
                    state.lastSummary,
                    state.lastError,
                    state.executionCount
            );
        }
    }

    private static final class MutableTaskState {
        private final String taskName;
        private String state = "NEVER_RUN";
        private boolean running;
        private Instant lastStartedAt;
        private Instant lastFinishedAt;
        private Instant lastSucceededAt;
        private Instant lastFailedAt;
        private String lastSummary;
        private String lastError;
        private long executionCount;

        private MutableTaskState(String taskName) {
            this.taskName = taskName;
        }
    }

    private Duration durationBetween(Instant startedAt, Instant finishedAt) {
        if (startedAt == null || finishedAt == null || finishedAt.isBefore(startedAt)) {
            return Duration.ZERO;
        }
        return Duration.between(startedAt, finishedAt);
    }

    private void incrementExecutionCounter(String taskName, String result) {
        if (meterRegistry == null) {
            return;
        }
        meterRegistry.counter("rolling_scheduler_execution_total", "task", taskName, "result", result)
                .increment();
    }

    private void recordDuration(String taskName, Duration duration) {
        if (meterRegistry == null || duration.isNegative()) {
            return;
        }
        meterRegistry.timer("rolling_scheduler_duration_seconds", "task", taskName)
                .record(duration);
    }

    private void setRunning(String taskName, boolean running) {
        if (meterRegistry == null) {
            return;
        }
        AtomicInteger runningGauge = runningGauges.computeIfAbsent(taskName, key ->
                meterRegistry.gauge("rolling_scheduler_running", io.micrometer.core.instrument.Tags.of("task", key), new AtomicInteger())
        );
        runningGauge.set(running ? 1 : 0);
    }

    private void setLastSuccessEpoch(String taskName, long epochSeconds) {
        if (meterRegistry == null) {
            return;
        }
        AtomicLong lastSuccessGauge = lastSuccessGauges.computeIfAbsent(taskName, key ->
                meterRegistry.gauge("rolling_scheduler_last_success_unixtime", io.micrometer.core.instrument.Tags.of("task", key), new AtomicLong())
        );
        lastSuccessGauge.set(epochSeconds);
    }
}
