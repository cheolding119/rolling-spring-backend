# Slack Webhook Phase 1: 앱 이벤트 기반 알림 (기준일: 2026-04-09)

## 목표

이 phase의 목표는 `rolling-api` 내부에서 직접 감지 가능한 장애 신호를 Slack Incoming Webhook으로 운영 채널에 즉시 전달하는 것이다.

이번 단계에서 다루는 알림 범위는 다음과 같다.

- 예상치 못한 `500` 계열 서버 예외
- 외부 API 실패가 실제 사용자 흐름 실패로 이어진 경우
- 스케줄러 작업 실패
- 앱 기동 직후 핵심 외부 의존성 `DOWN`

이번 단계에서 제외하는 범위는 다음과 같다.

- `BusinessException`, `AuthException`, `MethodArgumentNotValidException` 같은 의도된 4xx
- CPU, memory, latency, error rate 같은 메트릭 기반 경고
- health 상태 변화 polling 및 복구 알림 자동화

## 설계 원칙

- 감지와 전송을 분리한다.
- Slack 전송 실패가 원래 API 응답이나 스케줄러 흐름을 망치지 않게 한다.
- 첫 알림은 빠르게 보내되 동일 원인 반복 폭주는 억제한다.
- 민감정보는 Slack 메시지와 로그 어디에도 남기지 않는다.

## 권장 패키지 구조

```text
src/main/java/com/rolling/api/global/alert/
  AlertSeverity.java
  AlertType.java
  OperationalAlert.java
  OperationalAlertPublisher.java
  OperationalAlertEvent.java
  AlertDeduplicator.java
  SlackAlertProperties.java
  SlackWebhookClient.java
  SlackAlertDispatcher.java
  SlackMessageFormatter.java
  StartupHealthAlertRunner.java
```

## 설정 기준

```yaml
slack:
  alert:
    enabled: ${SLACK_ALERT_ENABLED:false}
    webhook-url: ${SLACK_ALERT_WEBHOOK_URL:}
    environment: ${SLACK_ALERT_ENVIRONMENT:${spring.application.name}}
    connect-timeout-ms: ${SLACK_ALERT_CONNECT_TIMEOUT_MS:1000}
    read-timeout-ms: ${SLACK_ALERT_READ_TIMEOUT_MS:3000}
    default-cooldown-seconds: ${SLACK_ALERT_DEFAULT_COOLDOWN_SECONDS:300}
    scheduler-cooldown-seconds: ${SLACK_ALERT_SCHEDULER_COOLDOWN_SECONDS:600}
    notify-unexpected-exception: ${SLACK_ALERT_NOTIFY_UNEXPECTED_EXCEPTION:true}
    notify-external-api-exception: ${SLACK_ALERT_NOTIFY_EXTERNAL_API_EXCEPTION:true}
    notify-scheduler-failure: ${SLACK_ALERT_NOTIFY_SCHEDULER_FAILURE:true}
    notify-startup-health-down: ${SLACK_ALERT_NOTIFY_STARTUP_HEALTH_DOWN:true}
```

운영 원칙:

- 실제 webhook URL은 문서나 코드에 적지 않는다.
- 실제 값은 `SLACK_ALERT_WEBHOOK_URL` secret로만 주입한다.
- `webhook-url`이 비어 있으면 no-op Bean으로 대체한다.

## 메시지 기준

예상치 못한 예외 예시:

```text
[ROLLING API][PROD][ERROR] Unexpected exception
- source: GlobalExceptionHandler
- requestId: 2c56...
- userId: 123
- method: POST
- path: /api/v1/open-mats
- status: 500
- errorCode: INTERNAL_ERROR
- exception: NullPointerException
- message: host user lookup failed
```

스케줄러 실패 예시:

```text
[ROLLING API][PROD][CRITICAL] Scheduler failure
- scheduler: tournamentCrawler
- status: FAILED
- exception: IllegalStateException
- message: crawl source parsing failed
- lastSummary: crawled=12,created=2,updated=1,skipped=9
```

## Phase 1 체크리스트

### Phase 1-1. 기반 구성

- [x] `SlackAlertProperties` 추가
- [x] `OperationalAlert`, `AlertSeverity`, `AlertType` 추가
- [x] `SlackWebhookClient` 추가
- [x] `SlackAlertDispatcher` 추가
- [x] no-op fallback 전략 반영

### Phase 1-2. 앱 예외 알림

- [x] `GlobalExceptionHandler.handleException(Exception e)` 알림 발행 연결
- [x] `GlobalExceptionHandler.handleRestClientException(RestClientException e)` 알림 발행 연결
- [x] 4xx 계열 예외는 기본 미발행 정책 유지
- [x] `requestId`, `userId`, `method`, `path`, `status`, `errorCode` 포함 포맷 확정

### Phase 1-3. 스케줄러 및 startup 알림

- [x] `OpenMatStatusScheduler` 실패 알림 연결
- [x] `TournamentCrawlerScheduler` 실패 알림 연결
- [x] `AuthService.processScheduledWithdrawals()` 실패 알림 연결
- [x] `StartupHealthAlertRunner` 추가
- [x] 앱 기동 직후 핵심 health `DOWN` 1회 경고 연결

### Phase 1-4. 운영 안정화

- [x] `AlertDeduplicator` 메모리 기반 구현
- [x] `@Async` + 전용 executor 적용
- [x] Slack 전송 실패 시 예외 재전파 금지
- [x] webhook URL 비로그 정책 점검
- [x] 민감정보 비포함 정책 점검

### Phase 1-5. 테스트

- [x] `SlackMessageFormatterTest`
- [x] `AlertDeduplicatorTest`
- [x] `SlackAlertDispatcherTest`
- [x] 500 예외 발생 시 publisher 호출 검증
- [x] `BusinessException` 발생 시 publisher 미호출 검증
- [x] 스케줄러 실패 시 publisher 호출 검증
- [x] `enabled=false` 또는 `webhook-url` 누락 시 no-op 동작 검증

## 완료 기준

- 운영자가 앱 내부 장애를 로그보다 먼저 Slack에서 인지할 수 있다.
- Slack 전송 실패가 비즈니스 흐름을 깨지 않는다.
- 반복 알림 폭주 없이 핵심 장애 신호를 전달할 수 있다.
