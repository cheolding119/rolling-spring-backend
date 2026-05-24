package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.FriendSearchRelationshipStatus;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendSearchResultResponse {

    private Long userId;
    private String nickname;
    private String affiliation;
    private BeltColor beltColor;
    private FriendSearchRelationshipStatus friendRequestStatus;
    private Long outgoingRequestId;

    public static FriendSearchResultResponse from(
            User user,
            FriendSearchRelationshipStatus friendRequestStatus,
            Long outgoingRequestId
    ) {
        return FriendSearchResultResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .affiliation(user.getAffiliation())
                .beltColor(user.getBeltColor())
                .friendRequestStatus(friendRequestStatus)
                .outgoingRequestId(outgoingRequestId)
                .build();
    }
}
