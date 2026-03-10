# Rolling 백엔드 Remaining Plan (Unit) - 2026-03-01

기준:
- 요구사항/명세 기준: `docs/AGENTS.md`
- 목표: 남은 작업을 바로 실행 가능한 `Unit` 단위로 분할

진행 표기:
- `[ ]` 미착수
- `[~]` 진행중
- `[x]` 완료

## Unit B-01. Auth 회원 탈퇴
- [x] `DELETE /api/v1/auth/withdraw` 컨트롤러/서비스 구현
- [x] 탈퇴 요청 시 즉시 삭제하지 않고, **다음날 21:00(Asia/Seoul)** 으로 예약
- [x] 예약된 탈퇴를 취소하는 API 구현 (`POST /api/v1/auth/withdraw/cancel`)
- [x] 예약 시각 도달 시 배치로 최종 탈퇴 실행 (`@Scheduled`)
- [x] 사용자 개인정보 삭제 정책 반영 (최종 탈퇴 시점에 제거)
- [x] 리프레시 토큰 무효화 처리
- [x] Swagger 및 API 명세 동기화
- [x] 단위/통합 테스트 작성

완료 기준:
- 인증 사용자 기준 탈퇴 요청 성공 응답(`withdrawalPending=true`, `scheduledAt` 포함)
- 예약 취소 요청 시 탈퇴 예약 해제(`withdrawalPending=false`)
- 예약 시각 도달 후 최종 탈퇴 실행 및 토큰 접근 차단

## Unit B-02. User 내 정보 수정 v2 (nickname, beltColor)
- [x] `PUT /api/v1/users/me`의 수정 범위를 `nickname`, `beltColor`로 고정
- [x] 응답 DTO/Swagger/문서 업데이트
- [x] 테스트 작성

완료 기준:
- `nickname`, `beltColor` 수정이 정상 동작

## Unit B-03. User FCM + 차단
- [ ] `POST /api/v1/users/me/fcm` 구현
- [ ] `POST /api/v1/users/{id}/block` 구현
- [ ] `DELETE /api/v1/users/{id}/block` 구현
- [ ] 자기 자신 차단 방지/존재하지 않는 사용자 예외 처리
- [ ] Swagger 및 테스트 작성

완료 기준:
- FCM 토큰 저장, 차단/해제 API가 idempotent하게 동작

## Unit B-04. Report 공통 도메인
- [ ] `Report` 엔티티/리포지토리/서비스/컨트롤러 기본 골격 구현
- [ ] Enum: `ReportTargetType`, `ReportReason` 반영
- [ ] 동일 유저 동일 대상 중복 신고 방지 제약
- [ ] 자기 게시글 신고 방지 공통 검증
- [ ] 공통 에러 코드/메시지 정리

완료 기준:
- OpenMat/Tournament 신고 로직에서 재사용 가능한 공통 모듈 완성

## Unit B-05. OpenMat 상태 자동화/정합성
- [x] 정원 도달 시 `RECRUITING -> CLOSED` 자동 전환
- [x] 신청 취소로 여유 발생 시 상태 처리 정책 확정 및 반영
- [x] `endDateTime` 경과 시 `FINISHED` 자동 전환 (스케줄러 + 조회시 보정)
- [x] 리스트 정렬/필터 정책을 명세와 일치화
- [x] 신고 임계치 정책 3건 기준으로 정합성 확정

완료 기준:
- 상태 전환이 수동 개입 없이 정책대로 유지됨

## Unit B-06. OpenMat 작성자 관리 API
- [ ] 참가자 목록 조회 API
- [ ] 참가자 강제 취소 API
- [ ] 모집 상태 수동 변경 API (`RECRUITING`, `CLOSED`)
- [ ] 작성자 권한 검증
- [ ] Swagger/테스트/문서 반영

완료 기준:
- 작성자 관리 기능 전체가 API로 노출되고 권한이 보장됨

## Unit B-07. OpenMat 신고 API
- [ ] OpenMat 신고 엔드포인트 구현
- [ ] `Report` 공통 모듈과 연동
- [ ] 신고 3건 이상 시 신규 신청 차단
- [ ] 상세/리스트에서 신고 상태 표기용 필드 정책 확정
- [ ] 테스트 작성

완료 기준:
- 중복 신고/자기 신고가 차단되고 3건 누적 정책이 적용됨

## Unit B-08. Tournament Core API
- [x] Tournament 엔티티/리포지토리/서비스/컨트롤러 구현
- [x] `GET /api/v1/tournaments` 페이징/정렬 정책 구현
- [x] `GET /api/v1/tournaments/{id}` 구현
- [x] `POST/PUT/DELETE /api/v1/tournaments` 구현
- [x] Swagger/명세 동기화

완료 기준:
- 대회 CRUD + 조회 정책이 프론트 연동 가능한 수준으로 완료

## Unit B-09. Tournament 신고/외부 링크 차단
- [ ] Tournament 신고 엔드포인트 구현
- [ ] 중복 신고/자기 신고 방지
- [ ] 신고 3건 이상 시 `applyLink` 차단 정책 구현
- [ ] 리스트/상세 응답에 차단 여부 전달 필드 확정
- [ ] 테스트 작성

완료 기준:
- 신고 누적에 따른 외부 링크 차단이 일관되게 동작

## Unit B-10. 소셜 Provider 정책 정합성
- [ ] `SocialProvider` 요구사항 범위 확정 (`KAKAO`, `GOOGLE`, `NAVER`)
- [ ] 구현/문서 간 불일치 제거
- [x] 미지원 provider 요청 시 에러 스펙 확정

완료 기준:
- 코드/Swagger/문서에 동일 provider 정책이 반영됨

## Unit B-11. 테스트 보강
- [ ] 서비스 단위 테스트 (Auth/User/OpenMat/Report/Tournament)
- [ ] 보안/권한 통합 테스트
- [ ] 오픈매트 신청 동시성 테스트(정원 경계)
- [ ] 회귀 테스트 케이스 문서화

완료 기준:
- 핵심 시나리오가 자동 테스트로 커버됨

## Unit B-12. 문서/운영 정리
- [ ] 엔드포인트 추가/수정 시 Swagger + `docs/AGENTS.md` 동시 갱신
- [ ] DB 전략 점검 (`ddl-auto=create` 대체)
- [ ] 배포 프로파일별 설정 분리
- [ ] 최종 API 변경 로그 정리

완료 기준:
- 개발/운영 환경에서 문서와 실행 코드가 일치

## 추천 구현 순서
1. B-02
2. B-01
3. B-03
4. B-04
5. B-05
6. B-06
7. B-07
8. B-08
9. B-09
10. B-10
11. B-11
12. B-12
