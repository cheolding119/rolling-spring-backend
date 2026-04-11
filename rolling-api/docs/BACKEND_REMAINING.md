### Phase 4. 관리자 페이지 준비
- [x] 보안 role 모델 추가 (`USER`, `ADMIN`) 및 관리자 권한 기준 정리
- [x] 관리자 인증/권한 정책을 `admin.user-ids` + JWT 기준으로 확정 (`X-Crawler-Admin-Key` 제거)
- [x] 관리자 페이지에서 사용할 백엔드 API 목록 확정
- [x] 대회단체 크롤링 API는 기존 `POST /api/v1/tournaments/crawl`를 `ROLE_ADMIN` 기준으로 사용
- [x] 공지사항 작성/수정/삭제는 기존 `POST /api/v1/notices`, `PUT /api/v1/notices/{id}`, `DELETE /api/v1/notices/{id}`를 `ROLE_ADMIN` 기준으로 사용
- [~] 남은 범위는 관리자 페이지 프론트 구현(라우팅, 진입 가드, ADMIN API 연결)

결과 메모:
- 현재 필요한 관리자 백엔드 API는 이미 존재한다: `POST /api/v1/tournaments/crawl`, `POST /api/v1/notices`, `PUT /api/v1/notices/{id}`, `DELETE /api/v1/notices/{id}`.
- 위 API는 모두 `Authorization: Bearer {accessToken}` + `ROLE_ADMIN` 기준으로 동작한다.
- `X-Crawler-Admin-Key`와 `tournament.crawler.admin-key` 기반 우회 인증은 제거됐다.
- 오픈매트 수정/삭제 accessToken 필수화는 함께 정리된 보안 선행 작업이다.
- 현재 Phase 4의 실질적 남은 작업은 신규 백엔드 개발보다 관리자 페이지 프론트 구현이다.

완료 기준:
- 관리자 페이지에서 필요한 백엔드 API와 인증 방식이 문서로 확정됨
- 남은 작업이 관리자 페이지 프론트 구현이라는 점이 명확해짐

## Unit B-01. Auth 회원 탈퇴
- [x] `DELETE /api/v1/auth/withdraw` 컨트롤러/서비스 구현
- [x] 탈퇴 요청 시 즉시 삭제하지 않고, **다음날 21:00(Asia/Seoul)** 으로 예약
- [x] 예약된 탈퇴를 취소하는 API 구현 (`POST /api/v1/auth/withdraw/cancel`)
- [x] 예약 시각 도달 시 배치로 최종 탈퇴 실행 (`@Scheduled`)
- [x] 사용자 개인정보 삭제 정책 반영 (최종 탈퇴 시점에 제거)
- [x] 리프레시 토큰 무효화 처리
- [x] Swagger 및 API 명세 동기화
- [x] 단위/통합 테스트 작성

완료 기준:
- 인증 사용자 기준 탈퇴 요청 성공 응답(`withdrawalPending=true`, `scheduledAt` 포함)
- 예약 취소 요청 시 탈퇴 예약 해제(`withdrawalPending=false`)
- 예약 시각 도달 후 최종 탈퇴 실행 및 토큰 접근 차단

## Unit B-02. User 내 정보 수정 v2 (nickname, beltColor)
- [x] `PUT /api/v1/users/me`의 수정 범위를 `nickname`, `beltColor`로 고정
- [x] 응답 DTO/Swagger/문서 업데이트
- [x] 테스트 작성

완료 기준:
- `nickname`, `beltColor` 수정이 정상 동작

## Unit B-03. User FCM + 차단
- [x] `POST /api/v1/users/me/fcm` 구현
- [x] `POST /api/v1/users/{id}/block` 구현
- [x] `DELETE /api/v1/users/{id}/block` 구현
- [x] 자기 자신 차단 방지/존재하지 않는 사용자 예외 처리
- [x] Swagger 및 테스트 작성
- [x] FCM 토큰 저장 구조를 `UserDevice` 1:N으로 확장
- [x] 동일 FCM 토큰 재등록 시 기존 디바이스 레코드 재사용 및 현재 사용자에게 재연결

완료 기준:
- FCM 토큰 저장, 차단/해제 API가 idempotent하게 동작
- 사용자 1명이 여러 디바이스 FCM 토큰을 저장할 수 있음

## Unit B-04. Report 공통 도메인
- [x] `Report` 엔티티/리포지토리/서비스/컨트롤러 기본 골격 구현
- [x] Enum: `ReportTargetType`, `ReportReason` 반영
- [x] 동일 유저 동일 대상 중복 신고 방지 제약
- [x] 자기 게시글 신고 방지 공통 검증
- [x] 공통 에러 코드/메시지 정리

완료 기준:
- OpenMat/Tournament 신고 로직에서 재사용 가능한 공통 모듈 완성

