# Rolling API Agent Guide

## 역할

- 이 문서는 Codex가 Rolling API 백엔드에서 개발할 때 따라야 하는 작업 기준을 정의한다.
- 상세 API 계약과 도메인 규칙은 각 도메인 문서에 보존하고, 이 문서에는 작업 흐름과 개발 시 자주 확인해야 할 기준만 둔다.
- 새 도메인을 추가할 때는 이 문서를 키우지 말고 새 도메인 문서를 추가한 뒤 문서 맵에만 연결한다.

## 1. 작업 원칙

- 기존 아키텍처를 유지하는 작고 안전한 변경을 우선한다.
- 요구사항이 불명확하면 코드, 테스트, 도메인 문서 순서로 근거를 확인한다.
- 모든 코드 변경 작업 전에는 `docs/hanes/FAILURE_LOG.md`를 먼저 확인해 변경 대상 도메인 또는 작업 유형과 관련된 과거 실패 사례를 작업 계획에 반영한다.
- 변경 대상 도메인의 `docs/domain_and_spec/*.md`를 확인해 API 계약, enum raw value, request/response 필드, 인증 요구사항, 도메인 규칙을 먼저 파악한다.
- 도메인 문서와 코드가 충돌하면 임의로 추측하지 말고 충돌 지점을 명시한다.
- API 계약을 바꾸면 DTO, controller/service 테스트, 도메인 문서를 함께 갱신한다.
- 플라이웨이 문서를 새로 작성하거나 수정할 때는 문서 최상단 주석에 작성 날짜와 시간을 함께 남긴다.
- 보안, 권한, 스케줄러, 외부 연동 변경은 정상 경로뿐 아니라 실패 경로도 확인한다.
- 기존 사용자가 의존할 수 있는 응답 필드, enum raw value, paging 구조는 명시적 이유 없이 깨지 않는다.
- unrelated 파일 변경, 포맷팅, 대규모 리팩터링은 요청 범위 밖이면 하지 않는다.

## 1.1 하네스 실행 규칙

- 복잡한 기능 추가, 새 도메인 추가, API 계약 변경, 보안/권한 변경, 운영 영향이 있는 작업은 구현 전 컨텍스트, 실패 로그, 도메인 계약, 테스트 범위, 문서 동기화 후보를 먼저 정리한다.
- 구현 후에는 테스트 누락, 문서 동기화, 보안/운영 영향, Failure Log 반영 필요 여부를 확인한다.
- 전문 에이전트나 하네스 도구가 현재 세션에 명시적으로 제공된 경우에만 사용한다. 제공되지 않은 도구 이름을 전제로 작업을 멈추지 않는다.
- 컨텍스트가 큰 작업은 파일 전체를 한 번에 읽지 말고 클래스명, endpoint, public method, DTO 필드, 테스트명으로 구조를 먼저 파악한 뒤 필요한 구현부만 깊게 읽는다.
- 테스트 선택 기준은 [hanes/TEST_MATRIX.md](hanes/TEST_MATRIX.md), 과거 실패 사례는 [hanes/FAILURE_LOG.md](hanes/FAILURE_LOG.md)를 따른다.

## 2. 실행과 검증 명령

작업 기준 디렉터리:

```powershell
cd C:\rolling\rolling-spring-backend\rolling-api
```

자주 쓰는 명령:

```powershell
.\gradlew.bat test
.\gradlew.bat test --tests "com.rolling.api.domain.openmat.service.OpenMatServiceTest"
.\gradlew.bat test --tests "com.rolling.api.global.security.SecurityAuthorizationIntegrationTest"
.\gradlew.bat bootRun --args='--spring.profiles.active=local'
```

검증 기준:

- 단순 문서 수정은 테스트를 생략할 수 있다.
- 특정 도메인 로직을 바꾸면 해당 도메인의 service/controller 테스트를 우선 실행한다.
- 보안 설정, 인증 principal, 관리자 권한, 제재 필터를 바꾸면 security 관련 테스트를 실행한다.
- repository query, entity graph, paging, 목록 응답을 바꾸면 N+1 또는 정렬 안정성 영향을 확인한다.
- scheduler, health, alert 경로를 바꾸면 tracker, health indicator, Slack alert 관련 테스트를 확인한다.
- 공통 응답, 예외, 설정 파일을 바꾸면 영향 범위가 넓으므로 전체 테스트를 고려한다.
- 변경 범위별 세부 테스트 선택은 [hanes/TEST_MATRIX.md](hanes/TEST_MATRIX.md)를 따른다.

## 3. 코드 구조

