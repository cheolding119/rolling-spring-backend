# Slack Webhook Phase 2: Prometheus/Grafana 메트릭 알림 (기준일: 2026-04-09)

## 목표

이 phase의 목표는 이미 노출된 `/actuator/prometheus` 기반 메트릭을 사용해 운영 지표 이상 징후를 Prometheus/Grafana 쪽에서 감지하고, 그 결과를 Slack으로 보내는 것이다.

이 단계는 앱 내부 예외 기반 Slack 알림을 대체하는 것이 아니라 보완하는 단계다.

- Phase 1은 앱 내부 장애 이벤트 push
- Phase 2는 메트릭 기반 이상 징후 탐지와 추세 관측

## 왜 분리하는가

- 앱 예외 알림과 메트릭 알림은 신호의 성격이 다르다.
- 처음부터 둘을 한 번에 구현하면 범위가 커지고 잡음이 늘어난다.
- 메트릭 알림은 threshold, 재알림, dashboard 운영 기준까지 함께 정리돼야 실효성이 있다.

## 전제 조건

- `spring-boot-starter-actuator`와 Prometheus registry가 이미 추가돼 있다.
- `/actuator/prometheus`가 노출돼 있다.
- Grafana/Prometheus 대시보드와 alert rule 문서는 아직 별도 정리가 더 필요하다.

## 권장 운영 모델

알림 주체는 다음 둘 중 하나로 결정한다.

1. Grafana Alerting
2. Prometheus Alertmanager

이번 phase에서는 `Prometheus Alertmanager`를 Slack 발송 주체로 확정한다.

결정 이유:

- Prometheus alert rule이 이미 코드로 버전 관리되고 있다.
- Grafana는 대시보드 소비자 역할에 집중하고, 알림 정책은 Prometheus 쪽에서 일원화하는 편이 중복이 적다.
- 앱 내부 예외 알림과 별개로 메트릭 기반 경고만 한 경로에서 관리하기 쉽다.

채널 분리 정책:

- 초기 운영은 앱 예외 알림과 동일한 Slack workspace 내 같은 운영 채널을 사용해도 된다.
- 다만 설정 키는 `SLACK_METRICS_ALERT_WEBHOOK_URL`로 별도 분리해 두고, 이후 채널 분리가 필요해지면 환경변수만 교체한다.

## 우선 알림 후보

- 인스턴스 down 또는 scrape 실패
- 5xx 비율 급증
- 응답 시간 p95 또는 p99 급증
- JVM heap 사용률 과다
- 컨테이너 CPU 사용률 과다
- 스케줄러 관련 커스텀 메트릭 이상

다음 항목은 나중에 검토한다.

- 너무 세분화된 endpoint별 경고
- 짧은 순간 스파이크에 과민한 경고
- 대시보드 없이 설명하기 어려운 복합 조건 경고

## Slack 메시지 원칙

- 메트릭명, 현재값, 임계치, 관찰 기간을 같이 보낸다.
- dashboard 링크와 1차 조치 runbook 문구를 함께 포함한다.
- 앱 예외 알림과 구별되도록 제목 prefix를 분리한다.

예시:

```text
[ROLLING API][PROD][WARN] High 5xx rate detected
- metric: http_server_requests_5xx_ratio
- currentValue: 6.2%
- threshold: 3.0%
- window: 5m
- action: check Grafana dashboard and request logs
```

## Phase 2 체크리스트

### Phase 2-1. 메트릭 기준 확정

- [x] Slack으로 보낼 메트릭 후보 목록 확정
- [x] 각 메트릭별 threshold 초안 작성
- [x] severity 기준 정의
- [x] false positive 가능성 높은 항목 제거

### Phase 2-2. 알림 주체 결정

- [x] Grafana Alerting 사용 여부 결정
- [x] Prometheus Alertmanager 사용 여부 결정
- [x] 실제 Slack 발송 주체를 하나로 확정
- [x] Slack 채널 분리 여부 결정

### Phase 2-3. 알림 정책

- [x] alert rule window 확정
- [x] cooldown 및 재알림 주기 확정
- [x] 복구 알림 발송 여부 결정
- [x] 앱 예외 알림과 중복되는 시나리오 정리

### Phase 2-4. 운영 문서화

- [x] dashboard 링크 목록 정리
- [x] runbook 연결
- [x] 메트릭별 1차 확인 절차 문서화
- [x] 운영자 대응 우선순위 문서화

### Phase 2-5. 검증

- [ ] 테스트용 threshold로 알림 발생 확인
- [x] Slack 메시지 포맷 가독성 검증
- [x] 반복 알림 폭주 여부 검증
- [ ] 복구 알림 동작 검증

### Phase 2-6. Slack 메시지 가독성 개선

