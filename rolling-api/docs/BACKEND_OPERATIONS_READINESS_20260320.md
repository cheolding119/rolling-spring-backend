# Backend Operations Readiness Checklist (기준일: 2026-03-20)

이 문서는 `docs/OPERATIONS_READINESS_20260320.md`를 바탕으로, Java Spring 백엔드 기준에서 바로 구현/점검 가능한 체크리스트로 다시 정리한 문서다.
`docs/AGENTS.md`는 API 계약과 현재 구현 상태를 확인하는 참고 문서로만 사용하고, 이 문서는 운영 준비의 남은 백로그를 관리하는 용도로 사용한다.

## 현재 확보된 기반

- [x] 인증 기본 흐름 존재
  로그인, 토큰 갱신, 로그아웃, 회원 탈퇴, `/users/me` 계약이 문서화돼 있다.
- [x] 오픈매트/대회/공지사항 조회 기본 계약 존재
  일반 사용자 앱이 읽는 주요 API 범위가 이미 정의돼 있다.
- [x] 신고 접수 계약 존재
  오픈매트 신고와 공통 신고 개념이 문서에 반영돼 있다.
- [x] 알림 저장 + 읽음 처리 계약 존재
  알림함 source of truth가 백엔드 `Notification` 저장 데이터로 정리돼 있다.
- [x] FCM 토큰 등록과 푸시 발송 정책 존재
  `user_devices` 구조, 토큰 재등록, 실패 토큰 정리 정책이 문서화돼 있다.
- [x] 관리자 권한 기준 존재
  `isAdmin`, `admin.user-ids`, ADMIN API 보호 기준이 정리돼 있다.
- [x] 대회 크롤링 수동 실행 API 존재
  관리자 전용 크롤링 API가 이미 준비돼 있다.
- [x] 공지사항 운영 API 존재
  `POST/PUT/DELETE /api/v1/notices`가 관리자 권한 기준으로 정리돼 있다.

## Unit OR-01. 운영 로그/요청 추적성

- [x] `OncePerRequestFilter` 또는 공통 필터에서 `requestId`를 생성하고 MDC에 주입
- [x] 외부에서 전달된 trace header가 있으면 우선 사용하고, 없으면 서버에서 신규 발급
- [x] 예외 로그, 서비스 로그, 응답 로그가 같은 `requestId`를 공유하도록 로깅 포맷 정리
- [x] 구조화 로그 기본 필드를 확정
  `timestamp`, `level`, `requestId`, `userId`, `path`, `method`, `status`, `errorCode`, `domainId`
- [~] 민감정보 로그 정책 반영
  access token, refresh token, email, phone, 외부 API raw response는 마스킹 또는 비로그 처리
- [x] 운영 로그와 개발용 디버그 로그를 분리
  운영 기본 레벨과 디버그 활성화 기준을 profile 기준으로 구분

결과 메모:

- `RequestTrackingFilter`를 추가해 `X-Request-Id`, `X-Trace-Id`, `X-Correlation-Id`, `traceparent` 중 들어온 값을 우선 사용하고, 없으면 서버에서 UUID를 발급한다.
- 요청 시작 시 MDC에 `requestId`, `traceId`, `method`, `path`, `status`, `errorCode`, `domainId`, `userId` 기본값을 주입하고, 응답 시 `status`를 기록한 뒤 정리한다.
- JWT 인증 성공 시 MDC의 `userId`를 실제 사용자 ID로 갱신한다.
- `GlobalExceptionHandler`와 인증 실패 응답에서도 `status`, `errorCode`를 MDC에 기록하도록 맞췄다.
- 기본 로그 패턴을 구조화 필드 중심으로 변경했고, 운영 기본 프로필은 `INFO`, 로컬 프로필은 `DEBUG`로 분리했다.
- `SecurityAuthorizationIntegrationTest`로 `requestId` 자동 생성과 외부 헤더 재사용을 검증했다.
- 민감정보 로그 정책은 핵심 인증/요청 추적 경로에는 직접 토큰 원문을 남기지 않도록 유지했지만, 전체 서비스 로그를 전수 점검한 상태는 아니라서 부분 완료로 둔다.

완료 기준:

- 모든 HTTP 요청과 주요 서비스/예외 로그를 `requestId`로 추적할 수 있다.
- 운영 로그만으로 어떤 사용자 요청이 어디서 실패했는지 1차 확인이 가능하다.

## Unit OR-02. 헬스체크/메트릭/장애 감지

- [x] Spring Boot Actuator 기반 health endpoint 노출 정책 확정
- [x] DB 연결 상태 health indicator 점검
- [x] JVM/HTTP 기본 메트릭 수집 활성화
- [x] 스케줄러 상태 또는 마지막 실행 시각 확인 수단 추가
- [x] 외부 연동 상태 점검 기준 정리
  FCM, S3, 소셜 로그인, 크롤러
- [ ] 운영 채널 알림 기준 확정
  health down, 외부 연동 반복 실패, 스케줄러 실패
- [~] 장애 감지 후 확인할 dashboard 또는 로그 질의 기준 문서화

결과 메모:

- `spring-boot-starter-actuator`와 Prometheus registry를 추가해 `/actuator/health`, `/actuator/info`, `/actuator/metrics`, `/actuator/prometheus`를 운영 점검 기본 endpoint로 노출했다.
- `/actuator/health`와 `/actuator/health/**`는 무인증 probe 용도로 열고, 그 외 `/actuator/**`는 `ROLE_ADMIN`으로 제한했다.
- `management.endpoint.health.show-details=when_authorized`와 `roles=ADMIN` 기준으로 health 상세 정보 노출 범위를 제한했다.
- DB, JVM, HTTP 기본 상태/메트릭은 Spring Boot Actuator 기본 health/metrics contributor에 위임한다.
- `ScheduledTaskTracker`와 `SchedulerHealthIndicator`를 추가해 오픈매트 상태 동기화, 대회 크롤링, 회원 탈퇴 처리 스케줄러의 마지막 시작/종료/성공/실패 시각과 마지막 결과를 health detail에서 확인할 수 있게 했다.
- 각 스케줄러 실행 코드에 tracker 기록을 연결해 마지막 실행 상태가 메모리 상에서 갱신되도록 맞췄다.
- `ExternalDependenciesHealthIndicator`를 추가해 Firebase 초기화 여부, S3 client/bucket 설정 여부, Google/Kakao client 초기화 여부, 크롤러 URL 설정 여부를 `/actuator/health` detail로 확인할 수 있게 했다.
- 현재 구현은 `관측 endpoint와 상태 근거`를 제공하는 단계까지 완료했고, Slack/Discord/PagerDuty 같은 운영 채널 알림 연결은 아직 미구현이다.
- dashboard/로그 질의 기준은 actuator endpoint 자체는 준비됐지만, Grafana/Prometheus 대시보드 쿼리나 운영 runbook 문서까지 적은 상태는 아니라서 부분 완료로 둔다.

완료 기준:

- 운영자가 서버 상태를 health, metrics, alert 기준으로 확인할 수 있다.
- 장애를 사용자가 제보하기 전에 운영 채널에서 먼저 인지할 수 있다.

## Unit OR-03. 운영 런북/장애 대응 절차

- [ ] 로그인 장애 대응 런북 작성
  토큰 발급, 소셜 연동, JWT 검증, DB 연결 확인 순서 정리
- [ ] 푸시 장애 대응 런북 작성
  `Notification` 저장 여부, `UserDevice` 상태, FCM 예외 로그 확인 순서 정리
- [ ] 크롤링 장애 대응 런북 작성
  관리자 실행 이력, 외부 소스 응답, 파서 예외, 저장 실패 확인 순서 정리
- [ ] 공지 조회/운영 장애 대응 런북 작성
  권한, DB 데이터, API 예외, 관리자 액션 이력 확인 순서 정리
- [ ] 운영 연락 채널과 담당 범위 문서화
  문의 대응, 장애 대응, 스토어 심사 대응

완료 기준:

- 운영 이슈별로 "어디부터 확인할지"가 문서로 정리돼 있다.
- 신규 운영자도 런북만 보고 1차 대응을 시작할 수 있다.

## Unit OR-06. 신고 운영 API

