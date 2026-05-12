# Seminar

- 세미나 도메인 모델과 API 스펙을 관리한다.
- 제품 범위와 출시 우선순위는 [seminar-product-plan.md](seminar-product-plan.md)를 본다.
- 공통 응답, 인증, 날짜/시간 형식은 [shared/common-models.md](shared/common-models.md)를 따른다.
- 세미나는 오픈매트와 유사한 공개 조회, 인증 기반 생성/신청, 차단 필터 정책을 재사용한다.
- 현재 구현 상태는 Phase 0~2이며, 호스트 신청자 관리, 모집 상태 변경, 신고, 알림 저장은 후속 Phase 범위다.

## 1. 도메인 개요

세미나는 호스트가 유료 또는 무료 주짓수 세미나를 등록하고, 사용자가 세미나 상세 정보를 확인한 뒤 참석 신청하는 도메인이다.

현재 구현 범위:

- 세미나 목록/상세 공개 조회
- 세미나 생성/수정/삭제
- 참석 신청/취소
- 내 신청 세미나 조회
- 로그인 사용자 기준 차단한 호스트 세미나 숨김
- 세미나 삭제 시 활성 신청 `SEMINAR_CANCELED` 전환

후속 Phase 범위:

- 앱 내 결제
- 대기 신청
- 현장 체크인
- 후기/평점
- 호스트 신청자 관리
- 모집 상태 변경
- 세미나 신고
- 주요 변경 알림 저장 및 FCM 발송 시도
- 내 주최 세미나 조회

## 2. 도메인 모델

### 2.1 SeminarModel

세미나 목록과 상세 응답에서 사용하는 기본 모델이다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 세미나 ID |
| `title` | `String` | 제목 |
| `description` | `String` | 설명 |
| `mainImageUrl` | `String?` | 대표 이미지 URL |
| `instructorName` | `String` | 강사명 |
| `instructorBio` | `String?` | 강사 소개 |
| `curriculum` | `String?` | 주제 또는 커리큘럼 |
| `targetAudience` | `String?` | 참가 대상 |
| `preparation` | `String?` | 준비물 |
| `contactInfo` | `String?` | 문의 연락처 |
| `hostInstagramId` | `String?` | 호스트 또는 세미나 문의용 인스타그램 ID |
| `startDateTime` | `DateTime` | 시작 일시 |
| `endDateTime` | `DateTime` | 종료 일시 |
| `applicationStartDateTime` | `DateTime?` | 신청 시작 일시. 없으면 생성 즉시 신청 가능 |
| `applicationEndDateTime` | `DateTime?` | 신청 마감 일시. 없으면 시작 전까지 신청 가능 |
| `locationName` | `String` | 장소명 |
| `address` | `String` | 주소 |
| `latitude` | `Decimal?` | 위도 |
| `longitude` | `Decimal?` | 경도 |
| `region` | `Region` | 지역 |
| `maxCapacity` | `Integer` | 최대 정원. `-1`이면 정원 제한 없음 |
| `appliedCount` | `Integer` | 참석 신청 완료 인원 |
| `remainingCapacity` | `Integer?` | 남은 정원. 무제한이면 `null` |
| `price` | `Integer` | 참가비. 무료면 `0` |
| `paymentGuide` | `String?` | 결제 안내. MVP에서는 외부 결제/현장 결제 안내 문구 |
| `refundPolicy` | `String?` | 환불 또는 취소 안내 |
| `status` | `SeminarStatus` | 세미나 모집/운영 상태 |
| `myApplicationStatus` | `SeminarApplicationStatus?` | 현재 로그인 사용자의 신청 상태. 비로그인 또는 미신청이면 `null` |
| `hostId` | `Long` | 호스트 사용자 ID |
| `hostNickname` | `String` | 호스트 닉네임 |
| `reported` | `Boolean` | 현재 구현에서는 항상 `false`. 신고 API 구현 후 현재 로그인 사용자의 신고 여부 |
| `deleted` | `Boolean` | soft delete 여부 |
| `deletedAt` | `DateTime?` | 삭제 일시 |
| `createdAt` | `DateTime` | 생성 일시 |
| `updatedAt` | `DateTime` | 수정 일시 |

현재 구현 메모:

- `myApplicationStatus`와 `reported`는 인증 사용자의 개인화 필드다.
- 비회원 공개 조회에서는 `myApplicationStatus=null`, `reported=false`로 응답해도 된다.
- 로그인 사용자가 차단한 작성자의 세미나는 목록과 상세에서 숨긴다.
- soft delete된 세미나는 일반 사용자에게 `NOT_FOUND`로 처리한다.
- 삭제된 세미나는 일반 조회에서 제외된다.

### 2.2 SeminarApplicationModel

호스트 신청자 목록과 내 신청 세미나 화면에서 사용하는 참석 신청 모델이다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 신청 ID |
| `seminarId` | `Long` | 세미나 ID |
| `seminarTitle` | `String` | 세미나 제목 |
| `userId` | `Long` | 신청자 사용자 ID |
| `nickname` | `String` | 신청자 닉네임 |
| `affiliation` | `String?` | 신청자 소속 |
| `beltColor` | `String` | 신청자 벨트 |
| `status` | `SeminarApplicationStatus` | 신청 상태 |
| `cancelReason` | `String?` | 취소 또는 강제 취소 사유 |
| `appliedAt` | `DateTime` | 신청 일시 |
| `canceledAt` | `DateTime?` | 취소 일시 |
| `createdAt` | `DateTime` | 생성 일시 |
| `updatedAt` | `DateTime` | 수정 일시 |

### 2.3 SeminarReportModel

후속 Phase에서 세미나 신고를 구현할 때 사용할 예정 모델이다. 현재 Phase 0~2 구현에는 세미나 신고 API가 없다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 신고 ID |
| `seminarId` | `Long` | 세미나 ID |
| `reporterUserId` | `Long` | 신고자 ID |
| `reason` | `ReportReason` | 신고 사유 |
| `customReason` | `String?` | 기타 신고 사유 |
| `createdAt` | `DateTime` | 신고 일시 |

## 3. Enum

### 3.1 SeminarStatus

| Raw value | 의미 | 수동 변경 가능 여부 |
| --- | --- | --- |
| `RECRUITING` | 모집 중 | O |
| `CLOSED` | 모집 마감 | O |
| `CANCELED` | 세미나 취소 | O |
| `FINISHED` | 종료 | X |
| `DELETED` | 삭제됨 | X |

상태 규칙:

- 생성 기본값은 `RECRUITING`이다.
- `maxCapacity > 0`이고 `appliedCount >= maxCapacity`이면 `CLOSED`로 전환할 수 있다.
- 신청 마감 시간이 지났거나 호스트가 직접 마감하면 `CLOSED`가 된다.
- 종료 시간이 지난 세미나는 `FINISHED`로 취급한다.
- `DELETED`는 soft delete된 데이터의 내부 상태이며 일반 사용자 목록에는 노출하지 않는다.
- MVP에서 호스트 수동 상태 변경은 `RECRUITING`, `CLOSED`, `CANCELED`만 허용한다.

### 3.2 SeminarApplicationStatus

| Raw value | 의미 | MVP 포함 |
| --- | --- | --- |
| `APPLIED` | 참석 신청 완료 | O |
| `CANCELED` | 참가자 본인 취소 | O |
| `HOST_CANCELED` | 호스트 강제 취소 | O |
| `SEMINAR_CANCELED` | 세미나 취소로 인한 취소 | O |
| `ATTENDED` | 실제 참석 확인 | - |
| `NO_SHOW` | 미참석 | - |

상태 규칙:

- 정원 계산에는 `APPLIED` 상태만 포함한다.
- 취소된 신청은 이력으로 남기고 같은 사용자가 재신청하면 기존 신청을 재활성화할지 새 row를 만들지 구현 전에 결정해야 한다. API 계약상 사용자는 한 세미나에 활성 신청을 1개만 가질 수 있다.
- `ATTENDED`, `NO_SHOW`는 체크인 기능과 함께 이후 범위로 둔다.

### 3.3 ReportReason

후속 Phase에서 세미나 신고를 구현할 때 기존 신고 정책과 raw value를 맞춘다.

| Raw value | 의미 |
| --- | --- |
| `SPAM` | 광고/도배 |
| `INAPPROPRIATE` | 부적절한 내용 |
| `FRAUD` | 사기 또는 허위 세미나 의심 |
| `OTHER` | 기타 |

## 4. 공통 정책

### 4.1 인증과 권한

