# OpenMat

- 오픈매트 도메인 모델과 API 스펙을 관리한다.
- 공통 응답, 인증, 날짜/시간 포맷, 사용자 벨트/그랄 계약은 [shared/common-models.md](shared/common-models.md)를 따른다.
- 오픈매트의 이미지 노출과 업로드 규칙은 이 문서에서만 관리하고, `shared/common-models.md`에는 두지 않는다.
- 오픈매트 참가자 조회의 `stripeCount`는 현재 사용자 프로필 `User.stripeCount`를 source of truth로 사용한다.

## 1. 도메인 개요

오픈매트는 로그인 사용자가 게시한 모집글을 목록/상세로 조회하고, 참가 신청/취소, 참가자 관리, 모집 상태 수동 변경, 신고, 삭제, 이미지 추가/조회, 관리자 신고 차단 해제를 수행하는 도메인이다.

현재 구현 범위:

- 오픈매트 생성
- 오픈매트 목록 조회
- 내가 신청한 오픈매트 목록 조회
- 내가 개최한 오픈매트 목록 조회
- 오픈매트 상세 조회
- 오픈매트 수정
- 오픈매트 이미지 업로드 URL 발급
- 오픈매트 신청
- 오픈매트 신청 취소
- 오픈매트 참가자 목록 조회
- 오픈매트 참가자 강제 취소
- 오픈매트 모집 상태 수동 변경
- 오픈매트 신고
- 오픈매트 삭제
- 관리자 신고 차단 해제
- 상태 자동 동기화 스케줄러
- 참가자 대상 수정/삭제 알림 발송

## 2. 도메인 모델

### 2.1 `OpenMat`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 오픈매트 ID |
| `host` | `User` | 작성자 |
| `title` | `String` | 제목 |
| `description` | `String` | 설명 |
| `startDateTime` | `LocalDateTime` | 시작 일시 |
| `endDateTime` | `LocalDateTime` | 종료 일시 |
| `locationName` | `String?` | 장소명 |
| `address` | `String?` | 주소 |
| `latitude` | `BigDecimal?` | 위도 |
| `longitude` | `BigDecimal?` | 경도 |
| `imageUrlsJson` | `String?` | 이미지 URL JSON 저장값 |
| `region` | `Region` | 지역 |
| `participantUids` | `List<Long>` | 참가자 사용자 ID 목록 |
| `maxCapacity` | `Integer` | 최대 정원, `-1`은 무제한 |
| `status` | `OpenMatStatus` | 현재 모집 상태 |
| `reportCount` | `Integer` | 신고 누적 수 |
| `isHidden` | `Boolean` | 삭제/숨김 여부 |
| `deletedAt` | `LocalDateTime?` | 삭제 시각 |
| `manualClosed` | `Boolean` | 작성자 수동 마감 여부 |
| `hostInstagramId` | `String?` | 작성자 인스타그램 ID |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

현재 구현 메모:

- `participantUids`는 신청 순서를 보존한다.
- `status`는 저장값이지만 읽기/쓰기 시점에 현재 시간과 정원 기준으로 다시 동기화될 수 있다.
- `manualClosed = true`이면 정원이 남아 있어도 `CLOSED` 상태를 유지한다.
- `reportCount >= 3`이면 신규 신청이 차단된다.
- `imageUrlsJson`은 응답용 `imageUrls`의 저장 원본이며, 비정상 JSON이면 응답은 빈 배열로 fallback 한다.

### 2.2 `OpenMatStatus`

현재 값:

- `RECRUITING`
- `CLOSED`
- `FINISHED`

현재 구현 메모:

- `RECRUITING`: 모집중
- `CLOSED`: 모집 마감
- `FINISHED`: 종료
- 종료 시각이 지나면 스케줄러와 조회 로직에서 `FINISHED`로 동기화한다.

### 2.3 `Region`

현재 값:

- `SEOUL`
- `GYEONGGI`
- `INCHEON`
- `DAEJEON`
- `SEJONG`
- `CHUNGBUK`
- `CHUNGNAM`
- `BUSAN`
- `DAEGU`
- `ULSAN`
- `GYEONGBUK`
- `GYEONGNAM`
- `GWANGJU`
- `JEONBUK`
- `JEONNAM`
- `GANGWON`
- `JEJU`

### 2.4 `OpenMatCreateRequest`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `title` | `String` | 제목 |
| `description` | `String` | 설명 |
| `startDateTime` | `LocalDateTime` | 시작 일시 |
| `endDateTime` | `LocalDateTime` | 종료 일시 |
| `locationName` | `String` | 장소명 |
| `address` | `String` | 주소 |
| `latitude` | `BigDecimal?` | 위도 |
| `longitude` | `BigDecimal?` | 경도 |
| `region` | `Region` | 지역 |
| `maxCapacity` | `Integer` | 최대 정원 |
| `hostInstagramId` | `String?` | 작성자 인스타그램 ID |
| `imageUrls` | `List<String>?` | 오픈매트 이미지 URL 목록 |

현재 구현 메모:

- 시작/종료 시간은 필수다.
- `endDateTime`은 `startDateTime`보다 뒤여야 한다.
- `maxCapacity`는 `-1`(무제한) 또는 `1` 이상만 허용한다.
- 좌표는 둘 다 보내거나 둘 다 비워야 한다. 하나만 보내면 검증 오류다.
- 좌표가 모두 `null`이면 좌표 없는 오픈매트로 저장된다.
- `imageUrls`는 최대 3장까지 허용한다.
- 생성 요청에서 `imageUrls`가 없거나 `null`이면 이미지 없이 저장한다.
- 저장 가능한 URL은 오픈매트 이미지 업로드 API가 발급한 공개 URL prefix를 따라야 한다.
- 중복 URL은 허용하지 않는다.

### 2.5 `OpenMatUpdateRequest`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `title` | `String?` | 제목 |
| `description` | `String?` | 설명 |
| `startDateTime` | `LocalDateTime?` | 시작 일시 |
| `endDateTime` | `LocalDateTime?` | 종료 일시 |
| `locationName` | `String?` | 장소명 |
| `address` | `String?` | 주소 |
| `latitude` | `BigDecimal?` | 위도 |
| `longitude` | `BigDecimal?` | 경도 |
| `region` | `Region?` | 지역 |
| `maxCapacity` | `Integer?` | 최대 정원 |
| `hostInstagramId` | `String?` | 작성자 인스타그램 ID |
| `imageUrls` | `List<String>?` | 오픈매트 이미지 URL 목록 |

현재 구현 메모:

- 전달되지 않은 필드는 기존 값을 유지한다.
- `latitude`와 `longitude`를 둘 다 명시적으로 `null`로 보내면 기존 좌표를 제거한다.
- 좌표는 둘 다 보내거나 둘 다 비워야 한다. 하나만 보내면 검증 오류다.
- `maxCapacity`를 현재 참가자 수보다 작게 줄일 수는 없다.
- `imageUrls`가 없으면 기존 이미지를 유지한다.
- `imageUrls`를 빈 배열로 보내면 이미지를 모두 삭제한다.
- `imageUrls`를 명시적으로 `null`로 보내면 요청 오류다.
- `imageUrls`는 최대 3장까지 허용한다.
- 중복 URL은 허용하지 않는다.

### 2.6 `OpenMatHostStatusUpdateRequest`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `status` | `OpenMatStatus` | 수동 변경할 상태 |

현재 구현 메모:

- 작성자는 `RECRUITING` 또는 `CLOSED`만 설정할 수 있다.
- `FINISHED`는 수동 지정할 수 없다.

