# iOS App Review 비회원 접근 Backend 작업 체크리스트

## 1. 사전 정보

이 문서는 App Store Review Guideline 5.1.1(v) 리젝 대응을 위한 Spring 백엔드 작업 범위를 정리한다.

Apple의 리젝 사유는 앱이 `오픈매트`와 `국내 대회 정보` 조회 전에 회원가입 또는 로그인을 요구한다는 것이다. 현재 백엔드 구현은 공개 조회 API를 이미 인증 없이 허용하고 있으므로, 백엔드의 주요 역할은 정책 변경보다는 다음 세 가지를 보증하는 것이다.

1. 공개 조회 API가 운영 환경에서도 토큰 없이 성공한다.
2. 공개 조회 응답에 불필요한 개인정보가 포함되지 않는다.
3. 인증이 필요한 계정 기반 기능은 계속 보호된다.

즉, 이번 대응의 주 구현 대상은 Flutter 진입 UX이고, 백엔드는 공개 API 계약과 보안 경계를 검증하는 역할이다.

## 2. 현재 Backend 동작

`SecurityConfig` 기준 공개 조회 API:

- `GET /api/v1/open-mats`
- `GET /api/v1/open-mats/{id}`
- `GET /api/v1/tournaments`
- `GET /api/v1/tournaments/{id}`
- `GET /api/v1/notices`
- `GET /api/v1/notices/{id}`

인증 필요 API:

- `POST /api/v1/open-mats`
- `PUT /api/v1/open-mats/{id}`
- `DELETE /api/v1/open-mats/{id}`
- `POST /api/v1/open-mats/{id}/apply`
- `DELETE /api/v1/open-mats/{id}/apply`
- `GET /api/v1/open-mats/my`
- `GET /api/v1/open-mats/my-hosting`
- `POST /api/v1/open-mats/{id}/report`
- `POST /api/v1/tournaments`
- `PUT /api/v1/tournaments/{id}`
- `DELETE /api/v1/tournaments/{id}`
- `POST /api/v1/tournaments/{id}/report`
- `GET /api/v1/notifications`
- `GET /api/v1/users/me`
- `GET /api/v1/inquiries`
- `POST /api/v1/inquiries`

로그인 사용자의 공개 조회 요청은 Authorization 헤더가 있으면 사용자 차단 필터 등 개인화가 적용된다. 비로그인 사용자는 Authorization 없이 공개 목록과 상세를 조회한다.

## 3. 목표

- 공개 정보 조회 API는 토큰 없이 안정적으로 성공한다.
- 계정 기반 API는 토큰 없이 실패한다.
- 공개 응답에는 이메일, 전화번호 등 개인정보가 노출되지 않는다.
- App Review 대응을 위해 백엔드 계약이 문서와 테스트로 설명 가능해야 한다.
- Flutter 비회원 모드가 백엔드 401 에러 없이 공개 조회를 사용할 수 있어야 한다.

## 4. 이번 대응에서 하지 않을 일

- 앱 최초 진입 UX를 백엔드에서 해결하지 않는다.
- 비회원용 임시 토큰을 발급하지 않는다.
- 익명 계정을 자동 생성하지 않는다.
- 공개 조회를 위해 로그인 API를 우회 호출하지 않는다.
- 인증이 필요한 신청/신고/작성 기능을 비회원에게 열지 않는다.

## 5. Phase 0. 정책 확인

- [x] `docs/auth/APP_REVIEW_GUEST_ACCESS_PLAN.md`를 기준 문서로 확인한다.
- [x] 이번 리젝 대응의 권장안이 `로그인 화면에 비회원 진입 버튼 추가`임을 확인한다.
- [x] 백엔드는 공개 조회 정책을 유지하고 검증/문서화를 담당한다는 점을 확인한다.
- [x] 오픈매트와 대회 정보 조회는 계정 기반 기능이 아니라 공개 정보 기능으로 분류한다.
- [x] 신청, 작성, 신고, 개인화 기능은 계정 기반 기능으로 분류한다.

## 6. Phase 1. SecurityConfig 공개/인증 경계 점검

- [x] `GET /api/v1/open-mats`가 `permitAll`인지 확인한다.
- [x] `GET /api/v1/open-mats/{id}`가 `permitAll`인지 확인한다.
- [x] `GET /api/v1/tournaments`가 `permitAll`인지 확인한다.
- [x] `GET /api/v1/tournaments/{id}`가 `permitAll`인지 확인한다.
- [x] `GET /api/v1/notices`가 `permitAll`인지 확인한다.
- [x] `GET /api/v1/notices/{id}`가 `permitAll`인지 확인한다.
- [x] `/api/v1/open-mats/my`가 공개 상세보다 먼저 매칭되어 인증 필요로 유지되는지 확인한다.
- [x] 관리자 API가 `ROLE_ADMIN` 보호를 유지하는지 확인한다.
- [x] 신청/작성/수정/삭제/신고 API가 토큰 없이 열리지 않았는지 확인한다.

