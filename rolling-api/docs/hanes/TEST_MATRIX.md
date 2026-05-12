# Rolling API 테스트 매트릭스

## 목적

이 문서는 AI 에이전트와 개발자가 변경 범위에 맞는 최소 테스트를 빠르게 선택하기 위한 기준이다. 테스트는 변경한 코드 경계에 가장 가까운 테스트부터 실행하고, 보안/계약/운영 영향이 있으면 관련 통합 테스트를 추가한다.

기본 실행 위치:

```powershell
cd C:\rolling\rolling-spring-backend\rolling-api
```

전체 테스트:

```powershell
.\gradlew.bat test
```

## 공통 선택 기준

| 변경 범위 | 우선 실행 | 추가 고려 |
| --- | --- | --- |
| 문서만 변경 | 테스트 생략 가능 | 링크, 경로, source of truth 충돌 확인 |
| 단일 service 로직 | 해당 service test | controller 계약 변경 시 controller test 추가 |
| controller/API 계약 | controller test + service test | DTO, validation, security requirement 문서 동기화 |
| security/auth/admin | security test + 통합 권한 테스트 | 공개 API, 관리자 API, 제재 필터 회귀 확인 |
| repository/query/list API | repository 또는 service test | `NPlusOneIntegrationTest`, paging/정렬 안정성 |
| scheduler/crawler/alert | scheduler/crawler/alert test | health indicator, Slack alert, 운영 재실행 가능성 |
| 공통 응답/예외/config | 관련 global test | 전체 테스트 고려 |

## 도메인별 테스트 명령

### Auth

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.auth.service.AuthServiceLifecycleTest"
.\gradlew.bat test --tests "com.rolling.api.domain.auth.service.AuthServiceLoginValidationTest"
.\gradlew.bat test --tests "com.rolling.api.domain.auth.service.AuthServiceWithdrawTest"
```

추가 기준:

- token 발급/refresh/logout/withdraw 흐름 변경 시 관련 service test를 함께 실행한다.
- 인증 필터나 공개/인증 경계가 바뀌면 `SecurityAuthorizationIntegrationTest`를 추가한다.

### User / Admin User / Sanction

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.user.service.UserServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.user.service.UserAdminServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.user.controller.UserControllerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.user.controller.UserAdminControllerTest"
.\gradlew.bat test --tests "com.rolling.api.global.security.UserSanctionAccessFilterTest"
.\gradlew.bat test --tests "com.rolling.api.SecurityAuthorizationIntegrationTest"
```

추가 기준:

- 관리자 권한, 제재 상태, 공개 조회 허용 경계가 바뀌면 security 통합 테스트를 포함한다.
- user repository query 변경 시 `UserRepositoryTest`, `UserDeviceRepositoryTest`를 추가한다.

### OpenMat

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.openmat.service.OpenMatServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.openmat.controller.OpenMatControllerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.openmat.controller.OpenMatAdminControllerTest"
.\gradlew.bat test --tests "com.rolling.api.OpenMatApplyConcurrencyTest"
.\gradlew.bat test --tests "com.rolling.api.domain.openmat.scheduler.OpenMatStatusSchedulerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.openmat.service.OpenMatPushNotificationEventHandlerTest"
```

추가 기준:

- 신청/취소/정원/상태 전이를 바꾸면 동시성 테스트를 고려한다.
- 공개 목록/상세 조회 경계가 바뀌면 `SecurityAuthorizationIntegrationTest`를 추가한다.
- 목록 query, 차단 필터, paging 변경 시 `NPlusOneIntegrationTest`를 고려한다.

### Tournament / Crawler

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.service.TournamentServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.service.TournamentManagerServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.controller.TournamentControllerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.controller.TournamentCrawlerControllerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.scheduler.TournamentCrawlerSchedulerTest"
```

Crawler별 테스트:

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.crawler.KoreaJiuCrawlerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.crawler.HeroesOfJiuJitsuCrawlerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.crawler.SpotliteCrawlerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.crawler.TournamentApplyLinkCrawlerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.service.KoreaJiuCrawlerServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.service.HeroesOfJiuJitsuCrawlerServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.tournament.service.StreetJiuJitsuCrawlerServiceTest"
```

추가 기준:

- 외부 HTML parsing, 중복 저장, 수동/자동 실행 공통 service 로직 변경 시 crawler service와 scheduler 테스트를 함께 실행한다.
- 포스터/S3 변경 시 `TournamentPosterServiceTest`, `S3UploaderTest`를 추가한다.

### Seminar

세미나 Phase 0~2는 기본 도메인과 참석 신청까지 구현되어 있다.

세미나 기능 변경 시 최소 추가 대상:

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.seminar.service.SeminarServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.seminar.controller.SeminarControllerTest"
```

추가 기준:

- 신청/취소 기능을 바꾸면 정원 초과, 중복 신청, 종료/마감 상태 차단 테스트를 포함한다.
- 공개 조회와 인증 액션이 함께 있으면 security 통합 테스트를 추가한다.
- 호스트 신청자 관리, 모집 상태 변경, 신고, 알림 저장을 추가하면 해당 service/controller 테스트를 추가한다.

