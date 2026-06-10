# Training Card

- 훈련카드 조회, 좋아요/즐겨찾기, 훈련일지 연결 계약과 운영 메모를 관리한다.
- 훈련일지 본문 계약은 [training-log.md](training-log.md), 친구 열람 계약은 [training-log-social.md](training-log-social.md)를 따른다.
- 공통 응답, 인증, 에러 포맷은 [shared/common-models.md](shared/common-models.md)를 따른다.

## 1. 도메인 개요

Training Card는 기술 복기용 읽기 콘텐츠 도메인이다. 현재 구현 범위는 아래와 같다.

- 로그인 사용자의 훈련카드 목록 조회
- 로그인 사용자의 훈련카드 상세 조회
- 카드 좋아요 추가/취소
- 카드 즐겨찾기 추가/취소
- 카드별 연관 훈련카드 다중 연결 및 상세 내 노출
- 훈련일지 생성/수정 payload에서 카드 연결
- 훈련일지 상세, 최근 목록, 친구 상세에서 연결 카드 읽기

구현 메모:

- 훈련카드는 훈련일지 본체를 대체하지 않고 `훈련일지 확장 콘텐츠`로 연결된다.
- 카드 연결 전용 API는 두지 않고 `trainingCardIds` payload로만 연결한다.
- 날짜별 요약 응답과 친구 피드 요약 응답에는 연결 카드를 포함하지 않는다.

## 2. 도메인 모델

### 2.1 `TrainingCard`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 훈련카드 ID |
| `title` | `String` | 기술 제목 |
| `summary` | `String` | 짧은 소개 문장 |
| `topic` | `String` | 기술 분류 |
| `level` | `TrainingCardLevel` | 기술 레벨 |
| `position` | `TrainingCardPosition` | 기술 포지션 |
| `situationSummary` | `String` | 어떤 상황에서 쓰는지에 대한 짧은 문구 |
| `description` | `String` | 기술 상세 설명 |
| `situationDescription` | `String` | 기술이 쓰이는 상황 설명 |
| `startingPositionDescription` | `String` | 시작 자세 또는 전제 상황 설명 |
| `flowDescription` | `String` | 단계형 기술 흐름 설명 |
| `keyPoints` | `String` | 핵심 포인트 |
| `commonMistakes` | `String` | 자주 틀리는 점 |
| `cautions` | `String` | 주의할 점 |
| `youtubeUrl` | `String?` | 외부 유튜브 링크 |
| `active` | `boolean` | 노출 여부 |
| `displayOrder` | `int` | 기본 노출 순서 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

구현 메모:

- 카드 본문은 읽기 모델 중심이라 soft delete 대신 `active` 플래그를 사용한다.
- 현재 쓰기 API는 없고, 카드 데이터는 마이그레이션 이후 운영 적재 또는 관리자 경로로 관리한다.

### 2.2 `TrainingCardLike`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `cardId` | `Long` | 대상 카드 ID |
| `userId` | `Long` | 좋아요 사용자 ID |
| `createdAt` | `LocalDateTime` | 생성 시각 |

구현 메모:

- 저장 테이블은 `training_card_likes`다.
- `(card_id, user_id)` unique 제약으로 중복 좋아요를 막는다.

### 2.3 `TrainingCardFavorite`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `cardId` | `Long` | 대상 카드 ID |
| `userId` | `Long` | 즐겨찾기 사용자 ID |
| `createdAt` | `LocalDateTime` | 생성 시각 |

구현 메모:

- 저장 테이블은 `training_card_favorites`다.
- `(card_id, user_id)` unique 제약으로 중복 즐겨찾기를 막는다.

### 2.4 `TrainingLogEntryCard`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `entryId` | `Long` | 훈련일지 ID |
| `cardId` | `Long` | 연결된 카드 ID |
| `createdAt` | `LocalDateTime` | 생성 시각 |

구현 메모:

- 저장 테이블은 `training_log_entry_cards`다.
- `(entry_id, card_id)` unique 제약으로 동일 카드 중복 연결을 막는다.
- 훈련일지 삭제 시 `ON DELETE CASCADE` 및 서비스 삭제로 함께 정리된다.

### 2.5 `TrainingCardRelation`

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | PK |
| `cardId` | `Long` | 기준 훈련카드 ID |
| `relatedCardId` | `Long` | 연관 훈련카드 ID |
| `displayOrder` | `int` | 상세 내 노출 순서 |
| `createdAt` | `LocalDateTime` | 생성 시각 |
| `updatedAt` | `LocalDateTime` | 수정 시각 |

