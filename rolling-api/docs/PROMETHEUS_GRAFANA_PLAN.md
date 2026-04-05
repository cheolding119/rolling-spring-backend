# Prometheus / Grafana 도입 기획

## 목표
- API 서버 상태를 숫자로 확인할 수 있게 만든다.
- 장애를 "로그를 뒤져서" 찾기 전에 대시보드와 알람으로 먼저 감지한다.
- 이 프로젝트의 핵심 운영 포인트인 오픈매트 스케줄러, 대회 크롤러, FCM 발송 상태를 바로 볼 수 있게 만든다.

## 현재 상태
- `Spring Boot Actuator`와 `Micrometer Prometheus Registry` 의존성은 이미 추가되어 있다.
- `/actuator/prometheus` 엔드포인트도 설정상 노출 대상이다.
- 하지만 보안 설정상 `/actuator/**` 는 `ADMIN` 권한이 필요하다.
- 현재 Nginx는 `api.rolling-app.com -> api:8080` 전체 경로를 프록시한다.
- 즉, 지금 상태에서 단순히 `/actuator/prometheus` 를 `permitAll` 로 열면 외부 공개 위험이 있다.
- 이미 커스텀 `HealthIndicator` 가 있어서 외부 의존성 상태와 스케줄러 상태를 건강 체크로 볼 수 있다.
- 반면 Prometheus용 "커스텀 메트릭" 은 아직 거의 없다.

## 추천 구조
```text
Internet
  -> Nginx
    -> API App :8080

Internal Docker Network
  -> Prometheus
    -> API Management :9090/actuator/prometheus
  -> Grafana
    -> Prometheus
```

## 왜 이 구조가 맞는가
### 1. 앱 포트와 관리 포트를 분리
- 추천: 사용자 요청은 `8080`, 모니터링은 `9090` 으로 분리한다.
- 이유: 지금은 Nginx가 `8080` 전체를 프록시하므로, 메트릭까지 같은 포트에 두면 공개 경로 통제가 복잡해진다.
- 이유: 관리 포트를 분리하면 Nginx가 건드리지 않아도 되고, Prometheus만 내부 네트워크에서 접근하면 된다.

### 2. Prometheus는 내부 네트워크에서만 scrape
- 추천: Prometheus는 `api:9090/actuator/prometheus` 를 직접 수집한다.
- 이유: 이 프로젝트는 JWT 기반 인증 구조라 Prometheus가 로그인해서 메트릭을 가져가는 방식이 맞지 않는다.
- 이유: 내부 Docker 네트워크 scrape 방식이 가장 단순하고 운영 실수가 적다.
- 참고: 여기서 `api` 는 Docker Compose 서비스명 기준 내부 DNS 이름이다.
- 참고: Prometheus가 같은 Docker 네트워크 안에서 뜨는 경우 `api.rolling-app.com:9090` 이 아니라 `api:9090` 을 써야 한다.
- 참고: 반대로 Prometheus를 Docker 바깥 별도 서버에 두고, 9090 포트를 외부에 열어둘 때만 `api.rolling-app.com:9090` 같은 주소를 검토한다.

### 3. Grafana는 1차로 외부 공개하지 않음
- 추천: 첫 단계에서는 Grafana를 내부 포트 또는 SSH 터널/VPN 뒤에 둔다.
- 이유: 현재 프로젝트에는 Grafana용 SSO나 별도 인증 체계가 없다.
- 이유: 대시보드보다 먼저 필요한 것은 "안전하게 보는 것" 이다.

### 4. Health 와 Metrics를 분리해서 사용
- 추천: `health` 는 생존/의존성 체크, `metrics` 는 추세/알람용으로 나눈다.
- 이유: 현재 `ExternalDependenciesHealthIndicator`, `SchedulerHealthIndicator` 는 health 용도로 이미 잘 맞아 있다.
- 이유: Prometheus는 시계열 데이터 수집이 목적이므로, 성공/실패 횟수와 지연 시간 같은 메트릭을 추가로 넣어야 가치가 커진다.

