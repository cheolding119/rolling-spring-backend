# Rolling API 명세서

> **Base URL**: `/api/v1`
>
> **인증 방식**: `Authorization: Bearer {accessToken}`
>
> **Content-Type**: `application/json`

---

## 공통 응답 형식

### 성공 응답
```json
{
  "success": true,
  "data": { ... }
}
```

### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "에러 설명"
  }
}
```

### 페이징 응답
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

### 공통 에러 코드

| 코드 | HTTP Status | 설명 |
|------|:-----------:|------|
| `UNAUTHORIZED` | 401 | 인증 토큰 없음 또는 만료 |
| `FORBIDDEN` | 403 | 권한 없음 |
| `NOT_FOUND` | 404 | 리소스를 찾을 수 없음 |
| `VALIDATION_ERROR` | 400 | 요청 데이터 유효성 검증 실패 |

### 공통 Enum

**SocialProvider**
| 값 | 설명 |
|------|------|
| `KAKAO` | 카카오 로그인 |
| `GOOGLE` | 구글 로그인 |
| `APPLE` | 애플 로그인 (예정) |
| `NAVER` | 네이버 로그인 (예정) |

**OpenMatStatus**
| 값 | 설명 |
|------|------|
| `RECRUITING` | 모집중 |
| `CLOSED` | 정원 마감 |
| `FINISHED` | 종료됨 (endDateTime 경과) |

**Region**
| 값 | 설명 |
|------|------|
| `SEOUL` | 서울 |
| `GYEONGGI` | 경기 |
| `INCHEON` | 인천 |
| `DAEJEON` | 대전 |
| `SEJONG` | 세종 |
| `CHUNGBUK` | 충북 |
| `CHUNGNAM` | 충남 |
| `BUSAN` | 부산 |
| `DAEGU` | 대구 |
| `ULSAN` | 울산 |
| `GYEONGBUK` | 경북 |
| `GYEONGNAM` | 경남 |
| `GWANGJU` | 광주 |
| `JEONBUK` | 전북 |
| `JEONNAM` | 전남 |
| `GANGWON` | 강원 |
| `JEJU` | 제주 |

---

## 1. 인증 API (Auth)

### 1.1 소셜 로그인

소셜 제공자에서 발급받은 Access Token으로 JWT를 발급합니다.
신규 유저인 경우 자동으로 회원가입 처리됩니다.

```
POST /api/v1/auth/login
```

**인증**: 불필요

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `provider` | `String` | O | 소셜 로그인 제공자 (`KAKAO`, `GOOGLE`) |
| `accessToken` | `String` | O | 소셜 제공자에서 발급받은 Access Token |

```json
{
  "provider": "GOOGLE",
  "accessToken": "ya29.a0AfH6SMB..."
}
```

**Response** `200 OK`
| 필드 | 타입 | 설명 |
|------|------|------|
| `accessToken` | `String` | JWT Access Token (만료: 30분) |
| `refreshToken` | `String` | JWT Refresh Token (만료: 14일) |
| `tokenType` | `String` | 항상 `"Bearer"` |
| `expiresIn` | `Long` | Access Token 만료 시간 (초) |
| `newUser` | `Boolean` | 신규 회원 여부 |
| `userId` | `Long` | 사용자 ID |
| `email` | `String` | 사용자 이메일 |
| `name` | `String` | 사용자 이름 |

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "newUser": true,
    "userId": 1,
    "email": "user@gmail.com",
    "name": "홍길동"
  }
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `UNSUPPORTED_PROVIDER` | 지원하지 않는 provider |
| `KAKAO_API_ERROR` | 카카오 토큰 검증 실패 |
| `GOOGLE_API_ERROR` | 구글 토큰 검증 실패 |

---

### 1.2 토큰 갱신

```
POST /api/v1/auth/refresh
```

**인증**: 불필요

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `refreshToken` | `String` | O | 기존에 발급받은 Refresh Token |

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

---

### 1.3 로그아웃

```
POST /api/v1/auth/logout
```

**인증**: 필요

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

### 1.4 회원 탈퇴

```
DELETE /api/v1/auth/withdraw
```

**인증**: 필요

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

> Apple 로그인 사용자의 경우 Apple Revoke Token API가 함께 호출됩니다.

---

## 2. 사용자 API (User)

### 2.1 내 정보 조회

```
GET /api/v1/users/me
```

**인증**: 필요

**Response** `200 OK`
| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | `Long` | 사용자 ID |
| `nickname` | `String` | 닉네임 |
| `email` | `String` | 이메일 |
| `phone` | `String` | 연락처 |
| `socialProvider` | `String` | 소셜 로그인 제공자 |
| `createdAt` | `String` | 가입 일시 (ISO 8601) |

```json
{
  "success": true,
  "data": {
    "id": 1,
    "nickname": "홍길동",
    "email": "user@gmail.com",
    "phone": "010-1234-5678",
    "socialProvider": "GOOGLE",
    "createdAt": "2026-01-15T10:30:00"
  }
}
```

---

### 2.2 내 정보 수정

```
PUT /api/v1/users/me
```

**인증**: 필요

**Request Body** (변경할 필드만 전송)
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `nickname` | `String` | - | 닉네임 |
| `phone` | `String` | - | 연락처 |

```json
{
  "nickname": "새닉네임",
  "phone": "010-9876-5432"
}
```

**Response** `200 OK` — 2.1과 동일한 형식

---

### 2.3 FCM 토큰 등록

```
POST /api/v1/users/me/fcm
```

**인증**: 필요

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `fcmToken` | `String` | O | Firebase Cloud Messaging 토큰 |

```json
{
  "fcmToken": "dK1x..."
}
```

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

### 2.4 사용자 차단

```
POST /api/v1/users/{id}/block
```

**인증**: 필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 차단할 사용자 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

### 2.5 차단 해제

```
DELETE /api/v1/users/{id}/block
```

**인증**: 필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 차단 해제할 사용자 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

## 3. 오픈매트 API (OpenMat)

### 3.1 오픈매트 리스트 조회

```
GET /api/v1/open-mats
```

**인증**: 불필요

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:----:|--------|------|
| `region` | `Region` | - | - | 지역 필터 (예: `SEOUL`, `BUSAN`) |
| `page` | `Integer` | - | `0` | 페이지 번호 (0부터 시작) |
| `size` | `Integer` | - | `20` | 페이지 크기 |
| `sort` | `String` | - | `startDateTime,asc` | 정렬 기준 |

**Response** `200 OK` — 페이징 응답
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "주말 오픈매트",
        "description": "자유롭게 참가 가능한 주말 오픈매트입니다.",
        "startDateTime": "2026-03-01T10:00:00",
        "endDateTime": "2026-03-01T12:00:00",
        "locationName": "롤링 주짓수 아카데미",
        "address": "서울시 강남구 역삼동 123-45",
        "region": "SEOUL",
        "maxCapacity": 20,
        "currentParticipants": 5,
        "status": "RECRUITING",
        "hostId": 1,
        "hostNickname": "관장님",
        "hostInstagramId": "rolling_bjj",
        "createdAt": "2026-02-15T09:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

> 숨김 처리된 오픈매트(`isHidden = true`)는 리스트에 노출되지 않습니다.

---

### 3.2 오픈매트 상세 조회

```
GET /api/v1/open-mats/{id}
```

**인증**: 불필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": {
    "id": 1,
    "title": "주말 오픈매트",
    "description": "자유롭게 참가 가능한 주말 오픈매트입니다.",
    "startDateTime": "2026-03-01T10:00:00",
    "endDateTime": "2026-03-01T12:00:00",
    "locationName": "롤링 주짓수 아카데미",
    "address": "서울시 강남구 역삼동 123-45",
    "region": "SEOUL",
    "maxCapacity": 20,
    "currentParticipants": 5,
    "participantUids": [2, 3, 5, 7, 11],
    "status": "RECRUITING",
    "hostId": 1,
    "hostNickname": "관장님",
    "hostInstagramId": "rolling_bjj",
    "createdAt": "2026-02-15T09:00:00"
  }
}
```