- `src/main/java/com/rolling/api/domain/*`: 도메인별 controller, service, repository, dto, entity
- `src/main/java/com/rolling/api/global/security`: Spring Security, JWT, 사용자 principal, 제재 접근 필터
- `src/main/java/com/rolling/api/global/exception`: 공통 예외와 에러 응답
- `src/main/java/com/rolling/api/global/response`: 공통 API 응답 포맷
- `src/main/java/com/rolling/api/global/page`: paging 응답 모델
- `src/main/java/com/rolling/api/global/monitoring`: actuator, scheduler health, 운영 지표
- `src/main/java/com/rolling/api/global/alert`: Slack 운영 알림과 중복 방지
- `src/main/java/com/rolling/api/infra/*`: Google, Kakao, Apple, S3 등 외부 연동
- `src/test/java/com/rolling/api/domain/*`: 도메인별 테스트
- `src/test/java/com/rolling/api/global/*`: 보안, 설정, 모니터링, 알림 테스트

## 4. 백엔드 구현 규칙

- Controller는 HTTP 요청/응답, validation, 인증 사용자 주입에 집중한다.
- 비즈니스 규칙과 상태 전이는 Service에 둔다.
- Repository 메서드는 조회 의도와 필터 기준이 드러나게 작성한다.
- 상태 변경 로직은 transaction 경계, 동시성, idempotency를 함께 검토한다.
- 목록 API는 paging, 정렬, soft delete, 차단 필터, 관리자/사용자 범위를 분리해서 확인한다.
- DTO에는 API 계약상 필요한 필드만 노출하고 entity를 직접 응답으로 내보내지 않는다.
- enum은 API raw value 호환성을 깨지 않도록 추가/변경 시 클라이언트 영향을 확인한다.
- 외부 API 연동은 timeout, 실패 응답, null/빈 응답, 운영 알림 필요 여부를 함께 다룬다.
- 운영 작업은 자동 실행과 수동 실행이 같은 service 로직을 재사용해야 한다.

## 5. 보안 규칙

- `.env`, Firebase service key, Apple key, AWS credential, Slack webhook은 출력하거나 커밋하지 않는다.
- 관리자 API는 `ROLE_ADMIN` 또는 관리자 판정을 유지한다.
- 관리자 판정은 이메일, 닉네임, provider가 아니라 서버 설정의 관리자 user id 기준을 따른다.
- 공개 API와 인증 API 경계를 바꾸면 security 설정과 테스트를 함께 갱신한다.
- `X-Test-User-Id` 같은 로컬 편의 인증은 local profile 밖으로 새지 않게 한다.
- actuator 공개 범위는 health, health 하위 경로, prometheus 기준을 벗어나지 않게 검토한다.
- CORS 운영 origin은 명시 허용 방식으로 유지하고 wildcard를 넣지 않는다.
- 로그에는 access token, refresh token, webhook URL, AWS secret, Firebase key 원문을 남기지 않는다.

## 6. 테스트 정책

- 정상 경로 1개, 실패/validation 경로 1개, 권한 또는 통합 경계 1개를 우선 확인한다.
- 버그 수정은 재발을 막는 회귀 테스트를 같이 추가하는 것을 기본값으로 한다.
- 동시성 변경은 단일 요청 테스트만으로 충분하지 않으며 경합 상황을 검증한다.
- 스케줄러 변경은 반복 실행 안전성, 실패 기록, health 상태를 확인한다.
- notification 변경은 FCM 성공 여부와 DB 저장 알림의 책임을 분리해서 테스트한다.
- crawler 변경은 외부 HTML 구조 변화와 중복 저장 방지 정책을 고려한다.
- 테스트가 실행 불가하면 원인과 미검증 위험을 작업 결과에 명시한다.

## 7. 문서 Source Of Truth

- 테스트 매트릭스: [hanes/TEST_MATRIX.md](hanes/TEST_MATRIX.md)
- 실패 로그: [hanes/FAILURE_LOG.md](hanes/FAILURE_LOG.md)
- 도메인/API 공통: [domain_and_spec/shared/common-models.md](domain_and_spec/shared/common-models.md)
- 세미나: [domain_and_spec/seminar.md](domain_and_spec/seminar.md)
- 세미나 제품 계획: [domain_and_spec/seminar-product-plan.md](domain_and_spec/seminar-product-plan.md)
- 훈련 기록: [domain_and_spec/training-log.md](domain_and_spec/training-log.md)
- 훈련 기록 인사이트: [domain_and_spec/training-log-insight.md](domain_and_spec/training-log-insight.md)
- 훈련 기록 소셜: [domain_and_spec/training-log-social.md](domain_and_spec/training-log-social.md)
- 커뮤니티: [domain_and_spec/community.md](domain_and_spec/community.md)
- 알림 배지 롤아웃: [domain_and_spec/notification-badge-rollout.md](domain_and_spec/notification-badge-rollout.md)
- 관리자 웹 React API: [rollingadmin/ADMIN_WEB_REACT_API.md](rollingadmin/ADMIN_WEB_REACT_API.md)
- 커밋 규칙: [convention/COMMIT_CONVENTION.md](convention/COMMIT_CONVENTION.md)
- 공용 enum, 공용 모델, 공용 도메인 규칙의 source of truth는 로컬 공유 문서 [domain-models.md](../../../.codex-shared/domain-models.md)다. 이 경로가 없는 환경에서는 `C:\rolling\.codex-shared\domain-models.md`를 확인한다.

