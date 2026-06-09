## 전체 진행 체크리스트

- [x] PHASE 1. 공통 규칙 및 인증 기반
- [x] PHASE 2. 관리자 진입 및 세션 처리
- [x] PHASE 3. 관리자 진입 화면 구성
- [x] PHASE 4. 대시보드 홈 구성
- [x] PHASE 5. 공지사항 관리
- [x] PHASE 6. 문의 관리
- [x] PHASE 7. 신고 관리
- [x] PHASE 8. 대회 운영
- [x] PHASE 9. 상태 화면 및 구현 마무리 체크

# Rolling 관리자 웹 React 연동 API

## 목적

- 이 문서는 `Rolling 관리자 대시보드 웹`이 현재 백엔드와 연결할 때 필요한 API만 정리한 문서다.
- 기준은 `실제 구현된 컨트롤러/DTO`다.
- 없는 기능은 포함하지 않는다.

관리자 웹 범위:

- 관리자 로그인
- 관리자 권한 없음
- 로그인 필요 / 세션 만료
- 대시보드 홈
- 공지사항 관리
- 문의 관리
- 신고 관리
- 대회 운영 / 크롤링
- 사용자 관리 / 제재

없는 기능:

- 권한 세분화
- 매출/통계
- 푸시 발송 관리자
- FAQ CMS
- 오픈매트 전체 관리자 목록

주의:

- 아래 사용자 관리 / 제재 섹션은 현재 백엔드 구현 기준의 계약이다.
- 관리자 웹 화면은 이 계약을 기준으로 별도 프론트에서 연결한다.
- 실제 제재 정책과 제한 모드는 `docs/ADMIN_USER_SANCTION_PLAN.md`를 기준으로 맞춘다.

---

## 1. 공통 규칙

### 1.1 Base

- Base URL: `/api/v1`
- 인증 헤더: `Authorization: Bearer {accessToken}`
- 관리자 권한 판단:
  - 로그인 응답의 `isAdmin`
  - 토큰 갱신 응답의 `isAdmin`
  - `/users/me` 응답의 `isAdmin`

### 1.2 공통 응답 형식

모든 API는 아래 래퍼를 사용한다.

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

에러 예시:

```json
{
  "success": false,
  "error": {
    "code": "FORBIDDEN",
    "message": "관리자 권한이 필요합니다"
  }
}
```

### 1.3 공통 에러 처리 규칙

- `401 UNAUTHORIZED`
  - 로그인 필요 / 세션 만료 화면으로 이동
  - 우선 `POST /api/v1/auth/refresh` 시도 후 실패하면 로그인 화면으로 이동
- `403 FORBIDDEN`
  - 관리자 라우트 진입 시: 권한 없음 화면
  - 대회 수정/삭제 시: 권한 실패 토스트 또는 인라인 에러
- `400 VALIDATION_ERROR`
  - 폼 필드 에러 또는 토스트
- `404 NOT_FOUND`
  - 상세가 삭제되었거나 존재하지 않는 경우

### 1.4 페이지 응답 규칙

- 페이지 목록 API는 `data.content` 배열을 사용한다.
- 최소 확인된 필드:
  - `data.content`
  - `data.totalElements`
- React에서는 아래 필드를 우선 사용하도록 구현한다.
  - `content`
  - `totalElements`
  - `totalPages`
  - `size`
  - `number`

예시:

```json
{
  "success": true,
  "data": {
    "content": [],
    "totalElements": 0,
    "totalPages": 0,
    "size": 10,
    "number": 0
  }
}
```

---

## 2. 관리자 진입 / 세션 처리

### 2.1 소셜 로그인

`POST /api/v1/auth/login`

Request:

```json
{
  "provider": "GOOGLE",
  "accessToken": "social-access-token"
}
```

지원 provider:

- `GOOGLE`
- `KAKAO`

미지원:

- `APPLE`

Response 핵심 필드:

- `accessToken`
- `refreshToken`
- `tokenType`
- `expiresIn`
- `newUser`
- `userId`
- `email`
- `name`
- `isAdmin`

React 처리:

