# Prometheus / Grafana Phase 3 Review

## 리뷰 범위
- Prometheus alert rule 파일 추가
- Prometheus rule 로딩 설정 및 compose 마운트
- HTTP p95 알람을 위한 histogram 설정
- Phase 3 운영 문서 및 체크리스트 반영

## 코드 리뷰 결과
- Blocking findings 없음

## 반영된 변경
- `monitoring/prometheus/alerts/rolling-alerts.yml` 추가
- API scrape 실패, 5xx 비율 급증, P95 응답 시간 급증 알람 추가
- `tournamentCrawler`, `openMatStatusSync`, FCM 실패율 알람 추가
- `tournamentCrawler` 알람에 프로세스 재시작 직후 오탐 방지 조건 추가
- `monitoring/prometheus/prometheus.yml` 에 `rule_files` 연결
- `docker-compose.monitoring.yml` 에 alert rule 디렉터리 마운트 추가
- `application.yml` 에 `http.server.requests` histogram 활성화 추가
- 운영 파일을 검증하는 `PrometheusMonitoringConfigTest` 추가
- 계획 문서의 Phase 3 체크리스트 반영

## Alertmanager 결정
- 1차 결정: Prometheus alert rule 평가는 이번 Phase 3 범위에 포함하고, Alertmanager 수신 채널 연결은 보류한다.
- 근거: 현재 저장소에는 Slack, Discord, Email, PagerDuty 같은 운영 수신 채널 정보가 없다.
- 영향: Prometheus UI 에서 alert 상태는 평가되지만, 외부 알림 전송은 별도 운영 설정 전까지 동작하지 않는다.

## 직접 검증한 내용
- `PrometheusMonitoringConfigTest` 통과
- `SchedulerHealthIndicatorTest` 통과
- `FcmPushNotificationServiceTest` 통과
- 임시 환경변수 주입 후 `docker compose -f docker-compose.yml -f docker-compose.monitoring.yml config` 조합 검증 완료

## 런타임에서 추가 확인이 필요한 내용
- 운영 트래픽에서 `http_server_requests_seconds_bucket` 가 실제로 수집되는지 확인
- Prometheus Alerts 화면에서 신규 규칙 6개가 `loaded` 상태인지 확인
- 실제 트래픽 기준으로 5xx 비율, p95, FCM 실패율 임계값이 과도하지 않은지 튜닝
- Alertmanager 또는 다른 수신 채널을 연결할 운영 채널을 확정

## 변경 파일
- `docker-compose.monitoring.yml`
- `monitoring/prometheus/prometheus.yml`
- `monitoring/prometheus/alerts/rolling-alerts.yml`
- `src/main/resources/application.yml`
- `src/test/java/com/rolling/api/global/monitoring/PrometheusMonitoringConfigTest.java`
- `docs/PROMETHEUS_GRAFANA_PLAN.md`

## 결론
- Phase 3의 핵심인 Prometheus alert rule 정의와 로딩 설정은 반영되었다.
- 남은 작업은 Alertmanager 수신 채널 연결과 운영 트래픽 기준 임계값 튜닝이다.
