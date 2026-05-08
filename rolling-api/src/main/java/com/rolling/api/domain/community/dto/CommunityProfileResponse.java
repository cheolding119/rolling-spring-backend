package com.rolling.api.domain.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "커뮤니티 닉네임 응답")
public class CommunityProfileResponse {

    @Schema(description = "커뮤니티 전용 닉네임", example = "노기좋아하는거북이", nullable = true)
    private String communityNickname;

    public static CommunityProfileResponse from(String communityNickname) {
        return CommunityProfileResponse.builder()
                .communityNickname(communityNickname)
                .build();
    }
}