## 도입 범위
### 1차 범위
- Prometheus 추가
- Grafana 추가
- API의 Prometheus scrape 경로 분리
- JVM / HTTP / DB / 스케줄러 기본 대시보드 구성

### 2차 범위
- 대회 크롤러, 오픈매트 동기화, FCM 발송에 대한 커스텀 메트릭 추가
- 운영 알람 추가

### 3차 범위
- Alertmanager 연동
- 필요 시 Node Exporter, cAdvisor 추가

## 구현안
### A. Spring Boot 설정
- `management.server.port=9090` 추가
- `management.endpoints.web.exposure.include=health,info,metrics,prometheus` 유지
- management 포트 전용 `SecurityFilterChain` 추가
- `/actuator/prometheus` 는 내부 scrape 전용으로 사용
- `/actuator/health` 는 기존처럼 기본 상태 확인에 사용

### 왜 이렇게 하는가
- 현재 구조에서 가장 큰 위험은 "메트릭 공개 범위" 다.
- 별도 관리 포트를 두면 Nginx 라우팅을 크게 건드리지 않아도 된다.
- 운영자가 봐야 할 엔드포인트와 일반 사용자가 쓰는 API 경로를 물리적으로 분리할 수 있다.
- 다만 포트만 분리해도 현재 `SecurityConfig` 는 여전히 `/actuator/**` 에 `ADMIN` 을 요구하므로, Prometheus scrape가 가능하도록 management 전용 보안 규칙이 추가로 필요하다.

### B. Docker Compose 구성
- 기존 `docker-compose.yml` 에 `prometheus`, `grafana` 서비스를 추가하거나, `docker-compose.monitoring.yml` 로 분리한다.
- 추천은 `docker-compose.monitoring.yml` 분리다.

### 왜 분리하는가
- 현재 Compose는 API/Nginx 중심의 운영 파일이다.
- 모니터링 스택은 라이프사이클과 데이터 볼륨이 다르다.
- 분리하면 장애 대응이나 재배포 시 API와 모니터링을 독립적으로 다루기 쉽다.

### 추천 디렉터리 구조
```text
monitoring/
  prometheus/
    prometheus.yml
    alerts/
      rolling-alerts.yml
  grafana/
    provisioning/
      datasources/
        datasource.yml
      dashboards/
        dashboard.yml
    dashboards/
      rolling-overview.json
      rolling-scheduler.json
      rolling-business.json
```

### 왜 파일 프로비저닝을 쓰는가
- 서버 재시작 후에도 설정이 다시 살아난다.
- 수동 클릭 기반 설정보다 재현성이 높다.
- 팀원이 바뀌어도 "대시보드가 코드처럼" 관리된다.

### 메트릭 정책 문서
- 커스텀 메트릭 이름과 태그 정책은 [`PROMETHEUS_GRAFANA_METRICS.md`](C:/rolling/rolling-spring-backend/rolling-api/docs/PROMETHEUS_GRAFANA_METRICS.md) 에 정리한다.

## 이 프로젝트에 꼭 맞는 메트릭
### 1. 기본 메트릭
- HTTP 요청 수, 응답 시간, 4xx/5xx 비율
- JVM 메모리, GC, CPU
- HikariCP 커넥션 풀
- 프로세스 업타임

### 이유
- Spring Boot + Micrometer가 거의 자동으로 제공한다.
- 가장 먼저 확인해야 하는 "느림, 에러, 자원 부족" 을 바로 볼 수 있다.

### 2. 스케줄러 메트릭
- `rolling_scheduler_execution_total{task,result}`
- `rolling_scheduler_duration_seconds{task}`
- `rolling_scheduler_last_success_unixtime{task}`
- 대상 task
  - `openMatStatusSync`
  - `tournamentCrawler`
  - `withdrawalProcessor`

### 이유
- 이 프로젝트는 스케줄러 정상 동작 여부가 서비스 신뢰성과 직접 연결된다.
- 현재는 `ScheduledTaskTracker` 로 상태는 남기고 있으므로, 이를 MeterRegistry와 연결하면 구현 난도가 높지 않다.
- 특히 `마지막 성공 시각` 은 알람 조건으로 쓰기 좋다.

