package com.rolling.api.global.alert;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SlackMessageFormatter {

    private static final String APP_NAME = "ROLLING API";

    public String format(OperationalAlert alert) {
        return switch (alert.getType()) {
            case STARTUP_HEALTH_DOWN -> formatStartupHealthDown(alert);
            case SCHEDULER_FAILURE -> formatSchedulerFailure(alert);
            case UNEXPECTED_EXCEPTION, EXTERNAL_API_FAILURE -> formatRequestFailure(alert);
        };
    }

    private String formatStartupHealthDown(OperationalAlert alert) {
        List<String> lines = new ArrayList<>();
        StartupInsight insight = summarizeStartupInsight(alert.getDetails());

        lines.add(header(alert, "서버 시작 점검 실패"));
        lines.add("- 무슨 문제인가: " + insight.problem());
        for (String cause : insight.causes()) {
            lines.add("- 확인된 원인: " + cause);
        }
        lines.add("- 영향: " + insight.impact());
        lines.add("- 지금 확인할 것: " + insight.action());
        addLine(lines, "발생 시각", alert.getOccurredAt());

        return String.join(System.lineSeparator(), lines);
    }

    private StartupInsight summarizeStartupInsight(Map<String, Object> details) {
        List<String> causes = new ArrayList<>();
        List<String> impacts = new ArrayList<>();
        List<String> actions = new ArrayList<>();

        if ("DOWN".equals(asText(details.get("externalDependenciesStatus")))) {
            summarizeExternalDependencies(asMap(details.get("externalDependencies")), causes, impacts, actions);
        }

        if ("DOWN".equals(asText(details.get("schedulerStatus")))) {
            summarizeSchedulers(asMap(details.get("scheduler")), causes, impacts, actions);
        }

        if (causes.isEmpty()) {
            causes.add("서버 시작 점검에서 비정상 항목이 발견됐습니다.");
        }

        return new StartupInsight(
                "서버 시작 시 필수 점검 항목 중 비정상 상태가 발견됐습니다.",
                limitDistinct(causes, 2),
                firstOrDefault(impacts, "일부 운영 기능이 정상 동작하지 않을 수 있습니다."),
                firstOrDefault(actions, "health detail과 관련 설정값을 확인하세요.")
        );
    }

    private void summarizeExternalDependencies(
            Map<String, Object> externalDependencies,
            List<String> causes,
            List<String> impacts,
            List<String> actions
    ) {
        for (Map.Entry<String, Object> entry : externalDependencies.entrySet()) {
            Map<String, Object> detail = asMap(entry.getValue());
            if (!"DOWN".equals(asText(detail.get("status")))) {
                continue;
            }

            switch (entry.getKey()) {
                case "firebase" -> {
                    causes.add("Firebase 초기화 상태가 비정상입니다.");
                    impacts.add("푸시 알림이 정상 전송되지 않을 수 있습니다.");
                    actions.add("Firebase projectId, credentialsPath, 초기화 상태를 확인하세요.");
                }
                case "s3" -> {
                    causes.add("S3 설정 또는 클라이언트 초기화 상태가 비정상입니다.");
                    impacts.add("S3 파일 업로드가 정상 동작하지 않을 수 있습니다.");
                    actions.add("AWS S3 bucket 설정과 S3 client 초기화 상태를 확인하세요.");
                }
                case "socialLogin" -> {
                    causes.add("소셜 로그인 클라이언트 초기화 상태가 비정상입니다.");
                    impacts.add("소셜 로그인 기능이 정상 동작하지 않을 수 있습니다.");
                    actions.add("Google/Kakao client 설정과 초기화 상태를 확인하세요.");
                }
                case "crawler" -> {
                    int urlCount = asInteger(detail.get("streetJiuJitsuUrlCount"));
                    if (urlCount == 0) {
                        causes.add("대회 크롤러 URL 설정이 비어 있습니다.");
                        impacts.add("대회 크롤러가 정상 동작하지 않을 수 있습니다.");
                        actions.add("`tournament.crawler.street-jiujitsu.list-page-urls` 설정값을 확인하세요.");
                    } else {
                        causes.add("대회 크롤러 의존성 상태가 비정상입니다.");
                        impacts.add("대회 크롤러가 정상 동작하지 않을 수 있습니다.");
                        actions.add("크롤러 설정값과 대상 URL 접근 가능 여부를 확인하세요.");
                    }
                }
                default -> {
                    causes.add(entry.getKey() + " 의존성 상태가 비정상입니다.");
                    actions.add(entry.getKey() + " 관련 설정과 초기화 상태를 확인하세요.");
                }
            }
        }
    }

    private void summarizeSchedulers(
            Map<String, Object> schedulerDetails,
            List<String> causes,
            List<String> impacts,
            List<String> actions
    ) {
        for (Map.Entry<String, Object> entry : schedulerDetails.entrySet()) {
            Map<String, Object> detail = asMap(entry.getValue());
            if (!Boolean.TRUE.equals(detail.get("enabled")) || !"FAILED".equals(asText(detail.get("state")))) {
                continue;
            }

            String schedulerName = schedulerDisplayName(entry.getKey());
            String lastError = asText(detail.get("lastError"));
            causes.add(StringUtils.hasText(lastError)
                    ? schedulerName + " 스케줄러가 실패 상태입니다. (" + lastError + ")"
                    : schedulerName + " 스케줄러가 실패 상태입니다.");
            impacts.add(schedulerName + " 백그라운드 작업이 정상 수행되지 않을 수 있습니다.");
            actions.add(schedulerName + " 스케줄러 로그와 최근 실행 결과를 확인하세요.");
        }
    }

    private String formatSchedulerFailure(OperationalAlert alert) {
        List<String> lines = new ArrayList<>();
        String schedulerName = schedulerDisplayName(safeValue(alert.getSchedulerName(), "알 수 없는 작업"));

        lines.add(header(alert, "스케줄러 실행 실패"));
        lines.add("- 무슨 문제인가: " + schedulerName + " 작업 실행 중 오류가 발생했습니다.");

        addLine(lines, "확인된 원인", joinNonBlank(": ", alert.getExceptionClass(), alert.getExceptionMessage()));
        addLine(lines, "참고 정보", formatLastSummary(alert.getDetails().get("lastSummary")));
        lines.add("- 영향: 백그라운드 작업이 정상 수행되지 않을 수 있습니다.");
        lines.add("- 지금 확인할 것: " + schedulerName + " 로그와 최근 실행 결과를 확인하세요.");
        addLine(lines, "발생 시각", alert.getOccurredAt());

        return String.join(System.lineSeparator(), lines);
    }

    private String formatRequestFailure(OperationalAlert alert) {
        List<String> lines = new ArrayList<>();
        lines.add(header(alert, titleForType(alert.getType())));
        lines.add("- 무슨 문제인가: " + problemForType(alert.getType()));
        addLine(lines, "요청 정보", buildRequestInfo(alert));
        addLine(lines, "응답 상태", translateStatus(alert.getStatus()));
        addLine(lines, "확인된 원인", joinNonBlank(": ", alert.getExceptionClass(), alert.getExceptionMessage()));
        addLine(lines, "추적 정보", buildTrackingInfo(alert));
        lines.add("- 지금 확인할 것: " + actionForType(alert.getType()));
        addLine(lines, "발생 시각", alert.getOccurredAt());

        return String.join(System.lineSeparator(), lines);
    }

    private String header(OperationalAlert alert, String title) {
        return String.format(
                "[%s][%s][%s] %s",
                APP_NAME,
                normalizeEnvironment(alert.getEnvironment()),
                severityLabel(alert.getSeverity()),
                title
        );
    }

    private String normalizeEnvironment(String environment) {
        if (!StringUtils.hasText(environment)) {
            return "로컬/LOCAL";
        }

        String normalized = environment.trim().toUpperCase();
        return switch (normalized) {
            case "PROD", "PRODUCTION" -> "운영/PROD";
            case "DEV", "DEVELOPMENT" -> "개발/DEV";
            case "STAGE", "STAGING" -> "스테이징/STAGING";
            case "LOCAL" -> "로컬/LOCAL";
            default -> normalized;
        };
    }

    private String severityLabel(AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "치명/CRITICAL";
            case ERROR -> "오류/ERROR";
            case WARN -> "경고/WARN";
        };
    }

    private String titleForType(AlertType type) {
        return switch (type) {
            case UNEXPECTED_EXCEPTION -> "예상하지 못한 서버 오류";
            case EXTERNAL_API_FAILURE -> "외부 API 호출 실패";
            case SCHEDULER_FAILURE -> "스케줄러 실행 실패";
            case STARTUP_HEALTH_DOWN -> "서버 시작 점검 실패";
        };
    }

    private String problemForType(AlertType type) {
        return switch (type) {
            case UNEXPECTED_EXCEPTION -> "요청 처리 중 예기치 않은 서버 오류가 발생했습니다.";
            case EXTERNAL_API_FAILURE -> "외부 API 연동 중 오류가 발생했습니다.";
            case SCHEDULER_FAILURE -> "스케줄러 실행 중 오류가 발생했습니다.";
            case STARTUP_HEALTH_DOWN -> "서버 시작 시 필수 점검 항목 중 비정상 상태가 발견됐습니다.";
        };
    }

    private String actionForType(AlertType type) {
        return switch (type) {
            case UNEXPECTED_EXCEPTION -> "requestId 기준으로 서버 로그와 스택트레이스를 확인하세요.";
            case EXTERNAL_API_FAILURE -> "외부 API 설정, 네트워크 연결, 응답 상태를 확인하세요.";
            case SCHEDULER_FAILURE -> "스케줄러 로그와 최근 실행 결과를 확인하세요.";
            case STARTUP_HEALTH_DOWN -> "health detail과 관련 설정값을 확인하세요.";
        };
    }

    private String translateStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return null;
        }

        return switch (status.trim().toUpperCase()) {
            case "UP" -> "정상/UP";
            case "DOWN" -> "비정상/DOWN";
            case "FAILED" -> "실패/FAILED";
            case "SUCCESS" -> "성공/SUCCESS";
            case "NEVER_RUN" -> "미실행/NEVER_RUN";
            default -> status;
        };
    }

    private String buildRequestInfo(OperationalAlert alert) {
        return joinNonBlank(" ", alert.getMethod(), alert.getPath());
    }

    private String buildTrackingInfo(OperationalAlert alert) {
        List<String> tokens = new ArrayList<>();
        if (StringUtils.hasText(alert.getRequestId())) {
            tokens.add("requestId=" + alert.getRequestId());
        }
        if (StringUtils.hasText(alert.getUserId())) {
            tokens.add("userId=" + alert.getUserId());
        }
        if (StringUtils.hasText(alert.getErrorCode())) {
            tokens.add("errorCode=" + alert.getErrorCode());
        }
        if (tokens.isEmpty()) {
            return null;
        }

        return String.join(", ", tokens);
    }

    private String formatLastSummary(Object summary) {
        String rendered = asText(summary);
        if (!StringUtils.hasText(rendered)) {
            return null;
        }
        return "직전 실행 요약 = " + rendered;
    }

    private String schedulerDisplayName(String schedulerKey) {
        return switch (schedulerKey) {
            case "openMatStatusSync" -> "오픈매트 상태 동기화";
            case "tournamentCrawler" -> "대회 크롤러";
            case "withdrawalProcessor" -> "탈퇴 처리";
            default -> schedulerKey;
        };
    }

    private Map<String, Object> asMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }

        Map<String, Object> converted = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            converted.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return converted;
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }

        String rendered = String.valueOf(value).trim();
        if (!StringUtils.hasText(rendered) || "-".equals(rendered)) {
            return null;
        }
        return rendered;
    }

    private int asInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(asText(value));
        } catch (Exception ignored) {
            return -1;
        }
    }

    private List<String> limitDistinct(List<String> values, int limit) {
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(limit)
                .toList();
    }

    private String firstOrDefault(List<String> values, String defaultValue) {
        return values.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .findFirst()
                .orElse(defaultValue);
    }

    private String joinNonBlank(String delimiter, String first, String second) {
        List<String> values = new ArrayList<>();
        if (StringUtils.hasText(first)) {
            values.add(first.trim());
        }
        if (StringUtils.hasText(second)) {
            values.add(second.trim());
        }

        String joined = String.join(delimiter, values);
        return StringUtils.hasText(joined) ? joined : null;
    }

    private String safeValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private void addLine(List<String> lines, String label, Object value) {
        String rendered = asText(value);
        if (!StringUtils.hasText(rendered)) {
            return;
        }
        lines.add("- " + label + ": " + rendered);
    }

    private record StartupInsight(
            String problem,
            List<String> causes,
            String impact,
            String action
    ) {
    }
}