## 7. Phase 2. 공개 응답 개인정보 점검

### 오픈매트

- [x] 목록 응답에 작성자 이메일이 포함되지 않는지 확인한다.
- [x] 목록 응답에 작성자 전화번호가 포함되지 않는지 확인한다.
- [x] 상세 응답에 참가자 전화번호가 포함되지 않는지 확인한다.
- [x] 상세 응답에 신청자 개인정보가 과도하게 포함되지 않는지 확인한다.
- [x] 공개 응답에 필요한 작성자 표시명은 `hostNickname` 수준으로 제한되는지 확인한다.

### 대회

- [x] 목록 응답에 작성자 이메일이 포함되지 않는지 확인한다.
- [x] 목록 응답에 작성자 전화번호가 포함되지 않는지 확인한다.
- [x] 상세 응답에 작성자 개인정보가 포함되지 않는지 확인한다.
- [ ] 포스터 URL이 공개 접근 가능한 이미지 URL인지 확인한다.
- [x] 외부 접수 링크가 개인정보 없이 단순 링크로 제공되는지 확인한다.

### 공지사항

- [x] 목록 응답에 운영자 개인 이메일이 포함되지 않는지 확인한다.
- [x] 상세 응답에 운영자 개인 전화번호가 포함되지 않는지 확인한다.
- [x] 앱 노출 작성자는 `authorName` 수준으로 제한되는지 확인한다.

## 8. Phase 3. 테스트 보강

- [x] 토큰 없이 `GET /api/v1/open-mats`가 200을 반환하는 테스트를 확인 또는 추가한다.
- [x] 토큰 없이 `GET /api/v1/open-mats/{id}`가 200을 반환하는 테스트를 확인 또는 추가한다.
- [x] 토큰 없이 `GET /api/v1/tournaments`가 200을 반환하는 테스트를 확인 또는 추가한다.
- [x] 토큰 없이 `GET /api/v1/tournaments/{id}`가 200을 반환하는 테스트를 확인 또는 추가한다.
- [x] 토큰 없이 `GET /api/v1/notices`가 200을 반환하는 테스트를 확인 또는 추가한다.
- [x] 토큰 없이 `GET /api/v1/notices/{id}`가 200을 반환하는 테스트를 확인 또는 추가한다.
- [x] 토큰 없이 `GET /api/v1/open-mats/my`가 401을 반환하는 테스트를 확인 또는 추가한다.
- [x] 토큰 없이 `POST /api/v1/open-mats/{id}/apply`가 401을 반환하는 테스트를 확인 또는 추가한다.
- [x] 토큰 없이 `POST /api/v1/open-mats/{id}/report`가 401을 반환하는 테스트를 확인 또는 추가한다.
- [x] 토큰 없이 `POST /api/v1/tournaments/{id}/report`가 401을 반환하는 테스트를 확인 또는 추가한다.
- [x] 일반 사용자 토큰으로 관리자 API가 403을 반환하는 테스트가 유지되는지 확인한다.

## 9. Phase 4. 운영 환경 Smoke Test

- [ ] 운영 또는 심사 대상 API base URL에서 `GET /api/v1/open-mats`를 토큰 없이 호출해 200을 확인한다.
- [ ] 운영 또는 심사 대상 API base URL에서 `GET /api/v1/tournaments`를 토큰 없이 호출해 200을 확인한다.
- [ ] 운영 또는 심사 대상 API base URL에서 `GET /api/v1/notices`를 토큰 없이 호출해 200을 확인한다.
- [ ] 실제 존재하는 오픈매트 ID로 상세 조회 200을 확인한다.
- [ ] 실제 존재하는 대회 ID로 상세 조회 200을 확인한다.
- [ ] 실제 존재하는 공지 ID로 상세 조회 200을 확인한다.
- [ ] 토큰 없이 `GET /api/v1/open-mats/my` 호출 시 401이 반환되는지 확인한다.
- [ ] 토큰 없이 오픈매트 신청 API 호출 시 401이 반환되는지 확인한다.
- [ ] 토큰 없이 신고 API 호출 시 401이 반환되는지 확인한다.
- [ ] 응답 헤더와 본문이 Flutter 클라이언트에서 파싱 가능한 JSON 형태인지 확인한다.

## 10. Phase 5. 프론트 연동 지원

