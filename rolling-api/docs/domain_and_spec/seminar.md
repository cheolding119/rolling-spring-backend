# Seminar

- 세미나 도메인 모델과 API 스펙을 관리한다.
- 제품 범위와 출시 우선순위는 [seminar-product-plan.md](seminar-product-plan.md)를 본다.
- 공통 응답, 인증, 날짜/시간 형식은 [shared/common-models.md](shared/common-models.md)를 따른다.
- 세미나는 오픈매트와 유사한 공개 조회, 인증 기반 생성/신청, 차단 필터, 신고, 알림 저장 패턴을 재사용한다.
- 현재 구현 상태는 Phase 0~4다.

## 1. 도메인 개요

세미나는 호스트가 유료 또는 무료 주짓수 세미나를 등록하고, 사용자가 상세 정보를 확인한 뒤 참석 신청과 취소를 진행하며, 호스트가 신청자와 모집 상태를 관리하는 도메인이다.

현재 구현 범위:

- 세미나 목록/상세 공개 조회
- 세미나 생성/수정/삭제
- 참석 신청/취소
- 내 신청 세미나 조회
- 내 주최 세미나 조회
- 호스트 신청자 목록 조회
- 호스트 참가자 강제 취소
- 호스트 모집 상태 변경
- 세미나 신고
- 세미나 알림 저장 및 FCM 발송 시도
- 로그인 사용자 기준 차단한 호스트 세미나 숨김
- 세미나 삭제/취소 시 활성 신청 `SEMINAR_CANCELED` 전환

후속 Phase 범위:

- 앱 내 결제
- 대기 신청
- 현장 체크인
- 후기/평점
- 호스트 공지 발송
- 관리자 전용 세미나 운영 API

## 2. 도메인 모델

### 2.1 SeminarModel

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
| `hostInstagramId` | `String?` | 호스트 또는 문의용 인스타그램 ID |
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
| `appliedCount` | `Integer` | `APPLIED` 신청 인원 |
| `remainingCapacity` | `Integer?` | 남은 정원. 무제한이면 `null` |
| `price` | `Integer` | 참가비. 무료면 `0` |
| `paymentGuide` | `String?` | 결제 안내 |
| `refundPolicy` | `String?` | 환불 또는 취소 안내 |
| `status` | `SeminarStatus` | 세미나 모집/운영 상태 |
| `myApplicationStatus` | `SeminarApplicationStatus?` | 현재 로그인 사용자의 신청 상태 |
| `hostId` | `Long` | 호스트 사용자 ID |
| `hostNickname` | `String` | 호스트 닉네임 |
| `reported` | `Boolean` | 현재 로그인 사용자의 세미나 신고 여부 |
| `deleted` | `Boolean` | soft delete 여부 |
| `deletedAt` | `DateTime?` | 삭제 일시 |
| `createdAt` | `DateTime` | 생성 일시 |
| `updatedAt` | `DateTime` | 수정 일시 |

현재 구현 메모:

- `myApplicationStatus`와 `reported`는 인증 사용자의 개인화 필드다.
- 비회원 공개 조회에서는 `myApplicationStatus=null`, `reported=false`다.
- 로그인 사용자가 차단한 작성자의 세미나는 목록과 상세에서 숨긴다.
- soft delete된 세미나는 일반 사용자에게 `NOT_FOUND`로 처리한다.

### 2.2 SeminarApplicationModel

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
| `cancelReason` | `String?` | 본인 취소, 호스트 강제 취소, 세미나 취소 사유 |
| `appliedAt` | `DateTime` | 신청 일시 |
| `canceledAt` | `DateTime?` | 취소 일시 |
| `createdAt` | `DateTime` | 생성 일시 |
| `updatedAt` | `DateTime` | 수정 일시 |

### 2.3 SeminarReportModel

세미나 신고는 별도 세미나 전용 테이블이 아니라 공용 `Report` 도메인을 재사용한다.

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

| Raw value | 의미 | 호스트 수동 변경 |
| --- | --- | --- |
| `RECRUITING` | 모집 중 | O |
| `CLOSED` | 모집 마감 | O |
| `CANCELED` | 세미나 취소 | O |
| `FINISHED` | 종료 | X |
| `DELETED` | 삭제됨 | X |

상태 규칙:

