# Slack Webhook 장애 알림 기획안 (기준일: 2026-04-09)

## 목적

이 문서는 `rolling-api`에서 장애 또는 운영상 주의가 필요한 상황이 발생했을 때 Slack Incoming Webhook으로 운영자에게 즉시 알림을 보내기 위한 설계안을 정리한 문서다.

이번 범위의 목표는 다음과 같다.

- 사용자 요청 중 `실제 장애`에 해당하는 예외를 운영 채널로 전파한다.
- 배치/스케줄러 실패를 앱 로그 확인 전에 먼저 인지할 수 있게 한다.
- 기존 `requestId`, `userId`, `path`, `status`, `errorCode` 같은 운영 추적 정보를 Slack 메시지에 재사용한다.
- Slack 전송 실패가 본래 API 응답이나 스케줄러 흐름을 망치지 않게 한다.

## 현재 진행 상태

- Phase 1 앱 이벤트 기반 Slack 알림은 구현과 기본 테스트까지 반영 완료
- Phase 2 메트릭 기반 Slack 알림은 `Prometheus Alertmanager` 기준 설정과 정적 검증까지 반영 완료
- 실제 Slack 수신 확인과 threshold 튜닝은 배포 환경 검증으로 남아 있음
- Phase 2는 앱 내부 예외 알림을 대체하는 작업이 아니라, 운영 지표 이상 징후 탐지를 보완하는 단계로 진행

## 현재 코드베이스 기준 현황

### 이미 준비된 기반

- `RequestTrackingFilter`에서 요청 단위 `requestId`, `traceId`, `userId`, `method`, `path`, `status`, `errorCode`를 MDC에 기록한다.
- `GlobalExceptionHandler`가 `AuthException`, `BusinessException`, `RestClientException`, `MethodArgumentNotValidException`, 그 외 `Exception`을 중앙 처리한다.
- `ScheduledTaskTracker`, `SchedulerHealthIndicator`가 스케줄러별 마지막 성공/실패 상태를 메모리에 유지한다.
- `ExternalDependenciesHealthIndicator`가 Firebase, S3, Google/Kakao client, crawler 설정 상태를 health detail로 제공한다.
- `docker-compose.yml` 기준으로 운영 환경변수 주입 패턴이 이미 정리돼 있다.
- `RestClient` 공용 Bean이 존재해 외부 HTTP POST 자체는 쉽게 붙일 수 있다.

### 아직 없는 것

- 운영 채널 알림 전용 모듈
- Slack webhook URL 설정 키
- 알림 중복 억제 정책
- 알림 실패 무시 정책과 관련 테스트
- Health 상태 변화 자체를 push하는 메커니즘

## 문제 정의

지금 구조는 "문제가 생긴 뒤 로그와 actuator를 보면 확인할 수 있는 상태"까지는 되어 있다. 하지만 운영자가 로그를 먼저 보지 않으면 장애를 늦게 인지할 수 있다.

특히 아래 상황은 Slack 알림 대상으로 보는 게 적절하다.

- `500` 계열의 예상치 못한 서버 예외
- 외부 API 호출 실패가 실제 사용자 흐름 실패로 이어진 경우
- 스케줄러 작업 실패
- 앱 기동 직후 핵심 외부 의존성이 미준비 상태인 경우

반대로 아래는 Slack 기본 알림 대상에서 제외하는 것이 맞다.

- `BusinessException`, `MethodArgumentNotValidException`, `AuthException`처럼 의도된 4xx 응답
- 잘못된 사용자 입력, 인증 누락, 권한 부족
- 이미 로그/헬스체크에서 충분히 확인 가능한 저심각도 정보성 이벤트

## 설계 원칙

### 1. 감지와 전송을 분리한다

예외를 감지하는 코드와 Slack으로 보내는 코드를 직접 결합하지 않는다. `GlobalExceptionHandler`, 스케줄러, 기동 점검 코드는 
`운영 알림 이벤트`를 발행만 하고, 실제 Slack 전송은 별도 컴포넌트가 맡는다.

이유는 다음과 같다.

- 장애 감지 지점이 늘어나도 전송 포맷과 정책을 한 곳에서 유지할 수 있다.
- Slack 전송 실패가 원래 예외 처리 흐름을 오염시키지 않는다.
- 추후 Slack 외 채널 추가가 쉬워진다.