### 3. 대회 크롤러 메트릭
- `rolling_tournament_crawl_items_total{source,result}`
- `rolling_tournament_crawl_duration_seconds{source}`
- `rolling_tournament_crawl_deleted_total`
- `result` 예시
  - `created`
  - `updated`
  - `skipped`
  - `failed`

### 이유
- 이 프로젝트는 대회 데이터가 외부 사이트 크롤링에 의존한다.
- 크롤러가 "죽었는지", "성공했지만 저장이 거의 안 되는지", "특정 소스만 실패하는지"를 분리해서 봐야 한다.

### 4. FCM 메트릭
- `rolling_fcm_send_total{result,error_code}`
- `rolling_fcm_batch_size`
- `rolling_fcm_invalid_token_cleanup_total`

### 이유
- 현재 FCM 발송은 중요한 사용자 알림 경로다.
- 실패율이 올라가도 API 자체는 살아 있을 수 있으므로 별도 관측이 필요하다.
- 잘못된 토큰 정리 수가 갑자기 늘면 클라이언트 토큰 수명 정책 문제를 빨리 볼 수 있다.

### 5. 오픈매트 비즈니스 메트릭
- `rolling_openmat_apply_total{result}`
- `rolling_openmat_cancel_total`
- `rolling_openmat_report_total`
- `rolling_openmat_sync_total`

### 이유
- 오픈매트는 핵심 도메인이다.
- 단순 인프라 메트릭만 보면 "서버는 정상인데 신청 실패가 늘어나는" 문제를 놓칠 수 있다.
- `CAPACITY_FULL`, `OPEN_MAT_CLOSED`, `ALREADY_APPLIED` 같은 실패 유형도 분리하면 운영 해석이 쉬워진다.

## Grafana 대시보드 구성
### 1. Rolling Overview
- API 요청량
- 에러율
- P95 응답 시간
- JVM 메모리
- DB 커넥션 사용량
- 최근 24시간 스케줄러 성공/실패

### 이유
- 운영자가 가장 먼저 열어야 하는 1장짜리 대시보드다.
- "서버가 느린가 / 에러가 많은가 / 배치가 죽었는가" 를 한 화면에서 확인할 수 있다.

### 2. Scheduler Dashboard
- task별 실행 횟수
- 최근 성공 시각
- 최근 실패 시각
- 평균 실행 시간
- 실패 비율

### 이유
- 현재 프로젝트는 예약 작업 품질이 매우 중요하다.
- 대회 크롤러와 오픈매트 상태 동기화는 별도 화면으로 보는 편이 운영 해석이 빠르다.

### 3. Business / Integration Dashboard
- 크롤러 source별 저장 결과
- FCM 성공/실패
- 오픈매트 신청/취소/신고 추이
- 외부 의존성 health 상태

### 이유
- 장애가 아니라도 "기능이 실제로 잘 동작하는가" 를 볼 수 있어야 한다.
- 운영 지표와 기능 지표를 분리해야 원인 파악이 빨라진다.

## 알람 설계
### 즉시 넣을 알람
- API scrape 실패
- 5xx 비율 급증
- P95 응답 시간 급증
- `tournamentCrawler` 마지막 성공 시각이 26시간 이상 지남
- `openMatStatusSync` 최근 실패 발생
- FCM 실패율 급증

### 왜 이 알람이 먼저인가
- 지금 서비스 특성상 가장 위험한 것은
  - API 자체 장애
  - 배치 멈춤
  - 외부 연동 실패 누적
- 이 3가지를 먼저 잡으면 대부분의 운영 리스크를 커버할 수 있다.

## 권장 구현 순서
### Phase 1. 안전한 scrape 경로 확보
- management port 분리
- Prometheus 컨테이너 추가
- Grafana 컨테이너 추가
- 기본 JVM/HTTP/DB 대시보드 연결

### 이유
- 메트릭이 없는데 커스텀 알람부터 만들면 운영 복잡도만 늘어난다.
- 먼저 "안전하게 수집되고 보이는 상태" 를 만드는 것이 우선이다.