- [x] 관리자용 신고 목록 조회 API 추가
- [x] 관리자용 신고 상세 조회 API 추가
- [x] 신고 처리 상태값 정의
  예: `RECEIVED`, `IN_REVIEW`, `RESOLVED`, `REJECTED`
- [x] 신고 처리 상태 변경 API 추가
- [x] 최종 조치 기록 필드 추가
  처리자, 처리 시각, 처리 메모, 최종 조치
- [x] 동일 신고 대상에 대한 누적 상태를 운영 화면에서 조회 가능하게 정리
- [x] Swagger 및 운영 문서 반영
- [x] 서비스/컨트롤러 테스트 작성

결과 메모:

- `Report` 엔티티에 `status`, `processedByUserId`, `processedAt`, `processingMemo`, `finalAction` 필드를 추가해 신고를 접수 데이터에서 운영 처리 데이터로 확장했다.
- 신고 생성 시 기본 상태는 `RECEIVED`다.
- 관리자 API로 `GET /api/v1/admin/reports`, `GET /api/v1/admin/reports/{id}`, `PATCH /api/v1/admin/reports/{id}/status`를 추가했다.
- 목록/상세 응답에는 동일 신고 대상 기준 누적 건수와 상태별 건수(`received`, `inReview`, `resolved`, `rejected`)를 함께 내려 운영 화면에서 바로 사용할 수 있게 했다.
- 상태 변경 시 `processedByUserId`, `processedAt`, `processingMemo`, `finalAction`을 함께 기록한다.
- `ReportServiceTest`, `ReportAdminControllerTest`로 신고 생성 규칙, 관리자 목록 조회, 상태 변경, 관리자 권한 보호를 검증했다.

완료 기준:

- 신고는 "접수만 되는 데이터"가 아니라 운영자가 실제로 처리 가능한 상태 모델을 가진다.

## Unit OR-07. 문의 도메인(Q&A) MVP

- [x] 도메인 명칭을 `Inquiry`로 확정
- [x] 문의 엔티티/리포지토리/서비스/컨트롤러 구현
- [x] 사용자 문의 생성 API 추가
- [x] 사용자 본인 문의 목록/상세 API 추가
- [x] 관리자 문의 목록/상세 API 추가
- [x] 문의 상태값 정의
  `RECEIVED`, `IN_REVIEW`, `ANSWERED`
- [x] 운영자 답변 저장 API 추가
- [x] 상태 변경 API 추가
- [x] 답변 완료 시 알림함 이벤트 연결
- [x] Swagger 및 테스트 작성

결과 메모:

- 사용자 API로 `POST /api/v1/inquiries`, `GET /api/v1/inquiries`, `GET /api/v1/inquiries/{id}`를 추가했다.
- 관리자 API로 `GET /api/v1/admin/inquiries`, `GET /api/v1/admin/inquiries/{id}`, `PATCH /api/v1/admin/inquiries/{id}/answer`, `PATCH /api/v1/admin/inquiries/{id}/status`를 추가했다.
- 문의 생성 시 기본 상태는 `RECEIVED`다.
- 답변 저장 시 `answerContent`, `answeredByUserId`, `answeredAt`을 기록하고 상태를 `ANSWERED`로 변경한다.
- 답변이 없는 문의는 `ANSWERED`로 직접 바꿀 수 없고, 답변이 저장된 문의는 `ANSWERED` 외 상태로 되돌리지 않도록 막았다.
- 첫 답변 완료 시 알림함에 `INQUIRY_ANSWERED` 타입과 `/inquiry/detail` route를 저장한다.
- `InquiryServiceTest`, `InquiryControllerTest`, `InquiryAdminControllerTest`로 핵심 흐름과 보안 매핑을 검증했다.

완료 기준:

- 앱 내 1:1 문의 흐름을 백엔드 API만으로 구성할 수 있다.
- 운영자가 문의를 보고 답변하고 상태를 변경할 수 있다.

## Unit OR-08. 사용자 제재/차단 운영 모델

- [ ] 현재 사용자 간 차단 API와 운영 제재 API를 개념적으로 분리할지 확정
- [ ] 운영 제재용 상태 모델 정의
  차단 사유, 시작 시각, 종료 시각, 해제 사유, 상태
