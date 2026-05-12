package com.rolling.api.domain.seminar.event;

public record SeminarAppliedEvent(
        Long seminarId,
        String seminarTitle,
        Long applicantUserId
) {
}