- 생성 기본값은 `RECRUITING`이다.
- `manualClosed=true`이면 `CLOSED`를 유지한다.
- 신청 마감 시간이 지났거나 정원이 가득 찼으면 `CLOSED`가 된다.
- 종료 시간이 지나면 `FINISHED`가 된다.
- `CANCELED`는 이후 자동 동기화로 되돌리지 않는다.
- `DELETED`는 soft delete된 데이터의 내부 상태다.

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
- 같은 사용자는 한 세미나에 활성 신청을 1개만 가진다.
- 취소 이력이 있는 사용자는 기존 row를 `APPLIED`로 재활성화한다.

### 3.3 ReportReason

세미나 신고는 기존 공용 신고 enum을 그대로 사용한다.

| Raw value | 의미 |
| --- | --- |
| `FALSE_INFO` | 허위 정보 |
| `INAPPROPRIATE` | 부적절한 내용 |
| `SPAM` | 광고/도배 |
| `OTHER` | 기타 |

### 3.4 PushNotificationType

세미나에서 추가된 알림 raw value:

- `SEMINAR_APPLIED`
- `SEMINAR_APPLICATION_CANCELED`
- `SEMINAR_APPLICATION_CANCELED_BY_HOST`
- `SEMINAR_UPDATED`
- `SEMINAR_DELETED`
- `SEMINAR_CANCELED`

## 4. 공통 정책

### 4.1 인증과 권한

- 목록과 상세 조회는 인증 없이 가능하다.
- 참석 신청, 신청 취소, 내 신청 목록, 내 주최 목록, 신고는 인증이 필요하다.
- 세미나 생성, 수정, 삭제는 인증이 필요하다.
- 신청자 목록, 참가자 강제 취소, 모집 상태 변경은 호스트만 가능하다.
- 작성자는 자신이 만든 세미나에 참석 신청할 수 없다.
- 현재 프로젝트의 전역 제재 필터에 따라 일시정지 사용자는 인증 상태에서 대부분의 세미나 액션이 차단된다.

### 4.2 날짜와 정렬

- `DateTime`은 ISO 8601 문자열을 사용한다.
- 목록 기본 정렬은 `startDateTime,asc`다.
- 신청자 목록 기본 정렬은 `appliedAt,asc`다.

### 4.3 좌표

- `latitude`, `longitude`는 둘 다 없으면 좌표 없이 저장한다.
- 둘 중 하나만 있으면 `VALIDATION_ERROR`다.
- `latitude`는 `-90..90`, `longitude`는 `-180..180` 범위만 허용한다.

### 4.4 정원

- `maxCapacity`는 `-1` 또는 `1 이상`이어야 한다.
- `APPLIED` 상태 신청 수가 정원 계산 기준이다.
- 취소로 빈자리가 생기면 호스트가 `RECRUITING`으로 직접 재오픈할 수 있다.
- `RECRUITING` 변경 시 이미 정원이 가득 찼으면 `CAPACITY_FULL`이다.

### 4.5 알림

알림 저장과 FCM 발송 시도는 구현되어 있다.

- 세미나 참석 신청 완료
- 세미나 참석 신청 취소
- 호스트의 참가자 강제 취소
- 세미나 일정 또는 장소 변경
- 세미나 삭제
- 세미나 취소

정책:

- 알림의 source of truth는 `Notification` 저장 데이터다.
- 알림 저장은 `@TransactionalEventListener(AFTER_COMMIT)`로 처리한다.
- FCM 실패는 핵심 트랜잭션을 실패시키지 않는다.
- 세미나 라우팅 경로는 `"/seminar/detail"`이다.

### 4.6 차단과 신고

- 로그인 사용자가 차단한 작성자의 세미나는 목록과 상세에서 제외한다.
- 세미나 신고는 공용 `ReportService`를 사용한다.
- 작성자는 자신의 세미나를 신고할 수 없다.
- 동일 사용자는 같은 세미나를 한 번만 신고할 수 있다.
- 신고 생성 후 `SeminarModel.reported`는 `true`가 된다.
- 신고 누적 자동 숨김은 아직 구현하지 않았다.

## 5.8 세미나 API

### 5.8.1 세미나 목록 조회

`GET /api/v1/seminars`

- 인증: 불필요
- Response: 페이징된 `SeminarModel`

Query parameters:

| 파라미터 | 타입 | 기본값 | 설명 |
| --- | --- | --- | --- |
| `region` | `String` | - | 지역 필터 |
| `status` | `String` | - | 상태 필터 |
| `q` | `String` | - | 제목/강사명/장소명/주소 부분 일치 검색 |
| `from` | `DateTime` | - | 시작 일시 하한 |
| `to` | `DateTime` | - | 시작 일시 상한 |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `20` | 페이지 크기 |
| `sort` | `String` | `startDateTime,asc` | 정렬 |

현재 구현 메모:

- 로그인 사용자가 `Authorization` 헤더를 보내면 차단 필터, `myApplicationStatus`, `reported`를 반영한다.
- soft delete된 세미나는 목록에서 제외한다.

### 5.8.2 세미나 상세 조회

`GET /api/v1/seminars/{id}`

- 인증: 불필요
- Response: `SeminarModel`

현재 구현 메모:

- 로그인 사용자가 차단한 작성자의 세미나는 `NOT_FOUND`다.
- soft delete된 세미나는 일반 사용자에게 `NOT_FOUND`다.

에러:

- `NOT_FOUND`

### 5.8.3 세미나 생성

`POST /api/v1/seminars`

- 인증: 필요
- Response: `SeminarModel`

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
| `maxCapacity` | `Integer` | O | 최대 정원 |
| `price` | `Integer` | O | 참가비 |
| `paymentGuide` | `String?` | - | 결제 안내 |
| `refundPolicy` | `String?` | - | 환불 또는 취소 안내 |

검증:

- 제목, 설명, 강사명, 시작 일시, 종료 일시, 장소명, 주소, 지역, 최대 정원, 가격은 필수다.
- 종료 시간은 시작 시간보다 이후여야 한다.
- 신청 시작/마감 시간이 모두 있으면 신청 마감 시간은 신청 시작보다 이후여야 한다.
- 신청 마감 시간은 세미나 시작 시간보다 이후일 수 없다.
- `maxCapacity`는 `-1` 또는 `1 이상`이어야 한다.
- `price`는 `0 이상`이어야 한다.
- 좌표는 둘 다 없거나 둘 다 있어야 한다.
- 제재 사용자는 전역 제재 필터로 차단된다.

에러:

- `VALIDATION_ERROR`
- `USER_SANCTIONED`

### 5.8.4 세미나 수정

`PUT /api/v1/seminars/{id}`

- 인증: 필요
- 권한: 호스트 전용
- Response: `SeminarModel`

현재 구현 메모:

- 호스트만 수정 가능하다.
- 종료된 세미나와 `CANCELED` 세미나는 수정할 수 없다.
- 최소 1개 필드는 전달해야 한다.
- 일정, 장소, 좌표, 지역, 가격, 정원이 바뀌면 현재 `APPLIED` 신청자에게 `SEMINAR_UPDATED` 알림을 저장하고 FCM 발송을 시도한다.
- `maxCapacity`를 현재 `APPLIED` 인원보다 작은 값으로 줄일 수 없다. 단 `-1`은 허용한다.
- `latitude`, `longitude`가 둘 다 `null`이면 기존 좌표를 제거한다.

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
- Response data: `null`

현재 구현 메모:

- 삭제는 soft delete다.
- `APPLIED` 신청자는 `SEMINAR_CANCELED`로 전환한다.
- 기존 `APPLIED` 신청자에게 `SEMINAR_DELETED` 알림을 저장하고 FCM 발송을 시도한다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`

### 5.8.6 세미나 참석 신청

`POST /api/v1/seminars/{id}/applications`

- 인증: 필요
- Response: `SeminarApplicationModel`

현재 구현 메모:

- 신청 완료 상태는 `APPLIED`다.
- 기존 취소 이력이 있으면 같은 row를 재활성화한다.
- 신청 완료 후 신청자에게 `SEMINAR_APPLIED` 알림을 저장하고 FCM 발송을 시도한다.
- 호스트는 자신이 만든 세미나에 신청할 수 없다.
- 동일 사용자는 같은 세미나에 활성 신청을 중복 생성할 수 없다.
- `RECRUITING` 상태에서만 신청할 수 있다.
- 신청 시작 전, 신청 마감 후, 정원 초과, 종료된 세미나는 신청할 수 없다.

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
- Response: `SeminarApplicationModel`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `cancelReason` | `String?` | - | 참가자 취소 사유 |

현재 구현 메모:

- 현재 로그인 사용자의 `APPLIED` 신청만 취소할 수 있다.
- 세미나 시작 전까지만 취소할 수 있다.
- 취소 후 상태는 `CANCELED`다.
- 취소 후 신청자에게 `SEMINAR_APPLICATION_CANCELED` 알림을 저장하고 FCM 발송을 시도한다.

에러:

- `NOT_FOUND`
- `APPLICATION_NOT_FOUND`
- `APPLICATION_ALREADY_CANCELED`
- `SEMINAR_FINISHED`

### 5.8.8 세미나 신청자 목록 조회

`GET /api/v1/seminars/{id}/applications`

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
- 기본 상태 필터는 `APPLIED`다.
- 참가자 연락처는 노출하지 않는다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`