- 목록과 상세 조회는 인증 없이 가능하다.
- 참석 신청, 신청 취소, 내 신청 목록 조회는 인증이 필요하다.
- 세미나 생성, 수정, 삭제는 인증이 필요하다.
- 세미나 수정/삭제는 호스트만 가능하다.
- 관리자는 별도 관리자 API가 생기기 전까지 일반 호스트 API 권한을 우회하지 않는다.
- 작성자는 자신이 만든 세미나에 참석 신청할 수 없다.
- 제재 사용자는 세미나 생성과 참석 신청이 차단된다.

### 4.2 날짜와 정렬

- `DateTime`은 ISO 8601 문자열을 사용한다.
- 기본 목록 정렬은 `startDateTime,asc`다.
- 종료된 세미나를 별도 필터로 조회할 수는 있지만 기본 목록에서는 운영 정책에 따라 제외하거나 하단 노출할 수 있다.

### 4.3 좌표

- `latitude`, `longitude`는 둘 다 없으면 좌표 없이 저장한다.
- 둘 중 하나만 있으면 `VALIDATION_ERROR`다.
- `latitude`는 `-90..90`, `longitude`는 `-180..180` 범위만 허용한다.
- 주소 기반 좌표 변환은 기존 `GET /api/v1/maps/kakao/geocode`를 사용한다.

### 4.4 정원

- `maxCapacity`는 `-1` 또는 `1 이상`이어야 한다.
- `-1`은 정원 제한 없음이다.
- `APPLIED` 상태 신청 수가 정원 계산 기준이다.
- 취소로 빈자리가 생겨도 MVP에서는 자동 대기자 승인을 하지 않는다.

### 4.5 알림

알림 저장과 FCM 발송은 아직 구현되지 않았다. 후속 Phase에서 다음 이벤트를 `Notification` 저장 후 FCM 발송 시도 대상으로 추가한다.

- 세미나 참석 신청 완료
- 세미나 참석 신청 취소
- 호스트의 참가자 강제 취소
- 세미나 일정 또는 장소 변경
- 세미나 삭제
- 세미나 취소

현재 구현 메모:

- 알림의 source of truth는 FCM 성공 여부가 아니라 백엔드 `Notification` 저장 데이터다.
- FCM 실패가 세미나 상태 변경 트랜잭션을 실패시키면 안 된다.

### 4.6 차단과 신고

- 로그인 사용자가 차단한 작성자의 세미나는 목록과 상세에서 제외한다.
- 세미나 신고 API는 아직 구현되지 않았다.
- 세미나 신고 구현 시 작성자 자기 신고 차단, 동일 사용자 중복 신고 차단, 신고 누적 자동 숨김 정책을 반영한다.

## 5.8 세미나 API

### 5.8.1 세미나 목록 조회

`GET /api/v1/seminars`

- 인증: 불필요

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `region` | `String` | - | 지역 필터 |
| `status` | `String` | - | 상태 필터. `RECRUITING`, `CLOSED`, `CANCELED`, `FINISHED` |
| `q` | `String` | - | 제목/강사명/장소명/주소 부분 일치 검색 |
| `from` | `DateTime` | - | 시작 일시 하한 |
| `to` | `DateTime` | - | 시작 일시 상한 |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `20` | 페이지 크기 |
| `sort` | `String` | `startDateTime,asc` | 정렬 |

Response: 페이징된 `SeminarModel`

현재 구현 메모:

- 비회원은 공개 세미나만 조회한다.
- 로그인 사용자가 `Authorization` 헤더를 보내면 차단 필터와 `myApplicationStatus`, `reported` 개인화 필드를 적용한다.
- 로그인 사용자가 차단한 작성자의 세미나는 목록에서 제외한다.
- soft delete된 세미나는 목록에서 제외한다.

### 5.8.2 세미나 상세 조회

`GET /api/v1/seminars/{id}`

- 인증: 불필요
- Response: `SeminarModel`

현재 구현 메모:

- 비회원 상세 조회는 `Authorization` 헤더 없이 호출해도 동작해야 한다.
- 로그인 사용자가 차단한 작성자의 세미나는 `NOT_FOUND`로 처리한다.
- soft delete된 세미나는 일반 사용자에게 `NOT_FOUND`로 처리한다.
- 삭제된 세미나는 일반 사용자에게 `NOT_FOUND`로 처리한다.

에러:

- `NOT_FOUND`

### 5.8.3 세미나 생성