### 2. 사용자 요청 성능과 분리한다

Slack webhook 호출은 비동기 처리한다. 요청 스레드 또는 스케줄러 본 흐름에서 네트워크 POST를 직접 기다리지 않는다.

### 3. 스팸보다 누락 방지가 우선이지만, 반복 폭주는 막는다

동일 원인으로 같은 알림이 짧은 시간에 수십 번 올라오는 것을 막기 위해 cooldown 기반 중복 억제를 둔다. 다만 첫 알림은 즉시 보내고, 동일 fingerprint 재발만 억제한다.

### 4. 민감정보는 보내지 않는다

Slack 메시지와 로그 모두에 아래 정보는 넣지 않는다.

- access token
- refresh token
- webhook URL
- 외부 API raw response
- 이메일/전화번호 전체값

## 권장 패키지 구조

신규 모듈은 `global` 하위 횡단 관심사로 두는 편이 현재 구조와 가장 잘 맞는다.

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

## 권장 구성 요소

### `SlackAlertProperties`

`@ConfigurationProperties(prefix = "slack.alert")` record로 관리한다.

권장 프로퍼티:

```yaml
slack:
  alert:
    enabled: false
    webhook-url: ${SLACK_ALERT_WEBHOOK_URL:}
    environment: ${APP_ENV:local}
    connect-timeout-ms: 1000
    read-timeout-ms: 3000
    default-cooldown-seconds: 300
    scheduler-cooldown-seconds: 600
    notify-unexpected-exception: true
    notify-external-api-exception: true
    notify-scheduler-failure: true
    notify-startup-health-down: true
```

설계 포인트:

- 기본값은 `enabled=false`로 둔다.
- `webhook-url`이 비어 있으면 Slack 전송 Bean 대신 no-op Bean을 등록한다.
- 로컬 개발 환경에서는 기본 비활성화로 둔다.

### `OperationalAlert`

Slack 전송 전 공통 도메인 객체다. 특정 채널 포맷에 종속되지 않게 유지한다.

권장 필드:

- `severity`
- `type`
- `source`
- `summary`
- `environment`
- `occurredAt`
- `requestId`
- `userId`
- `method`
- `path`
- `status`
- `errorCode`
- `schedulerName`
- `exceptionClass`
- `exceptionMessage`
- `details`

### `OperationalAlertPublisher`

애플리케이션 각 지점에서 이 인터페이스만 호출한다.

예시 메서드:

- `publishUnexpectedException(Exception e)`
- `publishExternalApiFailure(Exception e, String source)`
- `publishSchedulerFailure(String schedulerName, Exception e)`
- `publishStartupHealthDown(Map<String, Object> healthDetails)`

구현은 내부적으로 `ApplicationEventPublisher` 또는 직접 `SlackAlertDispatcher`를 호출할 수 있다. 현재 코드베이스 확장성을 고려하면 `ApplicationEventPublisher` 기반 이벤트 발행이 더 낫다.

### `SlackAlertDispatcher`

실제 Slack 전송 담당자다.

책임:

- `OperationalAlert`를 Slack 메시지 본문으로 변환
- 중복 억제 체크
- webhook POST 호출
- 실패 시 재전파하지 않고 에러 로그만 남김

권장 사항:

- `@Async` + 전용 `TaskExecutor` 사용
- 전송 실패 시 Slack 알림을 다시 보내지 않음
- 로그는 `warn` 또는 `error` 1회만 남김

### `AlertDeduplicator`

메모리 기반 `ConcurrentHashMap<String, Instant>`로 시작한다.

fingerprint 권장 구성:

- `type`
- `source`
- `status`
- `errorCode`
- `path` 또는 `schedulerName`
- `exceptionClass`

현재 배포 구성이 단일 API 컨테이너 기준이라면 메모리 dedupe로 충분하다. 추후 멀티 인스턴스가 되면 Redis 같은 외부 저장소 기반으로 승격한다.

### `SlackWebhookClient`

Slack webhook 전송만 담당하는 저수준 컴포넌트다.

권장 사항:

- 공용 `RestClient`를 그대로 재사용하지 말고, 짧은 timeout을 갖는 전용 client를 구성한다.
- `204` 또는 성공 응답이면 정상 처리한다.
- 실패 시 예외를 던지되 상위 dispatcher에서 삼킨다.
- webhook URL 전체 문자열은 어떤 로그에도 남기지 않는다.

## 알림 발생 지점 설계

### 1. HTTP 요청 예외

연결 지점:

- `GlobalExceptionHandler`

권장 정책:

- `handleException(Exception e)`는 무조건 Slack 알림 발행
- `handleRestClientException(RestClientException e)`는 알림 발행
- `handleAuthException`, `handleBusinessException`, `handleValidationException`은 기본 미발행

이유:

- 4xx는 운영 장애보다 사용자/클라이언트 입력 문제일 가능성이 높다.
- 5xx와 외부 API 실패만 먼저 운영 채널로 보내는 게 신호 대비 잡음 비율이 좋다.

메시지에 포함할 값:

- `requestId`
- `userId`
- `method`
- `path`
- `status`
- `errorCode`
- 예외 클래스명
- 예외 메시지 요약

### 2. 스케줄러 실패

연결 지점:

- `OpenMatStatusScheduler`
- `TournamentCrawlerScheduler`
- `AuthService.processScheduledWithdrawals()`

권장 정책:

- 각 `catch` 블록에서 `scheduledTaskTracker.recordFailure(...)` 직후 알림 발행
- 알림에는 `schedulerName`, cron, zone, 직전 summary가 포함되면 좋다
- 같은 스케줄러가 같은 예외로 반복 실패할 때는 cooldown 적용

스케줄러 알림은 요청 기반 예외보다 더 우선순위가 높다. 사용자가 직접 요청하지 않아도 백그라운드에서 조용히 실패할 수 있기 때문이다.

### 3. 앱 기동 시 health down

연결 지점:

- 신규 `ApplicationRunner` 또는 `StartupHealthAlertRunner`

권장 정책:

- 앱 기동 완료 후 `ExternalDependenciesHealthIndicator.health()`와 `SchedulerHealthIndicator.health()`를 읽어 핵심 상태를 점검
- 초기 상태가 `DOWN`이면 Slack 경고를 1회 전송

이 항목은 "장애 발생 이벤트"라기보다 "운영 준비 미완료 탐지"에 가깝다. 하지만 실제 운영에서는 Firebase 미초기화, S3 bucket 설정 누락 같은 문제를 배포 직후 바로 알아차리는 데 유용하다.

### 4. Health 상태 변화 감지

이 항목은 2차 단계로 둔다.

이유:

- 현재 actuator health는 pull 기반이며, 상태 변화 자체를 push할 이벤트 소스가 없다.
- 앱 내부에서 health 상태 변화를 push하려면 별도 polling scheduler와 이전 상태 캐시가 필요하다.
- 1차 목표인 `예상치 못한 예외`, `외부 API 실패`, `스케줄러 실패`, `기동 직후 misconfiguration`만 먼저 붙여도 운영 체감 개선이 크다.

## 메시지 포맷 권장안

Slack 메시지는 Block Kit까지 과도하게 가지 말고, webhook payload의 `text` 중심으로 먼저 시작하는 것이 관리가 쉽다.

예시:

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

스케줄러 예시:

```text
[ROLLING API][PROD][CRITICAL] Scheduler failure
- scheduler: tournamentCrawler
- status: FAILED
- exception: IllegalStateException
- message: crawl source parsing failed
- lastSummary: crawled=12,created=2,updated=1,skipped=9
```

메시지 작성 원칙:

- 첫 줄에서 시스템, 환경, 심각도, 이벤트 유형을 바로 식별 가능하게 한다.
- 본문은 5~10줄 내로 유지한다.
- 긴 stack trace 전체는 Slack에 넣지 않고 로그에서 `requestId`로 추적한다.

## 설정 반영 계획

### `application.yml`

기본 설정 추가:

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

### `docker-compose.yml`

운영 환경 변수 추가:

```yaml
      SLACK_ALERT_ENABLED: ${SLACK_ALERT_ENABLED:-true}
      SLACK_ALERT_WEBHOOK_URL: ${SLACK_ALERT_WEBHOOK_URL}
      SLACK_ALERT_ENVIRONMENT: ${SLACK_ALERT_ENVIRONMENT:-prod}
```