구현 메모:

- 저장 테이블은 `training_card_relations`다.
- 한 카드에 여러 연관 카드를 연결할 수 있다.
- `(card_id, related_card_id)` unique 제약으로 중복 연결을 막는다.
- 자기 자신을 연관 카드로 연결하지 못하도록 check 제약을 둔다.
- 현재 관계는 `card -> related cards` 방향성으로 저장한다. 양방향 노출이 필요하면 운영 데이터 적재 시 반대 방향도 함께 넣는다.

### 2.6 `TrainingLogLinkedTrainingCardResponse`

훈련일지 상세/최근 목록/친구 상세에서 공통으로 사용하는 연결 카드 요약 응답이다.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | `Long` | 카드 ID |
| `title` | `String` | 기술 제목 |
| `summary` | `String` | 짧은 요약 |
| `topic` | `String` | 분류 |
| `level` | `TrainingCardLevel` | 레벨 |
| `position` | `TrainingCardPosition` | 포지션 |
| `situationSummary` | `String` | 상황 요약 |

## 3. Enum

### 3.1 `TrainingCardLevel`

| Raw value | 설명 |
| --- | --- |
| `BEGINNER` | 초급 |
| `INTERMEDIATE` | 중급 |
| `ADVANCED` | 고급 |

### 3.2 `TrainingCardPosition`

| Raw value | 설명 |
| --- | --- |
| `STANDING` | 스탠딩 |
| `GUARD` | 가드 공통 분류, 기존 데이터 호환용 legacy 값 |
| `CLOSED_GUARD` | 클로즈드 가드 |
| `OPEN_GUARD` | 오픈 가드 |
| `HALF_GUARD` | 하프 가드 |
| `SIDE_CONTROL` | 사이드 컨트롤 |
| `MOUNT` | 마운트 |
| `BACK` | 백 |
| `TURTLE` | 터틀 |
| `LEG_ENTANGLEMENT` | 레그 엔탱글먼트 |

## 4. API

### 4.1 훈련카드 목록 조회

`GET /api/v1/training-logs/me/cards?q=knee&level=BEGINNER&position=GUARD`

- 인증: 필요
- Response data: `List<TrainingCardListItemResponse>`

응답 필드:

- `id`
- `title`
- `summary`
- `topic`
- `level`
- `position`
- `situationSummary`
- `likeCount`
- `likedByMe`
- `favoritedByMe`

정책:

- `q`는 제목/요약/분류 기준 부분 일치 검색이다.
- `level`, `position`은 동시에 조합 가능하다.
- 기본 정렬은 `displayOrder asc, id asc`다.

### 4.2 훈련카드 상세 조회

`GET /api/v1/training-logs/me/cards/{id}`

- 인증: 필요
- Response data: `TrainingCardDetailResponse`

응답 필드:

- 목록 응답 필드 전체
- `description`
- `situationDescription`
- `startingPositionDescription`
- `flowDescription`
- `keyPoints`
- `commonMistakes`
- `cautions`
- `youtubeUrl`
- `relatedCards`

`relatedCards` 응답 필드:

- `id`
- `title`
- `summary`
- `topic`
- `level`
- `position`
- `situationSummary`

### 4.3 훈련카드 좋아요

`POST /api/v1/training-logs/me/cards/{id}/like`

`DELETE /api/v1/training-logs/me/cards/{id}/like`

- 인증: 필요
- Response data: `null`

정책:

- 같은 사용자의 중복 좋아요 추가 요청은 idempotent 하게 처리한다.
- 좋아요 취소도 idempotent 하게 처리한다.

### 4.4 훈련카드 즐겨찾기

`POST /api/v1/training-logs/me/cards/{id}/favorite`

`DELETE /api/v1/training-logs/me/cards/{id}/favorite`

- 인증: 필요
- Response data: `null`

정책:

- 같은 사용자의 중복 즐겨찾기 추가 요청은 idempotent 하게 처리한다.
- 즐겨찾기 취소도 idempotent 하게 처리한다.

## 5. 훈련일지 연동 계약

### 5.1 훈련일지 생성/수정 request

`TrainingLogEntryCreateRequest.trainingCardIds`

`TrainingLogEntryUpdateRequest.trainingCardIds`

정책:

- 최대 5개까지 허용한다.
- `null` 또는 빈 배열이면 연결을 비운다.
- 중복 ID는 제거하되, 최초 입력 순서는 유지한다.
- 비활성 카드 또는 존재하지 않는 카드 ID는 허용하지 않는다.