`POST /api/v1/seminars`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | `String` | O | 제목 |
| `description` | `String` | O | 설명 |
| `mainImageUrl` | `String?` | - | 대표 이미지 URL |
| `instructorName` | `String` | O | 강사명 |
| `instructorBio` | `String?` | - | 강사 소개 |
| `curriculum` | `String?` | - | 주제 또는 커리큘럼 |
| `targetAudience` | `String?` | - | 참가 대상 |
| `preparation` | `String?` | - | 준비물 |
| `contactInfo` | `String?` | - | 문의 연락처 |
| `hostInstagramId` | `String?` | - | 인스타그램 ID |
| `startDateTime` | `DateTime` | O | 시작 일시 |
| `endDateTime` | `DateTime` | O | 종료 일시 |
| `applicationStartDateTime` | `DateTime?` | - | 신청 시작 일시 |
| `applicationEndDateTime` | `DateTime?` | - | 신청 마감 일시 |
| `locationName` | `String` | O | 장소명 |
| `address` | `String` | O | 주소 |
| `latitude` | `Decimal?` | - | 위도 |
| `longitude` | `Decimal?` | - | 경도 |
| `region` | `Region` | O | 지역 |
| `maxCapacity` | `Integer` | O | 최대 정원. `-1`이면 무제한 |
| `price` | `Integer` | O | 참가비. 무료면 `0` |
| `paymentGuide` | `String?` | - | 결제 안내 |
| `refundPolicy` | `String?` | - | 환불 또는 취소 안내 |

Response: `SeminarModel`

검증:

- 제목, 설명, 강사명, 시작 일시, 종료 일시, 장소명, 주소, 지역, 최대 정원, 가격은 필수다.
- 종료 시간은 시작 시간보다 이후여야 한다.
- 신청 시작 시간과 신청 마감 시간이 모두 있으면 신청 마감 시간은 신청 시작 시간보다 이후여야 한다.
- 신청 마감 시간은 세미나 시작 시간보다 이후일 수 없다.
- `maxCapacity`는 `-1` 또는 `1 이상`이어야 한다.
- `price`는 `0 이상`이어야 한다.
- `latitude`, `longitude`는 둘 다 없거나 둘 다 있어야 한다.
- `latitude`는 값이 있으면 `-90..90` 범위여야 한다.
- `longitude`는 값이 있으면 `-180..180` 범위여야 한다.
- 제재 사용자는 생성할 수 없다.

에러:

- `VALIDATION_ERROR`
- `USER_SANCTIONED`

### 5.8.4 세미나 수정

`PUT /api/v1/seminars/{id}`

- 인증: 필요
- 권한: 호스트 전용

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `title` | `String?` | - | 제목 |
| `description` | `String?` | - | 설명 |
| `mainImageUrl` | `String?` | - | 대표 이미지 URL |
| `instructorName` | `String?` | - | 강사명 |
| `instructorBio` | `String?` | - | 강사 소개 |
| `curriculum` | `String?` | - | 주제 또는 커리큘럼 |
| `targetAudience` | `String?` | - | 참가 대상 |
| `preparation` | `String?` | - | 준비물 |
| `contactInfo` | `String?` | - | 문의 연락처 |
| `hostInstagramId` | `String?` | - | 인스타그램 ID |
| `startDateTime` | `DateTime?` | - | 시작 일시 |
| `endDateTime` | `DateTime?` | - | 종료 일시 |
| `applicationStartDateTime` | `DateTime?` | - | 신청 시작 일시 |
| `applicationEndDateTime` | `DateTime?` | - | 신청 마감 일시 |
| `locationName` | `String?` | - | 장소명 |
| `address` | `String?` | - | 주소 |
| `latitude` | `Decimal?` | - | 위도 |
| `longitude` | `Decimal?` | - | 경도 |
| `region` | `Region?` | - | 지역 |
| `maxCapacity` | `Integer?` | - | 최대 정원 |
| `price` | `Integer?` | - | 참가비 |
| `paymentGuide` | `String?` | - | 결제 안내 |
| `refundPolicy` | `String?` | - | 환불 또는 취소 안내 |

Response: `SeminarModel`

현재 구현 메모:

- 호스트만 수정 가능하다.
- 종료된 세미나는 일반 수정이 불가능하다.
- `CANCELED`, `DELETED` 상태 세미나는 수정할 수 없다.
- 최소 1개 필드는 전달해야 한다.
- 전달하지 않은 필드는 기존 값을 유지한다.
- `latitude`, `longitude`가 둘 다 숫자면 기존 좌표를 새 좌표로 교체한다.
- `latitude`, `longitude`가 둘 다 `null`로 명시되면 기존 좌표를 제거한다.
- `latitude`, `longitude` 중 하나만 전달하면 `VALIDATION_ERROR`다.
- 시작 일시, 종료 일시, 장소명, 주소, 가격, 정원이 변경되어도 현재 구현에서는 알림을 저장하지 않는다.
- `maxCapacity`를 현재 `APPLIED` 인원보다 작은 값으로 줄일 수 없다. 단, `-1`은 허용한다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`
- `VALIDATION_ERROR`
- `SEMINAR_FINISHED`
- `SEMINAR_CANCELED`

### 5.8.5 세미나 삭제

`DELETE /api/v1/seminars/{id}`

- 인증: 필요
- 권한: 호스트 전용

Response data: `null`

현재 구현 메모:

- 삭제는 soft delete다.
- 호스트만 삭제 가능하다.
- 신청자가 있어도 삭제할 수 있다.
- `APPLIED` 신청자가 있으면 신청 상태를 `SEMINAR_CANCELED`로 전환한다.
- 현재 구현에서는 삭제 알림을 저장하지 않는다.
- 삭제된 세미나는 일반 목록과 상세에서 노출되지 않는다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`

### 5.8.6 세미나 참석 신청

`POST /api/v1/seminars/{id}/applications`

- 인증: 필요

Response: `SeminarApplicationModel`

현재 구현 메모:

- 신청 생성자는 accessToken 기준 현재 로그인 사용자다.
- 신청 완료 상태는 `APPLIED`다.
- 현재 구현에서는 신청 완료 알림을 저장하지 않는다.
- 호스트는 자신이 만든 세미나에 신청할 수 없다.
- 동일 사용자는 같은 세미나에 활성 신청을 중복 생성할 수 없다.
- 정원이 찬 세미나는 신청할 수 없다.
- `RECRUITING` 상태에서만 신청할 수 있다.
- 신청 시작 전 또는 신청 마감 후에는 신청할 수 없다.
- 제재 사용자는 신청할 수 없다.

에러:

- `NOT_FOUND`
- `HOST_CANNOT_APPLY`
- `ALREADY_APPLIED`
- `CAPACITY_FULL`
- `SEMINAR_NOT_RECRUITING`
- `APPLICATION_NOT_OPEN`
- `APPLICATION_CLOSED`
- `SEMINAR_FINISHED`
- `USER_SANCTIONED`

### 5.8.7 세미나 참석 신청 취소

`DELETE /api/v1/seminars/{id}/applications/me`

- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `cancelReason` | `String?` | - | 참가자 취소 사유 |

Response: `SeminarApplicationModel`

현재 구현 메모:

- 현재 로그인 사용자의 `APPLIED` 신청만 취소할 수 있다.
- 세미나 시작 전까지만 취소할 수 있다.
- 취소 후 상태는 `CANCELED`다.
- 현재 구현에서는 취소 알림을 저장하지 않는다.
- 취소로 빈자리가 생겨도 MVP에서는 자동 대기자 승인을 하지 않는다.

에러:

- `NOT_FOUND`
- `APPLICATION_NOT_FOUND`
- `APPLICATION_ALREADY_CANCELED`
- `SEMINAR_FINISHED`

### 5.8.8 세미나 신청자 목록 조회

`GET /api/v1/seminars/{id}/applications`

- 구현 상태: 미구현. Phase 3 호스트 관리 범위.
- 인증: 필요
- 권한: 호스트 전용
- Response: 페이징된 `SeminarApplicationModel`

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `status` | `String` | `APPLIED` | 신청 상태 필터 |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `20` | 페이지 크기 |
| `sort` | `String` | `appliedAt,asc` | 정렬 |

현재 구현 메모:

- 호스트만 조회할 수 있다.
- 기본 정렬은 신청 순서 오름차순이다.
- MVP에서는 참가자 연락처를 노출하지 않는다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`

### 5.8.9 세미나 참가자 강제 취소

`PATCH /api/v1/seminars/{id}/applications/{applicationId}/cancel`

- 구현 상태: 미구현. Phase 3 호스트 관리 범위.
- 인증: 필요
- 권한: 호스트 전용

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `cancelReason` | `String?` | - | 호스트 강제 취소 사유 |

Response: `SeminarApplicationModel`

현재 구현 메모:

- 호스트만 강제 취소할 수 있다.
- `APPLIED` 상태 신청만 강제 취소할 수 있다.
- 강제 취소 후 상태는 `HOST_CANCELED`다.
- 취소된 참가자에게 알림을 저장한다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`
- `APPLICATION_ALREADY_CANCELED`
- `SEMINAR_FINISHED`