- 로그인 성공 + `isAdmin=true`: 관리자 홈 진입
- 로그인 성공 + `isAdmin=false`: 권한 없음 화면으로 이동

### 2.1.1 카카오 소셜 로그인 연동 방식

현재 백엔드가 지원하는 카카오 로그인 방식은 `인가 코드(code) 직접 교환 방식`이 아니라 `카카오 accessToken 전달 방식`이다.

권장 구현은 아래 흐름이다.

1. React에서 `Kakao JavaScript SDK`로 카카오 로그인 진행
2. React가 카카오 `accessToken` 획득
3. React가 백엔드 `POST /api/v1/auth/login` 호출
4. 백엔드가 카카오 사용자 정보를 조회한 뒤 Rolling JWT 발급

현재 백엔드 로그인 요청 예시:

```json
{
  "provider": "KAKAO",
  "accessToken": "kakao-user-access-token"
}
```

중요:

- 현재 백엔드에는 `카카오 authorization code`를 직접 받는 전용 API가 없다.
- 즉 React가 `code`만 받아서 끝내면 안 되고, 최종적으로 백엔드에는 `카카오 accessToken`을 전달해야 한다.
- `카카오 OAuth redirect`를 쓰더라도 React 쪽에서 최종 `accessToken`을 확보하지 못하면 현재 백엔드와 바로 연결되지 않는다.
- 관리자 웹 로그인도 일반 사용자 로그인 API인 `POST /api/v1/auth/login`을 그대로 사용한다.
- 관리자 여부는 별도 로그인 방식이 아니라 로그인 후 응답의 `isAdmin`으로 판별한다.

### 2.1.2 React 관리자 웹 기준 카카오 Developers 설정

현재 구조가 `React(브라우저) -> Spring API`이면 카카오 웹 설정은 `React 앱 주소` 기준으로 잡는 것이 맞다.

로컬 개발 예시:

- React: `http://localhost:5173`
- Spring API: `http://localhost:8080`
- Kakao Redirect URI: `http://localhost:5173/auth/kakao/callback`

운영 예시:

- 관리자 웹: `https://admin.rolling-app.com`
- Spring API: `https://api.rolling-app.com` 또는 동일 도메인 reverse proxy
- Kakao Redirect URI: `https://admin.rolling-app.com/auth/kakao/callback`

Kakao Developers 권장 등록값:

- JavaScript SDK domain
  - `http://localhost:5173`
  - `https://admin.rolling-app.com`
- Kakao Login Redirect URI
  - `http://localhost:5173/auth/kakao/callback`
  - `https://admin.rolling-app.com/auth/kakao/callback`

주의:

- `JavaScript SDK domain`에는 경로 없이 origin만 등록한다.
- `Redirect URI`에는 실제 callback 경로까지 포함한 전체 URL을 등록한다.
- React에서 `5173`, Spring에서 `8080`을 써도 문제 없다. 로그인 리다이렉트와 API 서버는 서로 다른 포트를 써도 된다.
- 프론트가 `5173`, 백엔드가 `8080`이면 API 호출 시 CORS 또는 dev proxy 설정이 필요하다.
- 현재 백엔드 기준으로는 `JavaScript SDK로 accessToken을 받아 로그인 API에 전달하는 방식`이 가장 직접적이다.
- `Redirect URI`는 React에서 redirect/callback 방식을 함께 사용할 때 등록한다.

### 2.1.3 React 카카오 로그인 콜백 화면 처리

권장 callback path:

- `/auth/kakao/callback`

React callback 화면에서 할 일:

1. 카카오 로그인 결과 파싱
2. 카카오 `accessToken` 확보
3. `POST /api/v1/auth/login` 호출
4. Rolling `accessToken / refreshToken / isAdmin` 저장
5. `isAdmin`에 따라 관리자 홈 또는 권한 없음 화면으로 이동

실패 처리:

- 카카오 로그인 실패: 로그인 페이지 에러 상태
- 백엔드 로그인 실패: 토스트 + 로그인 페이지 복귀
- 로그인 성공 but `isAdmin=false`: 권한 없음 화면

