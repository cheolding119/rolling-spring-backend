# JVM Heap Tuning Guideline for Rolling API on Lightsail 2GB

기준일: 2026-04-16

## 목적

이 문서는 현재 운영 중인 Rolling API의 JVM heap 메모리를 얼마나 늘려야 하는지 판단하기 위한 기준 문서다.

현재 관찰된 증상은 다음과 같다.

- API 요청이 거의 없는데도 JVM Heap Usage가 95%까지 올라갔다가 다시 내려옴
- Grafana에서 heap 사용률이 10~15분 단위로 크게 출렁임
- 운영 서버는 AWS Lightsail $12 플랜을 사용 중
- 현재 서버 메모리 상태는 다음과 같음

```text
total:    1.9Gi
used:     1.2Gi
free:     166Mi
buff/cache: 767Mi
available: 729Mi
swap:     2.0Gi
swap used: 296Mi
```

## 현재 프로젝트 설정

현재 프로젝트 기준 설정은 JVM heap을 `384m`으로 상향하고, 컨테이너 메모리 제한을 `768M`으로 올린 상태다.

- [Dockerfile](../Dockerfile) 에서 `JAVA_OPTS="-Xmx384m -Xms256m"`
- [docker-compose.yml](../docker-compose.yml) 에서 `JAVA_OPTS: ${JAVA_OPTS:- -Xmx384m -Xms256m}`
- 같은 compose 파일의 API 서비스는 `memory: 768M` 제한을 두고 있다

즉, 현재는 다음 조합이다.

- JVM heap 최대값: `384m`
- JVM heap 시작값: `256m`
- 컨테이너 메모리 제한: `768M`

## 판단

### 결론

현재 상황에서는 `256m`은 너무 작은 편이고, `512m`로 바로 올리는 것은 비추천이다.

1차 권장치는 `Xmx384m`이다.

다만 heap만 올리면 끝나는 구조는 아니다.

- 컨테이너 메모리 제한과 JVM heap은 같이 조정해야 한다
- JVM은 heap 외에도 non-heap 메모리를 쓰므로 `heap = container limit`에 가깝게 맞추면 안 된다
- 현재 상황에서는 `heap`을 올릴 때 `컨테이너 limit`도 함께 올리고, heap이 limit의 대략 `60~75%` 정도가 되게 두는 편이 안전하다

### 이유

- heap이 95%까지 올라간다는 것은 현재 live set과 peak 변동폭을 감안할 때 `256m`이 충분한 여유를 주지 못한다는 뜻이다.
- `512m`는 `768M` 컨테이너에서도 가능 범위에 들어오지만, JVM의 비heap 메모리까지 감안하면 아직 1차 권장값으로는 공격적이다.
- JVM은 heap 외에도 Metaspace, Code Cache, Thread Stack, direct/native memory를 사용한다.
- 현재 호스트도 `available 729MiB`, `swap used 296MiB` 상태라서 여유가 넉넉하지 않다.

## 권장안

### 1단계 권장: `Xmx384m`

권장 설정:

```text
-Xmx384m
-Xms256m
container memory limit: 768M
```

의도:

- 현재 256m 대비 약 50%의 heap headroom을 확보한다
- startup 시점의 메모리 고정 부담은 크게 늘리지 않는다
- 급격한 출렁임을 완화하면서도 호스트 메모리 압박을 과도하게 키우지 않는다
- 컨테이너 메모리와 heap의 비율을 함께 맞춰 OOMKilled 위험을 줄인다

이 단계는 다음 상황에 가장 적합하다.

- GC 후 heap 사용량이 다시 내려오기는 하지만 자주 80% 이상으로 치솟는 경우
- 낮은 트래픽인데도 background task, scheduler, metric, logging, cache 영향으로 메모리 사용량이 출렁이는 경우
- 아직 메모리 누수 여부가 확정되지 않은 경우

### 2단계 권장: `Xmx512m`

`Xmx512m`는 다음 조건을 만족할 때만 검토한다.

- `Xmx384m`로 올린 뒤에도 GC 후 heap 바닥값이 계속 높음
- Full GC 빈도 또는 GC pause가 눈에 띄게 증가함
- 컨테이너 메모리 제한을 `512M`보다 넉넉하게 상향함

권장하는 최소 방향은 다음과 같다.

- 컨테이너 limit: `1G` 권장, 최소 `768M`
- JVM heap: `512m`

즉, `512m`는 heap만 독립적으로 올리는 값이 아니라, 컨테이너 메모리 제한까지 같이 올릴 때 고려해야 한다.

