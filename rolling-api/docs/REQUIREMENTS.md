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

## 2. 오픈매트 (OpenMat)

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

## 3. 대회 정보 (Tournament)

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

## 4. 푸시 알림 (FCM)

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
```
---

## 변경사항 (2026-02-19)

### OpenMat 사용자 기능 요구사항 보강
- 메인 화면의 "내가 신청한 오픈매트"는 최대 10건만 노출한다.
- "전체보기" 진입 시 동일 API를 통해 페이지네이션으로 전체 데이터를 조회한다.
- 페이지 크기는 10으로 고정하고, 페이지 번호를 기반으로 추가 조회한다.

### API 요구사항 반영
- `GET /api/v1/open-mats/my`는 페이지 기반 응답을 제공해야 한다.
- 기본 파라미터: `page=0`, `size=10`.
