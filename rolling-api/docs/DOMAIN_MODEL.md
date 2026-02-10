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

### DayOfWeek
요일 타입 (Java 기본 제공 사용)
```java
import java.time.DayOfWeek;
// MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
```

### OpenMatStatus
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

## 2. 체육관 (Gym)

**Entity**: `Gym`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `Long` | 체육관 고유 ID | PK |
| `owner` | `User` | 관리자(관장) | @ManyToOne |
| `name` | `String` | 체육관 명칭 | @Column(nullable = false) |
| `address` | `String` | 도로명/지번 주소 |  |
| `latitude` | `Double` | 위도 (Latitude) |  |
| `longitude` | `Double` | 경도 (Longitude) |  |
| `phone` | `String` | 대표 연락처 |  |
| `description` | `String` | 상세 소개 문구 | @Column(columnDefinition = "TEXT") |
| `priceInfo` | `String` | 가격 가이드 정보 |  |
| `isVisible` | `Boolean` | 앱 노출 여부 | default = true |
| `createdAt` | `LocalDateTime` | 생성 일시 | @CreatedDate |
| `updatedAt` | `LocalDateTime` | 수정 일시 | @LastModifiedDate |

```java
@Entity
@Table(name = "gyms")
public class Gym extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false)
    private String name;

    private String address;
    private Double latitude;
    private Double longitude;
    private String phone;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String priceInfo;

    @Column(nullable = false)
    private Boolean isVisible = true;

    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GymSchedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GymImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "gym", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GymAmenity> amenities = new ArrayList<>();
}
```

**Entity**: `GymSchedule` (시간표)

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `Long` | 시간표 ID | PK |
| `gym` | `Gym` | 체육관 | @ManyToOne |
| `dayOfWeek` | `DayOfWeek` | 요일 | @Enumerated |
| `startTime` | `LocalTime` | 시작 시간 |  |
| `endTime` | `LocalTime` | 종료 시간 |  |
| `className` | `String` | 클래스 명칭 | 예: "기초반", "오픈매트" |

```java
@Entity
@Table(name = "gym_schedules")
public class GymSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DayOfWeek dayOfWeek;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private String className;
}
```

**Entity**: `GymImage` (체육관 이미지)

```java
@Entity
@Table(name = "gym_images")
public class GymImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(nullable = false)
    private String imageUrl;

    private Integer sortOrder;
}
```

**Entity**: `GymAmenity` (편의시설)

```java
@Entity
@Table(name = "gym_amenities")
public class GymAmenity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gym_id", nullable = false)
    private Gym gym;

    @Column(nullable = false)
    private String amenityType; // parking, shower, locker 등

    @Column(nullable = false)
    private Boolean available = true;
}
```

## 3. 커뮤니티 (Community)

**Entity**: `Post`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `Long` | 게시글 고유 ID | PK |
| `author` | `User` | 작성자 | @ManyToOne |
| `title` | `String` | 게시글 제목 |  |
| `content` | `String` | 본문 내용 | @Column(columnDefinition = "TEXT") |
| `regionTag` | `String` | 지역 필터 태그 |  |
| `viewCount` | `Integer` | 조회수 | default = 0 |
| `commentCount` | `Integer` | 댓글 총 개수 | default = 0 |
| `isReported` | `Boolean` | 신고 접수 여부 | default = false |
| `reportedCount` | `Integer` | 신고 접수 개수 | default = 0 |
| `isDeleted` | `Boolean` | 삭제 여부 | Soft Delete |
| `createdAt` | `LocalDateTime` | 작성 일시 |  |
| `updatedAt` | `LocalDateTime` | 최종 수정 일시 |  |

```java
@Entity
@Table(name = "posts")
public class Post extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private String regionTag;

    @Column(nullable = false)
    private Integer viewCount = 0;

    @Column(nullable = false)
    private Integer commentCount = 0;

    @Column(nullable = false)
    private Boolean isReported = false;

    @Column(nullable = false)
    private Integer reportedCount = 0;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL)
    private List<PostImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "post")
    private List<PostLike> likes = new ArrayList<>();

    @OneToMany(mappedBy = "post")
    private List<Comment> comments = new ArrayList<>();
}
```

**Entity**: `PostLike` (좋아요/싫어요)

```java
@Entity
@Table(name = "post_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"post_id", "user_id"})
})
public class PostLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Boolean isLike; // true: 좋아요, false: 싫어요
}
```

## 4. 오픈매트 (OpenMat)

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

## 5. 댓글 (Comment)

**Entity**: `Comment`

| **필드명** | **타입** | **설명** | **비고** |
| --- | --- | --- | --- |
| `id` | `Long` | 댓글 고유 ID | PK |
| `post` | `Post` | 게시글 | @ManyToOne |
| `author` | `User` | 작성자 | @ManyToOne |
| `content` | `String` | 댓글 내용 |  |
| `isDeleted` | `Boolean` | 삭제 여부 | Soft Delete |
| `createdAt` | `LocalDateTime` | 작성 일시 |  |
| `updatedAt` | `LocalDateTime` | 최종 수정 일시 |  |

```java
@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Boolean isDeleted = false;
}
```

## 6. 대회 정보 (Tournament)

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

## 7. BaseTimeEntity (공통 시간 엔티티)

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