## 왜 512m를 바로 추천하지 않는가

현재 서버는 Lightsail 2GB 급이고, 이미 swap도 일부 사용 중이다.

이 상태에서 `heap 512m`를 바로 적용하면 다음 리스크가 있다.

- non-heap 영역 때문에 실제 RSS가 예상보다 더 커질 수 있음
- 컨테이너 메모리 한도에 근접하거나 초과할 수 있음
- swap 사용이 더 늘어나면 지연 시간이 커질 수 있음
- heap을 키웠는데도 GC 압박의 원인이 캐시나 백그라운드 작업이면 체감 개선이 제한적일 수 있음

## 운영 기준

heap 증설 후에는 아래 기준으로 결과를 판단한다.

### 괜찮은 상태

- GC 이후 heap 사용률이 대략 60% 이하로 내려감
- 80% 이상 구간이 일시적이고 길게 유지되지 않음
- Full GC가 잦지 않음
- OOMKilled가 발생하지 않음
- 응답 지연이 증가하지 않음

### 추가 조치가 필요한 상태

- GC 후에도 heap 바닥값이 계속 높아짐
- 시간이 갈수록 최소 사용량이 올라감
- `Xmx384m`에서도 80~90% 구간이 자주 반복됨
- heap 증설 후에도 지연이 줄지 않음

이 경우는 단순 증설보다 메모리 누수, 캐시 누적, scheduler 작업, 객체 보유 구조를 먼저 봐야 한다.

## 실행 순서

1. 현재 운영값을 `Xmx384m`로 상향한다.
2. 컨테이너 memory limit도 `768M` 정도로 함께 상향한다.
3. `24~72시간` 정도 heap usage, GC pause, RSS 변화를 관찰한다.
4. GC 후 바닥값이 충분히 낮아지면 유지한다.
5. 여전히 높으면 컨테이너 limit을 `1G`까지 올린 뒤 `Xmx512m`를 검토한다.

## 최종 결론

지금 환경에서는 `256m`은 작다.

다만 바로 `512m`로 점프하는 것보다, 먼저 `384m`로 올려서 운영 안정성을 확인하는 편이 맞다.

한 줄로 정리하면 다음과 같다.

- 현재 1차 권장치: `Xmx384m + container limit 768M`
- `Xmx512m`는 `container limit 1G`와 함께 검토
- 판단 기준은 peak가 아니라 `GC 이후 바닥값`과 `Full GC 여부`

## Phase Checklist

### Phase 1. 현재 상태 확인

- [ ] 현재 운영 중인 컨테이너의 실제 메모리 limit 확인
- [ ] JVM 시작 옵션이 `Dockerfile` 기준인지 `docker-compose.yml` 기준인지 확인
- [ ] `free -h`로 호스트 전체 메모리와 swap 사용량 확인
- [ ] Grafana에서 heap peak와 GC 이후 바닥값을 함께 확인
- [ ] Full GC 발생 여부와 GC pause 증가 여부 확인

### Phase 2. 1차 증설 적용

- [ ] JVM heap을 `Xmx384m`로 변경
- [ ] JVM 시작 heap은 우선 `Xms256m` 유지
- [ ] 컨테이너 memory limit을 `768M`으로 상향
- [ ] 배포 후 프로세스 RSS가 limit 안에 안정적으로 들어오는지 확인
- [ ] OOMKilled 발생 여부 확인

### Phase 3. 관찰

- [ ] 최소 `24시간`, 가능하면 `72시간` 동안 관찰
- [ ] GC 후 heap 사용률이 충분히 내려오는지 확인
- [ ] 80% 이상 구간이 일시적인지 반복적인지 확인
- [ ] 응답 지연이나 error rate 증가가 있는지 확인
- [ ] swap 사용량이 더 증가하지 않는지 확인

### Phase 4. 재판단

- [ ] `Xmx384m`에서도 heap 바닥값이 계속 높으면 원인 추가 조사
- [ ] scheduler, cache, logging, background task 영향 여부 점검
- [ ] 필요 시 container limit을 `1G`로 상향 검토
- [ ] `Xmx512m`는 container limit `1G`와 함께만 적용 검토

### Phase 5. 최종 정리

- [ ] 최종 적용값과 이유를 운영 문서에 기록
- [ ] 변경 전후 heap peak, GC pause, RSS 비교 결과 남기기
- [ ] 다음 증설 또는 롤백 조건을 문서화
- [ ] 메모리 이상 징후 발생 시 확인할 dashboard 기준을 고정
