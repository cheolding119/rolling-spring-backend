package com.rolling.api.domain.seminar.event;

import java.util.List;

public record SeminarCanceledEvent(
        Long seminarId,
        String seminarTitle,
        List<Long> participantUserIds
) {
}
