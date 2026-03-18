package com.rolling.api.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "공지사항 수정 요청")
public class NoticeUpdateRequest {

    @Size(max = 255, message = "공지사항 제목은 255자 이하여야 합니다")
    @Schema(description = "공지사항 제목", example = "3월 서비스 점검 시간 변경 안내")
    private String title;

    @Schema(description = "공지사항 본문", example = "점검 시간이 02:30부터 03:30으로 변경되었습니다.")
    private String content;

    @Size(max = 100, message = "작성자 표시는 100자 이하여야 합니다")
    @Schema(description = "앱에 노출할 작성자 이름", example = "Rolling Team")
    private String authorName;

    @Size(max = 100, message = "createdBy는 100자 이하여야 합니다")
    @Schema(description = "운영 내부 추적용 작성자 식별자", example = "ops-manager")
    private String createdBy;
}
