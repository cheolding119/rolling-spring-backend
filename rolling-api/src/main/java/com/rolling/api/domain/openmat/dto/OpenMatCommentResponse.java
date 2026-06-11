package com.rolling.api.domain.openmat.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class OpenMatCommentResponse {

    private Long id;
    private Long openMatId;
    private Long parentCommentId;
    private Long authorUserId;
    private String authorNickname;
    private String content;
    private boolean deleted;
    private boolean editableByMe;
    private boolean deletableByMe;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<OpenMatCommentResponse> replies;
}