### 2.7 `OpenMatResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 오픈매트 ID |
| `title` | `String` | 제목 |
| `description` | `String` | 설명 |
| `startDateTime` | `LocalDateTime` | 시작 일시 |
| `endDateTime` | `LocalDateTime` | 종료 일시 |
| `locationName` | `String?` | 장소명 |
| `address` | `String?` | 주소 |
| `latitude` | `BigDecimal?` | 위도 |
| `longitude` | `BigDecimal?` | 경도 |
| `imageUrls` | `List<String>` | 이미지 URL 목록 |
| `region` | `Region` | 지역 |
| `maxCapacity` | `Integer` | 최대 정원 |
| `currentParticipants` | `Integer` | 현재 참가자 수 |
| `status` | `OpenMatStatus` | 현재 상태 |
| `reported` | `Boolean` | 신고 차단 여부 |
| `hostId` | `Long` | 작성자 ID |
| `hostNickname` | `String` | 작성자 닉네임 |
| `hostInstagramId` | `String?` | 작성자 인스타그램 ID |
| `deleted` | `Boolean` | 삭제 여부 |
| `deletedAt` | `LocalDateTime?` | 삭제 시각 |
| `createdAt` | `LocalDateTime` | 생성 시각 |

현재 구현 메모:

- 목록/상세/내 오픈매트/작성 오픈매트/생성/수정/관리자 차단 해제 모두 같은 응답 타입을 사용한다.
- 참가자 목록은 `OpenMatResponse`가 아니라 별도 참가자 응답을 사용한다.
- `imageUrls`는 이미지가 없으면 빈 배열이다.
- `deleted`는 `isHidden`을 반영한 파생값이다.
- `reported`는 `reportCount >= 3` 상태를 의미한다.

### 2.8 `OpenMatParticipantResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | `Long` | 참가자 사용자 ID |
| `name` | `String` | 참가자 이름 |
| `affiliation` | `String?` | 소속 |
| `beltColor` | `BeltColor` | 참가자 벨트 |
| `stripeCount` | `Integer?` | 참가자 그랄 수 |

현재 구현 메모:

- 참가자 목록은 현재 사용자 상태를 보여주는 용도다.
- `beltColor`와 `stripeCount`는 모두 `User` 현재 상태를 사용한다.
- 훈련일지 `PROMOTION` 기록의 최신값을 다시 계산해서 쓰지 않는다.

### 2.9 `OpenMatImageUploadUrlRequest`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `fileName` | `String` | 원본 파일명 |
| `contentType` | `String` | 파일 content type |

### 2.10 `OpenMatImageUploadUrlResponse`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `uploadUrl` | `String` | S3 업로드용 presigned URL |
| `imageKey` | `String` | 서버 저장용 S3 object key |
| `imageUrl` | `String` | 업로드 후 접근 가능한 공개 이미지 URL |
| `expiresAt` | `LocalDateTime` | URL 만료 시각 |

### 2.11 `OpenMatUpdatedEvent`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `openMatId` | `Long` | 오픈매트 ID |
| `openMatTitle` | `String` | 오픈매트 제목 |
| `participantUserIds` | `List<Long>` | 알림 대상 참가자 ID |

### 2.12 `OpenMatDeletedEvent`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `openMatId` | `Long` | 오픈매트 ID |
| `openMatTitle` | `String` | 오픈매트 제목 |
| `participantUserIds` | `List<Long>` | 알림 대상 참가자 ID |

## 3. API 스펙

### 3.1 오픈매트 생성

`POST /api/v1/open-mats`

- 인증: 필요

Response data: `OpenMatResponse`

현재 구현 메모:

- 생성 직후 상태는 `RECRUITING` 기준으로 시작한다.
- 좌표는 optional이다.
- `imageUrls`는 0~3장까지 허용한다.

### 3.2 오픈매트 목록 조회

`GET /api/v1/open-mats`

- 인증: 선택

Query params:

- `region`
- `status`
- `q`
- pageable

Response data: `Page<OpenMatResponse>`

현재 구현 메모:

- 로그인 사용자가 호출하면 차단한 작성자의 오픈매트는 제외된다.
- 비회원 호출도 가능하다.
- 기본 정렬은 `createdAt desc, id desc`다.

### 3.3 내가 신청한 오픈매트 목록

`GET /api/v1/open-mats/my`

- 인증: 필요

Response data: `Page<OpenMatResponse>`