## 8. 문서 맵

- [shared/common-models.md](domain_and_spec/shared/common-models.md): 공통 모델, 인증/사용자/알림/공지/문의 API, 날짜/시간 형식
- [seminar.md](domain_and_spec/seminar.md): 세미나 도메인 모델 + API 스펙
- [seminar-product-plan.md](domain_and_spec/seminar-product-plan.md): 세미나 제품 범위와 출시 계획
- [training-log.md](domain_and_spec/training-log.md): 훈련 기록 도메인 모델 + API 스펙
- [training-log-insight.md](domain_and_spec/training-log-insight.md): 훈련 기록 출석 잔디와 주간/월간 인사이트 도메인/API 스펙
- [training-log-social.md](domain_and_spec/training-log-social.md): 훈련 기록 친구 관계, 친구 열람, 좋아요, 댓글, 대댓글, 댓글 알림 스펙
- [community.md](domain_and_spec/community.md): 커뮤니티 도메인 모델 + API 스펙
- [notification-badge-rollout.md](domain_and_spec/notification-badge-rollout.md): 알림 배지 기능 rollout 계획
- [ADMIN_WEB_REACT_API.md](rollingadmin/ADMIN_WEB_REACT_API.md): 관리자 웹 React API 계약
- [COMMIT_CONVENTION.md](convention/COMMIT_CONVENTION.md): 커밋 메시지 규칙

## 9. 도메인 핵심 메모

- 오픈매트는 정원이 차면 `CLOSED`, 종료 시점이 지나면 `FINISHED`가 되며, 신고 누적 3건 이상이면 신규 신청이 차단된다.
- 호스트는 자신이 주최한 오픈매트에 신청할 수 없다.
- 사용자 차단은 조회자 기준 개인화 필터이며, 차단한 작성자의 오픈매트/대회는 목록과 상세에서 숨긴다.
- 관리자 사용자 제재는 별도 운영 상태로 관리하며, `user_sanctions` 이력 테이블과 `users`의 상태 캐시를 분리해서 다룬다.
- 강한 제재는 별도 상태 추가 없이 장기 `TEMP_SUSPEND`로 운영한다.
- 로그인 사용자의 오픈매트/대회 목록·검색 요청은 `Authorization: Bearer {accessToken}`을 함께 보내야 차단 필터가 적용된다.
- 비회원/App Review 둘러보기 모드의 공개 조회 요청은 `Authorization` 헤더 없이 호출한다. 대상은 `GET /api/v1/open-mats`, `GET /api/v1/open-mats/{id}`, `GET /api/v1/tournaments`, `GET /api/v1/tournaments/{id}`, `GET /api/v1/notices`, `GET /api/v1/notices/{id}`다.
- 사용자 공유 링크는 API URL이 아니라 `https://rolling-app.com/open-mats/{id}`, `https://rolling-app.com/tournaments/{id}`를 사용한다. Flutter 앱은 딥링크 진입 후 공개 상세 API를 다시 조회한다.
- 오픈매트 신청/작성/수정/삭제/신고, 대회 작성/수정/삭제/신고, 알림, 마이페이지, 문의는 계속 계정 기반 기능이며 비회원 UI에서는 로그인 필요 안내로 처리한다.
- 알림의 source of truth는 FCM 성공 여부가 아니라 백엔드 `Notification` 저장 데이터다.
- 공지사항은 일반 사용자 앱에서 읽기 전용 기능으로 다룬다.
- 문의 도메인 명칭은 `Inquiry`로 통일하며, 첫 답변 완료 시 `INQUIRY_ANSWERED` 알림을 저장한다.

## 10. 프론트엔드 계약 메모

### 기술 스택

- Framework: Flutter
- Language: Dart
- Pattern: MVVM
- State Management: GetX
- Directory Structure: Feature-based
- HTTP Client: `http`
- Local Storage: `flutter_secure_storage`

### 구현 메모

