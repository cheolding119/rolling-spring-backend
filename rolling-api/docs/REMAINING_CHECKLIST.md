# Rolling 진행 점검 체크리스트 (2026-03-01)

기준:
- 요구사항 기준 문서: `docs/AGENTS.md`
- 코드 기준 범위: `src/main/java/com/rolling/api/**`

## 1. 구현 완료 확인

- [x] 공통 응답 포맷(`ApiResponse`) 및 전역 예외 처리
- [x] JWT 기반 인증 필터 및 보안 설정
- [x] Auth API: `POST /api/v1/auth/login`
- [x] Auth API: `POST /api/v1/auth/refresh`
- [x] Auth API: `POST /api/v1/auth/logout`
- [x] User API: `GET /api/v1/users/me`
- [x] User API: `PUT /api/v1/users/me` (현재 `nickname`, `beltColor` 지원)
- [x] OpenMat API: 생성/목록/상세/수정/삭제
- [x] OpenMat API: 신청/신청취소
- [x] OpenMat API: 내가 신청한 목록(`GET /api/v1/open-mats/my`, 기본 size=10)
- [x] OpenMat 삭제 시 soft delete(`isHidden=true`) 처리

## 2. 부분 완료 (요구사항 대비 갭 존재)

- [ ] OpenMat 정원 도달 시 `CLOSED` 자동 전환 정책 반영
- [ ] OpenMat 종료 시간 경과 시 `FINISHED` 자동 전환 정책 반영
- [ ] OpenMat 상태/정렬 정책을 요구사항과 동일하게 정렬
- [ ] OpenMat 신고 임계치 정책 정합성 확보 (요구사항: 3건, 현재 엔티티 로직: 2건 숨김)
- [ ] 소셜 제공자 정책 정합성 확보 (요구사항에 Naver 포함, 현재 구현은 Kakao/Google)

## 3. 미구현

- [ ] Auth API: 회원 탈퇴 `DELETE /api/v1/auth/withdraw`
- [ ] User API: FCM 토큰 등록 `POST /api/v1/users/me/fcm`
- [ ] User API: 사용자 차단 `POST /api/v1/users/{id}/block`
- [ ] User API: 사용자 차단 해제 `DELETE /api/v1/users/{id}/block`
- [ ] OpenMat 작성자 관리 기능 API (참가자 목록, 강제 취소, 모집 상태 수동 변경)
- [ ] OpenMat 신고 API
- [ ] Tournament 도메인/엔티티/리포지토리/서비스/컨트롤러 전체
- [ ] Tournament 신고 누적 시 외부 링크 차단 로직
- [ ] Report 도메인/중복 신고 방지/자기 글 신고 방지 전체

## 4. 품질/운영 항목

- [ ] 단위 테스트/통합 테스트 보강 (현재 테스트 코드 최소 상태)
- [ ] Swagger와 명세 문서 동기화 점검 자동화
- [ ] 운영 DB 기준 마이그레이션 전략 점검 (`ddl-auto=create` 탈피)
