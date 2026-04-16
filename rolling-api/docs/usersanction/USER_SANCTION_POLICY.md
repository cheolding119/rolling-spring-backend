# Flutter 사용자 제재 적용 정책

이 문서는 **Flutter 사용자 앱**에서 관리자 제재가 어떻게 보이고, 무엇을 수정해야 하는지 정리한 문서다.

범위는 `관리자 기능`이 아니라 `Flutter 앱의 화면, 상태 처리, 사용자용 API`다.

---

## 1. 상태 정의

### 1.1 계정 상태

- `ACTIVE`: 정상 사용 가능
- `WARNING`: 경고 상태, 서비스 이용은 정상
- `SUSPENDED`: 일시정지, 로그인은 허용하되 제한 모드만 허용
- `WITHDRAWN`: 회원 탈퇴 상태

### 1.2 관리자 제재 타입

- `WARNING`: 경고 부여
- `TEMP_SUSPEND`: 일시정지 부여
- `TEMP_SUSPEND` + 장기 `endsAt`: 무기한 정지에 해당하는 강한 제재

주의:

- 앱에서 받는 `accountStatus`와 관리자 제재 타입은 같은 값이 아니다.
- `TEMP_SUSPEND`는 제재 행위 이름이고, 실제 계정 상태는 `SUSPENDED`로 내려온다.
- 무기한 정지도 별도 계정 상태를 만들지 않고 `SUSPENDED`로 내려온다.

---

## 2. Flutter 앱에서의 정책

### 2.1 경고

- 사용자는 정상적으로 로그인하고 앱을 사용할 수 있다.
- 경고 상태는 배너나 마이페이지 안내로만 보여줄 수 있다.
- 서비스 이용 자체는 막지 않는다.

### 2.2 일시정지

- 사용자는 로그인할 수 있다.
- 앱은 `제한 모드`로 전환한다.
- 사용자는 지원/문의/설정 중심의 최소 기능만 사용할 수 있다.
- 주 서비스 기능은 서버와 앱 둘 다에서 막는다.

제한 모드에서 허용할 범위:

- 문의하기 작성
- 도움말 조회
- 알림 on/off
- 차단한 사용자 관리
- 탈퇴하기
- 로그아웃

제한 모드에서 차단할 범위:

- 사용자 정보 수정
- 오픈매트 관련 작성/참가
- 대회 관련 작성/수정
- 신고 생성
- 그 외 주 서비스 상호작용 기능

### 2.3 무기한 정지

- 기본적으로 장기 `SUSPENDED` 상태로 운영한다.
- 로그인은 가능하지만 제한 모드만 허용한다.
- 앱은 일반 일시정지보다 더 강한 안내 문구로 제한 모드를 유지한다.

---

## 3. Flutter 앱에서 수정할 것

### 3.1 로그인/세션 상태 처리

- `POST /api/v1/auth/login` 응답에 있는 `accountStatus`, `suspensionUntil`, `sanctionReasonSummary`를 받아서 앱 상태에 저장한다.
- `POST /api/v1/auth/refresh` 응답에도 같은 상태 정보를 반영한다.
- 앱 시작 시 또는 로그인 직후 계정 상태를 확인해 라우트를 분기한다.
- 상태 판정은 화면 제어용이고, 최종 차단은 서버 응답을 따른다.

### 3.2 제한 모드 진입 화면

- `TEMP_SUSPEND`면 메인 홈으로 바로 보내지 말고 제한 모드 안내 화면 또는 마이페이지로 보낸다.
- 제한 모드에서는 하단 탭이나 주요 진입 버튼을 줄인다.
- 기본 홈 대신 `마이페이지`, `문의`, `도움말` 중심으로 보여준다.

### 3.3 마이페이지

- 상단에 제재 상태 배너를 추가한다.
- 정지 사유, 정지 종료 시각, 안내 문구를 보여준다.
- `TEMP_SUSPEND`에서는 사용자 정보 수정 진입을 막는다.
- `TEMP_SUSPEND`에서도 탈퇴하기는 허용한다.
- `TEMP_SUSPEND`에서는 알림 설정, 차단한 사용자 목록, 문의 작성, 도움말, 탈퇴하기를 남긴다.

### 3.4 문의 / 도움말

- 제한 모드에서도 문의 작성은 가능해야 한다.
- 정지 사유에 대한 문의나 이의제기성 문의를 쉽게 넣을 수 있어야 한다.
- 도움말은 읽기 전용으로 유지한다.

### 3.5 차단한 사용자 관리

- 제한 모드에서도 차단 목록 조회와 차단 해제는 가능하게 유지한다.
- 차단 추가는 서버 정책과 함께 다시 판단한다.

### 3.6 프로필 / 설정

- `TEMP_SUSPEND`에서는 프로필 수정 화면을 막는다.
- 알림 설정은 허용한다.
- 탈퇴하기는 허용한다.
- `FCM` 토큰 등록/삭제는 제한 모드에서 막는다.

