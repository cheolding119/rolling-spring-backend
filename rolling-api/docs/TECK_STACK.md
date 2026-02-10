이 문서는 **롤링 (Rolling)** 프로젝트의 백엔드 기술 스택을 정의합니다.

---

## 1. Core Framework

- **Framework**: Spring Boot 3.x
- **Language**: Java 17+ (LTS)
- **Build Tool**: Gradle (Kotlin DSL)
- **Architecture**: Layered Architecture (Controller - Service - Repository)

---

## 2. Database & ORM

- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA (Hibernate)
- **Query DSL**: QueryDSL 5.x (동적 쿼리, 복잡한 검색/필터링)
- **Migration**: Flyway (스키마 버전 관리)
- **Connection Pool**: HikariCP (Spring Boot 기본)

### 설정 예시
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/rolling?useSSL=false&serverTimezone=Asia/Seoul
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
        default_batch_fetch_size: 100
```

---

## 3. Authentication & Security

- **Security Framework**: Spring Security 6.x
- **Token**: JWT (JSON Web Token)
    - Access Token: 30분
    - Refresh Token: 14일 (DB 또는 Redis 저장)
- **Social Login Verification**:
    - Apple: Apple Public Key로 ID Token 검증
    - Google: Google OAuth2 Token Info API
    - Kakao: Kakao User API
    - Naver: Naver User API

### JWT 라이브러리
```gradle
implementation 'io.jsonwebtoken:jjwt-api:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.3'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.3'
```

### Security Config 구조
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**").permitAll()
                .requestMatchers("/api/v1/gyms/**").permitAll()
                .requestMatchers("/api/v1/tournaments/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

---

## 4. API & Documentation

- **API Style**: RESTful API
- **Documentation**: Swagger/OpenAPI 3.0 (springdoc-openapi)
- **Validation**: Jakarta Bean Validation (Hibernate Validator)
- **Response Format**: 표준화된 JSON Response
- **JSON Naming Convention**: **camelCase** (모든 요청/응답 JSON 필드명)

### API 문서 설정
```gradle
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0'
```

### 표준 Response 형식
```java
@Getter
@AllArgsConstructor
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "Success", data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }
}
```

---

## 5. External Services

### Firebase (FCM Push Notification)
```gradle
implementation 'com.google.firebase:firebase-admin:9.2.0'
```

### AWS S3 (이미지 저장소)
```gradle
implementation 'software.amazon.awssdk:s3:2.21.0'
```

### Redis (캐싱, Refresh Token 저장)
```gradle
implementation 'org.springframework.boot:spring-boot-starter-data-redis'
```

---

## 6. Scheduling & Background Jobs

- **Scheduler**: Spring @Scheduled
- **Use Cases**:
    - 오픈매트 상태 자동 업데이트 (FINISHED 전환)
    - 만료된 Refresh Token 정리
    - 대회 마감일 체크

```java
@Component
@EnableScheduling
public class ScheduledTasks {
    @Scheduled(cron = "0 */10 * * * *") // 10분마다
    public void updateOpenMatStatus() {  }

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시
    public void cleanupExpiredTokens() {  }
}
```

---

## 7. Logging & Monitoring

- **Logging**: SLF4J + Logback
- **Log Format**: JSON (운영 환경)
- **Monitoring** (선택적):
    - Spring Boot Actuator
    - Prometheus + Grafana

```yaml
logging:
  level:
    root: INFO
    com.rolling: DEBUG
    org.hibernate.SQL: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
```

---

## 8. Testing

- **Unit Test**: JUnit 5 + Mockito
- **Integration Test**: @SpringBootTest + TestContainers
- **API Test**: MockMvc, RestAssured

```gradle
testImplementation 'org.springframework.boot:spring-boot-starter-test'
testImplementation 'org.testcontainers:mysql:1.19.3'
testImplementation 'org.testcontainers:junit-jupiter:1.19.3'
```

---

## 9. Build & Deployment

- **CI/CD**: GitHub Actions
- **Container**: Docker
- **Cloud**: AWS (EC2, RDS, S3, ElastiCache)

### Dockerfile 예시
```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 10. Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    // Database
    runtimeOnly("com.mysql:mysql-connector-j")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-mysql")

    // QueryDSL
    implementation("com.querydsl:querydsl-jpa:5.0.0:jakarta")
    annotationProcessor("com.querydsl:querydsl-apt:5.0.0:jakarta")
    annotationProcessor("jakarta.annotation:jakarta.annotation-api")
    annotationProcessor("jakarta.persistence:jakarta.persistence-api")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // API Documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")

    // Firebase Admin SDK
    implementation("com.google.firebase:firebase-admin:9.2.0")

    // AWS S3
    implementation("software.amazon.awssdk:s3:2.21.0")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:mysql:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
}
```

---

## 11. Project Structure

```
src/main/java/com/rolling/
├── RollingApplication.java
├── global/
│   ├── config/           # Security, Swagger, JPA 설정
│   ├── exception/        # GlobalExceptionHandler, CustomExceptions
│   ├── response/         # ApiResponse, ErrorResponse
│   ├── util/             # JwtProvider, S3Uploader 등
│   └── scheduler/        # Scheduled Tasks
├── domain/
│   ├── user/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   ├── controller/
│   │   └── dto/
│   ├── gym/
│   ├── openmat/
│   ├── post/
│   ├── comment/
│   └── tournament/
└── infra/
    ├── firebase/         # FCM Service
    ├── s3/               # S3 Upload Service
    └── social/           # Social Login Verifiers
```
