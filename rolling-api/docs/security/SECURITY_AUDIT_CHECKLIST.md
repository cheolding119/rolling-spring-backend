# Security Audit Checklist

현재 프로젝트의 보안 검토를 정적 리뷰와 운영 검증으로 나눠 실행할 수 있도록 phase 기준 체크리스트를 정리한다.

## 범위

- 인증과 권한
- 관리자 접근 제어
- 입력값 검증과 인젝션 방어
- 시크릿 및 민감정보 처리
- 네트워크 노출과 런타임 하드닝
- 로깅, 탐지, 사고 대응
- 공급망 및 변경관리

## 전제

- 이 문서는 정적 검토만으로 완전한 보안을 보장하지 않는다.
- 실제 운영값, 배포 토폴로지, 시크릿 저장소, 보안그룹, WAF, IAM 정책은 별도 환경 검증이 필요하다.
- 각 항목은 가능하면 증거와 함께 기록한다.

## Phase 1. 인증과 세션 수명주기

- [x] 로그인, refresh, logout, withdraw 흐름에서 명백한 서버측 우회 경로는 보이지 않았다.
- [ ] JWT secret 이 충분한 길이의 Base64 키인지 운영값까지 포함해 확인해야 한다.
- [x] refresh token rotation 은 구현되어 있다.
- [x] refresh token 재사용과 경쟁 갱신에 대한 기본 대응을 추가했다.
- [x] refresh token 은 DB에 SHA-256 해시로 저장되도록 변경했다.
- [x] 탈퇴 예약, 탈퇴 취소, 탈퇴 실행 시 토큰 무효화와 디바이스 토큰 정리 흐름은 구현되어 있다.
- [x] 탈퇴 계정은 refresh/login 시 차단되고, 제재 계정은 제한 모드로 동작하도록 설계되어 있다.
- [x] Kakao, Google, Apple 토큰 검증 방식 차이와 한계를 정리했다.

### Phase 1 분석 메모

- 근거 파일: `domain/auth/controller/AuthController.java`, `domain/auth/service/AuthService.java`, `domain/auth/entity/RefreshToken.java`, `global/security/jwt/JwtTokenProvider.java`, `global/security/jwt/JwtAuthenticationFilter.java`, `global/security/UserSanctionAccessFilter.java`, `infra/apple/AppleTokenVerifier.java`, `infra/google/GoogleClient.java`, `infra/kakao/KakaoClient.java`
- access token 만료시간은 `1800000ms` 로 30분, refresh token 만료시간은 `1209600000ms` 로 14일이다.
- 테스트 설정의 JWT secret 은 Base64 decode 시 32바이트 키로 확인되지만, 운영값은 환경 검증이 필요하다.
- refresh token rotation 은 기존 토큰 삭제 후 새 토큰 저장 방식으로 구현되어 있다.
- refresh token 은 `refresh_tokens.token` 컬럼에 SHA-256 해시로 저장되며 raw token 원문은 저장하지 않는다.
- refresh 조회는 해시 기준 + 비관적 잠금으로 수행하고, DB에는 사용자당 refresh token 1개만 유지되도록 unique 제약을 추가했다.
- `logout` 은 refresh token 삭제와 현재 디바이스 FCM token 제거를 함께 수행한다.
- `withdraw` 실행 시 refresh token 과 사용자 디바이스 token 이 함께 제거된다.
- 탈퇴 계정은 `findByIdAndIsWithdrawnFalse` 및 `ensureLoginAllowed` 경로로 차단된다.
- 제재 계정은 로그인과 refresh 자체를 막기보다 `UserSanctionAccessFilter` 에서 허용 경로만 열어두는 제한 모드로 동작한다.
- Apple 은 `issuer`, `audience`, `timestamp` 검증이 구현되어 있다.
- Google, Kakao 는 access token 으로 userinfo endpoint 를 호출하는 구조이며, Apple 과 같은 로컬 claim 검증 모델은 아니다.
- 현재 구현은 모바일 public client 기준으로는 허용 가능하지만, 여러 OAuth client 를 병행하거나 client binding 이 중요해지면 ID token 검증 또는 provider 별 audience 검증 강화를 검토해야 한다.
- 제재 계정 access token 이 비허용 API에서 `403` 을 반환하는 통합 테스트를 추가했다.

### Phase 1 추가 진행 체크리스트

- [ ] 운영 JWT secret 길이와 엔트로피를 점검하고, 회전 절차를 문서화한다.
- [x] refresh token 평문 저장을 해시 저장으로 전환했다.
- [x] refresh token 동시 재발급 경쟁 조건과 재사용 공격에 대해 사용자당 1개 유지 + 비관적 잠금으로 기본 대응을 넣었다.
- [x] 현재 단일 활성 refresh token 정책에서는 token family, reuse detection, `jti` 기반 무효화를 추가하지 않기로 결정했다.
- [x] Google, Kakao 에 대해 현재 userinfo 호출 방식의 한계와 추가 검증 필요 조건을 정리했다.
- [x] 제재 계정이 비허용 API 호출 시 실제 `403` 으로 제한되는 통합 테스트를 추가했다.

## Phase 2. 인가와 권한 상승 방지

- [ ] 관리자 권한이 `admin.user-ids` 기반으로만 판정되는 현재 구조가 운영상 허용 가능한지 검토한다.
- [ ] `/api/v1/admin/**` 와 관리자 전용 기능이 모두 서버측에서 강제되는지 확인한다.
- [ ] `permitAll` 경로가 과도하지 않은지 검토한다.
- [ ] 운영에서 Swagger 공개가 필요한지 별도로 판단한다.
- [ ] `TestUserHeaderAuthenticationFilter` 가 `local` 외 환경에 절대 로드되지 않는지 확인한다.
- [ ] 운영 우회성 플래그가 기본값과 실제 배포값 모두 안전한지 확인한다.
  예: `OPENMAT_ALLOW_UNAUTHENTICATED_UPDATE`
