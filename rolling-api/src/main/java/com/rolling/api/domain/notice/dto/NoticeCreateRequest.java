package com.rolling.api.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "공지사항 생성 요청")
public class NoticeCreateRequest {

    @NotBlank(message = "공지사항 제목은 필수입니다")
    @Size(max = 255, message = "공지사항 제목은 255자 이하여야 합니다")
    @Schema(description = "공지사항 제목", example = "3월 서비스 점검 안내")
    private String title;

    @NotBlank(message = "공지사항 본문은 필수입니다")
    @Schema(description = "공지사항 본문", example = "3월 20일 02:00부터 03:00까지 점검이 진행됩니다.")
    private String content;

    @NotBlank(message = "작성자 표시는 필수입니다")
    @Size(max = 100, message = "작성자 표시는 100자 이하여야 합니다")
    @Schema(description = "앱에 노출할 작성자 이름", example = "Rolling Admin")
    private String authorName;

    @Size(max = 100, message = "createdBy는 100자 이하여야 합니다")
    @Schema(description = "운영 내부 추적용 작성자 식별자. 없으면 authorName을 그대로 사용합니다.", example = "ops-admin")
    private String createdBy;
}
