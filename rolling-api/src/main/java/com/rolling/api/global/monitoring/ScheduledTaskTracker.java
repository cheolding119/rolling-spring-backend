package com.rolling.api.global.monitoring;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ScheduledTaskTracker {

    private final Map<String, MutableTaskState> states = new ConcurrentHashMap<>();

    public void recordStart(String taskName) {
        MutableTaskState state = states.computeIfAbsent(taskName, MutableTaskState::new);
        synchronized (state) {
            state.running = true;
            state.state = "RUNNING";
            state.lastStartedAt = Instant.now();
            state.executionCount++;
            state.lastError = null;
        }
    }

    public void recordSuccess(String taskName, String summary) {
        MutableTaskState state = states.computeIfAbsent(taskName, MutableTaskState::new);
        synchronized (state) {
            Instant now = Instant.now();
            state.running = false;
            state.state = "SUCCESS";
            state.lastFinishedAt = now;
            state.lastSucceededAt = now;
            state.lastSummary = summary;
            state.lastError = null;
        }
    }

    public void recordFailure(String taskName, Exception exception) {
        MutableTaskState state = states.computeIfAbsent(taskName, MutableTaskState::new);
        synchronized (state) {
            Instant now = Instant.now();
            state.running = false;
            state.state = "FAILED";
            state.lastFinishedAt = now;
            state.lastFailedAt = now;
            state.lastError = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
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
}