- [ ] 관리자용 사용자 제재 API 추가
- [ ] 제재 이력 저장 모델 추가
- [ ] 반복 신고/문의 악용/수동 제재 등 제재 사유 분류 기준 정리
- [ ] 제재 해제 API 및 감사 로그 연동
- [ ] Swagger 및 운영 문서 반영

완료 기준:

- 사용자 간 block 기능과 운영자 제재 기능의 책임이 분리된다.
- 운영자가 제재 사유와 기간을 기준으로 일관되게 처리할 수 있다.

## Unit OR-09. 관리자 검색/필터 API

- [ ] 신고 목록에 상태/기간/대상 타입 필터 추가
- [ ] 문의 목록에 상태/기간/문의 유형 필터 추가
- [ ] 공지 목록에 작성일/작성자 기준 조회 조건 추가 여부 결정
- [ ] 관리자 목록 조회 API의 공통 페이징/정렬 규칙 정리
- [ ] 관리자 화면에서 바로 쓰기 쉬운 응답 DTO 기준 정리

완료 기준:

- 운영 데이터가 단순 전체조회가 아니라 상태 기반 검색으로 확인 가능하다.

## Unit OR-10. FCM 토큰 정책 정합성

- [ ] `POST /api/v1/auth/logout`의 `fcmToken` 처리와 `DELETE /api/v1/users/me/fcm` 역할을 문서 기준으로 통일
- [ ] 로그아웃, 탈퇴 예약, 최종 탈퇴, 기기 변경 시 토큰 정리 규칙을 한 문서에 정리
- [ ] 실제 구현과 Swagger와 운영 문서의 표현을 동일하게 맞춤
- [ ] 무효 토큰 정리 정책과 운영 확인 포인트 문서화
- [ ] 토큰 삭제/재연결 관련 회귀 테스트 보강

완료 기준:

- 토큰 라이프사이클 정책이 문서와 구현에서 다르게 해석되지 않는다.

## Unit OR-11. FAQ/도움말 제공 방식 결정

- [ ] FAQ를 정적 문서로 둘지, 별도 API/DB 관리로 갈지 결정
- [ ] 운영 변경 빈도와 배포 필요 여부 기준으로 방식 선택
- [ ] API로 갈 경우 최소 조회 모델과 관리자 수정 범위 정의

완료 기준:

- FAQ/도움말이 임시 메모가 아니라 운영 가능한 데이터 소스로 정의된다.

## Unit OR-12. 운영 지표/권한 세분화

- [ ] 운영 KPI 후보 확정
  신고 수, 문의 수, 푸시 실패율, 공지 발행 수, 크롤링 실패율
- [ ] 최소 일/주 단위 집계 방식 결정
- [ ] 관리자 권한 세분화 필요 여부 결정
  공지 운영, 제재 운영, 크롤링 운영
- [ ] 권한 세분화가 필요하면 Spring Security role/authority 설계 초안 작성

완료 기준:

- 운영 데이터가 누적되고, 관리자 권한이 확장 가능한 구조인지 판단할 수 있다.

## 이번 단계에서 제외

- [ ] 대규모 통계 대시보드
  지금 단계의 운영 필수 범위는 아니다.
- [ ] 복잡한 다단계 권한 체계
  현재 운영 규모에서는 우선순위가 낮다.
- [ ] 장기 분석 리포트 자동화
  즉시 대응 가능한 운영 기능이 먼저다.

## 추천 구현 순서

1. OR-01 운영 로그/요청 추적성
2. OR-02 헬스체크/메트릭/장애 감지
3. OR-03 운영 런북/장애 대응 절차
4. OR-04 공지 운영 절차 + 감사 로그
5. OR-05 크롤링 실행 이력/감사
6. OR-06 신고 운영 API
7. OR-07 문의 도메인(Q&A) MVP
8. OR-08 사용자 제재/차단 운영 모델
9. OR-09 관리자 검색/필터 API
10. OR-10 FCM 토큰 정책 정합성
11. OR-11 FAQ/도움말 제공 방식 결정
12. OR-12 운영 지표/권한 세분화

## 참고 문서

- `docs/AGENTS.md`
- `docs/OPERATIONS_READINESS_20260320.md`
- `docs/BACKEND_REMAINING.md`
- `docs/FCM_INTEGRATION_CHECKLIST.md`

