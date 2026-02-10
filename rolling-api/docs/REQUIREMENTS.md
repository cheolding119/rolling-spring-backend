## 1. 인증 및 사용자 관리 (Auth & User)

### API Endpoints
```
POST   /api/v1/auth/login          - 소셜 로그인 (토큰 검증 후 JWT 발급)
POST   /api/v1/auth/refresh        - Access Token 갱신
POST   /api/v1/auth/logout         - 로그아웃 (FCM 토큰 제거)
DELETE /api/v1/auth/withdraw       - 회원 탈퇴
GET    /api/v1/users/me            - 내 정보 조회
PUT    /api/v1/users/me            - 내 정보 수정
POST   /api/v1/users/me/fcm-token  - FCM 토큰 등록/갱신
```

### 비즈니스 로직
- **소셜 로그인**: Apple, Kakao, Google, Naver 각 제공자별 토큰 검증 후 자체 JWT 발급
- **Apple 로그인 필수 사항**: Apple 토큰 검증 및 Revoke Token API 구현
- **회원 탈퇴**:
    - 사용자 개인정보 삭제 (GDPR 대응)
    - Apple 로그인의 경우 Revoke Token 처리
    - 작성한 게시글/댓글 익명화 또는 삭제 처리
- **JWT 토큰 관리**: Access Token (30분) + Refresh Token (14일) 방식

### Service Layer
```java
@Service
public class AuthService {
    public TokenResponse socialLogin(SocialLoginRequest request);
    public TokenResponse refreshToken(String refreshToken);
    public void logout(Long userId);
    public void withdraw(Long userId);
}
```

---

## 2. 체육관 탐색 및 조회 (Gym)

### API Endpoints
```
GET    /api/v1/gyms                     - 체육관 리스트 조회 (위치 기반)
GET    /api/v1/gyms/{id}                - 체육관 상세 조회
GET    /api/v1/gyms/search              - 체육관 검색
POST   /api/v1/gyms                     - 체육관 등록 (관장용)
PUT    /api/v1/gyms/{id}                - 체육관 수정 (관장용)
DELETE /api/v1/gyms/{id}                - 체육관 삭제 (관장용)
POST   /api/v1/gyms/{id}/schedules      - 시간표 추가
PUT    /api/v1/gyms/{id}/schedules/{scheduleId} - 시간표 수정
DELETE /api/v1/gyms/{id}/schedules/{scheduleId} - 시간표 삭제
POST   /api/v1/gyms/{id}/images         - 이미지 업로드
DELETE /api/v1/gyms/{id}/images/{imageId} - 이미지 삭제
```

### 비즈니스 로직
- **위치 기반 리스트 조회**:
    - 사용자 위도/경도 기준 Haversine 공식으로 거리 계산
    - 거리순 정렬 (가까운 순서)
    - `isVisible = true`인 체육관만 노출
- **편의시설 필터**: QueryDSL 동적 쿼리로 amenities 필터링
- **검색 기능**: 체육관 이름, 주소, 지역으로 Full-text 또는 LIKE 검색
- **시간 검증**: `startTime < endTime` 서버단 검증 필수
- **시간표 정렬**: dayOfWeek, startTime 기준 자동 정렬

### Service Layer
```java
@Service
public class GymService {
    public Page<GymListResponse> findNearbyGyms(Double lat, Double lng, Pageable pageable);
    public GymDetailResponse findById(Long id);
    public Page<GymListResponse> search(String keyword, Pageable pageable);
    public GymDetailResponse create(Long ownerId, GymCreateRequest request);
    public GymDetailResponse update(Long id, Long ownerId, GymUpdateRequest request);
    public void delete(Long id, Long ownerId);
}

@Service
public class GymScheduleService {
    public GymScheduleResponse addSchedule(Long gymId, Long ownerId, ScheduleRequest request);
    public GymScheduleResponse updateSchedule(Long gymId, Long scheduleId, Long ownerId, ScheduleRequest request);
    public void deleteSchedule(Long gymId, Long scheduleId, Long ownerId);
}
```

---

## 3. 오픈매트 (OpenMat)

### API Endpoints
```
GET    /api/v1/open-mats                - 오픈매트 리스트 조회
GET    /api/v1/open-mats/{id}           - 오픈매트 상세 조회
POST   /api/v1/open-mats                - 오픈매트 등록
PUT    /api/v1/open-mats/{id}           - 오픈매트 수정
DELETE /api/v1/open-mats/{id}           - 오픈매트 삭제
POST   /api/v1/open-mats/{id}/apply     - 오픈매트 신청
DELETE /api/v1/open-mats/{id}/apply     - 오픈매트 신청 취소
GET    /api/v1/open-mats/my             - 내가 신청한 오픈매트 목록
```

### 비즈니스 로직
- **작성 및 수정**:
    - 수정 시 신청자 존재하면 FCM 푸시 알림 발송
    - 일시/장소 변경 시 알림 필수
- **참여 신청**:
    - `participantUids`에 유저 ID 추가
    - 동시성 제어 (@Transactional, Pessimistic Lock)
- **정원 관리**:
    - `maxCapacity != -1` 경우 현재 인원 체크
    - 정원 초과 시 예외 발생 및 status를 CLOSED로 변경
- **삭제 로직**:
    - 작성자 본인만 삭제 가능
    - 신청자 존재 시 삭제 전 확인 플래그 필요 (`force=true`)
    - 삭제 시 신청자들에게 취소 알림 발송
- **상태 자동화**:
    - Scheduler로 `endDateTime` 지난 오픈매트 status를 FINISHED로 업데이트
    - `@Scheduled(cron = "0 */10 * * * *")` 10분마다 실행