## Unit B-05. OpenMat 상태 자동화/정합성
- [x] 정원 도달 시 `RECRUITING -> CLOSED` 자동 전환
- [x] 신청 취소로 여유 발생 시 상태 처리 정책 확정 및 반영
- [x] `endDateTime` 경과 시 `FINISHED` 자동 전환 (스케줄러 + 조회시 보정)
- [x] 리스트 정렬/필터 정책을 명세와 일치화
- [x] 신고 임계치 정책 3건 기준으로 정합성 확정

## Unit B-06. OpenMat 작성자 관리 API
- [x] 참가자 목록 조회 API
- [x] 참가자 강제 취소 API
- [x] 모집 상태 수동 변경 API (`RECRUITING`, `CLOSED`)
- [x] 작성자 권한 검증
- [x] Swagger/테스트/문서 반영

결과 메모:
- `GET /api/v1/open-mats/{id}/participants`로 로그인한 사용자가 참가자 목록을 조회할 수 있다.
- `DELETE /api/v1/open-mats/{id}/participants/{participantUserId}`로 작성자가 특정 참가자를 강제 취소할 수 있다.
- `PATCH /api/v1/open-mats/{id}/status`로 작성자가 모집 상태를 `RECRUITING`, `CLOSED`로 수동 변경할 수 있다.
- 수동 마감 상태는 내부 `manualClosed` 플래그로 유지되어, 참가 취소로 자리가 나도 작성자가 다시 열기 전까지 `CLOSED`를 유지한다.
- 상태 변경/참가자 관리 API는 모두 작성자 권한을 검증하고, 서비스 테스트와 API 문서를 함께 반영했다.

완료 기준:
- 작성자 관리 기능 전체가 API로 노출되고 권한이 보장됨

## Unit B-07. OpenMat 신고 API
- [x] OpenMat 신고 엔드포인트 구현
- [x] `Report` 공통 모듈과 연동
- [x] 신고 3건 이상 시 신규 신청 차단
- [x] 상세/리스트에서 신고 상태 표기용 필드 정책 확정
- [x] 테스트 작성

완료 기준:
- 중복 신고/자기 신고가 차단되고 3건 누적 정책이 적용됨

## Unit B-08. Tournament Core API
- [x] Tournament 엔티티/리포지토리/서비스/컨트롤러 구현
- [x] `GET /api/v1/tournaments` 페이징/정렬 정책 구현
- [x] `GET /api/v1/tournaments/{id}` 구현
- [x] `POST/PUT/DELETE /api/v1/tournaments` 구현
- [x] Swagger/명세 동기화

완료 기준:
- 대회 CRUD + 조회 정책이 프론트 연동 가능한 수준으로 완료

완료 기준:
- 신고 누적에 따른 외부 링크 차단이 일관되게 동작

## Unit B-10. 소셜 Provider 정책 정합성
- [ ] `SocialProvider` 요구사항 범위 확정 (`KAKAO`, `GOOGLE`, `APPLE`)
- [ ] 구현/문서 간 불일치 제거
- [x] 미지원 provider 요청 시 에러 스펙 확정

완료 기준:
- 코드/Swagger/문서에 동일 provider 정책이 반영됨


## Unit B-14. Notice 조회 API MVP
- [x] `notice` 도메인 패키지 추가 (`controller`, `dto`, `entity`, `repository`, `service`)
- [x] `Notice` 엔티티 추가 (`title`, `content`, `authorName`, `createdBy`) 및 `BaseTimeEntity` 기반 `createdAt`, `updatedAt` 관리
- [x] 응답 DTO `NoticeResponse` 추가 (`id`, `title`, `content`, `authorName`, `createdAt`, `updatedAt`)
- [x] `NoticeRepository` 목록/상세 조회 메서드 추가
- [x] `NoticeService` 목록/상세 조회 로직 구현
- [x] `GET /api/v1/notices` 구현 (무인증, 기본 `size=20`, 기본 정렬 `createdAt DESC`)
- [x] `GET /api/v1/notices/{id}` 구현 (무인증, 미존재 시 `NOT_FOUND`)
- [x] Swagger/OpenAPI 문서 반영
- [x] `docs/AGENTS.md`와 실제 응답 필드/정렬 규약 동기화
- [x] 서비스 테스트 작성 (최신순 정렬, 페이지네이션, 미존재 ID 예외)
- [x] 컨트롤러 또는 통합 테스트 작성 (무인증 접근 가능, 공통 응답 래핑 검증)
- [x] 로컬 검증용 샘플 공지 데이터 준비 방식 정리 (테스트 fixture 또는 수동 insert 기준)

