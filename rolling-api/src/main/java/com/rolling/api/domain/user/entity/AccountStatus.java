package com.rolling.api.domain.user.entity;

public enum AccountStatus {
    ACTIVE,
    WARNING,
    SUSPENDED,
    @Deprecated(forRemoval = false)
    BANNED,
    WITHDRAWN
}
