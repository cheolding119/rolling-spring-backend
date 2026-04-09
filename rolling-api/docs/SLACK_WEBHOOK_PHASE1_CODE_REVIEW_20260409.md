  # Slack Webhook Phase 1 코드 리뷰 (기준일: 2026-04-09)

## 리뷰 범위

- `global.alert` 신규 모듈
- `GlobalExceptionHandler`의 5xx / 외부 API 실패 알림 연결
- `OpenMatStatusScheduler`, `TournamentCrawlerScheduler`, `AuthService.processScheduledWithdrawals()` 알림 연결
- startup health down 1회 알림 경로
- 설정값, 비동기 executor, 테스트 보강

## Findings

### 1. 차단(blocking) 수준의 핵심 발견 사항 없음

정적 코드 리뷰와 대상 테스트 기준으로 이번 범위에서 즉시 수정이 필요한 차단급 결함은 발견하지 못했다.

근거:

- 예외 감지와 Slack 전송이 `OperationalAlertPublisher` + 이벤트 리스너로 분리돼 본래 요청/스케줄러 흐름 오염을 줄인다.
- Slack 전송 실패는 `SlackAlertDispatcher`에서 삼키고 로그만 남겨 원 흐름에 재전파되지 않는다.
- 스케줄러 실패와 startup health down 경로는 별도 테스트로 검증됐다.
- `enabled=false` 및 blank webhook URL 시 no-op client로 빠지도록 분기했다.

## 직접 검증한 내용

- `GlobalExceptionHandler`의 `Exception` 경로가 alert publisher를 호출하는지 확인
- `GlobalExceptionHandler`의 `RestClientException` 경로가 alert publisher를 호출하는지 확인
- `BusinessException`은 기본적으로 Slack alert를 발행하지 않는지 확인
- 오픈매트 상태 동기화, 대회 크롤링, 회원 탈퇴 스케줄러 실패 시 alert publisher 호출 여부 확인
- startup health 중 하나라도 `DOWN`이면 startup alert가 발행되는지 확인
- formatter, deduplicator, dispatcher, no-op config 분기 단위 테스트 확인

실행한 검증 명령:

```powershell
$env:GRADLE_USER_HOME='C:\rolling\rolling-spring-backend\rolling-api\.gradle-home'; .\gradlew.bat test --tests "com.rolling.api.global.alert.*" --tests "com.rolling.api.domain.openmat.scheduler.OpenMatStatusSchedulerTest" --tests "com.rolling.api.domain.tournament.scheduler.TournamentCrawlerSchedulerTest" --tests "com.rolling.api.domain.auth.service.AuthServiceWithdrawTest" --tests "com.rolling.api.domain.auth.service.AuthServiceLifecycleTest" --tests "com.rolling.api.domain.auth.service.AuthServiceLoginValidationTest"
```

결과:

- 대상 테스트 통과

## 잔존 리스크

### 1. 실제 Slack webhook 연동은 런타임 환경 검증이 아직 필요함

이번 테스트는 mock 기반 검증이라 실제 Slack 응답 코드, 네트워크 방화벽, 운영 secret 주입 상태까지 증명하지는 않는다.

권장 후속 조치:

- 운영 또는 스테이징에서 테스트용 webhook으로 1회 발송 검증
- timeout, DNS, outbound egress 정책 확인

### 2. dedupe는 단일 인스턴스 메모리 기준이라 멀티 인스턴스에서는 중복 알림 가능성이 남아 있음

현재 `AlertDeduplicator`는 메모리 기반이라 인스턴스가 여러 개면 같은 장애가 각 인스턴스에서 따로 발송될 수 있다.

권장 후속 조치:

- 멀티 인스턴스 운영 시 Redis 또는 외부 저장소 기반 dedupe 검토

### 3. startup health down 알림은 재기동마다 다시 발행될 수 있음

앱 기동 직후 misconfiguration을 빠르게 잡는 목적에는 맞지만, 의도적으로 비활성화된 의존성이 있거나 재배포가 잦으면 같은 경고가 반복될 수 있다.

권장 후속 조치:

- 운영 환경별 필수 의존성 기준 재점검
- 필요 시 startup alert enable 토글 또는 제외 규칙 추가

## 최종 판단

현재 구현은 Phase 1 범위에 대해 작고 일관된 방식으로 잘 정리됐다. 구조 분리, 실패 격리, 테스트 보강 모두 적절하며, 남은 리스크는 코드 결함보다는 실제 운영 환경 검증과 멀티 인스턴스 확장성 쪽에 가깝다.