### 2.1.4 카카오 로그인 관련 React 최소 연결 항목

- Kakao JavaScript SDK
- 선택 시 React callback route: `/auth/kakao/callback`
- 백엔드 로그인 API: `POST /api/v1/auth/login`
- 세션 유지 API: `POST /api/v1/auth/refresh`
- 현재 사용자 확인 API: `GET /api/v1/users/me`

### 2.2 토큰 갱신

`POST /api/v1/auth/refresh`

Request:

```json
{
  "refreshToken": "refresh-token"
}
```

Response 핵심 필드:

- `accessToken`
- `refreshToken`
- `tokenType`
- `expiresIn`
- `isAdmin`

실패 코드:

- `INVALID_REFRESH_TOKEN`
- `EXPIRED_REFRESH_TOKEN`

React 처리:

- 갱신 성공 시 토큰 교체
- 갱신 실패 시 세션 만료 화면 또는 로그인 화면

### 2.3 현재 사용자 확인

`GET /api/v1/users/me`

Response 핵심 필드:

- `id`
- `nickname`
- `email`
- `socialProvider`
- `createdAt`
- `withdrawalPending`
- `withdrawalScheduledAt`
- `isAdmin`

React 처리:

- 앱 부트 시 관리자 라우트 진입 전 확인용으로 사용 가능
- `isAdmin=false`면 관리자 화면 진입 금지

### 2.4 로그아웃

`POST /api/v1/auth/logout`

Request body는 선택이다.

```json
{
  "fcmToken": "optional"
}
```

관리자 웹 상단 사용자 메뉴에 로그아웃 버튼이 있으면 이 API 연결 가능.

---

## 3. 화면별 API 연결

## 3.1 관리자 로그인 페이지

필수 API:

- `POST /api/v1/auth/login`

선택 API:

- `GET /api/v1/users/me`

상태 처리:

- 로그인 로딩
- 로그인 실패 토스트
- `isAdmin=false`면 권한 없음 화면 이동
- 토큰 만료 후 재진입 시 `POST /api/v1/auth/refresh` 실패면 세션 만료 화면

---

## 3.2 관리자 권한 없음 화면

추가 API 없음.

진입 조건:

- 로그인/갱신 응답의 `isAdmin=false`
- `/users/me`의 `isAdmin=false`
- 관리자 API 호출 결과 `403 FORBIDDEN`

권장 액션:

- 일반 서비스로 돌아가기
- 다시 로그인하기

---

## 3.3 로그인 필요 / 세션 만료 화면

추가 API 없음.

진입 조건:

- 보호된 API에서 `401 UNAUTHORIZED`
- `POST /api/v1/auth/refresh` 실패

권장 액션:

- `Google로 다시 로그인`
- `Kakao로 다시 로그인`

---

## 3.4 대시보드 홈

주의:

- 전용 대시보드 API는 없다.
- 기존 목록 API를 조합해서 구성해야 한다.
- 차트용 집계 API는 없다.

사용 API:

- 답변 대기 문의 카드 / 최근 문의:
  - `GET /api/v1/admin/inquiries`
- 검토 대기 신고 카드 / 최근 신고:
  - `GET /api/v1/admin/reports`
- 최근 등록 공지:
  - `GET /api/v1/notices`
- 크롤링 바로 실행:
  - `POST /api/v1/tournaments/crawl`

권장 조합 방식:

- 최근 문의 미리보기:
  - `GET /api/v1/admin/inquiries?page=0&size=5`
- 최근 신고 미리보기:
  - `GET /api/v1/admin/reports?page=0&size=5`
- 최근 공지:
  - `GET /api/v1/notices?page=0&size=5`

합계 카드 주의:

- 문의/신고 API는 `status` 단일 필터만 지원한다.
- 예를 들어 `답변 대기 문의`를 `RECEIVED + IN_REVIEW` 합계로 보여주려면 2회 호출 후 `totalElements`를 합산해야 한다.
- `검토 대기 신고`도 동일하다.

예시:

- `GET /api/v1/admin/inquiries?status=RECEIVED&page=0&size=1`
- `GET /api/v1/admin/inquiries?status=IN_REVIEW&page=0&size=1`
- `GET /api/v1/admin/reports?status=RECEIVED&page=0&size=1`
- `GET /api/v1/admin/reports?status=IN_REVIEW&page=0&size=1`

---

## 4. 공지사항 관리

## 4.1 공지 목록

`GET /api/v1/notices?page=0&size=20`

Response item:

```json
{
  "id": 1,
  "title": "서비스 점검 안내",
  "content": "본문",
  "authorName": "Rolling Admin",
  "createdAt": "2026-03-20T09:00:00"
}
```

주의:

- 목록 응답에는 `updatedAt`이 없다.
- 따라서 기본 테이블 컬럼은 아래만 사용한다.
  - `title`
  - `authorName`
  - `createdAt`

## 4.2 공지 상세

`GET /api/v1/notices/{id}`

Response 필드:

- `id`
- `title`
- `content`
- `authorName`
- `createdAt`
- `updatedAt`

중요 제약:

- 상세 응답에도 `createdBy`는 없다.
- 즉, 수정 폼에서 `createdBy`를 기존값으로 미리 채울 수 없다.
- React는 아래 중 하나로 처리한다.
  - 수정 시 `createdBy` 입력을 선택 필드로 비워 둠
  - 생성 직후 로컬 상태에만 보관

## 4.3 공지 생성

`POST /api/v1/notices`

Request:

```json
{
  "title": "4월 공지",
  "content": "본문",
  "authorName": "운영팀",
  "createdBy": "ops-admin"
}
```

필수:

- `title`
- `content`
- `authorName`

선택:

- `createdBy`

## 4.4 공지 수정

`PUT /api/v1/notices/{id}`

Request:

```json
{
  "title": "수정 제목",
  "content": "수정 본문",
  "authorName": "운영팀",
  "createdBy": "ops-admin"
}
```

주의:

- 최소 1개 필드는 보내야 한다.
- 전달하지 않은 필드는 유지된다.

## 4.5 공지 삭제

`DELETE /api/v1/notices/{id}`

주의:

- hard delete
- 삭제 전 확인 모달 필수

React UX 연결:

- 생성/수정/삭제 성공 후 토스트
- 성공 후 목록 재조회

---

## 5. 문의 관리

## 5.1 문의 목록

`GET /api/v1/admin/inquiries`

Query:

- `status`: `RECEIVED | IN_REVIEW | ANSWERED`
- `type`: `ACCOUNT | OPEN_MAT | TOURNAMENT | NOTIFICATION | REPORT | OTHER`
- `createdFrom`: `yyyy-MM-dd`
- `createdTo`: `yyyy-MM-dd`
- `page`
- `size`

예시:

`GET /api/v1/admin/inquiries?status=IN_REVIEW&type=TOURNAMENT&createdFrom=2026-03-01&createdTo=2026-03-23&page=0&size=10`

Response item 핵심 필드:

- `id`
- `userId`
- `userNickname`
- `title`
- `content`
- `type`
- `status`
- `answerContent`
- `answeredByUserId`
- `answeredAt`
- `createdAt`
- `updatedAt`

테이블에서는 아래만 사용:

- `id`
- `type`
- `title`
- `userNickname`
- `status`
- `createdAt`

## 5.2 문의 상세

`GET /api/v1/admin/inquiries/{id}`

상세 패널 필드:

- `id`
- `userId`
- `userNickname`
- `title`
- `content`
- `type`
- `status`
- `answerContent`
- `answeredByUserId`
- `answeredAt`
- `createdAt`
- `updatedAt`

## 5.3 문의 답변 저장

`PATCH /api/v1/admin/inquiries/{id}/answer`

Request:

```json
{
  "answerContent": "문의 답변 내용"
}
```

주의:

- 답변 저장과 동시에 상태가 `ANSWERED`로 변경된다.
- 답변 작성/수정 CTA를 가장 강한 액션으로 노출한다.

## 5.4 문의 상태 변경

`PATCH /api/v1/admin/inquiries/{id}/status`

Request:

```json
{
  "status": "IN_REVIEW"
}
```

