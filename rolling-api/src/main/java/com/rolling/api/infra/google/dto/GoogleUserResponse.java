package com.rolling.api.infra.google.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class GoogleUserResponse {

    private String sub;

    private String name;

    private String email;

    @JsonProperty("picture")
    private String profileImageUrl;

    public String getSocialId() {
        return sub;
    }

    public String getNickname() {
        return name != null ? name : "Unknown";
    }
}