### 5.8.10 세미나 모집 상태 변경

`PATCH /api/v1/seminars/{id}/status`

- 구현 상태: 미구현. Phase 3 호스트 관리 범위.
- 인증: 필요
- 권한: 호스트 전용

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | `String` | O | `RECRUITING`, `CLOSED`, `CANCELED` |
| `reason` | `String?` | - | `CANCELED` 변경 사유 |

Response: `SeminarModel`

현재 구현 메모:

- 호스트만 변경할 수 있다.
- `FINISHED`, `DELETED`는 수동 변경 대상이 아니다.
- `CANCELED`로 변경하면 모든 `APPLIED` 신청을 `SEMINAR_CANCELED`로 전환한다.
- 알림 저장은 Phase 4 범위다.
- `RECRUITING`으로 변경할 때 정원이 이미 찼으면 `CAPACITY_FULL`로 실패한다.
- 종료된 세미나는 상태를 변경할 수 없다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`
- `VALIDATION_ERROR`
- `CAPACITY_FULL`
- `SEMINAR_FINISHED`

### 5.8.11 내 신청 세미나 목록

`GET /api/v1/seminars/my-applications`

- 인증: 필요
- Response: 페이징된 `SeminarModel`

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `status` | `String` | - | 신청 상태 필터 |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `10` | 페이지 크기 |
| `sort` | `String` | `startDateTime,asc` | 정렬 |

현재 구현 메모:

- 현재 로그인 사용자가 신청한 세미나만 조회한다.
- `status`가 없으면 `APPLIED` 신청만 조회한다.
- 취소 이력은 `status=CANCELED`, 세미나 삭제/취소 이력은 `status=SEMINAR_CANCELED`로 조회한다.
- 각 항목의 `myApplicationStatus`는 현재 사용자의 신청 상태를 반환한다.

### 5.8.12 내 주최 세미나 목록

`GET /api/v1/seminars/my`

- 구현 상태: 미구현. Phase 3 호스트 관리 범위.
- 인증: 필요
- Response: 페이징된 `SeminarModel`

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `status` | `String` | - | 세미나 상태 필터 |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `10` | 페이지 크기 |
| `sort` | `String` | `startDateTime,asc` | 정렬 |

현재 구현 메모:

- 현재 로그인 사용자가 호스트인 세미나만 조회한다.
- soft delete된 세미나는 기본 목록에서 제외한다.
- 호스트 관리 화면에서는 `appliedCount`, `remainingCapacity`, `status`를 사용해 운영 상태를 표시한다.

### 5.8.13 세미나 신고

`POST /api/v1/seminars/{id}/reports`

- 구현 상태: 미구현. Phase 4 신고 범위.
- 인증: 필요

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `String` | O | `SPAM`, `INAPPROPRIATE`, `FRAUD`, `OTHER` |
| `customReason` | `String?` | - | `OTHER` 선택 시 상세 사유 |

Response data: `null`

현재 구현 메모:

- 작성자는 자신의 세미나를 신고할 수 없다.
- 동일 사용자는 같은 세미나를 한 번만 신고할 수 있다.
- 신고 생성 후 `SeminarModel.reported`는 `true`가 된다.
- 신고 누적 자동 숨김은 MVP 필수 범위가 아니다.

에러:

- `NOT_FOUND`
- `ALREADY_REPORTED`
- `SELF_REPORT_NOT_ALLOWED`
- `VALIDATION_ERROR`

## 6. 테스트 기준

세미나 구현 시 우선 확인할 테스트 범위:

- 목록/상세 공개 조회와 차단 필터
- 생성/수정 validation
- 호스트 권한 검증
- 참석 신청 중복/정원/상태/자기 주최 검증
- 신청 취소와 호스트 강제 취소 상태 전이
- `CANCELED` 변경 시 신청 일괄 취소
- 삭제 시 활성 신청 `SEMINAR_CANCELED` 전환
- 후속 Phase에서 핵심 정보 변경/삭제/취소 알림 저장
- 후속 Phase에서 신고 중복과 자기 신고 차단
