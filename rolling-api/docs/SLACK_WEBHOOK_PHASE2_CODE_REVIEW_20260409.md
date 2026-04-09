# Slack Webhook Phase 2 Code Review (기준일: 2026-04-09)

## 리뷰 범위

- `Prometheus -> Alertmanager -> Slack` 메트릭 알림 경로
- `docker-compose.monitoring.yml` 의 Alertmanager 서비스와 환경변수 주입 경계
- `monitoring/prometheus/prometheus.yml` 의 alertmanager 연동 설정
- `monitoring/prometheus/alerts/rolling-alerts.yml` 의 운영 메타데이터와 알림 정책
- `PrometheusMonitoringConfigTest` 기반 정적 검증

## 코드 리뷰 결과

- Blocking findings 없음

## 확인된 설계 결정

- Slack 메트릭 알림 발송 주체는 `Prometheus Alertmanager`로 단일화했다.
- 앱 예외 알림과 메트릭 알림은 전송 경로를 분리하되, 초기 운영 채널은 동일 webhook을 재사용할 수 있게 했다.
- alert rule은 Prometheus에 두고, Alertmanager는 grouping, repeat interval, resolved 알림만 담당하게 유지했다.

## 직접 검증한 내용

- Prometheus 설정 파일에 alertmanager target이 연결되어 있는지 확인
- monitoring compose에 alertmanager 서비스와 필수 webhook 환경변수가 있는지 확인
- alert rule 파일에 threshold, observation window, dashboard, runbook annotation이 추가되었는지 확인
- Alertmanager 템플릿에 `send_resolved`, severity 기반 route, Grafana 링크 템플릿이 포함되었는지 확인

## 런타임에서 추가 확인이 필요한 내용

- 배포 환경에서 `SLACK_METRICS_ALERT_WEBHOOK_URL` 이 실제로 주입되는지 확인
- `docker compose -f docker-compose.yml -f docker-compose.monitoring.yml up` 기준으로 alertmanager 컨테이너가 정상 기동하는지 확인
- 테스트용 threshold 조정 또는 Prometheus UI 수동 fire로 실제 Slack 메시지 수신과 복구 알림을 확인
- `GRAFANA_ROOT_URL` 이 외부에서 접근 가능한 실제 운영 URL과 일치하는지 확인

## 잔존 위험

- Alertmanager 설정은 컨테이너 시작 시 `sed` 치환으로 렌더링하므로, 이미지 내부 shell/sed 가용성에 의존한다.
- 메트릭 알림 임계값은 운영 트래픽 기준 튜닝이 아직 필요하다.
- 앱 예외 알림과 메트릭 알림이 같은 채널을 공유하면 초기에는 가시성이 좋지만, 경고량이 늘면 채널 분리를 다시 검토해야 한다.

## 권장 후속 조치

- 운영 환경변수에 `SLACK_METRICS_ALERT_WEBHOOK_URL`, `SLACK_METRICS_ALERT_ENVIRONMENT`, `GRAFANA_ROOT_URL` 반영
- 배포 직후 Prometheus Alerts 화면과 Slack 수신 결과를 함께 확인
- 첫 1주일 운영 후 warning 알림 repeat interval과 threshold를 재조정