### 5.2 훈련일지 응답

아래 응답에는 `trainingCards: List<TrainingLogLinkedTrainingCardResponse>`가 포함된다.

- `TrainingLogEntryResponse`
- `TrainingLogFriendEntryDetailResponse`

현재 제외 범위:

- `TrainingLogEntrySummaryResponse`
- `TrainingLogFriendEntrySummaryResponse`

## 6. 데이터/마이그레이션

신규 마이그레이션:

- `V34__add_training_cards.sql`
- `V35__grant_training_cards_permissions.sql`
- `V36__add_training_card_interactions_and_entry_links.sql`
- `V37__grant_training_card_interactions_and_entry_links_permissions.sql`
- `V38__add_training_card_relations.sql`
- `V39__grant_training_card_relations_permissions.sql`
- `V40__expand_training_card_position_values.sql`

정리:

- 신규 테이블: `training_cards`, `training_card_likes`, `training_card_favorites`, `training_log_entry_cards`, `training_card_relations`
- 기존 `training_log_entries` 테이블 자체 컬럼 변경은 없다.
- 기존 운영 데이터와 충돌을 피하기 위해 모두 additive migration으로만 추가한다.

초기 데이터 적재 방침:

- 현재 구현은 카드 조회/연결 구조만 제공한다.
- 초기 카드 본문 데이터는 운영 SQL, 별도 시드 스크립트, 또는 추후 관리자 쓰기 API 중 하나로 적재한다.
- 기능 노출 전 최소 카드 데이터 세트 확보가 필요하다.

권장 배포 순서:

1. `V34`~`V39` 적용
2. 애플리케이션 배포
3. 카드 초기 데이터와 연관 카드 매핑 적재
4. 운영 smoke test
5. 사용자 노출

## 7. 보안과 권한 정책

- 현재 구현 기준 훈련카드 목록/상세는 인증이 필요하다.
- 좋아요/즐겨찾기/훈련일지 연결은 인증 사용자만 허용한다.
- 리스트/상세 응답의 `favoritedByMe`는 현재 로그인 사용자 기준의 개인 상태이며 타인에게 공유되지 않는다.
- 카드 생성/수정/삭제 관리자 API는 아직 구현하지 않았다. 카드 쓰기 경로를 열 경우 `ADMIN` 전용이 기본 정책이다.
- 훈련카드의 외부 링크는 현재 읽기 전용 `youtubeUrl` 단일 필드다. 추후 쓰기 API 추가 시 `youtube.com`, `youtu.be`만 허용하는 검증이 필요하다.
- 연관 카드는 같은 로그인 사용자의 상세 응답에서만 노출되며, 비활성 카드와 자기 자신은 응답에서 제외한다.

## 8. 성능과 운영 안정성

조회 전략:

- 카드 리스트는 카드 본문 조회 1회 + 좋아요 수 집계 1회 + 사용자 좋아요 상태 1회 + 사용자 즐겨찾기 상태 1회로 처리한다.
- 카드 상세는 카드 본문 조회 외에 연관 카드 fetch join 1회로 관련 카드 목록을 함께 읽는다.
- 훈련일지 상세/최근/친구 상세의 연결 카드는 `training_log_entry_cards -> card fetch join`으로 조회한다.
- 최근 목록은 entry ID 집합 단위로 연결 카드를 한 번에 불러와 N+1을 피한다.

운영 안정성:

- 모든 변경은 기존 API를 대체하지 않는 additive change다.
- 기존 훈련일지 작성/조회, 소셜, 인사이트 API 경로는 유지된다.
- 장애 시 `TRAINING_CARD_ENABLED=false`로 카드 조회/상호작용 기능을 빠르게 비활성화할 수 있다.
- feature flag가 꺼지면 카드 전용 API는 `NOT_FOUND`, 훈련일지 상세의 연결 카드는 빈 배열로 축소되고 새 카드 연결 쓰기는 차단된다.

환경 검증 메모:

- 배포 전 운영 DB에 `V34`~`V39` 적용 여부 확인
- 운영 카드 seed 데이터 존재 여부 확인
- 운영 연관 카드 매핑 데이터 존재 여부 확인
- `TRAINING_CARD_ENABLED` 기본값과 비상 시 변경 절차 확인
- 카드 상세 YouTube URL이 허용 도메인 정책에 맞는지 샘플 검증
