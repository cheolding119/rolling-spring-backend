# Prometheus / Grafana Phase 2 Review

## 리뷰 범위
- 스케줄러 커스텀 메트릭
- 대회 크롤링 커스텀 메트릭
- FCM 커스텀 메트릭
- OpenMat 커스텀 메트릭
- Phase 2 Grafana 대시보드

## 메트릭 이름 / 태그 정책
- 스케줄러: `rolling_scheduler_*`, 태그는 `task`, `result`
- 대회 크롤링: `rolling_tournament_crawl_*`, 태그는 `source`, `result`
- FCM: `rolling_fcm_*`, 태그는 `result`, `error_code`
- OpenMat: `rolling_openmat_*`, 태그는 `result`
- 원칙: 서비스별 prefix를 고정하고, 태그는 low-cardinality 값만 사용한다.

## 코드 리뷰 결과
- Blocking findings 없음

## 반영된 변경
- `ScheduledTaskTracker` 에 execution / duration / last success / running 메트릭 추가
- `TournamentManagerService` 에 source/result 기반 크롤링 메트릭 추가
- `FcmPushNotificationService` 에 전송 결과 / 배치 크기 / invalid token cleanup 메트릭 추가
- `OpenMatService` 에 apply / cancel / report / sync 메트릭 추가
- Grafana dashboard `rolling-scheduler`, `rolling-business` 추가
- 계획 문서의 Phase 2 체크리스트 반영

## 직접 검증한 내용
- 전체 테스트 실행 시 Phase 2 변경과 직접 관련된 신규 테스트는 통과
- `SecurityAuthorizationIntegrationTest` 회귀 통과
- 전체 테스트 실패는 기존 `RollingApiApplicationTests.contextLoads()` 의 DB 연결 실패 1건만 남음
- 전체 테스트 결과 기준, Phase 2 변경으로 인한 추가 실패는 확인되지 않음

## 런타임에서 추가 확인이 필요한 내용
- Prometheus가 신규 커스텀 메트릭을 실제로 scrape 하는지 확인
- Grafana에서 `rolling-scheduler`, `rolling-business` 대시보드가 provisioning 되는지 확인
- 메트릭 값이 운영 트래픽 기준으로 과도하게 증가하지 않는지 확인

## 변경 파일
- `src/main/java/com/rolling/api/global/monitoring/ScheduledTaskTracker.java`
- `src/main/java/com/rolling/api/domain/tournament/service/TournamentManagerService.java`
- `src/main/java/com/rolling/api/domain/notification/service/FcmPushNotificationService.java`
- `src/main/java/com/rolling/api/domain/notification/config/PushNotificationConfig.java`
- `src/main/java/com/rolling/api/domain/openmat/service/OpenMatService.java`
- `src/test/java/com/rolling/api/global/monitoring/SchedulerHealthIndicatorTest.java`
- `src/test/java/com/rolling/api/domain/tournament/service/TournamentManagerServiceTest.java`
- `src/test/java/com/rolling/api/domain/notification/service/FcmPushNotificationServiceTest.java`
- `src/test/java/com/rolling/api/domain/openmat/service/OpenMatServiceTest.java`
- `monitoring/grafana/dashboards/rolling-scheduler.json`
- `monitoring/grafana/dashboards/rolling-business.json`
- `docs/PROMETHEUS_GRAFANA_PLAN.md`

## 결론
- Phase 2 범위의 커스텀 메트릭과 대시보드 추가는 반영되었다.
- 남은 확인은 운영 환경에서 실제 scrape / dashboard provisioning / 값 해석 적정성 검증이다.
