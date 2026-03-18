package com.rolling.api.domain.notice.dto;

import com.rolling.api.domain.notice.entity.Notice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NoticeListItemResponse {

    private Long id;
    private String title;
    private String content;
    private String authorName;
    private LocalDateTime createdAt;

    public static NoticeListItemResponse from(Notice notice) {
        return NoticeListItemResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .authorName(notice.getAuthorName())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}
