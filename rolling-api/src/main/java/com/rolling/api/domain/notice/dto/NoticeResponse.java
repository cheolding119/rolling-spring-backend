package com.rolling.api.domain.notice.dto;

import com.rolling.api.domain.notice.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeResponse {

    private Long id;
    private String title;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .authorName(notice.getAuthorName())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
