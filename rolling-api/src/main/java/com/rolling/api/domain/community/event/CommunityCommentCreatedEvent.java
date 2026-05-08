package com.rolling.api.domain.community.event;

public record CommunityCommentCreatedEvent(
        Long commentId,
        Long postId,
        Long postAuthorId,
        Long commenterUserId,
        String commenterNickname,
        String postTitle,
        String commentContent
) {
}
