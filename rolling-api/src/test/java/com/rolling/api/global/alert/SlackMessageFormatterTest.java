package com.rolling.api.global.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SlackMessageFormatterTest {

    private final SlackMessageFormatter formatter = new SlackMessageFormatter();

    @Test
    @DisplayName("스케줄러 실패 메시지는 한국어 중심으로 핵심 원인과 조치만 보여준다")
    void format_schedulerFailure_isReadableInKorean() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lastSummary", "processed=0");

        OperationalAlert alert = OperationalAlert.builder()
                .severity(AlertSeverity.CRITICAL)
                .type(AlertType.SCHEDULER_FAILURE)
                .source("withdrawalProcessor")
                .summary("Scheduler failure")
                .environment("prod")
                .occurredAt(Instant.parse("2026-04-09T01:02:03Z"))
                .schedulerName("withdrawalProcessor")
                .status("FAILED")
                .exceptionClass("IllegalStateException")
                .exceptionMessage("withdraw failed")
                .details(details)
                .build();

        String formatted = formatter.format(alert);

        assertThat(formatted).contains("[ROLLING API][운영/PROD][치명/CRITICAL] 스케줄러 실행 실패");
        assertThat(formatted).contains("- 무슨 문제인가: 탈퇴 처리 작업 실행 중 오류가 발생했습니다.");
        assertThat(formatted).contains("- 확인된 원인: IllegalStateException: withdraw failed");
        assertThat(formatted).contains("- 참고 정보: 직전 실행 요약 = processed=0");
        assertThat(formatted).contains("- 지금 확인할 것: 탈퇴 처리 로그와 최근 실행 결과를 확인하세요.");
    }

    @Test
    @DisplayName("startup health down 메시지는 비정상 항목만 한국어로 요약한다")
    void format_startupHealthDown_summarizesOnlyProblematicItems() {
        Map<String, Object> crawler = new LinkedHashMap<>();
        crawler.put("streetJiuJitsuUrlCount", 0);
        crawler.put("status", "DOWN");

        Map<String, Object> firebase = new LinkedHashMap<>();
        firebase.put("status", "UP");

        Map<String, Object> externalDependencies = new LinkedHashMap<>();
        externalDependencies.put("firebase", firebase);
        externalDependencies.put("crawler", crawler);

        Map<String, Object> scheduler = new LinkedHashMap<>();
        scheduler.put("tournamentCrawler", Map.of("enabled", true, "state", "NEVER_RUN"));

        OperationalAlert alert = OperationalAlert.builder()
                .severity(AlertSeverity.CRITICAL)
                .type(AlertType.STARTUP_HEALTH_DOWN)
                .source("StartupHealthAlertRunner")
                .summary("Startup health down")
                .environment("prod")
                .occurredAt(Instant.parse("2026-04-09T05:32:58Z"))
                .details(Map.of(
                        "externalDependenciesStatus", "DOWN",
                        "schedulerStatus", "UP",
                        "externalDependencies", externalDependencies,
                        "scheduler", scheduler
                ))
                .build();

        String formatted = formatter.format(alert);

        assertThat(formatted).contains("[ROLLING API][운영/PROD][치명/CRITICAL] 서버 시작 점검 실패");
        assertThat(formatted).contains("- 무슨 문제인가: 서버 시작 시 필수 점검 항목 중 비정상 상태가 발견됐습니다.");
        assertThat(formatted).contains("- 확인된 원인: 대회 크롤러 URL 설정이 비어 있습니다.");
        assertThat(formatted).contains("- 영향: 대회 크롤러가 정상 동작하지 않을 수 있습니다.");
        assertThat(formatted).contains("- 지금 확인할 것: `tournament.crawler.street-jiujitsu.list-page-urls` 설정값을 확인하세요.");
        assertThat(formatted).doesNotContain("firebase");
        assertThat(formatted.lines().count()).isLessThanOrEqualTo(7);
    }

    @Test
    @DisplayName("예상하지 못한 서버 오류 메시지는 요청 정보와 추적 정보를 한국어로 정리한다")
    void format_unexpectedException_rendersTrackingInfo() {
        OperationalAlert alert = OperationalAlert.builder()
                .severity(AlertSeverity.ERROR)
                .type(AlertType.UNEXPECTED_EXCEPTION)
                .source("GlobalExceptionHandler")
                .summary("Unexpected exception")
                .environment("prod")
                .occurredAt(Instant.parse("2026-04-09T01:02:03Z"))
                .requestId("req-123")
                .userId("42")
                .method("POST")
                .path("/api/v1/open-mats")
                .status("500")
                .errorCode("INTERNAL_ERROR")
                .exceptionClass("IllegalStateException")
                .exceptionMessage("boom")
                .build();

        String formatted = formatter.format(alert);

        assertThat(formatted).contains("[ROLLING API][운영/PROD][오류/ERROR] 예상하지 못한 서버 오류");
        assertThat(formatted).contains("- 요청 정보: POST /api/v1/open-mats");
        assertThat(formatted).contains("- 응답 상태: 500");
        assertThat(formatted).contains("- 확인된 원인: IllegalStateException: boom");
        assertThat(formatted).contains("- 추적 정보: requestId=req-123, userId=42, errorCode=INTERNAL_ERROR");
    }
}
