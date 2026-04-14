# 사용자 차단 기능 코드 리뷰

검토 범위:
- `src/main/java/com/rolling/api/domain/openmat/controller/OpenMatController.java`
- `src/main/java/com/rolling/api/domain/openmat/repository/OpenMatRepository.java`
- `src/main/java/com/rolling/api/domain/openmat/service/OpenMatService.java`
- `src/main/java/com/rolling/api/domain/tournament/controller/TournamentController.java`
- `src/main/java/com/rolling/api/domain/tournament/service/TournamentService.java`
- `src/main/java/com/rolling/api/domain/user/repository/UserRepository.java`
- 관련 테스트 파일
- `docs/AGENTS.md`

## Findings

없음.

현재 구현은 조회자 기준 차단 필터를 오픈매트와 대회의 목록/상세에 반영하고 있고, 관련 서비스/컨트롤러 테스트도 함께 추가되어 있다.

## Residual Risks

- 대회 목록 필터는 현재 서비스 레벨에서 메모리 필터링을 사용한다. 데이터가 많아지면 성능 비용이 커질 수 있다.
- 차단 목록 조회 API는 아직 없다. 운영/UX 요구가 생기면 별도 엔드포인트가 필요하다.
- 전체 Gradle 테스트 스위트에는 이번 작업과 무관한 환경 의존성 테스트가 남아 있을 수 있다. 이번 변경과 직접 연결된 테스트는 통과했다.

## Verification

다음 테스트를 실행했고 모두 통과했다.

- `OpenMatServiceTest`
- `OpenMatControllerTest`
- `TournamentServiceTest`
- `TournamentControllerTest`
- `S3PublicBaseUrlValidatorTest`