### 5.8.9 세미나 참가자 강제 취소

`PATCH /api/v1/seminars/{id}/applications/{applicationId}/cancel`

- 인증: 필요
- 권한: 호스트 전용
- Response: `SeminarApplicationModel`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `cancelReason` | `String?` | - | 호스트 강제 취소 사유 |

현재 구현 메모:

- `APPLIED` 상태 신청만 강제 취소할 수 있다.
- 강제 취소 후 상태는 `HOST_CANCELED`다.
- 취소된 참가자에게 `SEMINAR_APPLICATION_CANCELED_BY_HOST` 알림을 저장하고 FCM 발송을 시도한다.

에러:

- `NOT_FOUND`
- `FORBIDDEN`
- `APPLICATION_ALREADY_CANCELED`
- `SEMINAR_FINISHED`

### 5.8.10 세미나 모집 상태 변경

`PATCH /api/v1/seminars/{id}/status`

- 인증: 필요
- 권한: 호스트 전용
- Response: `SeminarModel`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `status` | `String` | O | `RECRUITING`, `CLOSED`, `CANCELED` |
| `reason` | `String?` | - | 세미나 취소 사유 |

현재 구현 메모:

- `FINISHED`, `DELETED`는 수동 변경 대상이 아니다.
- `CLOSED`는 `manualClosed=true`로 유지한다.
- `RECRUITING` 재오픈 시 정원이 이미 찼으면 `CAPACITY_FULL`이다.
- `CANCELED`로 변경하면 모든 `APPLIED` 신청을 `SEMINAR_CANCELED`로 전환한다.
- `reason`이 있으면 `SEMINAR_CANCELED` 신청들의 `cancelReason`으로 저장한다.
- 세미나 취소 시 기존 `APPLIED` 신청자에게 `SEMINAR_CANCELED` 알림을 저장하고 FCM 발송을 시도한다.

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
| `status` | `String` | `APPLIED` | 신청 상태 필터 |
| `page` | `Integer` | `0` | 페이지 번호 |
| `size` | `Integer` | `10` | 페이지 크기 |
| `sort` | `String` | `startDateTime,asc` | 정렬 |

현재 구현 메모:

- `status`가 없으면 `APPLIED` 신청만 조회한다.
- 각 항목의 `myApplicationStatus`와 `reported`를 함께 반환한다.

### 5.8.12 내 주최 세미나 목록

`GET /api/v1/seminars/my`

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
- soft delete된 세미나는 제외한다.
- `status` 필터가 있으면 저장된 상태 기준으로 조회한 뒤 응답 변환 시 현재 시각 기준으로 상태를 동기화한다.

### 5.8.13 세미나 신고

`POST /api/v1/seminars/{id}/reports`

- 인증: 필요
- Response data: `null`

Request body:

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `reason` | `String` | O | `FALSE_INFO`, `INAPPROPRIATE`, `SPAM`, `OTHER` |
| `customReason` | `String?` | - | `OTHER` 선택 시 상세 사유 |

현재 구현 메모:

- 작성자는 자신의 세미나를 신고할 수 없다.
- 동일 사용자는 같은 세미나를 한 번만 신고할 수 있다.
- 신고 후 상세/목록/내 목록 응답의 `reported`는 `true`가 된다.
- 신고 누적 자동 숨김은 구현하지 않았다.

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
- 신청/취소/강제취소/수정/삭제/세미나 취소 알림 저장 이벤트
- 신고 중복과 자기 신고 차단
