package com.rolling.api.domain.openmat.event;

import java.util.List;

public record OpenMatUpdatedEvent(
        Long openMatId,
        String openMatTitle,
        List<Long> participantUserIds
) {
}
