package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.FriendRequest;
import com.rolling.api.domain.traininglog.entity.FriendRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FriendRequestResponse {

    private Long id;
    private Long senderUserId;
    private String senderNickname;
    private Long receiverUserId;
    private String receiverNickname;
    private FriendRequestStatus status;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;

    public static FriendRequestResponse from(FriendRequest request) {
        return FriendRequestResponse.builder()
                .id(request.getId())
                .senderUserId(request.getSender().getId())
                .senderNickname(request.getSender().getNickname())
                .receiverUserId(request.getReceiver().getId())
                .receiverNickname(request.getReceiver().getNickname())
                .status(request.getStatus())
                .respondedAt(request.getRespondedAt())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
