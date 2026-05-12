package com.rolling.api.domain.seminar.event;

public record SeminarApplicationCanceledEvent(
        Long seminarId,
        String seminarTitle,
        Long applicantUserId
) {
}
