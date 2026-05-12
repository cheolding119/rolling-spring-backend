package com.rolling.api.domain.seminar.event;

import java.util.List;

public record SeminarDeletedEvent(
        Long seminarId,
        String seminarTitle,
        List<Long> participantUserIds
) {
}