가능 값:

- `RECEIVED`
- `IN_REVIEW`
- `ANSWERED`

중요 제약:

- 답변 없는 문의는 `ANSWERED`로 직접 변경할 수 없다.
- 답변 저장된 문의는 `ANSWERED` 외 상태로 되돌리지 않는다.

React UX 규칙:

- 답변이 없을 때 상태 변경 드롭다운에서 `ANSWERED`를 비활성화하거나 설명 문구 표시
- 답변 저장 버튼을 주 액션으로 배치

---

## 6. 신고 관리

## 6.1 신고 목록

`GET /api/v1/admin/reports`

Query:

- `status`: `RECEIVED | IN_REVIEW | RESOLVED | REJECTED`
- `targetType`: `OPEN_MAT | TOURNAMENT`
- `createdFrom`: `yyyy-MM-dd`
- `createdTo`: `yyyy-MM-dd`
- `page`
- `size`

예시:

`GET /api/v1/admin/reports?status=RECEIVED&targetType=TOURNAMENT&page=0&size=10`

중요 제약:

- 관리자 목록에는 `동일 대상 누적 신고 3건 이상`인 신고만 노출된다.
- React는 이를 안내 문구로 노출할 수 있지만, 별도 우회 API는 없다.

Response item 핵심 필드:

- `id`
- `reporterUserId`
- `reporterNickname`
- `targetType`
- `targetId`
- `reason`
- `customReason`
- `status`
- `processedByUserId`
- `processedAt`
- `processingMemo`
- `finalAction`
- `targetSummary`
- `createdAt`
- `updatedAt`

테이블에서는 아래만 사용:

- `id`
- `targetType`
- `targetId`
- `reason`
- `reporterNickname`
- `status`
- `createdAt`

신고 사유 enum:

- `FALSE_INFO`
- `INAPPROPRIATE`
- `SPAM`
- `OTHER`

## 6.2 신고 상세

`GET /api/v1/admin/reports/{id}`

상세 패널 필드:

- `id`
- `reporterUserId`
- `reporterNickname`
- `targetType`
- `targetId`
- `reason`
- `customReason`
- `status`
- `processedByUserId`
- `processedAt`
- `processingMemo`
- `finalAction`
- `targetSummary.totalReportCount`
- `targetSummary.receivedCount`
- `targetSummary.inReviewCount`
- `targetSummary.resolvedCount`
- `targetSummary.rejectedCount`
- `createdAt`
- `updatedAt`

중요 UX 포인트:

- `targetSummary`는 별도 강조 카드로 표시
- 실행형 제재 버튼을 만들지 않는다
- 상태 변경 + 처리 메모 + 최종 조치 텍스트 기록 중심으로 설계

## 6.3 신고 상태 변경

`PATCH /api/v1/admin/reports/{id}/status`

Request:

```json
{
  "status": "IN_REVIEW",
  "processingMemo": "동일 대상 누적 신고 검토 중",
  "finalAction": "운영 검토 기록"
}
```

가능 값:

- `RECEIVED`
- `IN_REVIEW`
- `RESOLVED`
- `REJECTED`

주의:

- `processingMemo` 최대 1000자
- `finalAction` 최대 100자

---

## 7. 사용자 관리 / 제재

> 이 섹션은 현재 구현된 관리자 사용자 운영 계약이다.

### 7.1 목적

- 관리자에게 사용자 목록과 상세를 보여준다.
- 사용자 계정 상태를 확인하고 제재 이력을 조회할 수 있게 한다.
- 일시정지 상태에서는 로그인은 허용하되 제한 모드만 허용한다.
- 영구정지 상태에서는 기본적으로 로그인을 차단한다.

### 7.2 관리자 사용자 목록

`GET /api/v1/admin/users`

응답 필드:

- `id`
- `nickname`
- `email`
- `affiliation`
- `createdAt`
- `accountStatus`
- `lastSanctionAt`

### 7.3 관리자 사용자 상세

`GET /api/v1/admin/users/{id}`

상세 필드:

- `id`
- `nickname`
- `email`
- `phone`
- `affiliation`
- `createdAt`
- `accountStatus`
- `suspensionUntil`
- `isWithdrawn`

### 7.4 제재 이력

`GET /api/v1/admin/users/{id}/sanctions`

이력 필드:

- `id`
- `type`
- `reason`
- `memo`
- `startsAt`
- `endsAt`
- `createdBy`
- `createdAt`
- `releasedBy`
- `releasedAt`

### 7.5 제재 생성 / 해제

`POST /api/v1/admin/users/{id}/sanctions`

`DELETE /api/v1/admin/users/{id}/sanctions/{sanctionId}`

상태 값:

- `WARNING`
- `TEMP_SUSPEND`
- `PERMANENT_BAN`
- `RELEASE`

제한 모드에서 허용할 기능:

- 문의하기 작성
- 도움말 조회
- 알림 on/off
- 차단한 사용자 관리
- 로그아웃

제한 모드에서 차단할 기능:

- 사용자 정보 수정
- 탈퇴하기
- 오픈매트 / 대회 / 신고 생성

---

## 8. 대회 운영

## 8.1 대회 목록

`GET /api/v1/tournaments`

Query:

- `source`: `STREET_JIU_JITSU | KOREA_JIU | HEROES_OF_JIU_JITSU | SPOTLITE | MANUAL`
- `q`: 대회명 | 주최사 | 장소 | 접수 링크
- `page`
- `size`

예시:

`GET /api/v1/tournaments?source=MANUAL&page=0&size=20`

`GET /api/v1/tournaments?source=MANUAL&q=롤링&page=0&size=20`

Response item 필드:

- `id`
- `source`
- `title`
- `organizer`
- `posterUrl`
- `competitionDate`
- `registrationDeadline`
- `location`
- `applyLink`
- `registrationClosed`
- `createdAt`

테이블 컬럼:

- `title`
- `organizer`
- `source`
- `competitionDate`
- `registrationDeadline`
- `registrationClosed`
- `createdAt`

주의:

- 목록 정렬은 서버가 `접수 가능한 대회 우선`으로 정렬한다.

## 8.2 대회 상세

`GET /api/v1/tournaments/{id}`

상세 패널 필드:

- `title`
- `organizer`
- `posterUrl`
- `competitionDate`
- `registrationDeadline`
- `location`
- `applyLink`
- `source`

주의:

- 응답에는 `hostUserId`가 없다.
- 따라서 React는 수정/삭제 가능 여부를 사전 판별할 수 없다.
- 수정/삭제 시도 후 `403`을 처리해야 한다.

## 8.3 대회 수동 등록

`POST /api/v1/tournaments`

Request:

```json
{
  "title": "제5회 롤링컵",
  "organizer": "롤링 주짓수",
  "posterUrl": "https://cdn.rolling.com/posters/1.jpg",
  "competitionDate": "2026-04-15",
  "registrationDeadline": "2026-04-01",
  "location": "서울 올림픽공원 체조경기장",
  "applyLink": "https://example.com/apply"
}
```

주의:

- 생성 시 `source`는 서버에서 자동으로 `MANUAL` 저장

## 8.4 대회 수정

`PUT /api/v1/tournaments/{id}`

Request는 생성과 동일 필드, 모두 optional.

예시:

```json
{
  "title": "제5회 롤링컵",
  "location": "대구체육관",
  "region": "DAEGU"
}
```

주의:

- 최소 1개 필드는 보내야 한다
- `registrationDeadline <= competitionDate` 규칙 유지
- `region`도 생성과 동일하게 수정 가능하다
- 작성자 본인만 수정 가능
- 관리자 화면이어도 `403` 가능

## 8.5 대회 삭제

`DELETE /api/v1/tournaments/{id}`

주의:

- 작성자 본인만 삭제 가능
- 관리자 화면이어도 `403` 가능
- 삭제 전 확인 모달 필수

## 8.6 대회 크롤링 수동 실행

`POST /api/v1/tournaments/crawl`

전체 실행:

`POST /api/v1/tournaments/crawl`

source 지정 실행:

`POST /api/v1/tournaments/crawl?source=STREET_JIU_JITSU`

