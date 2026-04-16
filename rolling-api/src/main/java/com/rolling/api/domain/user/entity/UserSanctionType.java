package com.rolling.api.domain.user.entity;

public enum UserSanctionType {
    WARNING,
    TEMP_SUSPEND,
    @Deprecated(forRemoval = false)
    PERMANENT_BAN
}
