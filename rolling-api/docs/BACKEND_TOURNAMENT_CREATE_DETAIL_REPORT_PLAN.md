# 대회 생성/상세/신고 API 설계안

기준일: 2026-04-11

## 1. 목적

- 사용자가 직접 대회를 등록할 수 있게 한다.
- 대회 이미지 관리는 S3를 기준으로 정리하고, 목록/상세 응답 형태는 크롤링 대회와 동일하게 유지한다.
- 대회 상세 조회는 작성자 포함 모든 사용자가 동일한 응답으로 볼 수 있게 한다.
- 대회 신고는 공통 `Report` 도메인을 재사용해 운영 흐름과 연결한다.

## 2. 현재 코드 기준 확인 결과

- 대회 조회 응답은 현재 `TournamentResponse` 하나로 통일돼 있다.
- 크롤링 저장 흐름은 이미 `S3Uploader`를 사용해 포스터 이미지를 S3로 옮긴 뒤 저장한다.
- 수동 대회 생성 API는 현재 `posterUrl`을 문자열로만 저장한다.
- `ReportTargetType`에는 이미 `TOURNAMENT`가 존재한다.
- `ReportService`는 대상 타입만 맞으면 대회 신고에도 그대로 재사용할 수 있다.
- 대회 상세 조회는 현재도 공용 조회 API로 제공되며, 작성자만을 위한 별도 상세 API는 없다.

## 3. 설계 원칙

- 응답 형태는 수동 등록 대회와 크롤링 대회가 동일해야 한다.
- 저장 방식은 외부 URL 그대로 보관하는 방식보다, S3 key를 저장하고 응답 시 public URL로 변환하는 방식을 우선 권장한다.
- 사용자가 이미지를 올리는 경우에도 클라이언트는 최종적으로 `posterUrl` 형태의 응답을 받는다.
- 상세 조회는 작성자 포함 누구나 동일한 응답을 보게 하고, 작성자 전용 제어는 수정/삭제 같은 관리 API로 분리한다.
- 신고는 공통 신고 도메인을 사용하고, 대회 대상만 `TOURNAMENT`로 분기한다.

## 4. 권장 아키텍처

### 4.1 이미지 업로드

권장안: `presigned URL` 기반 S3 직접 업로드

- 서버는 이미지 업로드용 presigned URL과 S3 object key를 발급한다.
- 클라이언트는 presigned URL로 이미지를 S3에 직접 업로드한다.
- 대회 생성 시에는 업로드한 이미지의 `posterKey`만 서버로 전달한다.
- 조회 응답에서는 `posterKey`를 기반으로 public `posterUrl`을 만들어 내려준다.

선택 이유:

- 서버가 이미지 바이너리를 직접 중계하지 않아도 된다.
- 목록/상세 응답은 기존 `posterUrl` 필드 형태를 그대로 유지할 수 있다.
- 크롤링 대회와 수동 등록 대회를 같은 응답 모델로 합치기 쉽다.

### 4.2 상세 조회

- `GET /api/v1/tournaments/{id}`는 작성자 포함 모든 사용자가 동일한 `TournamentResponse`를 받는다.
- 작성자라고 해서 별도 상세 응답을 만들지 않는다.
- 작성자 전용 정보가 필요해지면 나중에 `editable` 같은 보조 필드를 추가하는 방식으로 확장한다.

### 4.3 신고

- `POST /api/v1/tournaments/{id}/report`를 추가한다.
- 내부 처리는 `ReportService.createReport(...)`를 재사용한다.
- 대상 타입은 `ReportTargetType.TOURNAMENT`로 고정한다.
- 신고 사유와 검증 규칙은 오픈매트 신고와 같은 공통 정책을 따른다.

## 5. API 제안

### 5.1 이미지 업로드 URL 발급

`POST /api/v1/tournaments/poster-upload-url`

- 인증: 필요
- 용도: 대회 포스터를 S3에 직접 업로드하기 위한 임시 URL 발급

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `fileName` | `String` | O | 원본 파일명 |
| `contentType` | `String` | O | 예: `image/jpeg`, `image/png` |

Response data:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `uploadUrl` | `String` | S3 업로드용 presigned URL |
| `posterKey` | `String` | 서버 저장용 S3 object key |
| `expiresAt` | `DateTime` | URL 만료 시각 |