Response:

```json
{
  "crawledCount": 12,
  "createdCount": 3,
  "updatedCount": 5,
  "skippedCount": 4
}
```

중요 제약:

- 크롤링 실행용 source selector에는 아래만 노출한다.
- `STREET_JIU_JITSU`
- `KOREA_JIU`
- `HEROES_OF_JIU_JITSU`
- `SPOTLITE`
- `MANUAL`은 목록 필터용 값이지 크롤링 실행용 값이 아니다.

React UX:

- 상단 강조 버튼으로 배치
- 실행 결과는 모달 또는 결과 패널에서 노출

---

## 9. 상태 화면 설계 시 API 연결 기준

### 9.1 loading

- 모든 페이지 최초 진입 시 스켈레톤 또는 로딩 행 표시
- 상세 패널은 선택 row 변경 시 별도 로딩 가능

### 9.2 empty

- 서버 `content.length === 0` 이고 필터 미적용 상태

예:

- 등록된 공지 없음
- 노출 기준을 만족하는 신고 없음

### 9.3 no-result

- 필터 적용 후 `content.length === 0`

예:

- 기간 필터 결과 없음
- 특정 상태 결과 없음

### 9.4 error

- `400/403/404/500` 또는 네트워크 실패
- 상세 패널과 목록 영역을 분리해 부분 에러 처리 가능

---

## 10. React 구현 시 바로 체크할 제약

- 관리자 홈은 전용 집계 API가 없으므로 목록 API 조합으로 구현
- 공지 수정 폼은 기존 `createdBy` 값을 API로 다시 받을 수 없음
- 공지 목록에는 `updatedAt` 없음
- 문의 `ANSWERED`는 답변 저장 API로 처리하는 것이 기본
- 신고는 `3건 이상 누적 대상`만 관리자 목록에 노출
- 신고 관리 화면은 제재 실행 UI처럼 보이면 안 됨
- 대회 수정/삭제는 관리자라도 owner가 아니면 `403`
- 대회 크롤링 source 선택에는 `MANUAL`을 넣지 않음
- 관리자 권한 판단은 클라이언트 보조일 뿐이고 최종 권한은 서버 `403` 기준

---

## 11. React 화면별 최소 연결 목록

### 로그인

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `GET /api/v1/users/me`

### 대시보드 홈

- `GET /api/v1/admin/inquiries`
- `GET /api/v1/admin/reports`
- `GET /api/v1/notices`
- `POST /api/v1/tournaments/crawl`

### 공지사항 관리

- `GET /api/v1/notices`
- `GET /api/v1/notices/{id}`
- `POST /api/v1/notices`
- `PUT /api/v1/notices/{id}`
- `DELETE /api/v1/notices/{id}`

### 문의 관리

- `GET /api/v1/admin/inquiries`
- `GET /api/v1/admin/inquiries/{id}`
- `PATCH /api/v1/admin/inquiries/{id}/answer`
- `PATCH /api/v1/admin/inquiries/{id}/status`

### 신고 관리

- `GET /api/v1/admin/reports`
- `GET /api/v1/admin/reports/{id}`
- `PATCH /api/v1/admin/reports/{id}/status`

### 대회 운영

- `GET /api/v1/tournaments`
- `GET /api/v1/tournaments/{id}`
- `POST /api/v1/tournaments`
- `PUT /api/v1/tournaments/{id}`
- `DELETE /api/v1/tournaments/{id}`
- `POST /api/v1/tournaments/crawl`

---

## 12. 소스 오브 트루스

- 인증: `src/main/java/com/rolling/api/domain/auth/controller`
- 사용자: `src/main/java/com/rolling/api/domain/user/controller`
- 공지: `src/main/java/com/rolling/api/domain/notice/controller`
- 문의: `src/main/java/com/rolling/api/domain/inquiry/controller`
- 신고: `src/main/java/com/rolling/api/domain/report/controller`
- 대회: `src/main/java/com/rolling/api/domain/tournament/controller`
- 공통 응답: `src/main/java/com/rolling/api/global/response/ApiResponse.java`