- [x] Flutter가 비회원 상태에서 Authorization 헤더 없이 공개 GET을 호출하는지 확인할 수 있도록 API 계약을 공유한다.
- [ ] 비회원 상태에서 401이 발생하면 해당 요청이 공개 API인지 인증 API인지 함께 분류한다.
- [ ] 공개 API에서 401이 발생하면 `SecurityConfig` 매칭 순서와 배포 환경 설정을 우선 점검한다.
- [ ] 인증 API에서 401이 발생하면 Flutter가 로그인 필요 안내로 처리하는지 확인한다.
- [ ] App Review Notes에 넣을 백엔드 정책 설명을 프론트 문서와 맞춘다.

## 11. Phase 6. 문서화와 릴리스 준비

- [x] `AGENTS.md`의 공개 API 인증 정책과 실제 구현이 일치하는지 확인한다.
- [x] App Review 대응 문서에 공개 조회/로그인 필요 기능 범위가 명확히 적혀 있는지 확인한다.
- [ ] 운영 smoke test 결과를 릴리스 체크리스트에 남긴다.
- [ ] 재심사 제출 전에 Flutter QA 결과와 백엔드 smoke test 결과를 함께 확인한다.
- [ ] App Review Notes에 사용할 문구를 제품/프론트 담당자와 합의한다.

## 12. 완료 기준

- [ ] 공개 조회 API가 토큰 없이 운영 환경에서 성공한다.
- [x] 인증 필요 API가 토큰 없이 열리지 않는다.
- [x] 공개 응답에 불필요한 개인정보가 없다.
- [ ] Flutter 비회원 모드가 백엔드 공개 API와 정상 연동된다.
- [x] 재심사 설명에 백엔드 정책을 명확히 적을 수 있다.
- [x] 기존 로그인 사용자와 관리자 권한 정책이 깨지지 않는다.

## 13. 진행 결과 (2026-05-03)

### 완료

- [x] `docs/auth/APP_REVIEW_GUEST_ACCESS_PLAN.md` 기준으로 백엔드 역할이 공개 API 계약 검증/문서화임을 확인했다.
- [x] `SecurityConfig`에서 공개 조회 API 6개가 `permitAll`임을 확인했다.
- [x] `/api/v1/open-mats/my`, `/api/v1/open-mats/my-hosting`이 공개 상세 경로보다 먼저 인증 필요로 매칭됨을 확인했다.
- [x] 신청/신고/작성/수정/삭제와 관리자 API가 비인증 사용자에게 열리지 않음을 통합 테스트로 확인했다.
- [x] 공개 응답 DTO를 점검했다. 오픈매트 공개 응답은 `hostNickname`, `hostInstagramId` 수준이고 이메일/전화번호/참가자 상세 개인정보를 포함하지 않는다. 대회 공개 응답은 `posterUrl`, `applyLink` 중심이고 작성자 이메일/전화번호를 포함하지 않는다. 공지사항 공개 응답은 `authorName` 중심이고 운영자 개인 이메일/전화번호를 포함하지 않는다.
- [x] `SecurityAuthorizationIntegrationTest`에 토큰 없는 공개 조회 6개 경로 검증을 추가했다.
- [x] `SecurityAuthorizationIntegrationTest`에 토큰 없는 오픈매트 신청, 오픈매트 신고, 대회 신고 401 검증을 추가했다.
- [x] 일반 사용자 토큰으로 관리자 API가 403을 반환하는 기존 검증이 유지됨을 확인했다.
- [x] `docs/AGENTS.md`에 비회원/App Review 둘러보기 공개 API 호출 계약을 반영했다.
- [x] 공유 계약 문서 `C:\rolling\.codex-shared\api-spec.md`, `C:\rolling\.codex-shared\domain-models.md`를 대조했다. 이번 작업은 외부 API 스키마/엔드포인트 변경이 아니라 테스트와 운영 문서 보강이므로 공유 계약 문서 수정은 하지 않았다.

### 직접 검증

- `.\gradlew.bat test --tests com.rolling.api.global.security.SecurityAuthorizationIntegrationTest`
- 결과: 성공

### 운영/연동 확인 필요

- [ ] 운영 또는 심사 대상 API base URL에서 토큰 없는 공개 조회 6개 smoke test를 수행한다.
- [ ] Flutter 비회원 모드가 공개 조회 API에 `Authorization` 헤더를 붙이지 않는지 네트워크 로그로 확인한다.
- [ ] App Review Notes에 `비회원으로 둘러보기 -> 오픈매트/대회/공지 조회 가능, 계정 기반 액션만 로그인 필요` 흐름을 반영한다.