범위 메모:
- 이번 Unit은 일반 사용자 앱용 `조회 전용` API만 포함한다.
- 목록/상세 응답은 같은 필드셋을 사용한다.
- 운영 작성/수정/삭제 API는 별도 Unit으로 분리한다.
- 운영 생성 API 전까지 로컬 검증용 공지 데이터는 테스트 fixture 또는 수동 DB insert로 준비한다.

완료 기준:
- 앱에서 공지사항 목록 페이지와 상세 페이지를 바로 연동할 수 있다.
- 목록은 페이징 + 최신순 정렬로 동작한다.
- 상세 조회 시 존재하지 않는 ID는 `NOT_FOUND`를 반환한다.

## Unit B-15. Notice 운영 API (후속)
- [x] 운영 전용 인증 정책을 `ROLE_ADMIN` accessToken 기준으로 확정 (`admin.user-ids` 사용, admin key 제거)
- [x] `JwtAuthenticationFilter`와 `UserPrincipal`을 `USER`/`ADMIN` role 기반으로 정리하고 공지사항 운영 API에도 공통 적용
- [x] `POST /api/v1/notices` 운영 생성 API 구현
- [x] `PUT /api/v1/notices/{id}` 운영 수정 API 구현
- [x] `DELETE /api/v1/notices/{id}` 운영 삭제 정책 확정 및 구현 (`hard delete`)
- [x] `createdBy`와 `authorName` 저장 규칙 확정 (운영자 식별자와 노출용 이름의 매핑 방식 포함)
- [x] 운영 API Swagger/Apidog 계약 정리
- [x] 운영 API 테스트 작성 (`ROLE_ADMIN`/일반 사용자/미인증, CRUD 기본 흐름)

범위 메모:
- 일반 사용자 앱은 계속 `조회 전용`으로 사용한다.
- 운영 API도 같은 JWT 인증 플로우를 사용하되 `ROLE_ADMIN`으로 제한한다.
- 운영 생성 시 `createdBy`가 없으면 `authorName`으로 저장한다.
- 삭제는 DB에서 바로 제거하는 `hard delete`로 구현한다.

완료 기준:
- 운영자는 Apidog, 관리자 페이지, 운영 도구에서 ADMIN accessToken으로 공지사항 생성/수정/삭제를 수행할 수 있다.
- 일반 사용자 조회 API와 운영 API의 권한 경계가 분리된다.

## Unit B-13. FCM MVP 푸시 연동
- [x] Firebase Admin SDK 의존성 추가
- [x] Firebase 설정 프로퍼티 및 `FirebaseApp` 초기화 구성 추가
- [x] `PushNotificationService` 인터페이스 및 FCM 구현체 추가
- [x] Firebase 비활성화 시 `NoOpPushNotificationService` fallback 추가
- [x] `UserDevice` 기준 다중 토큰 조회 및 멀티캐스트 발송 구현
- [x] `OPEN_MAT_UPDATED`, `OPEN_MAT_DELETED` payload 규칙 정의
- [x] 오픈매트 일정/장소 변경 시 참가자 대상 푸시 이벤트 연결
- [x] 오픈매트 삭제 시 참가자 대상 푸시 이벤트 연결
- [x] 무효 토큰(`UNREGISTERED`, `INVALID_ARGUMENT`) 자동 정리 구현
- [x] OpenMat/FCM 푸시 관련 테스트 작성
- [x] 실제 Android 디바이스 대상 수정/삭제 푸시 수신 검증
- [ ] 실제 iOS 디바이스 대상 수정/삭제 푸시 수신 검증
- [x] 포그라운드/백그라운드/종료 상태별 푸시 수신 및 탭 라우팅 검증 기록 정리
- [x] 테스트용 사용자 2명 이상 또는 디바이스 2대 이상 기준 멀티 디바이스 수신 검증

완료 기준:
- 서버가 `UserDevice` 기준으로 특정 사용자의 모든 디바이스에 FCM을 발송할 수 있음
- 오픈매트 수정/삭제 시 참여자 대상 푸시가 트랜잭션 커밋 이후 발송됨
- 실제 디바이스 수신 검증까지 끝나면 MVP 범위 완료

## Unit B-16. FCM 토큰 라이프사이클 정합성
- [x] 로그아웃 시 현재 디바이스 FCM 토큰 처리 정책 확정 (`삭제`, `비활성화`, `유지 후 재연결` 중 택1)
- [x] 탈퇴 예약/최종 탈퇴 시 `user_devices` 정리 및 푸시 발송 차단 정책 반영
- [x] `DELETE /api/v1/users/me/fcm` API 추가
- [x] `platform`, `deviceId`, `appVersion`, `updatedAt` 등 디바이스 메타데이터 저장 범위 확정
- [x] 동일 단말에서 사용자 전환 시 토큰 재연결/정리 시나리오 테스트 작성
- [x] Swagger + `docs/AGENTS.md` + `docs/FCM_INTEGRATION_CHECKLIST.md` 동기화