현재 구현 메모:

- 업로드 성공 후 클라이언트는 생성 API에 `posterKey`를 전달한다.
- 파일 확장자와 MIME type 검증은 서버가 담당한다.

### 5.2 대회 생성

`POST /api/v1/tournaments`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | `String` | O | 대회 제목 |
| `organizer` | `String?` | - | 주최사 |
| `posterKey` | `String` | O | S3에 업로드된 포스터 object key |
| `competitionDate` | `Date` | O | 대회 개최일 |
| `registrationDeadline` | `Date` | O | 접수 마감일 |
| `location` | `String?` | - | 개최 장소 |
| `applyLink` | `String` | O | 외부 접수 링크 |

Response data:

- `TournamentResponse`

현재 구현 메모:

- 응답의 `posterUrl`은 `posterKey`를 기반으로 생성한다.
- 목록 조회와 상세 조회는 같은 응답 형태를 사용한다.
- 크롤링 대회와 수동 등록 대회의 표시 구조가 같아야 한다.

### 5.3 대회 상세 조회

`GET /api/v1/tournaments/{id}`

- 인증: 불필요
- 작성자 포함 모든 사용자가 같은 응답을 본다.
- Response data: `TournamentResponse`

현재 구현 메모:

- 작성자 여부에 따라 응답 필드를 달리하지 않는다.
- 작성자도 비로그인 사용자와 동일하게 상세 정보를 볼 수 있다.

### 5.4 대회 신고

`POST /api/v1/tournaments/{id}/report`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `ReportReason` | O | `FALSE_INFO`, `INAPPROPRIATE`, `SPAM`, `OTHER` |
| `customReason` | `String?` | - | `OTHER`일 때만 사용 |

Response data:

- `null`

에러:

- `ALREADY_REPORTED`
- `SELF_REPORT_NOT_ALLOWED`
- `VALIDATION_ERROR`
- `NOT_FOUND`

현재 구현 메모:

- 신고 저장은 공통 `ReportService`를 사용한다.
- 동일 사용자는 같은 대회를 한 번만 신고할 수 있다.
- 자기 작성 대회 신고는 차단한다.

## 6. Phase별 실행 계획

### Phase 1. 요구사항 고정

- [x] 이미지 업로드 방식을 `presigned URL + S3 direct upload`로 확정한다.
- [x] 대회 생성 요청의 이미지 입력값을 `posterKey`로 고정한다.
- [x] 상세 조회는 작성자 포함 모든 사용자가 동일 응답을 받는 것으로 확정한다.
- [x] 대회 신고 대상 타입을 `TOURNAMENT`로 확정한다.
- [x] 대회 신고 사유를 공통 `ReportReason`으로 통일한다.
- [x] 이미지 허용 형식을 `jpg`, `png` 우선으로 두고 `webp`, `gif`는 선택 확장으로 둔다.

결정 메모:

- `posterUrl`은 응답 전용으로 두고, 저장은 `posterKey` 중심으로 관리하는 방식을 권장한다.
- 작성자 전용 상세 응답은 만들지 않는다.
- `webp`, `gif`는 필수 형식이 아니라 추후 확장 가능한 선택 형식으로 둔다.

완료 기준:

- 대회 생성, 상세, 신고의 제품 규칙이 하나로 정리된다.
- 이미지 저장 방식이 목록/상세 응답 형태와 충돌하지 않는다.

### Phase 2. API 계약 정리

- [x] `POST /api/v1/tournaments/poster-upload-url` 계약을 확정한다.
- [x] `POST /api/v1/tournaments` 요청/응답 계약을 확정한다.
- [x] `GET /api/v1/tournaments/{id}` 응답이 목록과 동일한 `TournamentResponse`인지 재확인한다.
- [x] `POST /api/v1/tournaments/{id}/report` 요청/응답 계약을 확정한다.
- [x] 에러 응답 목록을 정리한다.

결과 메모:

- 생성 API는 업로드된 파일 자체가 아니라 `posterKey`만 받는 구조를 권장한다.
- 신고 API는 공통 신고 도메인을 재사용한다.

완료 기준:

- 프론트와 백엔드가 같은 필드 이름으로 연동할 수 있다.
- 작성자/일반 사용자/신고 흐름의 API 경계가 명확해진다.

### Phase 3. 도메인/저장소 반영

- [x] `tournaments` 테이블에 `poster_key` 컬럼을 추가한다.
- [x] 수동 생성 대회는 `posterKey`를 저장하도록 변경한다.
- [x] 크롤링 대회는 기존처럼 `posterUrl`을 유지하되, 필요 시 `posterKey`를 비워둔다.
- [x] `TournamentResponse`에서 `posterKey` 우선, `posterUrl` 보조 방식으로 응답 URL을 조립한다.
- [x] S3 public URL 생성 로직을 공통화한다.

결정 메모:

- 기존 크롤링 파이프라인을 무리하게 뒤엎지 않고, 수동 생성만 먼저 S3 정규화한다.
- 이후 크롤러도 동일한 저장 규칙으로 천천히 맞출 수 있게 둔다.

완료 기준:

- 저장 구조와 응답 구조가 분리된다.
- 크롤링 대회와 수동 대회가 같은 화면에서 깨지지 않는다.

### Phase 4. 서비스/컨트롤러 구현

- [x] 이미지 업로드 URL 발급 API를 구현한다.
- [x] 대회 생성 API가 `posterKey`를 받아 저장하도록 구현한다.
- [x] 대회 상세 조회는 작성자 포함 공용 응답으로 유지한다.
- [x] 대회 신고 API를 `ReportService`와 연결한다.
- [x] 신고 시 자기 자신 대상, 중복 신고, 대상 없음 예외를 연결한다.

완료 기준:

- 사용자는 이미지를 업로드하고 대회를 생성할 수 있다.
- 작성자는 별도 제한 없이 상세 조회를 할 수 있다.
- 대회 신고가 운영 신고 도메인에 적재된다.

### Phase 5. 테스트 보강

- [x] 대회 생성 시 `posterKey`가 `posterUrl`로 응답되는지 테스트한다.
- [x] 대회 신고 기본 성공 경로 테스트를 추가한다.
- [x] 상세 조회가 작성자/비작성자 모두 동일 응답을 주는지 테스트한다.
- [x] 대회 신고 중복 방지 테스트를 추가한다.
- [x] 자기 작성 대회 신고 차단 테스트를 추가한다.
- [x] 크롤링 대회와 수동 대회의 응답 필드 일치 여부를 확인한다.

완료 기준:

- 이미지 업로드와 신고 로직이 회귀 없이 고정된다.
- 응답 형태가 수동/크롤링 간 동일함이 테스트로 보장된다.

### Phase 6. 문서 동기화

- [x] `docs/AGENTS.md`의 대회 계약을 갱신한다.
- [ ] 필요 시 관리자 문서에도 대회 신고 운영 경로를 반영한다.

완료 기준:

- 코드, 계약 문서, 운영 메모의 내용이 일치한다.

## 7. 우선순위

1. Phase 1 요구사항 고정
2. Phase 2 API 계약 정리
3. Phase 3 저장 구조 반영
4. Phase 4 서비스/컨트롤러 구현
5. Phase 5 테스트 보강
6. Phase 6 문서 동기화

## 8. 리스크 및 판단 포인트

- `posterUrl`만 저장하면 향후 삭제/교체/권한 관리가 어려워질 수 있다.
- presigned URL 방식은 구현이 깔끔하지만, 클라이언트 업로드 실패와 만료 재시도 UX를 같이 설계해야 한다.
- 상세 조회를 작성자 포함 공용으로 유지하면 단순하지만, 작성자 전용 메타데이터가 필요할 때 별도 필드 확장이 필요하다.
- 신고 API는 공통 도메인을 재사용하는 만큼, 대회 전용 추가 규칙이 생기면 `ReportService`의 대상별 검증 확장이 필요하다.

## 9. 이번 1차 결론

- 대회 이미지는 `S3 presigned URL + posterKey` 방식으로 관리하는 것을 권장한다.
- 대회 상세 조회는 작성자 포함 모든 사용자가 동일한 `TournamentResponse`를 보도록 유지한다.
- 대회 신고는 기존 공통 `Report` 도메인에 `TOURNAMENT`만 추가해 연결하는 것이 가장 현실적이다.
