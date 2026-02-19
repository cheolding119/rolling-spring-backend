## 1. 프로젝트 정의 (Project Definition)

**주짓수 라이프스타일 통합 플랫폼 '롤링(Rolling)' - Backend**

파편화된 주짓수 정보(오픈매트, 대회)를 하나로 모아 제공하는 REST API 서버입니다.
Flutter 모바일 앱에 데이터를 제공하며, 정보 탐색과 외부 활동 참여에 집중한 MVP(Minimum Viable Product) 모델입니다.

---

## 2. 주요 대상 (Target Audience)

- **수련생**: 자신의 체육관 밖에서 일어나는 오픈매트, 대회 등 다양한 '외부 활동'을 즐기는 적극적인 수련자.
- **관장님**: 최소한의 노력으로 오픈매트와 대회 정보를 홍보하려는 운영자.

---

## 3. 핵심 API 기능 상세 (Functional Requirements)

### 3.1 인증 API (Auth)

| Method | Endpoint | 설명 |
|--------|----------|------|
| POST | `/api/v1/auth/login` | 소셜 로그인 (JWT 발급) |
| POST | `/api/v1/auth/refresh` | Access Token 갱신 |
| POST | `/api/v1/auth/logout` | 로그아웃 |
| DELETE | `/api/v1/auth/withdraw` | 회원 탈퇴 (Apple Revoke 포함) |

**구현 요구사항**:
- Apple, Kakao, Google, Naver 소셜 로그인 토큰 검증
- JWT Access Token (30분) + Refresh Token (14일) 발급
- Apple 로그인 시 Revoke Token API 필수 구현

### 3.2 사용자 API (User)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/users/me` | 내 정보 조회 |
| PUT | `/api/v1/users/me` | 내 정보 수정 |
| POST | `/api/v1/users/me/fcm`| FCM 토큰 등록 |
| POST | `/api/v1/users/{id}/block` | 사용자 차단 |
| DELETE | `/api/v1/users/{id}/block` | 차단 해제 |

### 3.3 오픈매트 API (OpenMat)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/open-mats` | 오픈매트 리스트 (지역 필터) |
| GET | `/api/v1/open-mats/{id}` | 오픈매트 상세 조회 |
| POST | `/api/v1/open-mats` | 오픈매트 등록 |
| PUT | `/api/v1/open-mats/{id}` | 오픈매트 수정 (알림 발송) |
| DELETE | `/api/v1/open-mats/{id}` | 오픈매트 삭제 (알림 발송) |
| POST | `/api/v1/open-mats/{id}/apply` | 신청하기 |
| DELETE | `/api/v1/open-mats/{id}/apply` | 신청 취소 |
| GET | `/api/v1/open-mats/my` | 내가 신청한 오픈매트 |

**구현 요구사항**:
- 정원 관리 (maxCapacity 체크, CLOSED 자동 전환)
- Scheduler로 endDateTime 지난 오픈매트 FINISHED 전환
- 수정/삭제 시 신청자에게 FCM 푸시 알림

### 3.4 대회 API (Tournament)

| Method | Endpoint | 설명 |
|--------|----------|------|
| GET | `/api/v1/tournaments` | 대회 리스트 |
| GET | `/api/v1/tournaments/{id}` | 대회 상세 조회 |
| POST | `/api/v1/tournaments` | 대회 등록 (주최자/관리자) |
| PUT | `/api/v1/tournaments/{id}` | 대회 수정 |
| DELETE | `/api/v1/tournaments/{id}` | 대회 삭제 |

**구현 요구사항**:
- posterUrl, applyLink 필수
- registrationDeadline 기준 정렬 (마감 임박 상단)
- 접수 마감 여부 표시

---

## 4. 기술 스택 (Technical Stack)

| 분류 | 기술 |
|------|------|
| Framework | Spring Boot 3.x |
| Language | Java 17+ |
| Database | MySQL 8.0 |
| ORM | Spring Data JPA + QueryDSL |
| Security | Spring Security + JWT |
| Cache | Redis |
| Storage | AWS S3 |
| Push | Firebase Cloud Messaging |
| Docs | Swagger/OpenAPI 3.0 |
| Build | Gradle (Kotlin DSL) |
| CI/CD | GitHub Actions + Docker |

---

## 5. 아키텍처 및 설계 원칙

### Layered Architecture
```
Controller (API Layer)
    ↓
Service (Business Logic)
    ↓
Repository (Data Access)
    ↓
Entity (Domain Model)
```

### 패키지 구조
```
com.rolling/
├── global/           # 공통 설정, 예외, 유틸
├── domain/           # 도메인별 모듈
│   ├── user/
│   ├── openmat/
│   └── tournament/
└── infra/            # 외부 서비스 연동
    ├── firebase/
    ├── s3/
    └── social/
```

### 설계 원칙
- **단일 책임 원칙**: 각 Service는 하나의 도메인만 담당
- **DTO 분리**: Request/Response DTO와 Entity 분리
- **예외 처리**: GlobalExceptionHandler로 일관된 에러 응답
- **Validation**: Jakarta Bean Validation 활용
- **Transaction**: Service 레이어에서 @Transactional 관리

---

## 6. 응답 형식

### 성공 응답
```json
{
  "success": true,
  "message": "Success",
  "data": {  }
}
```

### 에러 응답
```json
{
  "success": false,
  "message": "에러 메시지",
  "data": null
}
```

### 페이징 응답
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [  ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

---

## 7. 인증 Flow

```
1. 클라이언트 → 소셜 로그인 → 소셜 Access Token 획득
2. 클라이언트 → POST /api/v1/auth/login (소셜 토큰 전송)
3. 서버 → 소셜 토큰 검증 (Apple/Kakao/Google/Naver API)
4. 서버 → 유저 조회 또는 생성
5. 서버 → JWT Access Token + Refresh Token 발급
6. 클라이언트 → API 요청 시 Authorization: Bearer {accessToken}
7. 만료 시 → POST /api/v1/auth/refresh (Refresh Token으로 갱신)
```