# User 소속(체육관) 컬럼 추가 기획안

기준일: 2026-04-11

## 1. 목적

- 사용자 프로필에 개인이 속한 체육관 정보를 저장할 수 있게 한다.
- 현재 `User` 도메인의 다른 프로필 속성과 같은 방식으로 관리한다.
- 백엔드 서브에이전트가 바로 구현할 수 있도록 DB, API, 테스트, 문서 작업 순서를 분리한다.

## 2. 현재 코드 기준 검토 결과

- `User` 엔티티는 `nickname`, `email`, `phone`, `socialProvider`, `socialId`, `beltColor`, `pushNotificationEnabled` 중심의 단일 테이블 구조다.
- 사용자 정보 조회/수정은 `GET /api/v1/users/me`, `PUT /api/v1/users/me`로 묶여 있다.
- 현재 `PUT /api/v1/users/me`는 `nickname`, `beltColor`만 수정한다.
- `GET /api/v1/users/me` 응답은 `UserResponse`로 내려가며, 프로필 정보와 `settings`를 함께 포함한다.
- 프로젝트는 Flyway를 사용하므로 새 컬럼 추가는 마이그레이션 SQL로 반영하는 것이 맞다.
- 이 코드베이스에는 체육관 마스터 테이블이나 소속 도메인 엔티티가 없다.

## 3. 설계 원칙

- 1차 구현은 별도 체육관 엔티티 없이 `users` 테이블의 단일 문자열 컬럼으로 저장한다.
- 컬럼은 nullable로 두어 기존 사용자 데이터를 깨지 않도록 한다.
- 소속명은 최대한 자유 입력으로 받되, 추후 정규화가 필요하면 2차 단계에서 별도 마스터 테이블로 분리한다.
- 현재 프로젝트의 사용자 프로필 확장 방식과 동일하게 `GET /me`와 `PUT /me`를 함께 갱신한다.

## 4. 권장 스키마

- 컬럼명: `affiliation`
- 타입: `VARCHAR(255)`
- null 허용: `true`
- 의미: 사용자의 체육관/도장/팀 소속명

### 선택 이유

- 현재 `User` 테이블이 단순 속성 위주라서, 소속 정보를 별도 테이블로 먼저 분리하면 구현 범위가 불필요하게 커진다.
- 사용자 입력값은 UI에서 짧은 문자열로 다루는 경우가 많아, 1차는 문자열 컬럼이 가장 적합하다.
- 이후 검색, 자동완성, 체육관별 집계가 필요해지면 별도 엔티티로 확장할 수 있다.

## 5. 반영 범위

### 포함

- `users` 테이블 컬럼 추가
- `User` 엔티티 수정
- `UserResponse` 수정
- `UserUpdateRequest` 수정
- `UserService` 수정
- `UserController` 문서 수정
- 테스트 추가/수정
- `docs/AGENTS.md` 또는 관련 계약 문서 동기화

### 제외

- 체육관 마스터 테이블
- 체육관 검색/목록/관리 API
- 소속 기반 권한 정책
- 소속 변경 이력 관리

## 6. Phase별 실행 계획

### Phase 1. 요구사항 고정

- [x] 소속 컬럼의 의미를 `체육관/도장/팀 이름` 범위로 확정한다.
- [x] 1명의 사용자가 소속을 1개만 가진다.
- [x] 소속값은 선택 입력으로 둔다.
- [x] 최대 길이 기준은 `255`로 둔다.
- [x] UI 노출 위치는 `내 정보 조회/수정`으로 고정한다.

결정 메모:

- 1차 구현은 체육관명을 자유 입력 문자열로 받는다.
- 별도 체육관 엔티티는 두지 않는다.

완료 기준:

- 소속 컬럼의 의미와 입력 정책이 하나로 합의된다.
- 백엔드가 구현해야 할 범위가 명확해진다.

### Phase 2. DB 스키마 반영

- [x] Flyway 마이그레이션 파일을 추가한다.
- [x] `users.affiliation` 컬럼을 nullable로 추가한다.
- [x] 기존 데이터에 대해 별도 backfill이 필요 없는지 확인한다.
- [x] 운영 DB에서 컬럼 추가가 기존 쿼리와 충돌하지 않는지 확인한다.

결과 메모:

- `src/main/resources/db/migration/V4__add_affiliation_to_users.sql`로 `users.affiliation`을 추가했다.
- 기존 사용자 레코드에는 영향을 주지 않도록 nullable 컬럼으로 반영했다.

완료 기준:

- `users` 테이블에 `affiliation` 컬럼이 안전하게 추가된다.
- 기존 사용자 레코드가 문제 없이 유지된다.