현재 구현 메모:

- 기본 정렬은 `startDateTime asc`다.

### 3.4 내가 개최한 오픈매트 목록

`GET /api/v1/open-mats/my-hosting`

- 인증: 필요

Response data: `Page<OpenMatResponse>`

현재 구현 메모:

- 기본 정렬은 `startDateTime asc`다.

### 3.5 오픈매트 상세 조회

`GET /api/v1/open-mats/{id}`

- 인증: 선택

Response data: `OpenMatResponse`

현재 구현 메모:

- 삭제된 오픈매트는 호스트, 참가자, 알림을 받은 사용자, 관리자만 볼 수 있다.
- 일반 조회에서는 숨김 처리된 오픈매트는 찾을 수 없는 것으로 응답한다.
- 로그인 사용자가 조회하면 차단한 작성자의 오픈매트는 `NOT_FOUND`로 처리된다.
- 응답에는 `imageUrls`가 포함된다.

### 3.6 오픈매트 수정

`PUT /api/v1/open-mats/{id}`

- 인증: 필요

Response data: `OpenMatResponse`

현재 구현 메모:

- 작성자만 수정할 수 있다.
- 일정, 장소, 좌표, 지역이 바뀌면 참가자에게 수정 알림이 발송된다.
- `imageUrls`가 없으면 기존 이미지를 유지한다.
- `imageUrls`를 빈 배열로 보내면 이미지가 모두 삭제된다.

### 3.7 이미지 업로드 URL 발급

`POST /api/v1/open-mats/image-upload-url`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `fileName` | `String` | Y | 원본 파일명 |
| `contentType` | `String` | Y | 파일 content type |

Response data:

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `uploadUrl` | `String` | S3 presigned PUT URL |
| `imageKey` | `String` | 서버 저장용 S3 object key |
| `imageUrl` | `String` | 업로드 후 공개 접근 URL |
| `expiresAt` | `LocalDateTime` | presigned URL 만료 시각 |

현재 구현 메모:

- 업로드 endpoint는 `jpg`, `jpeg`, `png`만 허용한다.
- 업로드 S3 key prefix는 `openmats/images/`다.
- 클라이언트는 발급받은 `imageUrl`을 `OpenMatCreateRequest.imageUrls` 또는 `OpenMatUpdateRequest.imageUrls`에 넣어 저장한다.
- `cloud.aws.s3.public-base-url`이 설정된 환경에서는 그 prefix로 시작하는 URL만 저장할 수 있다.

### 3.8 오픈매트 신청

`POST /api/v1/open-mats/{id}/apply`

- 인증: 필요

Response data: `null`

현재 구현 메모:

- 작성자는 자기 오픈매트에 신청할 수 없다.
- 이미 신청한 오픈매트에는 중복 신청할 수 없다.
- `CLOSED` 또는 `FINISHED` 상태, 신고 차단 상태, 정원 초과 상태에서는 신청할 수 없다.

### 3.9 오픈매트 신청 취소

`DELETE /api/v1/open-mats/{id}/apply`

- 인증: 필요

Response data: `null`

현재 구현 메모:

- 신청하지 않은 오픈매트는 취소할 수 없다.

### 3.10 오픈매트 참가자 목록 조회

`GET /api/v1/open-mats/{id}/participants`

- 인증: 필요

Response data: `List<OpenMatParticipantResponse>`

현재 구현 메모:

- 참가자 순서는 신청 순서를 유지한다.
- 참가자 목록에는 `beltColor`와 `stripeCount`가 함께 노출된다.
- 탈퇴한 사용자는 목록에서 제외된다.

### 3.11 오픈매트 참가자 강제 취소

`DELETE /api/v1/open-mats/{id}/participants/{participantUserId}`

- 인증: 필요

Response data: `null`

현재 구현 메모:

- 작성자만 호출할 수 있다.
- 참가자가 없으면 `PARTICIPANT_NOT_FOUND`를 반환한다.
- 종료된 오픈매트에서는 강제 취소할 수 없다.