- 날짜/시간은 ISO 8601 문자열을 `DateTime`으로 파싱한다.
- `Date` 타입 값은 `YYYY-MM-DD` 그대로 다룬다.
- enum은 Flutter 내부 camelCase로 관리하고 API 송수신 시 raw value 매핑을 명시적으로 둔다.
- `OpenMatModel.reported`는 서버 응답 필드 그대로 신뢰한다.
- `NotificationModel.readAt == null`이면 미읽음이다.
- 로그인 버튼/UI는 `GOOGLE`, `KAKAO`, `APPLE` 3개 기준으로 설계하되, 서버 호출 가능 여부는 구현 상태를 따른다.
- 공지사항은 별도 작성 플로우 없이 `목록 -> 상세` 읽기 흐름만 잡으면 된다.
- 공지사항 목록 item은 `id`, `title`, `content`, `authorName`, `createdAt`를 사용한다.
- 공지사항 상세는 같은 필드를 그대로 사용해 상세 페이지를 구성하면 된다.
- 문의는 인증 사용자 기준으로 `목록 -> 상세 -> 답변 확인` 흐름을 구현하면 된다.

### 주의사항

- 현재 `/users/me` 수정 API는 `phone` 수정 미지원이다.
- 현재 `/open-mats/my`는 배열이 아니라 페이징 응답이다.
- 현재 오픈매트 생성/수정 요청에는 `region`이 포함된다.
- 현재 오픈매트 생성/수정 요청에는 `latitude`, `longitude`가 선택 필드로 포함된다. 좌표는 nullable이며, 주소 기반 geocoding은 저장 API가 아니라 별도 지도 API에서 수행한다.
- 현재 우편번호 WebView에서 선택한 주소의 좌표 변환은 인증 API `GET /api/v1/maps/kakao/geocode?address={address}`로 수행한다.
- `https://rolling-app.com/maps/kakao/openmat.html`은 Nginx가 정적 HTML로 서빙하고 React SPA fallback 대상이 아니다.
- 오픈매트 작성자 관리 UI는 현재 상세 화면 안에서 바로 노출한다.
- 작성자 전용 관리 범위는 `참가자 강제 취소`, `모집 상태 수동 변경(RECRUITING, CLOSED)`이다.
- 현재 클라이언트는 작성자 권한을 상세 응답의 `hostId == 현재 사용자 id`로 판단한다.
- 다만 작성자 관리 API는 아직 `api-spec.json`에 정식 반영되지 않았다. 프론트는 현재 연결 기준으로 선반영했고, 최종 완료 판단은 백엔드 계약/실서버 검증 후 닫는다.
- 현재 로그인 API 요청 허용값은 `GOOGLE`, `KAKAO`, `APPLE`이다. `APPLE`은 iOS 네이티브 로그인에서 받은 `identityToken`을 `accessToken` 필드로 전달한다.
- 현재 FCM은 서버 저장 데이터 기반 알림함과 함께 동작한다. 다만 실제 릴리스 전에는 iOS 실기기 수신, 권한 거부 상태 UX, 리뷰어 안내 문구까지 별도 확인이 필요하다.
- 현재 사용자 전역 푸시 설정은 `/users/me.settings.pushNotificationEnabled`와 `PATCH /api/v1/users/me/settings`로 관리한다.
- 알림 권한을 거부해도 오픈매트/대회/공지 핵심 조회와 신청 흐름은 막히지 않게 설계한다.

## 11. 운영 작업 규칙

- 새 scheduler를 추가하면 `ScheduledTaskTracker`, `SchedulerHealthIndicator`, Slack alert 반영 여부를 함께 검토한다.
- 자동 실행과 관리자 수동 실행은 같은 service 로직을 재사용한다.
- cron은 `Asia/Seoul` 기준인지 명시하고, 시스템 기본 시간대에 암묵 의존하지 않는다.
- 실패를 catch 후 무시하지 말고 추적기와 운영 알림에 남긴다.
- 성공 summary는 운영자가 재실행 필요 여부를 판단할 수 있는 수준으로 작성한다.

## 12. Git과 커밋

- 기능 추가, 버그 수정, 리팩터링, 문서 수정 등 작업 단위가 달라지면 목적에 맞는 새 브랜치를 사용한다.
- 커밋 제목과 본문은 한글로 작성한다.
- 커밋 메시지 형식은 `<타입>(<적용 범위>): <제목>`을 사용한다.
- 예시: `feat(openmat): 오픈매트 신청 검증 보강`
- 타입은 `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`, `ci` 중 하나를 사용한다.
- 본문에는 변경 이유와 방식을 하이픈 목록으로 구체적으로 적는다.
