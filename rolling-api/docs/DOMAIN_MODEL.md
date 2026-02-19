## 0. Enum 정의

### SocialProvider
소셜 로그인 제공자 타입
```java
public enum SocialProvider {
    APPLE,    // Apple Login
    KAKAO,    // Kakao Login
    GOOGLE,   // Google Login

}
```

ㄴ### OpenMatStatus
오픈매트 모집 상태
```java
public enum OpenMatStatus {
    RECRUITING, // 모집중
    CLOSED,     // 모집 마감
    FINISHED    // 종료됨
}
```

---

## 1. 유저 (User)

**Entity**: `User`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `Long` | 고유 식별자 | PK, @GeneratedValue |
| `nickname` | `String` | 프로필 닉네임 | @Column(nullable = false) |
| `phone` | `String` | 연락처 | 관장 인증 시 필수 |
| `socialProvider` | `SocialProvider` | 소셜 로그인 제공자 | @Enumerated(EnumType.STRING) |
| `socialId` | `String` | 소셜 로그인 고유 ID | 소셜 제공자별 유니크 |
| `fcmToken` | `String` | 푸시 알림용 토큰 | Nullable |
| `createdAt` | `LocalDateTime` | 계정 생성 일시 | @CreatedDate |
| `updatedAt` | `LocalDateTime` | 수정 일시 | @LastModifiedDate |

**연관 테이블**: `user_joined_openmats` (N:M), `user_blocked_users` (N:M)

```java
@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider socialProvider;

    @Column(nullable = false, unique = true)
    private String socialId;

    private String fcmToken;

    @ManyToMany
    @JoinTable(name = "user_joined_openmats",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "openmat_id"))
    private Set<OpenMat> joinedOpenMats = new HashSet<>();

    @ManyToMany
    @JoinTable(name = "user_blocked_users",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "blocked_user_id"))
    private Set<User> blockedUsers = new HashSet<>();
}
```

## 2. 오픈매트 (OpenMat)

**Entity**: `OpenMat`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `Long` | 오픈매트 고유 ID | PK |
| `hostGym` | `Gym` | 호스트 체육관 | @ManyToOne |
| `host` | `User` | 호스트 유저 | @ManyToOne |
| `title` | `String` | 오픈매트 제목 |  |
| `description` | `String` | 상세 설명 및 공지 |  |
| `startDateTime` | `LocalDateTime` | 시작 시간 |  |
| `endDateTime` | `LocalDateTime` | 종료 시간 |  |
| `locationName` | `String` | 장소 명칭 |  |
| `address` | `String` | 상세 주소 |  |
| `maxCapacity` | `Integer` | 정원 제한 수 | -1 = 제한 없음 |
| `status` | `OpenMatStatus` | 현재 모집 상태 | @Enumerated |
| `createdAt` | `LocalDateTime` | 생성 일시 |  |
| `updatedAt` | `LocalDateTime` | 수정 일시 |  |

```java
@Entity
@Table(name = "open_mats")
public class OpenMat extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_gym_id")
    private Gym hostGym;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    private String locationName;
    private String address;

    @Column(nullable = false)
    private Integer maxCapacity = -1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenMatStatus status = OpenMatStatus.RECRUITING;

    @ManyToMany(mappedBy = "joinedOpenMats")
    private Set<User> participants = new HashSet<>();

    public int getCurrentParticipants() {
        return participants.size();
    }

    public boolean canApply() {
        return status == OpenMatStatus.RECRUITING &&
               (maxCapacity == -1 || getCurrentParticipants() < maxCapacity);
    }
}
```

## 3. 대회 정보 (Tournament)

**Entity**: `Tournament`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `Long` | 대회 고유 ID | PK |
| `host` | `User` | 대회 주최자 | @ManyToOne |
| `title` | `String` | 대회 명칭 |  |
| `organizer` | `String` | 주최사 정보 |  |
| `posterUrl` | `String` | 대회 포스터 이미지 URL |  |
| `competitionDate` | `LocalDate` | 대회 개최일 |  |
| `registrationDeadline` | `LocalDate` | 접수 마감 기한 |  |
| `location` | `String` | 개최 장소 |  |
| `applyLink` | `String` | 외부 접수처 링크 |  |
| `createdAt` | `LocalDateTime` | 생성 일시 |  |
| `updatedAt` | `LocalDateTime` | 수정 일시 |  |

```java
@Entity
@Table(name = "tournaments")
public class Tournament extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private String title;

    private String organizer;

    @Column(nullable = false)
    private String posterUrl;

    @Column(nullable = false)
    private LocalDate competitionDate;

    @Column(nullable = false)
    private LocalDate registrationDeadline;

    private String location;

    @Column(nullable = false)
    private String applyLink;

    @ElementCollection
    @CollectionTable(name = "tournament_category_tags", joinColumns = @JoinColumn(name = "tournament_id"))
    @Column(name = "tag")
    private List<String> categoryTags = new ArrayList<>();

    public boolean isRegistrationClosed() {
        return LocalDate.now().isAfter(registrationDeadline);
    }
}
```

## 4. BaseTimeEntity (공통 시간 엔티티)

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseTimeEntity {
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```
---

## 변경사항 (2026-02-19)

### OpenMat 조회 모델 보완
- `my applied open mats`는 목록 반환 대신 **페이지 반환**을 사용한다.
- 엔티티(`OpenMat`, `User`)의 컬럼/관계는 변경 없다.
- 응답 모델은 페이지 메타데이터를 포함한다.
  - `content`
  - `page`
  - `size` (기본 10)
  - `totalElements`
  - `totalPages`
  - `last`