---

### 3.3 오픈매트 등록

```
POST /api/v1/open-mats
```

**인증**: 필요

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `title` | `String` | O | 오픈매트 제목 |
| `description` | `String` | O | 상세 설명 |
| `startDateTime` | `String` | O | 시작 일시 (ISO 8601) |
| `endDateTime` | `String` | O | 종료 일시 (ISO 8601) |
| `locationName` | `String` | O | 장소명 |
| `address` | `String` | O | 상세 주소 |
| `region` | `Region` | O | 지역 (예: `SEOUL`) |
| `maxCapacity` | `Integer` | O | 최대 정원 (`-1` = 제한 없음) |
| `hostInstagramId` | `String` | - | 호스트 인스타그램 ID |

```json
{
  "title": "주말 오픈매트",
  "description": "자유롭게 참가 가능한 주말 오픈매트입니다.",
  "startDateTime": "2026-03-01T10:00:00",
  "endDateTime": "2026-03-01T12:00:00",
  "locationName": "롤링 주짓수 아카데미",
  "address": "서울시 강남구 역삼동 123-45",
  "region": "SEOUL",
  "maxCapacity": 20,
  "hostInstagramId": "rolling_bjj"
}
```

**Response** `200 OK` — 3.2 상세 조회와 동일한 형식

