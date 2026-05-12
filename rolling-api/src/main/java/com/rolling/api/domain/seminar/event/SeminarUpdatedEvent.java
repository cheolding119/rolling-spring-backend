package com.rolling.api.domain.seminar.event;

import java.util.List;

public record SeminarUpdatedEvent(
        Long seminarId,
        String seminarTitle,
        List<Long> participantUserIds
) {
}
