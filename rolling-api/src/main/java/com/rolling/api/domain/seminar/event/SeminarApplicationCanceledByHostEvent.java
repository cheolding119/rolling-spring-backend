package com.rolling.api.domain.seminar.event;

public record SeminarApplicationCanceledByHostEvent(
        Long seminarId,
        String seminarTitle,
        Long applicantUserId
) {
}
