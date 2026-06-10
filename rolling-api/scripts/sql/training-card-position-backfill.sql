-- 훈련카드 포지션 백필용 SQL
-- 목적:
-- 1. TrainingCardPosition enum 확장 이후 기존 적재 카드의 position 값을 더 구체적인 값으로 보정한다.
-- 2. title 기준으로만 갱신하므로, 현재 수동 시드로 넣은 카드 세트에 대해 실행하는 것을 전제로 한다.

BEGIN;

UPDATE training_cards
SET position = 'CLOSED_GUARD',
    updated_at = NOW()
WHERE title IN (
    '클로즈드 가드 암바',
    '트라이앵글 초크',
    '클로즈드 가드 기무라',
    '크로스 칼라 초크',
    '힙 범프 스윕',
    '플라워 스윕',
    '펜듈럼 스윕',
    '거버 가드',
    '스탠딩 가드 브레이크'
);

UPDATE training_cards
SET position = 'OPEN_GUARD',
    updated_at = NOW()
WHERE title IN (
    '시저스 스윕',
    '엑스 패스',
    '스파이더 가드 스윕',
    '라소 가드 스윕',
    '델라히바 스윕',
    '토레안도 패스',
    '오버 언더 패스',
    '버터플라이 스윕',
    '스파이더 가드 컨트롤',
    '오모플라타',
    '라소 가드 컨트롤',
    '레그 드래그 패스',
    '베림보로'
);

UPDATE training_cards
SET position = 'HALF_GUARD',
    updated_at = NOW()
WHERE title IN (
    '스매시 패스',
    '하프 가드 언더훅 스윕',
    '인버티드 암바'
);

UPDATE training_cards
SET position = 'LEG_ENTANGLEMENT',
    updated_at = NOW()
WHERE title IN (
    '싱글 레그 엑스 스윕',
    '엑스 가드 스윕',
    '스트레이트 앵클 락'
);

UPDATE training_cards
SET position = 'STANDING',
    updated_at = NOW()
WHERE title IN (
    '더블 레그 테이크다운',
    '싱글 레그 테이크다운',
    '오소토 가리 (큰바깥후리기)',
    '가드 풀',
    '길로틴 초크',
    '앵클 픽'
);

UPDATE training_cards
SET position = 'MOUNT',
    updated_at = NOW()
WHERE title IN (
    '우파 이스케이프',
    '엘보우 이스케이프',
    '에제키엘 초크 (마운트)'
);

UPDATE training_cards
SET position = 'BACK',
    updated_at = NOW()
WHERE title IN (
    '리어 네이키드 초크',
    '보우 앤 애로우 초크'
);

UPDATE training_cards
SET position = 'TURTLE',
    updated_at = NOW()
WHERE title IN (
    '다스 초크',
    '터틀 싯 아웃 이스케이프'
);

COMMIT;
