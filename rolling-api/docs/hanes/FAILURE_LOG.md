# Failure Log

## 2026-05-03 - App Review 비회원 접근 백엔드 점검

### 결과

- 공개 조회 API 6개는 토큰 없이 접근 가능하도록 `SecurityConfig`와 통합 테스트 기준으로 확인됐다.
- 계정 기반 액션인 오픈매트 신청, 오픈매트 신고, 대회 신고는 토큰 없이 `401 UNAUTHORIZED`를 반환하도록 확인됐다.
- 일반 사용자 토큰으로 관리자 API 호출 시 `403 FORBIDDEN`이 유지된다.

### 발견 사항

- 운영 smoke test는 아직 수행하지 않았다. 운영 또는 심사 대상 API base URL이 필요하다.
- Flutter 비회원 모드가 기존 secure storage의 토큰을 공개 조회 요청에 붙이면 완전한 비회원 요청이 아니다. 특히 정지 계정 토큰이 붙은 경우 `UserSanctionAccessFilter` 정책에 따라 공지사항은 허용되지만 오픈매트/대회 공개 조회는 제한될 수 있다. 이번 백엔드 계약에서는 비회원 둘러보기 공개 조회 요청을 `Authorization` 헤더 없이 호출하는 것으로 문서화했다.

### 후속 조치

- Flutter 네트워크 로그에서 비회원 공개 조회 요청에 `Authorization` 헤더가 없는지 확인한다.
- 재심사 전 운영 API에서 공개 조회 6개와 비인증 차단 경로 smoke test 결과를 체크리스트에 남긴다.