### 3.12 오픈매트 모집 상태 수동 변경

`PATCH /api/v1/open-mats/{id}/status`

- 인증: 필요

Response data: `OpenMatResponse`

현재 구현 메모:

- 작성자만 호출할 수 있다.
- `RECRUITING`, `CLOSED`만 허용한다.
- `CLOSED`로 바꾸면 수동 마감 상태가 유지된다.
- 정원이 가득 찬 상태에서는 `RECRUITING`으로 되돌릴 수 없다.

### 3.13 오픈매트 신고

`POST /api/v1/open-mats/{id}/report`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `ReportReason` | Y | 신고 사유 |
| `customReason` | `String` | - | 기타 신고 사유 |

Response data: `null`

현재 구현 메모:

- 신고는 `FALSE_INFO`, `INAPPROPRIATE`, `SPAM`, `OTHER`를 허용한다.
- 동일 사용자는 같은 오픈매트를 한 번만 신고할 수 있다.
- 작성자는 자신의 오픈매트를 신고할 수 없다.
- 신고 누적이 3건 이상이면 신규 신청이 차단된다.

### 3.14 오픈매트 삭제

`DELETE /api/v1/open-mats/{id}`

- 인증: 필요

Response data: `null`

현재 구현 메모:

- 작성자만 삭제할 수 있다.
- 삭제 시 참가자에게 삭제 알림이 발송된다.
- 삭제된 오픈매트는 `deleted = true`, `deletedAt`을 포함한다.

### 3.15 관리자 신고 차단 해제

`PATCH /api/v1/admin/open-mats/{id}/report-block`

- 인증: 관리자 필요

Response data: `OpenMatResponse`

현재 구현 메모:

- 신고 누적으로 차단된 오픈매트의 `reportCount`를 0으로 초기화한다.
- 운영자 전용 API다.

## 4. 도메인 규칙

- 오픈매트는 `startDateTime`, `endDateTime`, `maxCapacity`, `region`을 핵심 운영 필드로 사용한다.
- `maxCapacity = -1`은 무제한이다.
- `endDateTime`이 현재 시각을 지나면 `FINISHED`로 동기화된다.
- `manualClosed = true`이면 `CLOSED`를 유지한다.
- `reportCount >= 3`이면 신규 신청을 막는다.
- 목록 조회는 작성자 차단 상태를 반영하고, 상세 조회는 삭제된 오픈매트의 접근 권한을 별도로 검사한다.
- 오픈매트 수정/삭제는 작성자만 가능하다.
- 참가자 알림은 `OpenMatUpdatedEvent`, `OpenMatDeletedEvent`를 통해 after-commit으로 발송한다.
- 오픈매트 상태 자동 동기화는 스케줄러가 수행한다.
- 좌표는 생성/수정 모두에서 pair 단위로 다룬다.
- 이미지 URL은 최대 3장까지 허용한다.
- 저장 가능한 이미지 URL은 업로드 API가 발급한 공개 URL prefix만 허용한다.

## 5. 구현 메모

- `OpenMatResponse`는 참가자 목록을 포함하지 않는다. 참가자 목록은 `GET /api/v1/open-mats/{id}/participants`에서 별도로 조회한다.
- `GET /api/v1/open-mats/{id}`는 공개 조회이지만, 삭제된 오픈매트는 호스트/참가자/알림 수신자/관리자만 볼 수 있다.
- `GET /api/v1/open-mats`는 로그인 사용자의 `Authorization` 헤더가 있으면 차단 필터가 적용된다.
- `GET /api/v1/open-mats/my`와 `GET /api/v1/open-mats/my-hosting`은 정렬 기본값이 `startDateTime asc`다.
- 좌표 업데이트는 `latitude`와 `longitude`가 모두 명시된 경우에만 반영한다.
- 작성자 관리 UI는 상세 화면에서 직접 참가자 강제 취소와 모집 상태 변경을 호출한다.
- 이미지가 없거나 저장된 JSON이 비정상이면 응답의 `imageUrls`는 빈 배열로 fallback 한다.