원칙:

- 실제 webhook URL은 `.env` 또는 배포 환경 secret로만 관리한다.
- Git에 URL을 커밋하지 않는다.
- 로그나 actuator detail에 URL이 노출되지 않게 한다.

## 구현 순서 제안

### 1단계

- `SlackAlertProperties`
- `OperationalAlert`, `AlertSeverity`, `AlertType`
- `SlackWebhookClient`
- `SlackAlertDispatcher`
- no-op fallback 구현

### 2단계

- `GlobalExceptionHandler`에 5xx 예외 연동
- 스케줄러 실패 지점 연동
- startup health down 1회 알림 추가

### 3단계

- 중복 억제 추가
- 전용 executor 및 비동기 처리 추가
- 테스트 보강

### 4단계

- 필요 시 관리자용 테스트 발송 API 추가
- 필요 시 health 상태 변화 polling 추가

## 테스트 전략

### 단위 테스트

- `SlackMessageFormatterTest`
  포맷이 필수 필드를 누락하지 않는지 검증
- `AlertDeduplicatorTest`
  cooldown 내 중복 억제가 동작하는지 검증
- `SlackAlertDispatcherTest`
  webhook 전송 실패가 예외를 재전파하지 않는지 검증

### 통합 테스트

- `GlobalExceptionHandler` 경유 500 예외 발생 시 publisher 호출 검증
- `BusinessException` 발생 시 publisher 미호출 검증
- 스케줄러 실패 시 publisher 호출 검증
- `enabled=false` 또는 `webhook-url` 누락 시 no-op 동작 검증

테스트 구현 팁:

- 전송 자체는 실 Slack 호출 대신 mock `SlackWebhookClient` 또는 mock `OperationalAlertPublisher`로 검증한다.
- webhook payload 직렬화 로직이 분리되면 해당 부분만 별도 테스트한다.

## 운영 정책 제안

### 심각도 기준

- `CRITICAL`
  스케줄러 핵심 작업 실패, 앱 기동 직후 핵심 의존성 `DOWN`
- `ERROR`
  예상치 못한 서버 예외, 외부 API 실패로 5xx 응답 발생
- `WARN`
  향후 필요 시 반복 실패 전조, health 상태 저하, 복구 알림

### 알림 억제 기준

- 동일 fingerprint는 기본 5분 억제
- 스케줄러 실패는 기본 10분 억제
- 복구 알림을 넣는다면 `DOWN -> UP` 전환 시 1회만 발송

### 알림 실패 처리

- Slack 전송 실패는 비즈니스 실패로 간주하지 않는다.
- 전송 실패 로그는 남기되 재귀적으로 또 다른 Slack 알림은 만들지 않는다.

## 오픈 이슈

- 멀티 인스턴스 확장 계획이 생기면 dedupe를 메모리에서 Redis로 옮길지 여부
- 운영자가 수동으로 Slack 테스트를 보낼 admin endpoint가 필요한지 여부
- 복구 알림까지 포함할지, 아니면 장애 알림만 먼저 운영할지 여부
- Sentry 같은 예외 집계 도구 없이 Slack만으로 충분한지 여부

## 최종 제안

현재 코드베이스에서는 다음 범위로 시작하는 것이 가장 현실적이다.

1. `GlobalExceptionHandler`의 `RestClientException` 및 `Exception`만 Slack 알림 연결
2. `OpenMatStatusScheduler`, `TournamentCrawlerScheduler`, `processScheduledWithdrawals` 실패 알림 연결
3. 앱 기동 시 `ExternalDependenciesHealthIndicator` 기준 초기 `DOWN` 상태 1회 경고
4. 메모리 기반 dedupe와 비동기 전송 적용
5. Slack 전송 실패는 로그만 남기고 본 흐름에 영향 주지 않음

이렇게 시작하면 현재 구조를 크게 흔들지 않으면서도 운영자가 가장 먼저 알아야 할 장애 신호를 Slack으로 받을 수 있다.

## 관련 문서

- `docs/SLACK_WEBHOOK_PHASE1_APP_ALERTS_20260409.md`
- `docs/SLACK_WEBHOOK_PHASE2_METRICS_ALERTING_20260409.md`
