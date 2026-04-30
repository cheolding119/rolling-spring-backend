package com.rolling.api.infra.apple.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AppleUserResponse {

    private final String socialId;
    private final String email;

    public String getNickname() {
        return null;
    }
}
