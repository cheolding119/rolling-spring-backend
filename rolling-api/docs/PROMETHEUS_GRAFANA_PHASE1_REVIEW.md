# Prometheus / Grafana Phase 1 Review

- 리뷰 기준: `.codex/agents/code-reviewer.toml`

## 리뷰 범위
- Spring Boot management port 분리
- actuator 보안 체인 분리
- Prometheus / Grafana compose 및 provisioning 파일
- Phase 1 체크리스트 반영 상태

## 코드 리뷰 결과
- Blocking findings 없음

## 확인한 변경 사항
- `management.server.port=9090` 추가
- `/actuator/**` 전용 보안 체인 추가
- `/actuator/prometheus` 와 `/actuator/health` 는 무인증 접근 허용
- 나머지 actuator 경로는 기존처럼 관리자 권한 필요
- Prometheus 전용 compose 파일 추가
- Grafana provisioning 파일 추가
- 기본 overview 대시보드 추가

## 직접 검증한 내용
- `SecurityAuthorizationIntegrationTest` 에서 아래 경로를 검증함
- `/actuator/prometheus` 는 access token 없이 접근 가능
- `/actuator/info` 는 무인증 시 401
- `/actuator/info` 는 일반 사용자 token 시 403
- `/actuator/info` 는 관리자 token 시 200
- Grafana 대시보드 JSON 파싱 확인
- 임시 환경변수(`GRAFANA_ADMIN_PASSWORD`, `DOCKER_USERNAME`) 
- 주입 후 `docker compose -f docker-compose.yml -f docker-compose.monitoring.yml config` 
- 조합 검증 완료

## 런타임에서 추가 확인이 필요한 내용
- Prometheus 컨테이너가 실제로 `api:9090/actuator/prometheus` 를 scrape 하는지 확인
- Grafana가 provisioning 된 datasource 와 dashboard 를 정상 로드하는지 확인
- `9090` 포트가 외부에 publish 되지 않는지 배포 환경에서 재확인
- 실제 애플리케이션 구동 후 JVM / HTTP / DB 메트릭이 Grafana에 표시되는지 확인

## 남은 운영 체크
- 배포 환경에 `GRAFANA_ADMIN_PASSWORD` 를 반드시 설정해야 함
- 배포 환경에 기존 이미지 변수(`DOCKER_USERNAME`, `IMAGE_TAG`)가 유지되어야 함
- monitoring compose 는 기본 compose 와 함께 실행해야 함

## 변경 파일
- `src/main/resources/application.yml`
- `src/main/java/com/rolling/api/global/config/SecurityConfig.java`
- `src/test/java/com/rolling/api/SecurityAuthorizationIntegrationTest.java`
- `docker-compose.monitoring.yml`
- `monitoring/prometheus/prometheus.yml`
- `monitoring/grafana/provisioning/datasources/datasource.yml`
- `monitoring/grafana/provisioning/dashboards/dashboard.yml`
- `monitoring/grafana/dashboards/rolling-overview.json`
- `docs/PROMETHEUS_GRAFANA_PLAN.md`

## 결론
- Phase 1 범위는 코드와 운영 파일 기준으로 반영되었고, 정적 검증과 보안 테스트까지 통과했다.
- 남은 것은 실제 컨테이너 기동 후 scrape 및 dashboard 표시 여부를 확인하는 런타임 검증이다.