### Phase 3. 도메인/DTO 반영

- [x] `User` 엔티티에 `affiliation` 필드를 추가한다.
- [x] `User` 생성자와 builder 경로에서 초기값 처리 규칙을 정한다.
- [x] `User.updateAffiliation(...)` 메서드를 추가한다.
- [x] `UserResponse`에 `affiliation`을 포함한다.
- [x] `UserUpdateRequest`에 `affiliation`을 추가한다.
- [x] API 문서 주석에 예시와 nullable 여부를 반영한다.

결과 메모:

- `UserResponse.from(...)`에 `affiliation`을 포함했다.
- `UserUpdateRequest`는 선택 입력 `affiliation`을 받는다.
- `User` 빌더와 수정 메서드는 null 허용 부분 수정 패턴을 유지한다.

완료 기준:

- 사용자 조회 응답과 수정 요청에 소속 정보가 포함된다.
- 엔티티 변경이 서비스 레이어에서 일관되게 사용된다.

### Phase 4. 서비스/컨트롤러 연결

- [x] `UserService.updateMe(...)`가 소속 수정도 반영하도록 수정한다.
- [x] `UserService.getMe(...)` 응답에 소속이 포함되는지 확인한다.
- [x] `UserController`의 `PUT /api/v1/users/me` 설명을 갱신한다.
- [x] 요청 바디에서 `affiliation`만 전달된 경우에도 기존 필드와 함께 정상 동작하는지 확인한다.

결과 메모:

- `updateMe`는 `nickname`, `affiliation`, `beltColor`를 모두 부분 수정 방식으로 처리한다.
- `getMe`는 기존 응답에 `affiliation`을 추가해 내려준다.
- `affiliation=null`은 기존 값 유지로 해석하고, 빈 문자열은 기존 API 패턴대로 그대로 반영한다.

완료 기준:

- `/api/v1/users/me` 조회/수정 API가 소속 값을 읽고 쓸 수 있다.
- 기존 nickname/beltColor 동작이 깨지지 않는다.

### Phase 5. 테스트 보강

- [x] `UserServiceTest`에 소속 조회/수정 케이스를 추가한다.
- [x] `UserControllerTest`에 응답 필드 검증을 추가한다.
- [x] 필요한 경우 JPA 매핑 확인용 통합 테스트를 보강한다.
- [x] nullable 값, 빈 문자열, 미전달 케이스를 구분해서 검증한다.

결과 메모:

- 서비스 테스트에서 조회/수정/빈 문자열/미전달 케이스를 추가했다.
- 컨트롤러 테스트에서 `GET /api/v1/users/me` 응답 필드와 `PUT /api/v1/users/me` 수정 성공 경로를 확인했다.

완료 기준:

- 소속 추가가 기존 사용자 API 회귀를 만들지 않는다.
- 요청/응답 계약이 테스트로 고정된다.

### Phase 6. 문서 동기화

- [x] `docs/AGENTS.md`의 `/users/me` 계약에 `affiliation`을 반영한다.
- [ ] 필요 시 프론트 연동 문서에도 `UserResponse` 변경을 반영한다.

결과 메모:

- `docs/AGENTS.md`에 응답/요청 필드와 수정 정책을 반영했다.
- 나머지 운영/공용 계약 문서는 별도 동기화 절차로 이어서 처리한다.

완료 기준:

- 코드와 문서의 사용자 프로필 계약이 일치한다.

## 7. 구현 우선순위

1. Phase 1 요구사항 고정
2. Phase 2 DB 스키마 반영
3. Phase 3 도메인/DTO 반영
4. Phase 4 서비스/컨트롤러 연결
5. Phase 5 테스트 보강
6. Phase 6 문서 동기화

## 8. 리스크 및 판단 포인트

- 소속을 문자열로만 저장하면 중복값 정리가 어렵다.
- 추후 체육관 검색/필터 기능이 필요하면 별도 `gym` 엔티티로 분리해야 할 수 있다.
- 소속을 필수값으로 강제하면 기존 사용자 수정 UX가 불편해질 수 있다.
- `PUT /api/v1/users/me`가 이미 부분 수정 방식이므로, 신규 필드도 같은 패턴으로 맞추는 편이 일관적이다.

## 9. 이번 1차 결론

- 1차 구현은 `users.affiliation` 문자열 컬럼 추가가 가장 적절하다.
- 사용자 프로필 조회/수정 API에 바로 연결하는 범위로 마무리한다.
- 체육관 마스터 관리가 필요해지는 시점에 2차 구조 개선을 검토한다.
