# N+1 리팩터링 코드 리뷰

검토 일시: 2026-04-06

검토 범위:
- `src/main/java/com/rolling/api/domain/inquiry/repository/InquiryRepository.java`
- `src/main/java/com/rolling/api/domain/openmat/dto/OpenMatResponse.java`
- `src/main/java/com/rolling/api/domain/openmat/entity/OpenMat.java`
- `src/main/java/com/rolling/api/domain/openmat/repository/OpenMatRepository.java`
- `src/main/java/com/rolling/api/domain/openmat/service/OpenMatService.java`
- `src/main/java/com/rolling/api/domain/report/repository/ReportRepository.java`
- `src/main/java/com/rolling/api/domain/report/service/ReportService.java`
- `src/test/java/com/rolling/api/domain/openmat/service/OpenMatServiceTest.java`
- `src/test/java/com/rolling/api/domain/report/service/ReportServiceTest.java`

## 1. Findings

이번 변경분 기준으로 즉시 수정이 필요한 blocking bug 또는 명확한 회귀는 발견하지 못했다.

## 2. 남는 리스크 / 테스트 갭

- `ReportRepository.summarizeTargets(...)` 는 `targetType in (...) and targetId in (...)` 뒤에 서비스 레이어에서 요청 키만 다시 필터링하는 구조다.
  - 현재 페이지 크기 기준으로는 실용적이지만, 동일한 `targetId` 가 여러 `targetType` 에 많이 겹치는 데이터셋에서는 필요 이상 집계 row를 읽을 수 있다.
- `OpenMatService` 는 목록 조회 전에 `syncExpiredOpenMats()` 를 항상 수행하므로, 목록 API의 기본 쿼리 수는 여전히 다른 도메인보다 높다.
  - 현재 통합 테스트에서는 lazy entity/collection fetch 없이 상수 개수 쿼리로 고정되는 것을 확인했지만, 추후 성능 최적화가 더 필요하면 이 사전 동기화 전략 자체를 별도 배치/스케줄링으로 분리하는 방향도 검토할 수 있다.

## 3. 전반 평가

- `OpenMat` 목록 계열은 host 선로딩과 participant count 배치 조회로 가장 큰 N+1 구간을 우회했다.
- `Inquiry` 는 목록/상세 DTO 매핑 전에 `user` 를 함께 읽도록 정리돼 위험도가 낮아졌다.
- `Report` 는 reporter LAZY 접근과 대상별 count 반복 호출을 분리해서, 기존 연관 N+1 과 집계 N+1 을 모두 줄이는 방향으로 개선됐다.
- 서비스 단위 테스트와 Hibernate statistics 기반 JPA 통합 테스트가 추가됐다.
  - `NPlusOneIntegrationTest` 에서 `OpenMat`, `Inquiry`, `Report` 목록 조회 시 `entityFetchCount=0`, `collectionFetchCount=0` 을 확인했다.