- [x] 영어 위주의 메시지 제목을 한국어 중심 표현으로 정리
- [x] `CRITICAL`, `ERROR`, `WARN` 표기를 `치명`, `오류`, `경고`로 병기
- [x] `UP`, `DOWN` 상태값을 `정상`, `비정상`으로 변환
- [x] 원본 detail 전체 출력 대신 이상 항목만 요약해 표시
- [x] 정상 항목은 숨기고 실제 문제 원인만 상단에 노출
- [x] `무슨 문제인가`, `확인된 원인`, `영향`, `지금 확인할 것` 순서로 메시지 구조 재정리
- [x] startup health down 전용 한국어 포맷 추가
- [x] Prometheus/Alertmanager 메트릭 메시지에도 동일한 한국어 가독성 규칙 적용
- [x] dashboard 링크는 유지하되 운영자가 바로 이해할 수 있는 설명 문구로 치환
- [ ] 실제 Slack 모바일 화면 기준으로 10줄 안쪽 가독성 점검

## Phase 2-6 반영 내용

- 앱 내부 Slack 메시지는 alert type별 한국어 전용 포맷으로 재구성했다.
- startup health down 메시지는 정상 항목을 숨기고 비정상 항목과 조치만 요약하도록 변경했다.
- `CRITICAL`, `ERROR`, `WARN`, `UP`, `DOWN`, `FAILED` 등 주요 상태 표현은 한국어 병기 기준으로 통일했다.
- Alertmanager Slack 템플릿도 `무슨 문제인가`, `확인된 값`, `기준`, `지금 확인할 것`, `대시보드` 구조로 정리했다.
- Prometheus alert rule annotation의 summary, description, runbook도 한국어 운영 문구로 변경했다.

## 이번 반영 범위

- `docker-compose.monitoring.yml`에 `alertmanager` 서비스를 추가하고 Slack webhook 환경변수를 분리했다.
- `monitoring/prometheus/prometheus.yml`에 `alertmanager:9093` 수신 대상을 연결했다.
- `monitoring/prometheus/alerts/rolling-alerts.yml`에 alert별 `threshold`, `window`, `dashboard`, `runbook` annotation을 추가했다.
- `monitoring/alertmanager/alertmanager.yml.tmpl`에서 severity별 재알림 주기와 `send_resolved` 정책을 정의했다.
- 검증은 설정 파일을 읽는 테스트로 수행하고, 실제 Slack 수신 확인은 배포 환경에서 남겨 둔다.

## 운영 정책 확정안

- Slack 발송 주체: `Prometheus Alertmanager`
- Slack webhook 환경변수: `SLACK_METRICS_ALERT_WEBHOOK_URL`
- 환경명 환경변수: `SLACK_METRICS_ALERT_ENVIRONMENT`
- 기본 재알림 주기: `warning=4h`, `critical=30m`
- 복구 알림: `send_resolved=true`
- 대시보드 링크 기준 URL: `GRAFANA_ROOT_URL`

## 메트릭별 1차 확인 절차

- `RollingApiScrapeFailed`: 컨테이너 상태, Docker 네트워크, `/actuator/prometheus` 접근 가능 여부 확인
- `RollingApiHigh5xxErrorRate`: Grafana overview 대시보드와 요청 로그에서 5xx URI 분포 확인
- `RollingApiHighP95Latency`: overview 대시보드에서 JVM, DB pool, 느린 엔드포인트 확인
- `RollingTournamentCrawlerDidNotSucceed`: scheduler 대시보드와 crawler 로그에서 최근 성공 시각과 source 장애 확인
- `RollingOpenMatStatusSyncFailure`: scheduler 로그와 sync 실패 메트릭이 일시적 실패인지 반복 실패인지 확인
- `RollingFcmHighFailureRate`: business 대시보드에서 FCM error code, invalid token cleanup, Firebase 설정 상태 확인

## 다음 개선 예정

현재 메시지는 운영 정보는 충분하지만, 처음 보는 운영자가 읽기에는 원본 detail이 많고 영어 상태값 비중이 높다.

다음 단계에서는 아래 방향으로 개선한다.

- 문제 원인을 맨 위 1줄로 요약
- 정상 항목은 숨기고 비정상 항목만 노출
- 영어 상태값과 severity를 한국어로 병기
- startup health down, 앱 예외, 메트릭 알림 각각에 맞는 전용 포맷 적용
- Slack 모바일 푸시 화면에서 바로 이해되는 길이와 문장으로 축약

## 완료 기준

- 운영자가 앱 예외뿐 아니라 지표 이상 징후도 Slack에서 먼저 인지할 수 있다.
- 각 알림은 dashboard 또는 runbook으로 바로 이어질 수 있다.
- 메트릭 알림이 운영 채널을 과도한 잡음으로 오염시키지 않는다.