### Phase 2. 커스텀 메트릭 추가
- `ScheduledTaskTracker` 와 MeterRegistry 연동
- TournamentManagerService 결과 카운터 추가
- FCM 발송 성공/실패 카운터 추가
- OpenMat 핵심 이벤트 카운터 추가

### 이유
- 이 단계부터 프로젝트 전용 모니터링이 된다.
- 단순 시스템 모니터링에서 "비즈니스 운영 모니터링" 으로 넘어간다.

### Phase 3. 알람 운영화
- Alert rule 추가
- 초기 임계값을 코드와 함께 버전 관리
- Alertmanager / 수신 채널은 운영 환경 정보가 정해진 뒤 연결
- 알람 수신 채널 연결
- 노이즈가 큰 알람 튜닝

### 이유
- 알람은 처음부터 많이 넣으면 운영 피로도만 높아진다.
- 실제 대시보드를 보면서 임계값을 조정하는 편이 안전하다.

## 최종 추천
- 1차 도입은 `Prometheus + Grafana + 관리 포트 분리 + 기본 대시보드`
- 2차 도입은 `스케줄러/크롤러/FCM/오픈매트 커스텀 메트릭`
- Grafana는 우선 외부 비공개
- Prometheus scrape 대상은 `api:9090/actuator/prometheus`
- 단, 이 주소는 `Prometheus가 API와 같은 Docker 네트워크에 있을 때` 기준이다.

## 한 줄 결론
- 이 프로젝트는 이미 Micrometer/Actuator 기반이 깔려 있으므로, "관리 포트 분리 + 내부 scrape + 프로젝트 전용 메트릭 추가" 방식이 가장 안전하고 현재 구조에도 잘 맞는다.

## Phase별 체크리스트
### Phase 1. 기본 수집 환경 구성
- [x] `management.server.port=9090` 설정 추가
- [x] management 포트 전용 `SecurityFilterChain` 설계 및 적용
- [x] `/actuator/prometheus` 가 내부 scrape 가능하도록 보안 정책 정리
- [x] `docker-compose.monitoring.yml` 생성
- [x] `prometheus` 서비스 추가
- [x] `grafana` 서비스 추가
- [x] Prometheus 설정 파일 `prometheus.yml` 추가
- [x] Prometheus scrape target 을 `api:9090/actuator/prometheus` 기준으로 설정
- [x] Grafana datasource provisioning 파일 추가
- [x] Grafana dashboard provisioning 파일 추가
- [x] 기본 대시보드 1차 구성
- [ ] JVM / HTTP / DB 메트릭이 실제 수집되는지 검증

### Phase 2. 프로젝트 전용 메트릭 추가
- [x] `ScheduledTaskTracker` 와 `MeterRegistry` 연동
- [x] 스케줄러 실행 횟수 메트릭 추가
- [x] 스케줄러 실행 시간 메트릭 추가
- [x] 스케줄러 마지막 성공 시각 메트릭 추가
- [x] `TournamentManagerService` 에 크롤링 결과 메트릭 추가
- [x] source별 크롤링 성공/실패 메트릭 추가
- [x] FCM 발송 성공/실패 메트릭 추가
- [x] FCM invalid token 정리 메트릭 추가
- [x] OpenMat 신청/취소/신고 메트릭 추가
- [x] OpenMat 상태 동기화 건수 메트릭 추가
- [x] 커스텀 메트릭용 Grafana 대시보드 추가
- [x] 메트릭 이름/태그 정책 문서화

### Phase 3. 알람 운영화
- [x] Prometheus alert rule 파일 추가
- [x] API scrape 실패 알람 추가
- [x] 5xx 비율 급증 알람 추가
- [x] P95 응답 시간 급증 알람 추가
- [x] `tournamentCrawler` 미실행 알람 추가
- [x] `openMatStatusSync` 실패 알람 추가
- [x] FCM 실패율 급증 알람 추가
- [x] Alertmanager 도입 여부 결정
- [ ] 알람 수신 채널 정의
- [ ] 알람 임계값 튜닝
- [x] 운영 점검 절차 문서화