### Community

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.community.service.CommunityServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.community.service.CommunityCommentNotificationEventHandlerTest"
```

추가 기준:

- 게시글/댓글 신고, 삭제 상태, 작성자/관리자 권한 변경 시 controller 또는 admin controller 테스트 존재 여부를 먼저 확인하고 부족하면 추가한다.
- 알림 저장 책임 변경 시 notification service test를 함께 실행한다.

### Report

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.report.service.ReportServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.report.controller.ReportAdminControllerTest"
```

추가 기준:

- 신고 상태 전이, 관리자 처리, target summary 변경 시 service와 admin controller 테스트를 함께 실행한다.

### Notice / Inquiry

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.notice.service.NoticeServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.notice.controller.NoticeControllerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.notice.controller.NoticeAdminControllerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.inquiry.service.InquiryServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.inquiry.controller.InquiryControllerTest"
.\gradlew.bat test --tests "com.rolling.api.domain.inquiry.controller.InquiryAdminControllerTest"
```

추가 기준:

- 공지 공개 조회 경계가 바뀌면 security 통합 테스트를 추가한다.
- 문의 답변 알림 흐름 변경 시 notification 관련 테스트를 함께 확인한다.

### Notification / FCM

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.notification.service.NotificationServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.notification.service.FcmPushNotificationServiceTest"
.\gradlew.bat test --tests "com.rolling.api.domain.notification.config.NotificationSchemaConfigTest"
```

추가 기준:

- FCM 성공 여부와 DB 저장 알림의 책임을 혼동하지 않는다.
- push payload schema 변경 시 client 계약 문서 갱신 필요 여부를 확인한다.

### Map / S3 / Infra

```powershell
.\gradlew.bat test --tests "com.rolling.api.domain.map.service.KakaoGeocodeServiceTest"
.\gradlew.bat test --tests "com.rolling.api.infra.s3.S3UploaderTest"
.\gradlew.bat test --tests "com.rolling.api.global.config.S3PublicBaseUrlValidatorTest"
.\gradlew.bat test --tests "com.rolling.api.global.config.FirebaseAdminConfigTest"
```

추가 기준:

- 외부 API timeout, credential, null/empty response, 운영 알림 필요 여부를 함께 확인한다.
- geocoding API 계약 변경 시 지도 관련 client 계약을 함께 확인한다.

### Global Security

```powershell
.\gradlew.bat test --tests "com.rolling.api.SecurityAuthorizationIntegrationTest"
.\gradlew.bat test --tests "com.rolling.api.global.security.AdminAccessConfigTest"
.\gradlew.bat test --tests "com.rolling.api.global.security.UserPrincipalTest"
.\gradlew.bat test --tests "com.rolling.api.global.security.UserSanctionAccessFilterTest"
```

필수 실행 조건:

- `SecurityConfig`, `JwtAuthenticationFilter`, `UserSanctionAccessFilter`, `AdminAccessConfig` 변경
- 공개 API와 인증 API 경계 변경
- 관리자 API path 또는 권한 판정 변경
- 비회원/App Review 접근 정책 변경

### Monitoring / Alert / Scheduler Health

```powershell
.\gradlew.bat test --tests "com.rolling.api.global.monitoring.SchedulerHealthIndicatorTest"
.\gradlew.bat test --tests "com.rolling.api.global.monitoring.PrometheusMonitoringConfigTest"
.\gradlew.bat test --tests "com.rolling.api.global.alert.AlertDeduplicatorTest"
.\gradlew.bat test --tests "com.rolling.api.global.alert.SlackAlertDispatcherTest"
.\gradlew.bat test --tests "com.rolling.api.global.alert.SlackMessageFormatterTest"
.\gradlew.bat test --tests "com.rolling.api.global.alert.StartupHealthAlertRunnerTest"
.\gradlew.bat test --tests "com.rolling.api.global.alert.GlobalExceptionHandlerAlertTest"
```

추가 기준:

- 새 scheduler를 추가하면 task tracker, health indicator, Slack alert 반영 여부를 확인한다.
- alert message에는 secret, token, webhook URL, 개인정보를 포함하지 않는다.

### N+1 / Application Context

```powershell
.\gradlew.bat test --tests "com.rolling.api.NPlusOneIntegrationTest"
.\gradlew.bat test --tests "com.rolling.api.RollingApiApplicationTests"
```

추가 기준:

- 목록 API, entity graph, fetch join, paging query를 바꾸면 N+1 테스트를 고려한다.
- 설정, bean wiring, profile 조건을 바꾸면 application context 테스트를 고려한다.

## 결과 보고 기준

작업 결과에는 아래 항목을 남긴다.

- 실행한 테스트 명령
- 통과/실패 결과
- 실패했다면 실패 테스트명과 원인
- 실행하지 못했다면 이유와 남은 위험
- 추가로 실행해야 할 테스트 후보