### 3.7 무기한 정지 처리

- 장기 `TEMP_SUSPEND`도 로그인 성공 후 메인 화면 진입을 막고 제한 모드로 보낸다.
- 정지 사유, 문의 진입, 로그아웃을 우선 노출한다.
- `suspensionUntil`은 그대로 노출하지 말고 `무기한 정지` 문구로 가공할 수 있다.

### 3.8 화면별 사용자 경험

- 일반 사용자: 기존 흐름 유지
- 경고 사용자: 배너만 노출하고 이용 제한 없음
- 일시정지 사용자: 제한 모드
- 무기한 정지 사용자: 강한 안내가 포함된 제한 모드

---

## 4. Flutter에서 바뀌는 사용자용 API

아래 API들은 Flutter 앱에서 상태 처리나 화면 전환 로직에 직접 영향을 준다.

### 4.1 로그인

`POST /api/v1/auth/login`

앱에서 확인할 변경 사항:

- `accountStatus`
- `suspensionUntil`
- `sanctionReasonSummary`

### 4.2 토큰 갱신

`POST /api/v1/auth/refresh`

앱에서 확인할 변경 사항:

- `accountStatus`
- `suspensionUntil`
- `sanctionReasonSummary`

### 4.3 로그아웃

`POST /api/v1/auth/logout`

앱에서의 의미:

- 제한 모드에서도 로그아웃은 가능해야 한다.
- 무기한 정지 상태에서도 세션 정리 용도로 사용할 수 있다.

### 4.4 내 정보 조회

`GET /api/v1/users/me`

앱에서 확인할 변경 사항:

- `accountStatus`
- `suspensionUntil`
- `sanctionReasonSummary`

### 4.5 내 정보 수정

`PUT /api/v1/users/me`

앱에서의 적용:

- `TEMP_SUSPEND`에서는 진입 버튼 비활성화 또는 숨김

### 4.5.1 회원 탈퇴 요청

`DELETE /api/v1/auth/withdraw`

앱에서의 적용:

- `TEMP_SUSPEND`에서도 허용
- 장기 정지/무기한 정지에서도 허용
- 탈퇴 전 확인 모달에서 `탈퇴해도 제재 이력은 운영 기록으로 유지된다`는 안내를 보여줄 수 있다

### 4.6 내 설정 수정

`PATCH /api/v1/users/me/settings`

앱에서의 적용:

- `TEMP_SUSPEND`에서도 허용
- 알림 설정은 제한 모드에서도 유지

### 4.7 FCM 토큰 관리

`POST /api/v1/users/me/fcm`

`DELETE /api/v1/users/me/fcm`

앱에서의 적용:

- 제한 모드에서는 기본 차단
- 새 디바이스 등록이나 토큰 변경은 막는다

### 4.8 문의

`GET /api/v1/inquiries`

`POST /api/v1/inquiries`

`GET /api/v1/inquiries/{id}`

앱에서의 적용:

- `TEMP_SUSPEND`에서도 허용
- 정지 사유 문의 경로로 사용 가능

### 4.9 공지사항

`GET /api/v1/notices`

`GET /api/v1/notices/{id}`

앱에서의 적용:

- `TEMP_SUSPEND`에서도 허용
- 읽기 전용 유지

### 4.10 사용자 차단

`GET /api/v1/users/blocks`

`POST /api/v1/users/{id}/block`

`DELETE /api/v1/users/{id}/block`

앱에서의 적용:

- `TEMP_SUSPEND`에서도 차단 목록 조회와 해제는 허용
- 차단 추가는 정책 확정 후 유지 여부를 결정한다

---

## 5. Flutter에서 앱 레벨로 추가할 것

- 제재 상태 배너 컴포넌트
- 제한 모드 전용 안내 화면
- 로그인 성공 후 상태 분기 로직
- 마이페이지의 기능별 활성/비활성 제어
- 무기한 정지 안내 문구
- 문의 진입 경로 강조

---

## 6. Flutter 앱에서 보여줄 권장 문구

- 일시정지: `현재 계정은 일시정지 상태입니다. 문의와 도움말만 이용할 수 있습니다.`
- 무기한 정지: `현재 계정은 무기한 정지 상태입니다. 문의와 도움말만 이용할 수 있습니다.`
- 경고: `현재 계정은 경고 상태입니다.`

---

## 7. 관련 문서

- [Flutter 사용자 제재 실행 계획](C:/rolling/rolling-spring-backend/rolling-api/docs/usersanction/FLUTTER_USER_SANCTION_EXECUTION_PLAN.md)
- [관리자 사용자 제재 기획서](C:/rolling/rolling-spring-backend/rolling-api/docs/usersanction/ADMIN_USER_SANCTION_PLAN.md)
- [관리자 웹 React 연동 API](C:/rolling/rolling-spring-backend/rolling-api/docs/rollingadmin/ADMIN_WEB_REACT_API.md)