### Service Layer
```java
@Service
public class OpenMatService {
    public Page<OpenMatListResponse> findAll(String region, Pageable pageable);
    public OpenMatDetailResponse findById(Long id);
    public OpenMatDetailResponse create(Long hostId, OpenMatCreateRequest request);
    public OpenMatDetailResponse update(Long id, Long hostId, OpenMatUpdateRequest request);
    public void delete(Long id, Long hostId, boolean force);
    public void apply(Long id, Long userId);
    public void cancelApply(Long id, Long userId);
    public List<OpenMatListResponse> findMyApplied(Long userId);
}

@Component
public class OpenMatScheduler {
    @Scheduled(cron = "0 */10 * * * *")
    public void updateFinishedOpenMats();
}
```

---

## 4. 커뮤니티 (Community)

### API Endpoints
```
GET    /api/v1/posts                    - 게시글 리스트 조회
GET    /api/v1/posts/{id}               - 게시글 상세 조회
POST   /api/v1/posts                    - 게시글 작성
PUT    /api/v1/posts/{id}               - 게시글 수정
DELETE /api/v1/posts/{id}               - 게시글 삭제
POST   /api/v1/posts/{id}/like          - 좋아요
POST   /api/v1/posts/{id}/dislike       - 싫어요
DELETE /api/v1/posts/{id}/like          - 좋아요/싫어요 취소
POST   /api/v1/posts/{id}/report        - 게시글 신고
GET    /api/v1/posts/{id}/comments      - 댓글 조회
POST   /api/v1/posts/{id}/comments      - 댓글 작성
PUT    /api/v1/posts/{id}/comments/{commentId}    - 댓글 수정
DELETE /api/v1/posts/{id}/comments/{commentId}    - 댓글 삭제
```

### 비즈니스 로직
- **게시글 작성**: regionTag, title, content 필수
- **게시글 수정**: 작성자 본인만 가능, updatedAt 자동 갱신
- **게시글 삭제**:
    - 작성자 본인 또는 관리자만 가능
    - Soft Delete (isDeleted = true)
    - 연관 댓글도 Soft Delete 처리
- **지역 필터**: regionTag 기반 필터링
- **익명성 보장**: 응답 DTO에 작성자 nickname만 포함 (id 노출 X)
- **신고 및 제재**:
    - reportedCount 3회 이상 시 isReported = true, 자동 숨김
    - 중복 신고 방지 (user별 1회)
- **좋아요/싫어요**: 중복 방지 (user당 1회, 변경 가능)

### Service Layer
```java
@Service
public class PostService {
    public Page<PostListResponse> findAll(String regionTag, Pageable pageable);
    public PostDetailResponse findById(Long id, Long viewerId);
    public PostDetailResponse create(Long authorId, PostCreateRequest request);
    public PostDetailResponse update(Long id, Long authorId, PostUpdateRequest request);
    public void delete(Long id, Long userId);
    public void like(Long id, Long userId);
    public void dislike(Long id, Long userId);
    public void cancelLike(Long id, Long userId);
    public void report(Long id, Long reporterId, ReportRequest request);
}

@Service
public class CommentService {
    public Page<CommentResponse> findByPostId(Long postId, Pageable pageable);
    public CommentResponse create(Long postId, Long authorId, CommentCreateRequest request);
    public CommentResponse update(Long postId, Long commentId, Long authorId, CommentUpdateRequest request);
    public void delete(Long postId, Long commentId, Long userId);
}
```

---

## 5. 대회 정보 (Tournament)

### API Endpoints
```
GET    /api/v1/tournaments              - 대회 리스트 조회
GET    /api/v1/tournaments/{id}         - 대회 상세 조회
POST   /api/v1/tournaments              - 대회 등록 (관리자/주최자)
PUT    /api/v1/tournaments/{id}         - 대회 수정 (관리자/주최자)
DELETE /api/v1/tournaments/{id}         - 대회 삭제 (관리자/주최자)
```

### 비즈니스 로직
- **대회 등록 및 수정**:
    - 대회 주최자(hostId) 또는 시스템 관리자만 가능
    - posterUrl, applyLink 필수
- **대회 삭제**: 접수 기간 중이라도 삭제 가능
- **외부 연동**: applyLink URL 검증 (유효한 URL인지)
- **마감 관리**:
    - registrationDeadline 지난 대회는 '접수 종료' 표시
    - 리스트 조회 시 접수 가능한 대회 상단, 마감된 대회 하단 정렬
- **카테고리 태그**: categoryTags로 Gi, No-Gi 등 필터링 지원

### Service Layer
```java
@Service
public class TournamentService {
    public Page<TournamentListResponse> findAll(Pageable pageable);
    public TournamentDetailResponse findById(Long id);
    public TournamentDetailResponse create(Long hostId, TournamentCreateRequest request);
    public TournamentDetailResponse update(Long id, Long hostId, TournamentUpdateRequest request);
    public void delete(Long id, Long userId);
}
```

---

## 6. 푸시 알림 (FCM)

### Service Layer
```java
@Service
public class NotificationService {
    public void sendToUser(Long userId, String title, String body);
    public void sendToUsers(List<Long> userIds, String title, String body);
    public void sendOpenMatUpdateNotification(Long openMatId, String message);
    public void sendOpenMatCancelNotification(Long openMatId);
}
```

### 알림 발송 케이스
- 오픈매트 정보 변경 시 신청자들에게 알림
- 오픈매트 취소 시 신청자들에게 알림
- 댓글 알림 (선택적)
- 게시글 신고 처리 결과 알림 (관리자 기능)