**에러**
| 코드 | 상황 |
|------|------|
| `VALIDATION_ERROR` | 필수 필드 누락 |
| `VALIDATION_ERROR` | 종료 시간이 시작 시간 이전 |

---

### 3.4 오픈매트 수정

```
PUT /api/v1/open-mats/{id}
```

**인증**: 필요 (작성자 본인만 가능)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Request Body** (변경할 필드만 전송)
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `title` | `String` | - | 오픈매트 제목 |
| `description` | `String` | - | 상세 설명 |
| `startDateTime` | `String` | - | 시작 일시 (ISO 8601) |
| `endDateTime` | `String` | - | 종료 일시 (ISO 8601) |
| `locationName` | `String` | - | 장소명 |
| `address` | `String` | - | 상세 주소 |
| `region` | `Region` | - | 지역 (예: `SEOUL`) |
| `maxCapacity` | `Integer` | - | 최대 정원 |
| `hostInstagramId` | `String` | - | 호스트 인스타그램 ID |

**Response** `200 OK` — 3.2 상세 조회와 동일한 형식

> 신청자가 있는 경우, 일시/장소 변경 시 FCM 푸시 알림이 발송됩니다.

---

### 3.5 오픈매트 삭제

```
DELETE /api/v1/open-mats/{id}
```

**인증**: 필요 (작성자 본인만 가능)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Query Parameter**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:----:|--------|------|
| `force` | `Boolean` | - | `false` | 신청자가 있어도 강제 삭제 |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

> 신청자가 있는 경우 `force=true`가 필요하며, 삭제 시 신청자들에게 취소 알림이 발송됩니다.

---

### 3.6 오픈매트 신청

```
POST /api/v1/open-mats/{id}/apply
```

**인증**: 필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

**에러**
| 코드 | 상황 |
|------|------|
| `OPEN_MAT_CLOSED` | 모집이 마감된 오픈매트 |
| `OPEN_MAT_FINISHED` | 이미 종료된 오픈매트 |
| `ALREADY_APPLIED` | 이미 신청한 오픈매트 |
| `CAPACITY_FULL` | 정원 초과 |

---

### 3.7 오픈매트 신청 취소

```
DELETE /api/v1/open-mats/{id}/apply
```

**인증**: 필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 오픈매트 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

### 3.8 내가 신청한 오픈매트 목록

```
GET /api/v1/open-mats/my
```

**인증**: 필요

**Response** `200 OK`
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "title": "주말 오픈매트",
      "startDateTime": "2026-03-01T10:00:00",
      "endDateTime": "2026-03-01T12:00:00",
      "locationName": "롤링 주짓수 아카데미",
      "address": "서울시 강남구 역삼동 123-45",
      "maxCapacity": 20,
      "currentParticipants": 5,
      "status": "RECRUITING",
      "hostNickname": "관장님",
      "hostInstagramId": "rolling_bjj"
    }
  ]
}
```

---

## 4. 대회 API (Tournament)

### 4.1 대회 리스트 조회

```
GET /api/v1/tournaments
```

**인증**: 불필요

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:----:|--------|------|
| `page` | `Integer` | - | `0` | 페이지 번호 |
| `size` | `Integer` | - | `20` | 페이지 크기 |

**Response** `200 OK` — 페이징 응답

> 접수 가능한 대회가 상단, 마감된 대회가 하단으로 정렬됩니다.

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "제5회 롤링컵",
        "organizer": "롤링 주짓수",
        "posterUrl": "https://cdn.rolling.com/posters/1.jpg",
        "competitionDate": "2026-04-15",
        "registrationDeadline": "2026-04-01",
        "location": "서울 올림픽공원 체조경기장",
        "applyLink": "https://forms.google.com/...",
        "categoryTags": ["Gi", "No-Gi"],
        "registrationClosed": false,
        "createdAt": "2026-02-01T12:00:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1,
    "last": true
  }
}
```

