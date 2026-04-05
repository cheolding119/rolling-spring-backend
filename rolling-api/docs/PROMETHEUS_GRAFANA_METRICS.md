# Prometheus / Grafana Metrics Policy

## 목적
- Phase 2에서 추가한 커스텀 메트릭의 이름과 태그를 한 곳에서 관리한다.
- Grafana 대시보드와 Phase 3 알람 규칙이 같은 기준을 사용하게 만든다.

## 공통 규칙
- 모든 커스텀 메트릭은 `rolling_` 접두사를 사용한다.
- Counter는 `_total` 로 끝낸다.
- Timer는 Prometheus 노출 기준 `_seconds` 계열로 사용한다.
- 시각 값 gauge는 `*_unixtime` 형식을 사용한다.
- 태그는 low-cardinality 값만 사용한다.
- `userId`, `openMatId`, `applyLink`, `fcmToken` 같은 식별자는 태그로 넣지 않는다.

## 스케줄러 메트릭
### `rolling_scheduler_execution_total`
- 타입: Counter
- 태그
- `task`: `openMatStatusSync`, `tournamentCrawler`, `withdrawalProcessor`
- `result`: `success`, `failure`

### `rolling_scheduler_duration_seconds`
- 타입: Timer
- 태그
- `task`

### `rolling_scheduler_running`
- 타입: Gauge
- 태그
- `task`
- 값
- `1`: 실행 중
- `0`: 비실행 중

### `rolling_scheduler_last_success_unixtime`
- 타입: Gauge
- 태그
- `task`
- 값
- 마지막 성공 시각의 unix epoch seconds

## 대회 크롤링 메트릭
### `rolling_tournament_crawl_items_total`
- 타입: Counter
- 태그
- `source`: `street_jiu_jitsu`, `korea_jiu`, `heroes_of_jiu_jitsu`, `unknown`
- `result`: `created`, `updated`, `skipped`, `failed`

### `rolling_tournament_crawl_duration_seconds`
- 타입: Timer
- 태그
- `source`

### `rolling_tournament_crawl_deleted_total`
- 타입: Counter
- 설명
- 접수 마감일이 지나 삭제된 대회 수

## FCM 메트릭
### `rolling_fcm_send_total`
- 타입: Counter
- 태그
- `result`: `success`, `failure`
- `error_code`: `none`, `unknown`, Firebase Messaging error code 소문자 값

### `rolling_fcm_batch_size`
- 타입: Distribution Summary
- 설명
- 멀티캐스트 배치 크기 분포

### `rolling_fcm_invalid_token_cleanup_total`
- 타입: Counter
- 설명
- 정리된 invalid token 수

## OpenMat 메트릭
### `rolling_openmat_apply_total`
- 타입: Counter
- 태그
- `result`: `success`, `not_found`, `host_cannot_apply`, `open_mat_reported`, `open_mat_closed`, `open_mat_finished`, `already_applied`, `capacity_full`

### `rolling_openmat_cancel_total`
- 타입: Counter
- 태그
- `result`: `success`, `not_found`, `not_applied`

### `rolling_openmat_report_total`
- 타입: Counter
- 태그
- `result`: `success`, `not_found`

### `rolling_openmat_sync_total`
- 타입: Counter
- 태그
- `result`: `success`

## Grafana 연결
- [`rolling-overview.json`](/C:/rolling/rolling-spring-backend/rolling-api/monitoring/grafana/dashboards/rolling-overview.json): 기본 JVM / HTTP / DB
- [`rolling-scheduler.json`](/C:/rolling/rolling-spring-backend/rolling-api/monitoring/grafana/dashboards/rolling-scheduler.json): 스케줄러 상태와 실행 추이
- [`rolling-business.json`](/C:/rolling/rolling-spring-backend/rolling-api/monitoring/grafana/dashboards/rolling-business.json): 대회 크롤링, FCM, OpenMat 비즈니스 지표