- [ ] 사용자 ID 기반 리소스 조회/수정 경로에서 IDOR 가능성을 확인한다.

## Phase 3. 시크릿과 민감정보 처리

- [ ] `.env`, `secrets`, Firebase JSON, 인증서 파일이 Git 에 포함되지 않는지 확인한다.
- [ ] 운영 시크릿이 코드나 설정 상수에 하드코딩되지 않는지 확인한다.
- [ ] AWS access key 와 secret key 정적 주입 대신 역할 기반 인증 전환 가능성을 평가한다.
- [ ] JWT secret, DB 비밀번호, Slack webhook, Firebase key 가 로그나 예외에 남지 않는지 확인한다.
- [ ] FCM token, refresh token, email 등 개인정보가 알림이나 로그에 과도하게 남지 않는지 확인한다.
- [ ] S3 공개 URL 정책과 버킷 권한이 일치하는지 확인한다.

## Phase 4. 네트워크 노출과 런타임 하드닝

- [ ] HTTP 가 HTTPS 로 강제 리다이렉트되는지 확인한다.
- [ ] 보안 헤더 적용 여부를 확인한다.
  HSTS, X-Content-Type-Options, Referrer-Policy, Content-Security-Policy
- [ ] `/actuator/health`, `/actuator/prometheus` 외 경로가 외부에 공개되지 않는지 검증한다.
- [ ] `management.server.port` 가 외부에서 직접 접근 가능한지 확인한다.

## Phase 5. 입력값 검증과 인젝션 방어

- [ ] `@Valid` 가 누락된 요청 DTO, 쿼리 파라미터, 경로 파라미터가 없는지 확인한다.
- [ ] presigned URL 발급 API 에서 파일명과 content type 검증이 충분한지 검토한다.
- [ ] 업로드 가능한 확장자, MIME type, object key prefix 가 목적별로 제한되는지 확인한다.
- [ ] 외부 URL 다운로드 경로가 있으면 SSRF 가능성을 점검한다.
- [ ] 크롤러나 외부 API 응답을 DB에 저장하는 흐름에서 stored XSS 가능성을 확인한다.

## Phase 6. 범위 확정과 자산 식별

- [x] 외부 노출 엔드포인트를 사용자 API, 관리자 API, Actuator, Swagger, 크롤러 실행 API로 분류했다.
- [x] 인증 필요 경로와 익명 허용 경로를 서버 설정 기준으로 정리했다.
- [x] 외부 연동 자산을 정리했다.
  Kakao, Google, Apple, AWS S3, Firebase FCM, Slack Webhook
- [x] 민감 데이터 흐름을 식별했다.
  access token, refresh token, FCM token, DB 계정정보, AWS 키, Firebase 서비스 계정, Slack webhook

### Phase 6 분석 메모

- 근거 파일: `global/config/SecurityConfig.java`, `src/main/resources/application.yml`, `src/main/resources/application-local.yml`, `src/test/resources/application.yml`, `docker-compose.yml`, `nginx/default.conf`
- 익명 허용 경로는 `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, Swagger, `GET` 기반 공개 조회 API, `/error`, 일부 Actuator health/prometheus 로 확인했다.
- 관리자 경로는 `/api/v1/admin/**`, `POST /api/v1/tournaments/crawl`, 공지사항 쓰기/수정/삭제, Actuator 비공개 경로로 확인했다.
- 운영 노출면은 `api.rolling-app.com` 의 Nginx reverse proxy 와 `management.server.port=9090` 설정을 통해 API/Actuator 분리가 의도되어 있다.
- `local` 프로필에서는 `X-Test-User-Id` 헤더 인증 필터가 추가되고, `test` 프로필은 H2 및 테스트용 JWT secret 을 사용한다.

### Phase 6 추가 진행 체크리스트

- [ ] 운영 환경에서 `9090` 포트가 외부에 직접 노출되지 않는지 보안그룹, 방화벽, reverse proxy 기준으로 확인한다.
- [ ] 운영에서 Swagger 경로가 실제 외부 공개 상태인지 확인하고, 필요 없으면 차단 정책을 정한다.

## Phase 7. 로깅, 탐지, 운영 대응

- [ ] 인증 실패, 권한 거부, 토큰 재발급, 관리자 행위에 대한 감사 로그가 충분한지 확인한다.
- [ ] 실패 로그가 토큰 원문 대신 요약 정보만 남기는지 확인한다.
- [ ] Slack 장애 알림에 개인정보나 시크릿이 포함되지 않는지 점검한다.

## Phase 8. 공급망과 변경관리

- [ ] `build.gradle` 의 주요 라이브러리 버전과 알려진 취약점을 점검한다.
- [ ] Docker base image, Nginx image, JDK image 에 대한 취약점 스캔 절차가 있는지 확인한다.

## 우선 검토 권장 항목

- [ ] 운영에서 Swagger 공개를 유지할지 결정한다.
- [ ] refresh token 평문 저장 여부와 개선 필요성을 판단한다.
- [ ] 로컬 전용 테스트 인증 우회가 운영으로 유입될 여지가 없는지 재확인한다.
- [ ] `OPENMAT_ALLOW_UNAUTHENTICATED_UPDATE` 실제 운영값을 검증한다.
- [ ] `Actuator` 와 `management.port` 외부 노출 상태를 재검증한다.
- [ ] 외부 URL 다운로드 경로의 SSRF 가능성을 점검한다.
- [ ] 정적 AWS 키 사용 축소 또는 역할 기반 인증 전환 여부를 검토한다.