---

### 4.2 대회 상세 조회

```
GET /api/v1/tournaments/{id}
```

**인증**: 불필요

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 대회 ID |

**Response** `200 OK` — 4.1 리스트 항목과 동일한 형식

---

### 4.3 대회 등록

```
POST /api/v1/tournaments
```

**인증**: 필요 (주최자/관리자)

**Request Body**
| 필드 | 타입 | 필수 | 설명 |
|------|------|:----:|------|
| `title` | `String` | O | 대회 제목 |
| `organizer` | `String` | - | 주최사 |
| `posterUrl` | `String` | O | 포스터 이미지 URL |
| `competitionDate` | `String` | O | 대회 개최일 (`YYYY-MM-DD`) |
| `registrationDeadline` | `String` | O | 접수 마감일 (`YYYY-MM-DD`) |
| `location` | `String` | - | 개최 장소 |
| `applyLink` | `String` | O | 외부 접수 링크 |
| `categoryTags` | `String[]` | - | 카테고리 태그 (`Gi`, `No-Gi` 등) |

```json
{
  "title": "제5회 롤링컵",
  "organizer": "롤링 주짓수",
  "posterUrl": "https://cdn.rolling.com/posters/1.jpg",
  "competitionDate": "2026-04-15",
  "registrationDeadline": "2026-04-01",
  "location": "서울 올림픽공원 체조경기장",
  "applyLink": "https://forms.google.com/...",
  "categoryTags": ["Gi", "No-Gi"]
}
```

**Response** `200 OK` — 4.2 상세 조회와 동일한 형식

---

### 4.4 대회 수정

```
PUT /api/v1/tournaments/{id}
```

**인증**: 필요 (주최자/관리자)

**Request Body** — 4.3과 동일 (변경할 필드만 전송)

**Response** `200 OK` — 4.2 상세 조회와 동일한 형식

---

### 4.5 대회 삭제

```
DELETE /api/v1/tournaments/{id}
```

**인증**: 필요 (주최자/관리자)

**Path Parameter**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `id` | `Long` | 대회 ID |

**Response** `200 OK`
```json
{
  "success": true,
  "data": null
}
```

---

## 날짜/시간 형식

| 타입 | 형식 | 예시 |
|------|------|------|
| DateTime | ISO 8601 | `2026-03-01T10:00:00` |
| Date | ISO 8601 | `2026-03-01` |
---

## 변경사항 (2026-02-19)

### 3.8 내가 신청한 오픈매트 목록 (변경)

```
GET /api/v1/open-mats/my
```

**인증**: 필요

**Query Parameters**
| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|:----:|--------|------|
| `page` | `Integer` | - | `0` | 페이지 번호 (0부터 시작) |
| `size` | `Integer` | - | `10` | 페이지 크기 |
| `sort` | `String` | - | `startDateTime,asc` | 정렬 기준 |

**Response** `200 OK` (페이지 응답)
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "title": "주말 오픈매트",
        "startDateTime": "2026-03-01T10:00:00",
        "endDateTime": "2026-03-01T12:00:00",
        "locationName": "롤링 주짓수 강남점",
        "address": "서울시 강남구 ...",
        "maxCapacity": 20,
        "currentParticipants": 5,
        "status": "RECRUITING",
        "hostNickname": "관리자"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 37,
    "totalPages": 4,
    "last": false
  }
}
```

### 프론트 연동 규칙
- 기본 화면: `GET /api/v1/open-mats/my?page=0&size=10`
- 전체보기: `page`를 증가시키며 같은 API로 조회