완료 기준:
- 로그아웃/탈퇴 이후 이전 사용자에게 푸시가 가지 않음
- 동일 단말 재로그인/다른 계정 로그인 시 토큰 소유권이 일관되게 정리됨
- 운영자가 토큰 상태를 추적할 수 있을 정도의 최소 메타데이터가 확보됨

## Unit B-17. 푸시 릴리스 검증 게이트
- [x] `FirebaseApp` 초기화 스모크 테스트 또는 운영 전 점검 절차 추가
- [x] FCM 발송 실패 로깅/모니터링 기준 정리 (`UNREGISTERED`, `INVALID_ARGUMENT`, 기타 예외`)
- [x] 재시도 정책 또는 `재시도 안 함` 정책을 명시적으로 문서화
- [x] Android/iOS 각각 포그라운드/백그라운드/종료 상태 검증 표 작성
- [~] 알림 클릭 후 `route`, `targetId`, `404 fallback` 동작 회귀 테스트 보강
- [ ] 알림 권한 거부 상태에서도 핵심 기능 사용 가능 여부 확인 및 릴리스 메모 반영

범위 메모:
- 이 Unit은 서버 구현 자체보다 `릴리스 증빙`과 `운영 안정성` 확보가 목적이다.
- 심사 제출 전 `works on my device` 수준을 넘는 검증 기록을 남긴다.

완료 기준:
- 플랫폼별 상태 매트릭스와 예외 케이스까지 검증 근거가 남아 있음
- 장애 발생 시 토큰 정리/로그 확인/재현 경로가 문서로 남아 있음

## Unit B-18. 스토어 심사 제출 준비
- [ ] 개인정보처리방침에 푸시 토큰 수집, 알림 발송 목적, 보관/삭제 정책 반영
- [ ] App Store `App Privacy` / Google Play `Data safety` 제출 항목 정리
- [ ] 리뷰어용 테스트 계정, 로그인 방법, 재현 절차, 리뷰 노트 준비
- [ ] iOS 출시 범위 기준 `APPLE` 로그인 대응 여부 최종 결정
- [ ] 앱 내 회원 탈퇴 UI 노출 여부와 백엔드 탈퇴 API 연결 상태 확인
- [ ] 알림 권한을 거부해도 핵심 기능 사용이 막히지 않는다는 점 확인
- [ ] `docs/FCM_INTEGRATION_CHECKLIST.md`와 본 문서의 완료 상태를 릴리스 직전 한 번 더 맞춤

범위 메모:
- 이 Unit은 백엔드만으로 끝나지 않는 `크로스 기능` 릴리스 준비 항목을 묶는다.
- Android 푸시 동작 확인만으로는 스토어 심사 통과를 보장할 수 없으므로 별도 게이트로 관리한다.

완료 기준:
- 스토어 제출 메타데이터, 리뷰 접근 정보, 개인정보 고지가 모두 준비됨
- 로그인/탈퇴/푸시 권한 관련 정책 이슈로 즉시 리젝될 만한 공백이 없음

## Unit B-11. 테스트 보강
- [~] 서비스 단위 테스트 (Auth/User/OpenMat/Report 일부 보강, FCM/UserDevice/OpenMat 푸시 테스트 추가)
- [x] 보안/권한 통합 테스트
- [x] 오픈매트 신청 동시성 테스트(정원 경계)
- [x] 회귀 테스트 케이스 문서화

결과 메모:
- 공개/인증/관리자 전용 엔드포인트 경계를 `SecurityAuthorizationIntegrationTest`로 고정했다.
- 오픈매트 정원 1명 경계에서 동시 신청 시 1명만 성공하는 DB 잠금 기반 테스트를 추가했다.
- `docs/B-11_REGRESSION_CASES.md`에 자동/수동 회귀 점검 기준을 정리했다.

완료 기준:
- 핵심 시나리오가 자동 테스트로 커버됨

## Unit B-12. 문서/운영 정리
- [~] 엔드포인트 추가/수정 시 Swagger + `docs/AGENTS.md` 동시 갱신
- [~] 배포 프로파일별 설정 분리 (`firebase` 환경변수 구조 반영)
- [~] 최종 API 변경 로그 정리
- [x] `docs/FCM_INTEGRATION_CHECKLIST.md` 작성 및 현재 구현 상태 반영

완료 기준:
- 개발/운영 환경에서 문서와 실행 코드가 일치

## 추천 구현 순서
1. B-02
2. B-01
3. B-03
4. B-04
5. B-05
6. B-06
7. B-07
8. B-08
9. B-09
10. B-10
11. B-14
12. B-15
13. B-13
14. B-16
15. B-17
16. B-11
17. B-18
18. B-12










