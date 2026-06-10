-- 훈련카드 수동 적재용 SQL
-- 주의:
-- 1. Flyway 마이그레이션 파일이 아니다.
-- 2. V34~V39가 적용된 DB에서만 수동 실행한다.
-- 3. 아래 파일은 한 세션에서 처음부터 끝까지 한 번에 실행한다.
-- 4. seed_key는 DB 컬럼이 아니라 이 SQL 파일 안에서 카드끼리 연결하기 위한 임시 키다.

BEGIN;

CREATE TEMP TABLE tmp_training_card_seed_map (
    seed_key VARCHAR(100) PRIMARY KEY,
    card_id BIGINT NOT NULL
) ON COMMIT DROP;

-- =========================================================
-- 1. 훈련카드 본문 데이터
-- 카드 1개 추가할 때마다 아래 블록을 복사해서 계속 추가
-- =========================================================

-- 예시
-- INSERT INTO training_cards (
--     title,
--     summary,
--     topic,
--     level,
--     position,
--     situation_summary,
--     description,
--     situation_description,
--     starting_position_description,
--     flow_description,
--     key_points,
--     common_mistakes,
--     cautions,
--     youtube_url,
--     active,
--     display_order,
--     created_at,
--     updated_at
-- ) VALUES (
--     'Knee Cut Pass',
--     '상대 다리 라인을 가르며 압박으로 통과하는 대표적인 패스 기술',
--     'PASS',
--     'BEGINNER',
--     'GUARD',
--     '상대의 니쉴드나 하프가드 방어를 압박하면서 패스할 때 사용',
--     '기술 상세 설명',
--     '사용 상황 설명',
--     '시작 자세 또는 전제 상황',
--     '기술 흐름 설명',
--     '핵심 포인트',
--     '자주 틀리는 점',
--     '주의할 점',
--     'https://www.youtube.com/watch?v=example',
--     TRUE,
--     1,
--     NOW(),
--     NOW()
-- );
-- INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
-- VALUES ('knee-cut-pass', currval('training_cards_id_seq'));

-- 실제 데이터 추가 구간

-- 2026-06-10 1차 적재 데이터
-- 클로즈드 가드 암바 / closed-guard-armbar
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '클로즈드 가드 암바',
    '클로즈드 가드에서 상대의 팔을 펴지게 만들어 팔꿈치 관절을 꺾는 서브미션',
    'SUBMISSION',
    'BEGINNER',
    'CLOSED_GUARD',
    '클로즈드 가드에서 상대가 체중을 싣고 압박하거나 팔을 깊게 뻗었을 때 사용',
    '클로즈드 가드 암바는 하위 포지션에서 상대의 팔을 제압하고, 다리로 상대의 어깨와 머리를 통제하여 팔꿈치 관절을 꺾는 주짓수의 가장 대표적인 서브미션 중 하나이다. 엉덩이의 움직임과 다리의 각도가 성공 여부를 결정한다.',
    '하위 포지션(클로즈드 가드)에서 상대방이 목 깃을 잡거나 가슴을 짚고 일어서려 할 때, 또는 팔을 무방비하게 뻗었을 때 그 팔을 공격하기 매우 좋은 상황이다.',
    '클로즈드 가드 상태에서 상대의 한쪽 팔을 내 두 팔로 강하게 컨트롤하고 있는 상태.',
    '상대의 팔을 대각선으로 당겨 그립을 확보하고, 반대쪽 발로 상대의 골반을 밟는다. 골반을 틀어 각도를 만든 뒤, 남은 다리로 상대의 등 상단을 누르고 골반을 밟았던 다리를 상대 머리 위로 넘겨 팔꿈치를 꺾는다.',
    '상대의 엄지손가락이 하늘을 향하도록 팔 방향을 유지해야 하며, 내 무릎을 단단히 조여 상대의 어깨가 빠져나가지 않도록 해야 한다.',
    '골반을 충분히 틀지 않고 다리만 넘기려다가 상대에게 방어할 공간을 내주는 실수가 잦다.',
    '다리를 머리 위로 넘길 때 상대가 체중을 실어 스택(Stack) 압박을 들어올 수 있으므로 엉덩이를 무겁게 유지해야 한다.',
    'https://www.youtube.com/watch?v=armbar123',
    TRUE,
    1,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('closed-guard-armbar', currval('training_cards_id_seq'));

-- 트라이앵글 초크 / triangle-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '트라이앵글 초크',
    '두 다리를 이용해 상대의 목과 한쪽 팔을 감싸 조르는 서브미션',
    'SUBMISSION',
    'BEGINNER',
    'CLOSED_GUARD',
    '상대의 한쪽 팔은 가드 안에, 다른 한 팔은 가드 밖에 있을 때 사용',
    '트라이앵글 초크는 하위 가드 포지션에서 두 다리를 삼각형 모양으로 결합해 상대의 경동맥을 강하게 압박하는 기술이다. 다리 힘을 이용하므로 체급 차이를 극복하기 좋으며, 암바와 연계하기가 매우 훌륭하다.',
    '오픈 가드나 클로즈드 가드에서 상대가 가드를 패스하기 위해 한 팔을 내 다리 사이로 집어넣거나, 한 팔의 통제가 풀렸을 때 기습적으로 시도한다.',
    '상대의 한 팔과 머리만 내 두 다리 사이에 들어와 있고 상체가 어느 정도 숙여져 있는 상태.',
    '다리를 들어 올려 상대의 목과 한쪽 어깨를 감싼 뒤 발목을 오금에 걸어 가채결 상태를 만든다. 상대의 갇힌 팔을 내 골반 반대편으로 넘기고, 내 골반을 살짝 들어올리며 머리나 정강이를 잡아 각도를 튼 뒤 다리를 완전히 잠가 압박한다.',
    '상대의 갇힌 팔이 완전히 교차되어야 경동맥 압박이 강해진다. 정면보다는 상대의 갇힌 팔 쪽으로 각도를 크게 틀어주는 것이 핵심이다.',
    '각도를 틀지 않고 정면에서만 다리를 잠그려고 하면 조르기가 완성되지 않고 다리만 아픈 경우가 많다.',
    '상대가 들어 올려서 슬램(바닥에 내리찍기)을 시도할 수 있으므로, 다리를 잠근 후 상대의 다리 한쪽을 팔로 안아 방어해야 한다.',
    'https://www.youtube.com/watch?v=triangle123',
    TRUE,
    2,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('triangle-choke', currval('training_cards_id_seq'));

-- 클로즈드 가드 기무라 / kimura-from-closed-guard
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '클로즈드 가드 기무라',
    '상대의 팔을 숫자 4자 모양으로 얽어 어깨 관절을 회전시켜 꺾는 서브미션',
    'SUBMISSION',
    'BEGINNER',
    'CLOSED_GUARD',
    '상대가 바닥을 짚거나 내 몸통 쪽에 손을 두어 팔꿈치가 굽혀진 상태일 때 사용',
    '키무라 락은 상대의 손목을 잡고 내 다른 팔을 상대의 팔에 얽어 강한 지렛대 원리를 생성해 어깨를 꺾는 관절기다. 서브미션뿐만 아니라 스윕(Hip Bump Sweep)으로 전환하는 연결고리로도 훌륭하게 쓰인다.',
    '클로즈드 가드에서 상대의 상체를 끌어당겼을 때, 상대가 방어하기 위해 매트 바닥을 양손으로 짚었을 때 그 빈틈을 노려 손목을 캐치한다.',
    '클로즈드 가드에서 상대방의 손이 매트에 닿아있고, 몸을 일으켜 상대 팔에 내 팔을 얽을 수 있는 상태.',
    '상대의 한쪽 손목을 같은 쪽 손으로 잡는다. 윗몸일으키기를 하듯 상체를 일으켜 내 반대쪽 팔을 상대의 팔 위로 넘겨 내 손목을 잡는다(기무라 그립). 등 뒤로 눕으면서 골반을 틀고 상대의 팔을 상대의 등 뒤쪽으로 밀어 올려 어깨를 꺾는다.',
    '내쉬는 호흡과 함께 빠르게 상체를 일으키는 스피드, 그리고 서브미션 마무리 시 팔만 쓰는 것이 아니라 몸 전체의 회전을 이용해야 한다.',
    '기무라 그립은 잡았으나 골반의 위치를 고정하지 않아 상대가 앞구르기를 하거나 쉽게 팔을 빼는 경우가 흔하다.',
    '상대가 강한 힘으로 벨트나 바지를 잡고 버틸 수 있으므로, 그립을 뜯어내거나 다리를 이용해 상대 자세를 무너뜨리는 작업이 병행되어야 한다.',
    NULL,
    TRUE,
    3,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('kimura-from-closed-guard', currval('training_cards_id_seq'));

-- 크로스 칼라 초크 / cross-collar-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '크로스 칼라 초크',
    '도복의 양쪽 목 깃을 엇갈려 잡아 상대의 경동맥을 조르는 기본 초크',
    'SUBMISSION',
    'BEGINNER',
    'CLOSED_GUARD',
    '가드에서 상대가 상체를 세우지 못하고 도복 깃이 노출되었을 때 사용',
    '크로스 칼라 초크는 도복(Gi) 주짓수의 가장 상징적이고 기초적인 초크 기술이다. 상대의 양쪽 목 깃을 깊숙하게 X자로 교차하여 잡아 손목의 날과 전완근으로 양쪽 경동맥을 압박하여 탭을 받아낸다.',
    '상대가 클로즈드 가드 안에서 자세를 낮추고 방어하거나, 가드 패스를 시도할 때 중심이 쏠려 목 깃을 쉽게 내어준 상황에서 유리하다.',
    '상대의 목 깃에 접근할 수 있도록 거리가 가깝게 유지된 클로즈드 가드 상태.',
    '한 손으로 상대의 목 깃을 최대한 깊게 잡는다(손바닥이 하늘을 향하게). 반대 손은 상대의 턱 아래를 지나 반대쪽 목 깃을 깊게 잡는다. 양 팔꿈치를 내 갈비뼈 쪽으로 강하게 당기며 손목을 바깥쪽으로 회전시켜 압박한다.',
    '첫 번째 그립을 잡을 때 손이 상대의 목 뒤까지 깊숙이 들어가야 초크가 완벽하게 걸린다. 손목의 꺾임이 아니라 팔꿈치를 몸으로 당기는 힘을 써야 한다.',
    '그립이 얕은 상태에서 힘으로만 조르려다 손가락만 아프고 상대는 방어에 성공하는 경우가 많다.',
    '두 번째 손이 들어갈 때 상대가 방어하거나 암바를 시도할 수 있으므로, 다리를 이용해 상대 상체를 계속 내 쪽으로 당겨두어야 한다.',
    'https://www.youtube.com/watch?v=crosscollar',
    TRUE,
    4,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('cross-collar-choke', currval('training_cards_id_seq'));

-- 시저스 스윕 / scissor-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '시저스 스윕',
    '다리를 가위처럼 교차하여 상대를 넘어뜨리고 탑 마운트로 올라가는 스윕',
    'SWEEP',
    'BEGINNER',
    'OPEN_GUARD',
    '상대가 한쪽 무릎을 세우거나 압박해 올 때 중심을 띄워 넘길 때 사용',
    '시저스 스윕은 가위차기 형태의 다리 움직임을 이용해 상대의 중심을 무너뜨리는 클래식한 가드 스윕이다. 상체의 깃과 소매 컨트롤을 통해 상대의 밸런스를 앞으로 쏠리게 한 뒤 하체를 베어내듯 넘기는 것이 특징이다.',
    '클로즈드 가드에서 오픈 가드로 전환하면서, 상대가 몸을 세우지 못하게 목 깃을 잡고 있으며 한쪽 무릎을 상대 배 위에 대고 방패를 만들었을 때 사용한다.',
    '한 손은 목 깃, 한 손은 소매를 통제하고 내 한쪽 정강이는 상대의 배를 가로지르며 방패 역할을 하는 니쉴드 상태.',
    '상대의 상체를 내 쪽으로 강하게 당겨 체중을 내 배 위 정강이에 싣게 한다. 상대 엉덩이가 뒤꿈치에서 떨어져 가벼워지는 순간, 배에 있던 정강이로 상대의 가슴을 밀어 넘기고 반대쪽 다리로 상대의 지지하는 무릎을 가위차기하듯 쓸어낸다.',
    '상대를 내 몸 위로 확실하게 당겨서 ''띄우는'' 동작이 필수적이다. 무겁게 앉아있는 상대를 다리 힘만으로 넘길 수는 없다.',
    '상대를 당겨 중심을 이동시키지 않고 단순히 다리만 차서 넘기려다 힘을 빼는 경우가 가장 많다.',
    '상대가 베이스를 강하게 잡고 버티면 무리하게 시도하지 말고, 즉각적으로 트라이앵글 초크나 다른 기술로 전환할 준비를 해야 한다.',
    NULL,
    TRUE,
    5,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('scissor-sweep', currval('training_cards_id_seq'));

-- 힙 범프 스윕 / hip-bump-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '힙 범프 스윕',
    '골반을 튕겨 일어나며 상대의 상체를 감싸 안아 넘기는 스윕',
    'SWEEP',
    'BEGINNER',
    'CLOSED_GUARD',
    '클로즈드 가드에서 상대가 체중을 뒤로 빼고 허리를 꼿꼿이 세웠을 때 사용',
    '힙 범프 스윕은 상대가 가드 안에서 상체를 세우며 도망가려 할 때, 빠르게 상체를 일으켜 상대의 팔을 묶고 골반의 힘(Bump)으로 상대를 뒤집어 탑 마운트를 차지하는 기술이다.',
    '상대가 크로스 칼라 초크나 상체 컨트롤을 피하기 위해 엉덩이를 뒤로 빼고 상체를 완전히 뒤로 젖혔을 때 아주 유용하다.',
    '클로즈드 가드를 풀고 바닥을 손으로 짚은 뒤, 엉덩이를 살짝 빼 상체를 일으킬 수 있는 공간을 만든 상태.',
    '클로즈드 가드를 열고 한 손으로 매트를 짚어 상체를 빠르게 일으킨다. 반대쪽 팔로 상대의 목이나 어깨를 깊숙이 감싸 안고 팔꿈치를 통제한다. 매트를 짚은 손과 발을 이용해 엉덩이를 높게 쳐올리며 골반으로 상대를 밀어 대각선 방향으로 넘긴다.',
    '상대를 넘길 때 단순히 팔로 당기는 것이 아니라, 내 골반이 상대의 가슴이나 어깨 쪽으로 폭발적으로 부딪히는(Bump) 힘이 중요하다.',
    '골반을 충분히 들어올리지 않고 회전만 하려다 상대 체중에 깔리는 실수가 잦다. 상대의 팔을 단단히 묶지 않아 베이스를 잡는 경우도 있다.',
    '일어나는 타이밍이 늦으면 상대가 밀고 들어와 오히려 가드가 패스당할 위험이 있으니 과감하고 빠르게 실행해야 한다.',
    'https://www.youtube.com/watch?v=hipbump',
    TRUE,
    6,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('hip-bump-sweep', currval('training_cards_id_seq'));

-- 플라워 스윕 / flower-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '플라워 스윕',
    '상대의 바지 깃을 잡고 진자 운동처럼 다리를 휘둘러 넘기는 스윕',
    'SWEEP',
    'INTERMEDIATE',
    'CLOSED_GUARD',
    '클로즈드 가드에서 상대가 한쪽 무릎을 세워 압박을 시작할 때 사용',
    '플라워 스윕(펜듈럼 스윕과 유사)은 상대의 바지나 다리를 통제한 상태에서 내 다리를 꽃잎이 떨어지거나 시계추가 움직이는 것처럼 크게 원을 그리며 회전력을 만들어 상대를 뒤집는 우아하고 효율적인 스윕이다.',
    '상대가 가드 패스를 위해 내 골반을 누르며 한쪽 무릎을 꿇고 한쪽 다리를 세웠을 때, 상대의 체중이 앞이나 옆으로 살짝 쏠리는 타이밍을 노린다.',
    '한 손으로 상대의 소매를 깊게 잡고, 다른 한 손으로는 상대의 세워진 다리 쪽 바지 밑단을 컨트롤하고 있는 상태.',
    '소매를 잡은 쪽으로 상대의 체중을 살짝 당긴다. 내 다리를 크게 휘둘러 상대의 겨드랑이 밑을 차올리며 회전력을 만든다. 동시에 바지를 잡은 손을 위로 들어 올려 상대의 다리 베이스를 완전히 없애며 상위 포지션으로 굴러 올라간다.',
    '내 양다리의 회전 반경과 속도가 가장 중요하다. 진자 운동을 하듯 골반과 다리를 크게 흔들어 그 관성으로 상대를 넘겨야 한다.',
    '바지 깃을 잡은 손으로 다리를 충분히 들어올리지 않아 상대가 무릎을 꿇으며 방어하는 경우가 많다.',
    '타이밍을 놓치면 다리가 얽힌 상태에서 상대에게 패스 압박을 강하게 받을 수 있으므로, 다리를 휘두를 때 망설이지 않아야 한다.',
    NULL,
    TRUE,
    7,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('flower-sweep', currval('training_cards_id_seq'));

-- 더블 레그 테이크다운 / double-leg-takedown
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '더블 레그 테이크다운',
    '자세를 낮춰 상대의 양다리를 감싸 안고 들어서 넘어뜨리는 테이크다운',
    'TAKEDOWN',
    'BEGINNER',
    'STANDING',
    '스탠딩 상황에서 상대의 중심이 높거나 다리가 벌어져 있을 때 사용',
    '더블 레그 테이크다운은 태클의 가장 기본이 되는 기술로, 레슬링과 주짓수 모두에서 널리 쓰인다. 폭발적인 스피드로 자세를 낮춰(레벨 체인지) 상대의 품으로 파고들어 양다리를 싸잡고 넘어뜨리는 것이 목적이다.',
    '서로 그립 싸움을 하다가 상대가 양팔을 올리거나 자세가 높아져 하체에 빈 공간이 노출되었을 때 기습적으로 파고든다.',
    '상대와 적절한 거리를 유지한 스탠딩 상태. 양손으로 가드를 올리고 무릎이 살짝 굽혀진 애슬레틱 스탠스.',
    '순간적으로 무릎을 굽혀 상대의 골반 아래로 내 머리 위치를 낮춘다(레벨 체인지). 앞발을 상대 다리 사이로 깊게 내딛으며 양손으로 상대의 오금을 감싸 안는다. 뒷발을 따라오게 하며 머리로 상대의 옆구리를 밀고 다리를 들어 올리며 옆이나 대각선으로 상대를 넘어뜨린다.',
    '고개를 절대 숙이지 말고 가슴을 편 상태로 들어가야 한다. 상대를 잡았을 때 내 척추가 곧게 펴져 있어야 강한 힘을 낼 수 있다.',
    '레벨 체인지 없이 허리만 숙여서 들어가다가 상대에게 길로틴 초크를 내어주거나 스프롤 방어에 깔리는 실수가 흔하다.',
    '주짓수에서는 목이 노출되면 길로틴 초크의 위험이 매우 크므로, 테이크다운 성공 직후 목 방어와 가드 패스 연결에 신경 써야 한다.',
    'https://www.youtube.com/watch?v=doubleleg',
    TRUE,
    8,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('double-leg-takedown', currval('training_cards_id_seq'));

-- 싱글 레그 테이크다운 / single-leg-takedown
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '싱글 레그 테이크다운',
    '상대의 한쪽 다리를 잡고 내 몸에 밀착시켜 균형을 깨고 넘어뜨리는 테이크다운',
    'TAKEDOWN',
    'INTERMEDIATE',
    'STANDING',
    '더블 레그 타이밍이 안 나오거나 상대의 한쪽 다리가 내 쪽으로 나와 있을 때 사용',
    '싱글 레그 테이크다운은 상대의 앞다리를 공략하여 양손으로 안아 올린 뒤, 다양한 방향으로 중심을 흔들어 넘어뜨리는 기술이다. 더블 레그보다 리스크가 적고 주짓수의 가드 풀 상황에서도 스윕 형태로 자주 활용된다.',
    '상대가 한쪽 발을 과도하게 앞으로 내밀고 있거나, 내가 앉은 자세에서 일어나는 스크램블 상황에서 상대 다리를 잡을 수 있을 때 유용하다.',
    '상대와 앞다리가 가까운 스탠딩 상태이거나 그립 싸움 중 상대가 체중을 앞발에 실은 상황.',
    '레벨 체인지 후 바깥쪽 발을 상대의 앞발 안쪽이나 바깥쪽으로 스텝하며 파고든다. 양손으로 상대의 종아리와 허벅지를 감싸 안아 다리를 가슴 높이까지 들어 올린다. 상대의 다리를 두 다리 사이에 끼우거나 골반에 붙이고 원을 그리며 회전하거나 반대쪽 다리를 걸어 넘어뜨린다.',
    '잡은 다리를 내 가슴과 배에 완벽하게 밀착시켜야 상대가 다리를 빼기 어렵다. 머리는 상대의 가슴팍 쪽에 밀착해 상체를 세운다.',
    '다리를 잡고 무겁게 늘어지는 상대를 힘으로만 들려고 하다가 스태미나를 잃고 깔리는 경우가 많다.',
    '머리가 바깥쪽으로 빠지면 기무라 락이나 크루시픽스 등 치명적인 서브미션 및 포지션을 뺏길 수 있으므로 머리 위치를 상대 가슴 쪽에 두어야 한다.',
    NULL,
    TRUE,
    9,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('single-leg-takedown', currval('training_cards_id_seq'));

-- 오소토 가리 (큰바깥후리기) / osoto-gari
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '오소토 가리 (큰바깥후리기)',
    '상대의 상체를 꺾어 중심을 무너뜨리고 바깥쪽 다리를 강하게 후려 넘어뜨리는 기술',
    'TAKEDOWN',
    'INTERMEDIATE',
    'STANDING',
    '도복 그립을 강하게 잡고 상대를 내 몸 측면으로 끌어당겼을 때 사용',
    '오소토 가리는 유도 베이스의 대표적인 메치기 기술로, 주짓수 도복 스탠딩 공방에서 매우 강력하게 쓰인다. 상대의 체중을 한쪽 다리에 싣게 만든 뒤, 내 다리로 그 지지 다리를 강하게 후려쳐서 뒤로 넘어뜨린다.',
    '상대와 서로 목 깃과 소매를 잡고 힘겨루기를 하다가 상대가 뒤로 물러나지 않고 뻣뻣하게 서서 버티는 타이밍에 시도한다.',
    '한 손은 목 깃(칼라), 한 손은 소매를 단단히 잡고 있으며 상대방과 반측면으로 서 있는 상태.',
    '상대의 목 깃과 소매를 내 쪽으로 강하게 당기며 체중을 상대의 한쪽 다리에 100% 쏠리게 한다. 내 지지발을 상대 발 옆으로 깊게 스텝인한다. 반대쪽 다리를 들어 상대 다리 바깥쪽 허벅지 뒤를 강하게 후려치면서 상체 그립을 매트 방향으로 꺾어 내린다.',
    '다리를 거는 것보다 중요한 것은 상체의 기울기(쿠즈시)다. 상대의 체중이 후려칠 다리에 완벽히 실려 있어야 기술이 통한다.',
    '상대의 중심을 꺾지 않고 다리만 걸려고 하면 오히려 카운터를 맞아 내가 뒤로 넘어갈 수 있다.',
    '기술 실패 시 내 등이 상대에게 노출될 위험이 있으므로, 그립을 놓치지 말고 실패 시 즉각 가드로 당겨 내려가는 판단도 필요하다.',
    NULL,
    TRUE,
    10,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('osoto-gari', currval('training_cards_id_seq'));

-- 가드 풀 / guard-pull
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '가드 풀',
    '스탠딩 상황에서 테이크다운을 하는 대신 먼저 바닥으로 내려가 가드 포지션을 잡는 기술',
    'TAKEDOWN',
    'BEGINNER',
    'STANDING',
    '테이크다운 싸움을 피하고 빠르게 자신의 가드 게임으로 진입하고 싶을 때 사용',
    '가드 풀은 주짓수 경기에서 하위 포지션을 선호하는 선수가 테이크다운 점수를 잃지 않고 안전하게 그라운드로 경기를 끌고 가는 가장 보편적인 전략이다. 상대의 옷깃을 잡고 한쪽 발을 상대 골반에 대며 뒤로 눕는다.',
    '스탠딩에서 상대가 테이크다운 압박이 강하거나 내가 스파이더, 델라히바 등 특정 오픈 가드를 빠르게 세팅하고 싶을 때 사용한다.',
    '스탠딩에서 최소 한 곳 이상의 그립(보통 칼라와 소매)을 확실히 확보한 상태.',
    '상대의 칼라와 소매 그립을 잡는다. 소매를 잡은 쪽의 발을 들어 상대의 골반이나 허벅지에 얹는다. 그립을 내 쪽으로 당기면서 체중을 실어 뒤로 눕고, 골반에 댄 발로 거리를 조절하며 반대쪽 다리를 말아 올려 클로즈드 가드나 원하는 오픈 가드를 세팅한다.',
    '반드시 상대의 도복(그립)을 단단히 잡은 상태에서 누워야 한다. 잡지 않고 누우면 페널티(루지 포인터)를 받을 수 있다.',
    '발을 골반에 정확히 대지 못하고 미끄러지면서 누우면 상대에게 그대로 가드 패스(패스 가드)를 헌납하게 된다.',
    '가드 풀 타이밍에 상대가 발목을 잡거나 옆으로 돌며 패스를 시도할 수 있으니 누울 때 다리 프레임을 견고하게 유지해야 한다.',
    'https://www.youtube.com/watch?v=guardpull',
    TRUE,
    11,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('guard-pull', currval('training_cards_id_seq'));

-- 엑스 패스 / x-pass
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '엑스 패스',
    '상대의 다리를 차내며 몸을 X자 모양으로 뻗어 빠르게 지나가는 패스 기술',
    'PASS',
    'INTERMEDIATE',
    'OPEN_GUARD',
    '서서 상대를 압박하다가 상대가 한쪽 다리로 방어 프레임을 만들었을 때 사용',
    'X 패스는 스탠딩 상태에서 순간적인 스피드와 체중 이동을 이용해 상대의 가드를 돌파하는 다이내믹한 패스다. 양팔과 양다리가 X자 모양으로 벌어지는 형태를 띠며, 도복 바지와 목 깃 그립이 중요하다.',
    '오픈 가드에 있는 상대에게 다가가 한쪽 다리를 통제했고, 상대가 다른 다리로 내 골반을 밀어 거리를 두려 할 때 그 힘을 역이용해 패스한다.',
    '나는 서 있고 상대는 누워있는 상태에서, 내 한 손은 상대 바지 무릎 쪽, 다른 손은 목 깃을 잡고 있는 상태.',
    '상대 다리를 밀어내며 내 골반을 차올린다는 느낌으로 체중을 실어 상대의 다리 방향 반대로 강하게 스텝 아웃한다. 잡고 있던 바지를 아래로 강하게 밀어내고(Kick), 동시에 상체를 상대의 빈 공간으로 돌진시켜 니온벨리(Knee on Belly)나 사이드 컨트롤로 안착한다.',
    '발을 빼는 동작과 손으로 상대 다리를 미는 동작, 상체가 들어가는 세 가지 동작이 폭발적으로 동시에 이루어져야 한다.',
    '상체를 낮추지 않고 다리만 옆으로 이동하다가 상대에게 다리가 잡혀 역공당하는 경우가 많다.',
    '스피드가 빠른 만큼 밸런스를 잃기 쉽다. 패스 후 정확하게 상대 상체를 눌러주지 못하면 스크램블 상황이 발생한다.',
    NULL,
    TRUE,
    12,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('x-pass', currval('training_cards_id_seq'));

-- 스매시 패스 / smash-pass
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '스매시 패스',
    '상대의 양다리를 한쪽 방향으로 접어 포개서 하체를 무력화하고 통과하는 패스',
    'PASS',
    'INTERMEDIATE',
    'HALF_GUARD',
    '상대의 무릎 프레임이 세워져 있을 때 그 다리를 바닥으로 짓눌러 접을 때 사용',
    '스매시 패스(또는 폴딩 패스)는 상대의 두 다리를 한쪽으로 겹쳐서 포개놓고 내 체중으로 짓눌러 하체 움직임을 완전히 봉쇄한 뒤 패스하는 압박형 기술이다. 니 컷 패스와 반대 방향의 압박을 가할 때 자주 연계된다.',
    '하프 가드나 오픈 가드에서 상대가 니쉴드를 세우며 강하게 저항할 때, 그 무릎을 억지로 뚫는 대신 반대 방향으로 눕혀버린다.',
    '탑에서 상대 다리 사이에 위치하며 상대방이 무릎을 세워 프레임을 만들고 있는 상태.',
    '상대의 세워진 무릎의 바깥쪽을 손이나 내 가슴으로 눌러 상대의 반대쪽 다리 위로 포개어 접는다(Smash). 내 체중을 실어 접힌 다리가 펴지지 않게 가슴과 배로 강하게 압박한다. 상대의 상체를 끌어안거나 언더훅을 파고, 골반을 넘어 마운트나 사이드로 이동한다.',
    '다리를 접어놓은 후에는 상대의 엉덩이가 하늘을 보지 못하도록 내 골반 위치를 낮추고 체중을 무겁게 실어야 한다.',
    '다리를 접었으나 상체 컨트롤(목 끌어안기 등)을 하지 않아 상대가 허리를 펴며 다리를 빼내는 경우가 잦다.',
    '상대가 강하게 다리를 뻗으며 밀어낼 때 무리하게 짓누르면 내 베이스가 뒤집힐 수 있으므로 압박 각도를 잘 맞춰야 한다.',
    NULL,
    TRUE,
    13,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('smash-pass', currval('training_cards_id_seq'));

-- 스파이더 가드 스윕 / spider-guard-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '스파이더 가드 스윕',
    '양쪽 소매를 잡고 발바닥으로 상대의 이두근을 밟아 띄우고 넘기는 스윕',
    'SWEEP',
    'INTERMEDIATE',
    'OPEN_GUARD',
    '오픈 가드에서 상대의 양팔을 완벽히 통제했을 때 사용',
    '스파이더 가드는 상대의 양쪽 소매를 잡고 발바닥을 이두근(팔 굽히는 부위)에 대어 상대의 움직임을 조종하는 포지션이다. 이 상태에서 다리를 쭉 펴서 상대의 밸런스를 띄우고 가위차기 형태 등으로 넘기는 스윕이 핵심이다.',
    '가드 풀 직후나 클로즈드 가드가 열렸을 때 상대가 패스하려고 일어서려 하거나 거리를 좁힐 때 양팔을 묶어두고 시도한다.',
    '등을 대고 누운 상태에서 양손은 상대의 양 소매 그립을 단단히 잡고, 한쪽 또는 양쪽 발바닥이 상대의 팔 안쪽(이두근)을 밟고 있는 상태.',
    '한쪽 발은 이두근을 강하게 밟아 상대의 팔을 펴게 만들고, 반대쪽 발은 상대의 무릎이나 골반을 밟는다. 이두근을 밟은 쪽의 다리를 크게 휘두르며 상대를 내 몸 위로 당겨 띄우고, 골반을 밟았던 다리로 상대 다리를 쳐내며 옆으로 넘긴다.',
    '상대의 팔이 구부러지지 않도록 발바닥으로 이두근을 강하게 밀어 텐션을 유지해야 한다. 그립의 힘과 하체의 텐션 조화가 중요하다.',
    '다리를 충분히 뻗지 않아 텐션이 헐거워지면 상대가 팔을 돌려 스파이더 그립을 쉽게 뜯어낸다.',
    '악력이 많이 소모되는 기술이므로 손가락 부상에 주의해야 하며, 상대가 좌우로 빠르게 패스스텝을 밟을 때 골반이 잘 따라가야 한다.',
    NULL,
    TRUE,
    14,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('spider-guard-sweep', currval('training_cards_id_seq'));

-- 라소 가드 스윕 / lasso-guard-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '라소 가드 스윕',
    '상대 팔에 내 다리를 채찍처럼 휘감아 완전히 묶고 옆으로 굴려 넘기는 스윕',
    'SWEEP',
    'ADVANCED',
    'OPEN_GUARD',
    '스파이더 가드 공방 중 상대가 내 다리를 밀어내려 할 때 팔을 휘감아 방어/공격할 때 사용',
    '라소 가드는 스파이더 가드의 변형으로, 상대의 소매를 잡은 상태에서 내 다리를 상대 팔 바깥쪽에서 안쪽으로 감아 넣어(올가미처럼) 팔을 등 뒤로 꺾이게 묶어버리는 강력한 통제 기술이며, 묶인 쪽으로 구르며 스윕을 만들어낸다.',
    '스파이더 그립을 잡고 있는데 상대가 내 다리를 치우려 하거나 팔을 빼려 할 때 다리를 깊게 감아 방어와 공격을 동시에 세팅한다.',
    '한쪽 다리가 상대의 팔을 감아 등 뒤쪽이나 겨드랑이에 깊숙이 끼워져 있고, 소매를 단단히 잡은 라소 포지션.',
    '라소를 건 쪽의 손을 단단히 당겨 상대 팔을 내 배에 붙인다. 남은 발로 상대의 반대쪽 무릎이나 골반을 막아 베이스를 무너뜨린다. 상대의 중심이 라소가 감긴 쪽으로 기울 때, 내 몸을 그 방향으로 굴리며 상대방을 어깨 너머로 뒤집어 넘긴다.',
    '라소 발등이 상대의 겨드랑이나 등 깊숙한 곳에 강하게 밀착되어야 상대가 팔을 뺄 수 없다.',
    '라소 훅이 얕으면 상대가 팔을 돌려서 쉽게 빼버리며, 넘길 때 골반을 들어올리지 않으면 무거운 상대를 뒤집지 못한다.',
    '라소를 건 상태에서 상대가 반대 방향으로 과도하게 뛰어서 패스(점핑 패스)를 시도할 수 있으므로 반대쪽 손과 발의 견제가 필수적이다.',
    'https://www.youtube.com/watch?v=lasso123',
    TRUE,
    15,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('lasso-guard-sweep', currval('training_cards_id_seq'));

-- 델라히바 스윕 / de-la-riva-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '델라히바 스윕',
    '바깥쪽에서 상대 다리를 감아 중심을 뒤로 무너뜨리며 일어나는 스윕',
    'SWEEP',
    'INTERMEDIATE',
    'OPEN_GUARD',
    '상대가 서서 오픈 가드를 패스하려 할 때 앞다리를 감아 통제했을 때 사용',
    '델라히바 가드는 브라질리언 주짓수의 현대 오픈 가드 시스템의 핵심으로, 상대의 앞다리를 내 바깥쪽 다리로 감아 허벅지 안쪽에 훅을 거는 형태다. 이 포지션에서 상대의 중심을 엉덩방아 찧게 만들며 일어나는 스윕이다.',
    '상대가 스탠딩 자세에서 내 가드 안으로 걸어 들어오거나, 내가 가드 풀을 한 직후 상대의 한쪽 다리가 내 가까이 있을 때 세팅한다.',
    '바닥에 누운 상태에서 내 바깥쪽 다리가 상대의 앞다리를 감아 허벅지 안쪽에 걸려있고, 손으로는 상대 발목과 소매를 컨트롤하는 상태.',
    '상대의 발목을 잡은 손을 강하게 당기고, 델라히바 훅을 건 다리와 반대쪽 다리(골반이나 무릎을 미는 다리)를 이용해 상대방을 내 쪽으로 살짝 당겼다가 뒤로 강하게 밀어낸다. 상대가 중심을 잃고 엉덩방아를 찧으면 발목을 잡은 채로 내 상체를 세워 탑으로 올라간다.',
    '델라히바 훅은 발등만 거는 것이 아니라 무릎과 허벅지를 조여 상대 무릎을 비틀어주는 텐션이 있어야 한다.',
    '훅만 걸어놓고 상대 발목을 놓치거나, 상대를 밀어내고 나서 일어나는 속도가 느려 상대가 다시 일어서는 경우가 많다.',
    '상대가 내 델라히바 훅을 뜯어내고 강하게 롱스텝이나 니컷으로 밀고 들어올 수 있으므로, 항상 소매 그립과 반대쪽 발의 프레임 유지가 중요하다.',
    NULL,
    TRUE,
    16,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('de-la-riva-sweep', currval('training_cards_id_seq'));

-- 싱글 레그 엑스 스윕 / single-leg-x-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '싱글 레그 엑스 스윕',
    '상대의 한쪽 다리를 내 양다리로 감싸 하체를 통제한 뒤 밀어 넘어뜨리는 스윕',
    'SWEEP',
    'INTERMEDIATE',
    'LEG_ENTANGLEMENT',
    '버터플라이 가드나 시팅 가드에서 상대가 일어섰을 때 상대 다리 밑으로 파고들어 사용',
    '싱글 레그 X 가드는 상대의 한쪽 다리를 내 다리로 엮어 골반을 통제하는 매우 강력한 하체 관절기 및 스윕 포지션이다. 상대의 무게 중심을 내 배 위에 띄워 손쉽게 앞뒤로 넘어뜨릴 수 있다.',
    '상대가 서 있거나 한쪽 무릎을 세웠을 때, 상대의 다리 밑으로 미끄러져 들어가 발목을 겨드랑이에 끼우고 다리를 세팅한다.',
    '내 몸이 상대의 다리 밑에 위치하고, 한쪽 다리는 상대 골반 바깥쪽, 다른 다리는 상대 허벅지 안쪽을 훅으로 걸어 상대 다리를 완벽히 조인 상태.',
    '상대의 발목을 내 겨드랑이에 깊숙이 끼워 단단히 잡는다. 내 두 다리를 벌리며 골반을 높이 치켜올려 상대의 베이스를 띄운다. 상대의 중심이 불안정해진 순간, 바깥쪽 다리로 상대 골반을 뒤로 강하게 밀어내어 상대를 엉덩방아 찧게 만들고 그대로 일어난다.',
    '골반을 들어 올리는(브릿지) 동작이 필수적이다. 골반을 들어 상대 다리를 내 배 위에 띄워야 다리가 고정되어 강력한 미는 힘이 생긴다.',
    '골반을 땅에 댄 채 다리 힘으로만 상대방을 밀어내려다 상대에게 베이스를 회복할 시간을 준다.',
    'IBJJF 룰에서는 바깥쪽 발이 상대의 반대쪽 골반 선을 넘어가면 니리핑(Knee Reaping) 반칙이 되므로 발목 위치에 극도로 주의해야 한다.',
    'https://www.youtube.com/watch?v=slx123',
    TRUE,
    17,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('single-leg-x-sweep', currval('training_cards_id_seq'));

-- 엑스 가드 스윕 / x-guard-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '엑스 가드 스윕',
    '상대 다리 밑에서 양다리를 X자로 교차해 상대의 베이스를 크게 흔들어 넘기는 스윕',
    'SWEEP',
    'ADVANCED',
    'LEG_ENTANGLEMENT',
    '싱글 레그 X 가드에서 상대가 방어하며 몸을 돌리려 할 때 X 가드로 전환하여 사용',
    'X 가드는 싱글 레그 X보다 상대 몸체 아래로 더 깊게 파고들어가 상대의 양다리를 모두 통제하는 가드 포지션이다. 내 다리가 상대의 양다리를 밀어주기 때문에 체급 차이를 극복하고 상대를 완전히 띄워서 넘기기 가장 좋은 스윕 중 하나다.',
    '상대가 스탠딩 상태로 나를 압박할 때, 타이밍을 맞춰 상대의 다리 가랑이 사이 깊숙이 미끄러져 들어가 중심을 빼앗는다.',
    '내 몸이 상대의 완전한 바로 아래에 있으며, 한 손으로는 상대 한쪽 다리를 어깨에 메고 다른 다리는 내 양다리로 X자로 얽어 통제하는 상태.',
    '어깨에 상대 다리를 단단히 메어 고정한다. 내 발목을 상대 무릎 위와 골반 쪽에 X자로 걸어 텐션을 만든다. 두 다리를 대각선 위로 쭉 뻗으며 상대의 몸 전체를 공중으로 띄운 뒤, 손으로 상대의 지지하는 발목을 잡거나 차내며 베이스를 뺏고 일어나 상위 포지션을 점유한다.',
    'X 가드 세팅 시 내 머리가 상대의 다리 바깥쪽이 아니라 안쪽이나 다리 밑 정중앙에 있어야 상대의 무게 중심을 정확히 띄울 수 있다.',
    '상대를 다리 위로 띄우지 않고 눕힌 채로 넘기려다가 상대가 내 다리를 밟고 빠져나오는 상황을 허용한다.',
    '상대가 중심을 낮춰 내 상체를 강하게 눌러 올 수 있으니 항상 다리를 펴는 텐션을 유지해 거리를 벌려야 한다.',
    NULL,
    TRUE,
    18,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('x-guard-sweep', currval('training_cards_id_seq'));

-- 우파 이스케이프 / upa-escape
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '우파 이스케이프',
    '마운트 깔린 상태에서 강한 브릿지를 통해 상대를 뒤집고 탈출하는 기술',
    'ESCAPE',
    'BEGINNER',
    'MOUNT',
    '상대에게 마운트를 빼앗겼을 때 상대가 목을 조르거나 짚기 위해 손을 뻗었을 때 사용',
    '우파(Upa) 또는 브릿지 이스케이프는 그라운드 생존의 가장 기초가 되는 기술이다. 깔린 상태에서 상대의 한쪽 팔과 발을 봉쇄한 뒤, 내 골반의 폭발적인 들어올림을 이용해 상대를 봉쇄된 방향으로 굴려버린다.',
    '상대가 마운트를 타고 내 목을 초크하려 들어오거나 베이스를 유지하기 위해 매트를 짚었을 때, 그 뻗은 팔을 가두고 시도한다.',
    '완전히 마운트에 깔린 상태에서 상대의 공격하는 손목이나 팔꿈치를 내 양손으로 감싸 잡은 상황.',
    '상대의 한 팔을 강하게 잡고 내 몸에 밀착시킨다. 동시에 같은 쪽 상대의 발목 바깥쪽에 내 발을 걸어 지지대를 없앤다. 양발을 엉덩이 쪽으로 당겨 바닥을 강하게 딛고 브릿지를 치며 대각선 어깨 방향으로 굴러 상대를 뒤집고 클로즈드 가드 탑으로 올라간다.',
    '잡은 팔 쪽으로 굴릴 때 정면으로 브릿지를 치는 것이 아니라 대각선 어깨 너머를 쳐다보며 몸을 비틀어 브릿지를 해야 완벽하게 넘어간다.',
    '상대의 발목을 걸어 베이스를 없애지 않으면 아무리 브릿지를 강하게 쳐도 상대가 발로 버티며 넘어가지 않는다.',
    '브릿지 실패 시 체력 소모가 크고 상대가 하이 마운트로 올라올 수 있으니, 팔과 다리를 확실히 가둔 상태에서 폭발적으로 실행해야 한다.',
    'https://www.youtube.com/watch?v=upa123',
    TRUE,
    19,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('upa-escape', currval('training_cards_id_seq'));

-- 엘보우 이스케이프 / elbow-escape
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '엘보우 이스케이프',
    '마운트에서 팔꿈치로 방어하며 골반을 빼내 한 다리씩 가드로 회복하는 기술',
    'ESCAPE',
    'BEGINNER',
    'MOUNT',
    '우파 이스케이프가 막혔거나 상대의 무게 중심이 낮아 뒤집기 어려울 때 사용',
    '엘보우 이스케이프(또는 슈림프 이스케이프)는 깔린 상태에서 새우빼기 움직임을 이용해 내 다리를 상대 다리 사이로 빼내어 가드를 회복(리커버리)하는 가장 중요하고 필수적인 방어 기술이다.',
    '마운트에 깔렸을 때 상대가 엉덩이를 낮추고 넓게 퍼져 베이스를 단단히 잡고 있어 브릿지로 넘기기 불가능할 때 필수적으로 쓰인다.',
    '마운트 하위에서 양손을 가슴 위에 모아 프레임을 만들고 상대의 압박을 버티고 있는 상황.',
    '한쪽 팔꿈치를 상대의 무릎 안쪽 골반에 대고 프레임을 만든다. 반대쪽 다리를 눕혀 바닥에 대고 엉덩이를 옆으로 강하게 뺀다(새우빼기). 공간이 생기면 상대의 다리 위로 내 무릎을 미끄러뜨리듯 집어넣어 하프 가드를 만든다. 반대쪽도 동일하게 진행하여 클로즈드 가드를 회복한다.',
    '손이나 팔 힘으로 상대를 밀어내려 하지 말고, 프레임을 유지한 채 내 엉덩이를 뒤로 빼내어(새우빼기) 공간을 만드는 것이 핵심이다.',
    '엉덩이를 빼지 않고 팔로만 상대 다리를 누르려다가 오히려 암바나 트라이앵글 초크의 표적이 되는 경우가 흔하다.',
    '몸을 옆으로 돌릴 때 상대방에게 등을 보일 정도로 과하게 돌리면 백 마운트를 뺏길 수 있으니 각도 조절에 주의해야 한다.',
    NULL,
    TRUE,
    20,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('elbow-escape', currval('training_cards_id_seq'));


-- =========================================================
-- 2. 연관 기술 매핑 데이터
-- 모든 카드 본문 INSERT 아래에 추가
-- =========================================================

-- 예시
-- INSERT INTO training_card_relations (
--     card_id,
--     related_card_id,
--     display_order,
--     created_at,
--     updated_at
-- )
-- SELECT source.card_id, related.card_id, 0, NOW(), NOW()
-- FROM tmp_training_card_seed_map source
-- JOIN tmp_training_card_seed_map related
--   ON related.seed_key = 'toreando-pass'
-- WHERE source.seed_key = 'knee-cut-pass'
-- ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- 실제 연관 기술 추가 구간

-- 2026-06-10 1차 적재 데이터
-- closed-guard-armbar -> triangle-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'triangle-choke'
WHERE source.seed_key = 'closed-guard-armbar'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- closed-guard-armbar -> cross-collar-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'cross-collar-choke'
WHERE source.seed_key = 'closed-guard-armbar'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- triangle-choke -> closed-guard-armbar
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'closed-guard-armbar'
WHERE source.seed_key = 'triangle-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- triangle-choke -> kimura-from-closed-guard
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'kimura-from-closed-guard'
WHERE source.seed_key = 'triangle-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- kimura-from-closed-guard -> hip-bump-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'hip-bump-sweep'
WHERE source.seed_key = 'kimura-from-closed-guard'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- kimura-from-closed-guard -> triangle-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'triangle-choke'
WHERE source.seed_key = 'kimura-from-closed-guard'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- cross-collar-choke -> closed-guard-armbar
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'closed-guard-armbar'
WHERE source.seed_key = 'cross-collar-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- cross-collar-choke -> scissor-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'scissor-sweep'
WHERE source.seed_key = 'cross-collar-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- scissor-sweep -> cross-collar-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'cross-collar-choke'
WHERE source.seed_key = 'scissor-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- scissor-sweep -> triangle-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'triangle-choke'
WHERE source.seed_key = 'scissor-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- hip-bump-sweep -> kimura-from-closed-guard
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'kimura-from-closed-guard'
WHERE source.seed_key = 'hip-bump-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- hip-bump-sweep -> flower-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'flower-sweep'
WHERE source.seed_key = 'hip-bump-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- flower-sweep -> closed-guard-armbar
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'closed-guard-armbar'
WHERE source.seed_key = 'flower-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- flower-sweep -> hip-bump-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'hip-bump-sweep'
WHERE source.seed_key = 'flower-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- double-leg-takedown -> single-leg-takedown
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-takedown'
WHERE source.seed_key = 'double-leg-takedown'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- double-leg-takedown -> osoto-gari
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'osoto-gari'
WHERE source.seed_key = 'double-leg-takedown'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- single-leg-takedown -> double-leg-takedown
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'double-leg-takedown'
WHERE source.seed_key = 'single-leg-takedown'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- single-leg-takedown -> guard-pull
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'guard-pull'
WHERE source.seed_key = 'single-leg-takedown'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- osoto-gari -> double-leg-takedown
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'double-leg-takedown'
WHERE source.seed_key = 'osoto-gari'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- osoto-gari -> single-leg-takedown
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-takedown'
WHERE source.seed_key = 'osoto-gari'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- guard-pull -> spider-guard-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'spider-guard-sweep'
WHERE source.seed_key = 'guard-pull'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- guard-pull -> de-la-riva-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'de-la-riva-sweep'
WHERE source.seed_key = 'guard-pull'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- x-pass -> smash-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'smash-pass'
WHERE source.seed_key = 'x-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- smash-pass -> x-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'x-pass'
WHERE source.seed_key = 'smash-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- spider-guard-sweep -> lasso-guard-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'lasso-guard-sweep'
WHERE source.seed_key = 'spider-guard-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- spider-guard-sweep -> guard-pull
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'guard-pull'
WHERE source.seed_key = 'spider-guard-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- lasso-guard-sweep -> spider-guard-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'spider-guard-sweep'
WHERE source.seed_key = 'lasso-guard-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- lasso-guard-sweep -> de-la-riva-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'de-la-riva-sweep'
WHERE source.seed_key = 'lasso-guard-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- de-la-riva-sweep -> spider-guard-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'spider-guard-sweep'
WHERE source.seed_key = 'de-la-riva-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- de-la-riva-sweep -> single-leg-x-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-x-sweep'
WHERE source.seed_key = 'de-la-riva-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- single-leg-x-sweep -> x-guard-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'x-guard-sweep'
WHERE source.seed_key = 'single-leg-x-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- single-leg-x-sweep -> de-la-riva-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'de-la-riva-sweep'
WHERE source.seed_key = 'single-leg-x-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- x-guard-sweep -> single-leg-x-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-x-sweep'
WHERE source.seed_key = 'x-guard-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- upa-escape -> elbow-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'elbow-escape'
WHERE source.seed_key = 'upa-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- elbow-escape -> upa-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'upa-escape'
WHERE source.seed_key = 'elbow-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- 2026-06-10 2차 적재 데이터
-- 토레안도 패스 / toreando-pass
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '토레안도 패스',
    '투우사가 소를 피하듯 상대의 양다리를 치우고 옆으로 돌아가는 패스',
    'PASS',
    'BEGINNER',
    'OPEN_GUARD',
    '스탠딩에서 상대의 오픈 가드를 상대할 때 민첩하게 옆으로 이동할 때 사용',
    '토레안도 패스는 힘보다는 스텝과 상하체 타이밍을 이용해 상대의 다리를 통제하고 옆으로 돌아가 사이드 컨트롤을 점유하는 기술이다.',
    '상대가 누워 오픈 가드를 취하고 있을 때 상대의 무릎이나 바짓깃을 잡고 좌우로 스텝을 밟으며 빈틈을 찾는다.',
    '나는 스탠딩이고 상대는 누워있는 상태에서 상대의 바지나 무릎 부위를 양손으로 잡은 자세.',
    '양손으로 상대의 다리를 내 몸통 밖으로 강하게 밀어내거나 핀 다운시킨 직후, 다리가 없는 방향으로 빠르게 스텝을 밟아 어깨로 압박하며 들어간다.',
    '팔 힘으로만 다리를 미는 것이 아니라 체중을 실어 다리를 바닥에 고정시키고 내 몸이 빠르게 돌아야 한다.',
    '스텝을 밟지 않고 제자리에서 다리만 치우려다가 상대가 쉽게 가드를 회복한다.',
    '그립을 너무 꽉 쥐고 있으면 상대의 움직임에 끌려갈 수 있으므로 민첩하게 치고 빠지는 감각이 필요하다.',
    'https://www.youtube.com/watch?v=1uJU1egPrLs',
    TRUE,
    21,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('toreando-pass', currval('training_cards_id_seq'));

-- 오버 언더 패스 / over-under-pass
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '오버 언더 패스',
    '상대의 한 다리는 어깨 위로, 다른 다리는 아래로 통제하며 압박하는 패스',
    'PASS',
    'INTERMEDIATE',
    'OPEN_GUARD',
    '하프 가드나 오픈 가드에서 상대의 다리를 무겁게 압박하며 묶어두고 싶을 때 사용',
    '오버 언더 패스는 한 팔은 상대 다리 위로, 한 팔은 아래로 감싸 안고 어깨로 강한 압박을 주며 서서히 가드를 찌그러뜨리는 전형적인 압박형 가드 패스이다.',
    '가볍고 빠른 스텝 위주의 패스보다는 체중을 온전히 실어 상대를 지치게 만들고 안정적으로 패스할 때 유효하다.',
    '가드 앞에서 상대의 한쪽 다리 밑으로 손을 파넣고 반대쪽 다리는 위에서 안고 있는 상태.',
    '양팔로 다리를 묶고 어깨로 상대의 골반을 짓누른다. 골반을 높이 들어 압박감을 더한 뒤 엉덩이 쪽으로 걸어가며 상대의 방어 다리를 넘어 사이드로 떨어뜨린다.',
    '머리는 항상 통제하고 있는 다리의 반대쪽 골반 방향에 단단히 고정해야 트라이앵글 초크의 위험이 없다.',
    '압박을 주지 않고 너무 빨리 다리를 넘으려다가 상대의 유연성에 막혀 실패한다.',
    '상대가 내 허리띠를 잡고 들어 올려 롤링 스윕을 시도할 수 있으므로 무게 중심을 낮춰야 한다.',
    'https://www.youtube.com/results?search_query=over+under+pass+bjj',
    TRUE,
    22,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('over-under-pass', currval('training_cards_id_seq'));

-- 버터플라이 스윕 / butterfly-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '버터플라이 스윕',
    '버터플라이 가드에서 발등으로 상대를 띄워 넘기는 스윕',
    'SWEEP',
    'INTERMEDIATE',
    'OPEN_GUARD',
    '버터플라이 가드에서 상대의 한쪽 팔을 제압하고 상체를 밀착했을 때 사용',
    '버터플라이 스윕은 양 발등을 상대의 허벅지 안쪽에 훅처럼 걸고, 상체 컨트롤을 통해 상대를 공중으로 띄운 뒤 기울여 넘기는 역동적인 스윕이다.',
    '가드에 앉은 상태에서 상대가 다가올 때 언더훅과 오버훅을 파고 빈틈을 만들 때 사용한다.',
    '양 발등을 상대 허벅지 안쪽에 대고, 한쪽 팔은 언더훅, 다른 팔은 상대 팔을 제압한 버터플라이 가드 상태.',
    '뒤로 눕는 동시에 제압한 팔 쪽으로 상체를 틀며 언더훅을 판 쪽의 발등으로 상대를 강하게 띄워 넘긴다.',
    '뒤로 완전히 평평하게 눕는 것이 아니라 어깨로 떨어지며 몸을 둥글게 말아 각도를 살려야 한다.',
    '발로만 차올리려 하고 상체의 기울임이 동반되지 않아 상대가 베이스를 회복한다.',
    '상대가 내 다리를 누르고 패스를 시도할 수 있으므로 발등의 텐션을 항상 유지해야 한다.',
    'https://www.youtube.com/watch?v=vC8YqySFreQ',
    TRUE,
    23,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('butterfly-sweep', currval('training_cards_id_seq'));

-- 스파이더 가드 컨트롤 / spider-guard-control
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '스파이더 가드 컨트롤',
    '상대의 양 소매를 잡고 발바닥으로 팔뚝이나 이두근을 밀어 통제하는 가드',
    'OPEN_GUARD',
    'INTERMEDIATE',
    'OPEN_GUARD',
    '오픈 가드 상황에서 상대의 거리를 통제하고 공격을 세팅할 때 사용',
    '스파이더 가드는 상대의 양쪽 소매 그립과 내 양쪽 발바닥을 이용해 거미줄처럼 상대를 얽어매어 상대의 패스를 막고 스윕과 서브미션을 준비하는 핵심 모던 가드이다.',
    '상대가 서 있거나 무릎을 꿇고 다가올 때 팔의 움직임을 원천 봉쇄하고 거리를 넓게 유지할 때 유효하다.',
    '양손으로 상대의 소매를 강하게 틀어쥐고 양발은 상대의 이두근이나 골반을 밟고 있는 오픈 가드 상태.',
    '한쪽 발은 상대의 이두근을 길게 뻗어 압박하고, 다른 발은 골반이나 무릎을 밟아 좌우 밸런스를 계속해서 흔들며 텐션을 유지한다.',
    '팔을 당기는 힘과 다리를 뻗는 힘이 팽팽한 장력(텐션)을 이뤄야 상대가 벗어나지 못한다.',
    '다리를 쭉 펴기만 하고 골반을 움직이지 않아 상대가 쉽게 다리를 치우게 만든다.',
    '그립을 잡은 손가락에 무리가 갈 수 있으므로 그립을 무리하게 쥐기보다 몸 전체의 구조로 버텨야 한다.',
    'https://www.youtube.com/results?search_query=spider+guard+control+bjj',
    TRUE,
    24,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('spider-guard-control', currval('training_cards_id_seq'));

-- 오모플라타 / omoplata
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '오모플라타',
    '다리로 상대의 팔을 엮어 어깨 관절을 꺾는 서브미션',
    'SUBMISSION',
    'INTERMEDIATE',
    'OPEN_GUARD',
    '가드에서 상대의 한쪽 팔이 내 다리 사이에 깊게 위치했을 때 사용',
    '오모플라타는 하체의 강한 힘을 이용해 상대의 팔을 등 뒤로 꺾어 기무라와 유사한 압박을 주는 기술로, 서브미션뿐 아니라 스윕으로도 전환하기 좋은 기술이다.',
    '스파이더 가드나 클로즈드 가드에서 상대가 패스를 위해 팔을 깊숙이 집어넣었을 때 역이용하여 공격한다.',
    '상대의 한쪽 팔을 소매 그립 등으로 제압한 가드 상태.',
    '골반을 크게 틀며 제압한 팔 쪽으로 내 다리를 넘겨 상대의 어깨를 덮은 뒤, 몸을 일으켜 세워 상대의 어깨를 앞으로 꺾는다.',
    '다리를 넘긴 후 내 몸이 바닥에 누워있지 않고 빠르게 상체를 세워 상대가 앞구르기하지 못하게 압박해야 한다.',
    '상체를 세우지 않고 다리만 꼬고 누워 있다가 상대에게 탈출 공간을 준다.',
    '상대가 앞구르기로 탈출할 때 다리를 놓치면 오히려 불리한 위치에 깔릴 수 있다.',
    'https://www.youtube.com/results?search_query=omoplata+bjj',
    TRUE,
    25,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('omoplata', currval('training_cards_id_seq'));

-- 길로틴 초크 / guillotine-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '길로틴 초크',
    '겨드랑이 사이로 상대의 목을 감싸 안고 끌어올려 조르는 서브미션',
    'SUBMISSION',
    'BEGINNER',
    'STANDING',
    '상대가 테이크다운을 시도하거나 상체를 숙여 목을 노출했을 때 사용',
    '길로틴 초크는 단두대(Guillotine)라는 이름처럼 굽혀진 상대의 목을 한 팔로 감싸 안고 두 손을 맞잡아 압박하는 매우 빠르고 치명적인 초크이다.',
    '스탠딩에서 상대가 무리하게 하단 태클을 들어오거나, 가드 안에서 머리를 깊숙이 숙이고 있을 때 목을 낚아챈다.',
    '상대의 머리가 내 가슴 쪽으로 숙여져 있고 내 한쪽 팔이 상대의 목을 감쌀 수 있는 상태.',
    '팔을 깊게 파 목을 감싸고, 반대 손으로 그립을 잡은 뒤 가드로 끌어들이거나 골반을 당기며 팔꿈치를 옆구리에 붙여 목을 조른다.',
    '팔 힘으로 당기지 말고, 다리로 상대의 골반을 막아 상대의 몸통이 따라오지 못하게 고정한 후 상체를 뒤로 젖히는 구조가 중요하다.',
    '목이 깊이 파이지 않은 상태에서 억지로 조르려다 힘만 빠지고 패스를 허용한다.',
    '그립을 잡고 누울 때 상대방이 사이드로 넘어가면 기술이 풀리고 압박당할 위험이 있다.',
    'https://www.youtube.com/watch?v=7r3EtZi5HmA',
    TRUE,
    26,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('guillotine-choke', currval('training_cards_id_seq'));

-- 하프 가드 언더훅 스윕 / half-guard-underhook-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '하프 가드 언더훅 스윕',
    '하프 가드에서 겨드랑이를 파고 상대의 중심 밑으로 들어가 넘기는 스윕',
    'SWEEP',
    'BEGINNER',
    'HALF_GUARD',
    '하프 가드 하위에서 상대가 상체를 누르며 패스를 시도할 때 사용',
    '하프 가드 언더훅 스윕은 방어적인 하프 가드 상황에서 상대의 겨드랑이를 깊게 파(언더훅) 공격의 주도권을 되찾고 상대를 백이나 옆으로 넘기는 필수 기본기이다.',
    '상대가 크로스페이스(얼굴 압박)를 시도하기 전에 먼저 팔을 넣어 겨드랑이를 장악했을 때 시도한다.',
    '하프 가드 하위에서 무릎(니 실드)으로 방어하며 한쪽 팔은 상대의 겨드랑이를 깊게 판(언더훅) 상태.',
    '언더훅을 파고 상대의 체중 밑으로 몸을 집어넣은 뒤, 반대쪽 무릎을 잡거나 몸통을 들어 올려 상대를 옆으로 굴려 넘기거나 백으로 돌아간다.',
    '언더훅을 판 손이 무의미하게 등에 얹혀있는 것이 아니라 상대의 골반이나 띠를 향해 깊고 단단하게 들어가야 한다.',
    '언더훅을 팠음에도 등이 바닥에 완전히 닿아 있어 상대가 짓누르기 쉽게 만든다. 항상 옆으로 누워 있어야 한다.',
    '상대가 위저(오버훅)로 강하게 감아 누르거나 다스 초크를 시도할 수 있으므로 머리 위치를 상대 가슴에 밀착해야 한다.',
    'https://www.youtube.com/results?search_query=half+guard+underhook+sweep+bjj',
    TRUE,
    27,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('half-guard-underhook-sweep', currval('training_cards_id_seq'));

-- toreando-pass -> knee-cut-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'knee-cut-pass'
WHERE source.seed_key = 'toreando-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- toreando-pass -> spider-guard-control
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'spider-guard-control'
WHERE source.seed_key = 'toreando-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- over-under-pass -> knee-cut-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'knee-cut-pass'
WHERE source.seed_key = 'over-under-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- over-under-pass -> butterfly-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'butterfly-sweep'
WHERE source.seed_key = 'over-under-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- butterfly-sweep -> x-guard-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'x-guard-sweep'
WHERE source.seed_key = 'butterfly-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- butterfly-sweep -> single-leg-x-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-x-sweep'
WHERE source.seed_key = 'butterfly-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- spider-guard-control -> toreando-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'toreando-pass'
WHERE source.seed_key = 'spider-guard-control'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- spider-guard-control -> triangle-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'triangle-choke'
WHERE source.seed_key = 'spider-guard-control'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- omoplata -> triangle-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'triangle-choke'
WHERE source.seed_key = 'omoplata'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- omoplata -> spider-guard-control
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'spider-guard-control'
WHERE source.seed_key = 'omoplata'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- guillotine-choke -> d-arce-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'd-arce-choke'
WHERE source.seed_key = 'guillotine-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- guillotine-choke -> single-leg-takedown
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-takedown'
WHERE source.seed_key = 'guillotine-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- half-guard-underhook-sweep -> knee-cut-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'knee-cut-pass'
WHERE source.seed_key = 'half-guard-underhook-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- half-guard-underhook-sweep -> elbow-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'elbow-escape'
WHERE source.seed_key = 'half-guard-underhook-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- 2026-06-10 3차 적재 데이터
-- 리어 네이키드 초크 / rear-naked-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '리어 네이키드 초크',
    '상대의 등 뒤에서 양팔로 목을 감아 조르는 강력한 초크',
    'SUBMISSION',
    'BEGINNER',
    'BACK',
    '백 포지션을 점유하고 상대의 등 뒤에 완벽히 밀착했을 때 사용',
    '리어 네이키드 초크는 백 마운트에서 상대의 양쪽 경동맥을 강하게 압박하여 기절시키는 주짓수 최고의 피니시 기술 중 하나이다.',
    '가드 패스 후 등을 보이거나, 트랜지션 중 백을 잡았을 때 훅을 걸고 기회를 노린다.',
    '상대의 등 뒤에서 두 다리로 훅을 걸고 안전벨트 그립을 잡은 상태.',
    '목을 감싼 팔의 이두근을 반대쪽 손으로 잡고, 남은 손을 상대의 머리 뒤로 넘겨 압박하며 가슴을 펴서 조른다.',
    '조르는 팔이 목 깊숙이 들어가 턱 아래에 위치해야 하며, 등과 가슴을 밀착하여 공간을 없애야 한다.',
    '팔을 깊게 넣지 않고 턱 위에서 억지로 조르려다 힘만 빠진다.',
    '손가락이 눈이나 입에 들어가지 않도록 주의하며, 그립을 잡을 때 손 방어에 대비해야 한다.',
    'https://www.youtube.com/watch?v=1bM-1-nU4-M',
    TRUE,
    28,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('rear-naked-choke', currval('training_cards_id_seq'));

-- 보우 앤 애로우 초크 / bow-and-arrow-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '보우 앤 애로우 초크',
    '활을 당기듯 상대의 도복 깃과 바지를 잡아당겨 조르는 초크',
    'SUBMISSION',
    'INTERMEDIATE',
    'BACK',
    '백 포지션에서 상대가 방어하며 몸을 틀었을 때 사용',
    '보우 앤 애로우 초크는 도복 깃과 다리 바지를 잡고 체중을 뒤로 눕혀 지렛대의 원리를 극대화하는 매우 강력하고 확실한 서브미션이다.',
    '백 컨트롤 중 리어 네이키드 초크 방어가 심할 때, 목 깃을 깊게 잡고 각도를 튼다.',
    '백 포지션에서 한 손은 상대의 목 깃을 깊게 잡고, 반대 손은 상대의 바지 깃이나 다리를 잡은 상태.',
    '바지를 잡은 손으로 상대의 하체를 당기며, 목 깃을 잡은 손과 내 몸통을 뒤로 젖히듯 누워 상대의 목을 강하게 압박한다.',
    '내 다리로 상대의 어깨와 머리를 확실하게 눌러주어 상대가 도망가지 못하게 하는 것이 중요하다.',
    '상대의 다리를 잡지 않고 누우려다가 상대가 롤링하며 빠져나간다.',
    '순식간에 강한 압박이 들어가므로 탭이 나오면 즉각 놓아주어야 한다.',
    'https://www.youtube.com/watch?v=Xh0Y92Qo8J4',
    TRUE,
    29,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('bow-and-arrow-choke', currval('training_cards_id_seq'));

-- 라소 가드 컨트롤 / lasso-guard-control
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '라소 가드 컨트롤',
    '다리를 상대 팔에 휘감아 올가미처럼 묶는 방어적 오픈 가드',
    'OPEN_GUARD',
    'INTERMEDIATE',
    'OPEN_GUARD',
    '오픈 가드 상황에서 상대가 강력한 그립을 잡고 패스를 시도하려 할 때 억제용으로 사용',
    '라소 가드는 자신의 다리를 상대의 팔 안쪽에서 바깥쪽으로 감아 소매 그립과 함께 강력하게 통제하는 구조를 만들어 상대의 움직임을 마비시키는 기술이다.',
    '스파이더 가드 상황 등에서 상대가 패스를 위해 압박을 가할 때 방어와 스윕의 전환점으로 쓴다.',
    '양 소매를 잡은 상태에서 내 한쪽 다리가 상대의 팔 위를 넘어 겨드랑이 안쪽으로 훅이 걸린 상태.',
    '상대의 소매를 당기며 다리를 상대 팔 바깥에서 안으로 휘감아 발등을 상대의 견갑골 쪽에 밀착시키고 단단히 고정한다.',
    '라소 훅을 건 다리의 무릎이 펴지지 않고 단단하게 접힌 상태에서 상대 팔을 꽉 물고 있어야 한다.',
    '소매를 잡은 손을 너무 헐겁게 놔두어 상대가 팔을 빙글 돌려 풀어버리게 만든다.',
    '상대가 반대쪽으로 빠르게 스텝을 밟으면 레그 드래그의 위험이 있으므로 반대쪽 발의 통제도 중요하다.',
    'https://www.youtube.com/results?search_query=lasso+guard+bjj',
    TRUE,
    30,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('lasso-guard-control', currval('training_cards_id_seq'));

-- 앵클 픽 / ankle-pick
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '앵클 픽',
    '상대의 상체를 끌어내리며 동시에 발목을 낚아채 넘어뜨리는 테이크다운',
    'TAKEDOWN',
    'INTERMEDIATE',
    'STANDING',
    '스탠딩 상황에서 상대가 앞발에 체중을 싣고 있거나 머리를 내밀었을 때 사용',
    '앵클 픽은 상체 컨트롤을 통해 상대의 중심을 낮추게 만들고, 가벼워진 반대쪽 또는 체중이 실린 발목 부위를 직접 잡아채어 중심을 완전히 잃게 만드는 기술이다.',
    '깃 그립 등을 이용해 상대의 머리를 내 쪽으로 강하게 끌어당길 때 순간적으로 시도한다.',
    '스탠딩에서 한 손으로 상대의 목 뒷깃이나 머리를 잡고 통제하는 상태.',
    '상대의 목을 강하게 당겨 체중을 앞발로 쏠리게 한 직후, 자세를 낮춰 앞발의 발목이나 뒤꿈치를 낚아채며 위로 들어올린다.',
    '상대의 머리가 무릎 위치까지 내려올 정도로 강력하게 기울이기(쿠즈시)를 하는 것이 핵심이다.',
    '상체를 당기지 않고 멀리서 발목만 잡으려고 손을 뻗어 카운터를 허용한다.',
    '고개를 숙이고 들어가다 길로틴 초크에 잡힐 수 있으므로 타이밍이 생명이다.',
    'https://www.youtube.com/watch?v=FjIexfD2kOE',
    TRUE,
    31,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('ankle-pick', currval('training_cards_id_seq'));

-- 레그 드래그 패스 / leg-drag-pass
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '레그 드래그 패스',
    '상대의 다리를 내 몸통 바깥으로 치워버리고 골반을 장악하는 가드 패스',
    'PASS',
    'ADVANCED',
    'OPEN_GUARD',
    '상대의 오픈 가드를 파훼할 때 다리를 완전히 무력화시키고 측면을 잡고 싶을 때 사용',
    '레그 드래그 패스는 상대의 한쪽 다리를 내 반대쪽 골반 위치로 강력하게 넘겨버림으로써 상대의 골반 가동성을 없애고 등을 바닥에 고정시켜 패스하는 기술이다.',
    '상대가 발을 들어 방어하려 할 때 그립을 뜯어내고 순간적으로 다리 방향을 틀어버린다.',
    '스탠딩에서 상대의 오픈 가드를 마주하고 상대의 바짓깃이나 발목을 잡은 상태.',
    '상대의 한쪽 다리를 내 대각선 방향으로 당겨 내 허벅지와 골반 사이에 끼워 넣고, 체중으로 눌러 골반을 고정한 뒤 사이드나 백으로 이동한다.',
    '다리를 넘긴 즉시 내 체중을 실어 상대의 다리가 다시 돌아오지 못하도록 핀(Pin)하는 것이 가장 중요하다.',
    '다리만 당기고 체중 압박을 가하지 않아 상대가 쉽게 다리를 빼내 가드를 회복한다.',
    '그립 싸움 중 무리하게 다리를 당기다 트라이앵글 초크의 함정에 빠지지 않도록 유의해야 한다.',
    'https://www.youtube.com/watch?v=Jm3mYk_M74g',
    TRUE,
    32,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('leg-drag-pass', currval('training_cards_id_seq'));

-- 다스 초크 / d-arce-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '다스 초크',
    '상대의 겨드랑이 밑으로 팔을 넣어 목과 팔을 함께 조르는 초크',
    'SUBMISSION',
    'INTERMEDIATE',
    'TURTLE',
    '상대가 하프 가드나 터틀 포지션에서 상체를 일으키거나 팔을 뻗을 때 사용',
    '다스 초크는 상대의 겨드랑이 아래로 팔을 찔러 넣어 상대의 목과 팔을 동시에 감싼 뒤, 자신의 이두근을 잡아 암 트라이앵글 형태로 조르는 노기에서도 강력한 서브미션이다.',
    '하프 가드 탑에서 상대가 언더훅을 파고 일어나는 타이밍에 역으로 겨드랑이에 팔을 집어넣는다.',
    '상대의 상체 측면에 위치하거나 상대가 터틀, 하프 가드에서 언더훅을 판 상태.',
    '상대의 겨드랑이 쪽으로 내 팔을 깊숙이 찔러 넣어 반대쪽 목덜미를 관통하게 한 뒤, 내 반대쪽 팔의 이두를 잡고 잠가 체중을 싣어 압박한다.',
    '팔이 목을 깊게 감싸는 것은 물론, 체중을 앞으로 쏠리게 하여 상대의 경동맥에 강한 압박을 주어야 한다.',
    '팔이 충분히 얕게 들어간 상태에서 락을 걸려다 그립이 완성되지 않는다.',
    '무리하게 자세를 틀면 스윕을 당할 수 있으므로 베이스를 낮고 넓게 유지해야 한다.',
    'https://www.youtube.com/watch?v=R0_Jc05_m8s',
    TRUE,
    33,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('d-arce-choke', currval('training_cards_id_seq'));

-- 펜듈럼 스윕 / pendulum-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '펜듈럼 스윕',
    '다리를 시계추처럼 크게 휘둘러 반동으로 상대를 넘기는 스윕',
    'SWEEP',
    'INTERMEDIATE',
    'CLOSED_GUARD',
    '클로즈드 가드에서 상대의 한쪽 팔을 내 몸통 쪽으로 제압했을 때 사용',
    '펜듈럼 스윕은 다리의 무게와 원심력을 시계추(Pendulum)처럼 이용하여, 상대의 베이스가 없는 방향으로 강력하게 회전시켜 상대를 넘기는 기술이다.',
    '상대가 베이스를 유지하려 하지만 한쪽 팔의 통제권을 나에게 빼앗겨 체중이 쏠릴 때 유효하다.',
    '클로즈드 가드에서 상대의 소매와 겨드랑이(또는 다리)를 통제하고 몸의 각도를 측면으로 튼 상태.',
    '한쪽 다리를 머리 쪽으로 강하게 차올리는 반동을 이용하고, 그 힘을 전달하여 내 몸을 회전시키면서 상대를 내 제압된 팔 방향으로 크게 넘긴다.',
    '다리를 흔드는 반동과 골반의 회전이 끊기지 않고 하나의 부드러운 움직임으로 이어져야 한다.',
    '다리의 반동 없이 근력으로만 상대를 넘기려다 상대의 저항에 막힌다.',
    '회전 과정에서 상대가 엉덩이를 빼며 방어할 때 암바 등 다른 기술로 트랜지션할 준비를 해야 한다.',
    'https://www.youtube.com/results?search_query=pendulum+sweep+bjj',
    TRUE,
    34,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('pendulum-sweep', currval('training_cards_id_seq'));

-- rear-naked-choke -> bow-and-arrow-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'bow-and-arrow-choke'
WHERE source.seed_key = 'rear-naked-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- bow-and-arrow-choke -> rear-naked-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'rear-naked-choke'
WHERE source.seed_key = 'bow-and-arrow-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- lasso-guard-control -> spider-guard-control
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'spider-guard-control'
WHERE source.seed_key = 'lasso-guard-control'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- lasso-guard-control -> omoplata
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'omoplata'
WHERE source.seed_key = 'lasso-guard-control'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- ankle-pick -> single-leg-takedown
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-takedown'
WHERE source.seed_key = 'ankle-pick'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- ankle-pick -> toreando-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'toreando-pass'
WHERE source.seed_key = 'ankle-pick'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- leg-drag-pass -> toreando-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'toreando-pass'
WHERE source.seed_key = 'leg-drag-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- leg-drag-pass -> knee-cut-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'knee-cut-pass'
WHERE source.seed_key = 'leg-drag-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- d-arce-choke -> guillotine-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'guillotine-choke'
WHERE source.seed_key = 'd-arce-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- pendulum-sweep -> flower-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'flower-sweep'
WHERE source.seed_key = 'pendulum-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- pendulum-sweep -> closed-guard-armbar
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'closed-guard-armbar'
WHERE source.seed_key = 'pendulum-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- 2026-06-10 4차 적재 데이터
-- 에제키엘 초크 (마운트) / ezekiel-from-mount
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '에제키엘 초크 (마운트)',
    '마운트 포지션에서 자신의 소매를 잡고 상대의 목을 강하게 압박하는 초크',
    'SUBMISSION',
    'BEGINNER',
    'MOUNT',
    'MOUNT 포지션에서 상대가 가슴을 밀며 타이트하게 방어할 때 기습적으로 사용',
    '에제키엘 초크는 자신의 도복 소매 안에 네 손가락을 집어넣어 단단한 지지대를 만들고, 반대쪽 팔뚝으로 상대의 기도를 직접 압박하는 탑 서브미션이다.',
    '마운트를 탄 상태에서 상대가 브릿지를 방어하기 위해 상체를 웅크리거나 내 골반을 밀어낼 때 목 주변 공간을 공략한다.',
    '마운트 포지션에서 상대의 머리 뒤로 한쪽 팔을 깊숙이 밀어 넣은 상태.',
    '상대의 머리를 받친 팔의 소매를 반대쪽 손으로 잡고, 소매를 잡은 손의 팔뚝을 상대의 목 앞으로 강하게 칼처럼 가로질러 내리누르며 조른다.',
    '팔 힘으로만 누르기보다 머리를 받친 팔을 내 가슴 쪽으로 당겨 지렛대 원리를 완성해야 한다.',
    '자신의 소매를 헐겁게 잡거나 상대 목 앞이 아닌 턱을 누르면 기술이 풀리기 쉽다.',
    '상대가 이 틈을 타 우파 이스케이프를 시도할 수 있으므로 하체 베이스를 무겁게 유지해야 한다.',
    'http://www.youtube.com/watch?v=5M1wkbaOYUM',
    TRUE,
    35,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('ezekiel-from-mount', currval('training_cards_id_seq'));

-- 베림보로 / berimbolo
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '베림보로',
    '데라히바 가드에서 등 구르기를 통해 상대의 등 뒤를 차지하는 백 테이크 기술',
    'GUARD',
    'ADVANCED',
    'OPEN_GUARD',
    'OPEN_GUARD(데라히바) 상황에서 상대를 앉히고 백 포지션을 노릴 때 사용',
    '베림보로는 모던 주짓수를 대표하는 화려한 롤링 기술로, 상대의 다리와 골반을 옭아맨 채 엉덩이를 들고 회전하여 상대의 방어벽을 우회하고 백 마운트를 점유하는 상급자용 기술이다.',
    '데라히바 가드에서 상대의 중심을 무너뜨려 매트에 엉덩이를 꿇리게 만든 직후 롤링을 시도한다.',
    '데라히바 가드 그립을 잡고 상대방의 골반과 허리 벨트(또는 바지)를 선점한 상태.',
    '상대의 발목과 허리를 잡은 상태로 내 머리를 상대 다리 사이로 집어넣으며 역으로 구른다. 회전하면서 상대의 다리를 밀어내고 골반을 타서 등 뒤로 진입한다.',
    '구르는 동안 상대의 골반 그립을 절대 놓치지 않아야 하며, 내 엉덩이가 상대 골반보다 높은 위치를 선점해야 한다.',
    '회전 반동이 부족하거나 롤링 중에 그립이 풀려 오히려 탑 포지션을 뺏기게 된다.',
    '척추와 목에 체중이 실리는 인버티드(역전) 동작이 포함되므로 유연성과 매트 숙련도가 필요하다.',
    'http://www.youtube.com/watch?v=A6rFk8pUCIc',
    TRUE,
    36,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('berimbolo', currval('training_cards_id_seq'));

-- 거버 가드 / gubber-guard-control
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '거버 가드',
    '도복의 라펠을 상대 등 뒤로 넘겨 어깨와 상체를 완벽하게 묶어두는 클로즈드 가드 변형',
    'GUARD',
    'ADVANCED',
    'CLOSED_GUARD',
    'CLOSED_GUARD에서 상대의 가드 패스 시도를 원천 봉쇄하고 서브미션을 세팅할 때 사용',
    '거버 가드는 노기의 러버 가드(Rubber Guard) 개념을 도복(Gi) 버전으로 결합한 형태로, 상대방 도복의 끝자락(라펠)을 뽑아 상대의 어깨 위로 감싸 쥐어 상체를 박제시키는 강력한 컨트롤 기술이다.',
    '상대가 클로즈드 가드 안에서 상체를 세우지 못하게 하거나, 압박 패스를 시도하려고 몸을 밀착해 올 때 라펠을 뽑아 잠근다.',
    '클로즈드 가드 상태에서 상대의 도복 라펠을 길게 뽑아 놓은 상태.',
    '내 한쪽 다리를 상대 등 뒤로 높게 올리고, 뽑아낸 상대의 라펠을 내 다리 위를 가로질러 반대쪽 손으로 강하게 틀어쥐어 상대의 어깨를 묶는다.',
    '라펠을 쥔 손이 상대의 목과 어깨를 완전히 밀착 압박해야 상대가 허리를 펴고 일어날 수 없다.',
    '라펠을 대충 느슨하게 잡으면 공간이 생겨 상대가 팔을 빼거나 압박 패스로 전환한다.',
    '과도하게 다리를 꺾어 잡으려 하면 무릎 관절에 무리가 갈 수 있으므로 골반의 유연한 각도 조절이 동반되어야 한다.',
    'http://www.youtube.com/watch?v=6ve_IvIMZ3Q',
    TRUE,
    37,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('gubber-guard-control', currval('training_cards_id_seq'));

-- 인버티드 암바 / inverted-armbar
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '인버티드 암바',
    '상대의 팔꿈치를 반대 방향(위쪽)으로 꺾어 제압하는 변형 암바 서브미션',
    'SUBMISSION',
    'INTERMEDIATE',
    'HALF_GUARD',
    'HALF_GUARD 또는 가드 하위에서 상대가 내 상체를 압박하며 팔을 깊게 뻗었을 때 사용',
    '인버티드 암바는 일반적인 암바와 달리 상대의 팔꿈치 관절을 내 가슴이나 어깨를 지점 삼아 반대 각도로 비틀어 꺾는 기습적인 하위 서브미션이다.',
    '하프 가드나 오픈 가드에서 패스를 시도하려는 탑 라이더가 내 목을 파거나 겨드랑이를 파려고 팔을 깊게 찔러 넣었을 때 순간적으로 낚아챈다.',
    '가드 하위 포지션에서 상대의 한쪽 팔뚝이 내 가슴이나 어깨 위에 얹혀져 있는 상태.',
    '상대의 손목을 내 어깨 쪽에 단단히 고정하고, 골반을 틀어 내 가슴과 골반의 압박으로 상대의 팔꿈치 관절을 위 방향으로 강하게 꺾어 올린다.',
    '상대가 팔꿈치를 회전시켜 빼내지 못하도록 손목 그립을 내 몸에 완벽히 밀착 고정해야 한다.',
    '손목 통제가 헐거우면 상대가 팔을 구부리거나 회전시켜 사이드 패스로 연결한다.',
    '관절이 가동 범위 반대로 순간적으로 꺾이므로 부상 위험이 높아 훈련 시 부드럽게 압박해야 한다.',
    'http://www.youtube.com/watch?v=CJiB1QcvuEg',
    TRUE,
    38,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('inverted-armbar', currval('training_cards_id_seq'));

-- 스트레이트 앵클 락 / straight-ankle-lock
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '스트레이트 앵클 락',
    '상대의 아킬레스건을 팔뚝으로 강하게 조여 제압하는 하체 서브미션',
    'SUBMISSION',
    'BEGINNER',
    'LEG_ENTANGLEMENT',
    'LEG_ENTANGLEMENT 포지션에서 상대의 발목을 장악했을 때 사용',
    '스트레이트 앵클 락은 상대의 발목을 내 겨드랑이에 끼우고 날카로운 팔뚝 뼈로 아킬레스건을 강하게 압박하며 골반을 튕겨 꺾는 흰 띠부터 사용 가능한 합법 하체 관절기이다.',
    '오픈 가드에서 상대가 일어설 때 다리를 옭아매며 하체 포지션(아시 가라미)을 세팅했을 때 진입한다.',
    '싱글 레그 X 가드 혹은 아시 가라미 포지션에서 상대의 한쪽 다리를 내 팔로 감싸 안은 상태.',
    '상대의 발목을 겨드랑이에 단단히 끼우고 내 팔뚝 날을 아킬레스건에 위치시킨 뒤, 뒤로 누우며 골반을 하늘로 들어 올려 강한 압박을 가한다.',
    '내 팔꿈치를 등 뒤로 숨기듯 단단히 조여야 하며, 상대 골반을 밟은 다리의 텐션이 유지되어야 상대가 탈출하지 못한다.',
    '상대의 발이 내 겨드랑이에서 느슨하게 빠지면 압박점이 어긋나 기가 들어가지 않는다.',
    '방어하는 상대가 무릎을 비틀 때 니 리핑 반칙 규정에 걸리지 않도록 다리 위치를 엄격히 고수해야 한다.',
    'https://www.youtube.com/watch?v=sc_K1VCHn1A',
    TRUE,
    39,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('straight-ankle-lock', currval('training_cards_id_seq'));

-- 터틀 싯 아웃 이스케이프 / turtle-sit-out-escape
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '터틀 싯 아웃 이스케이프',
    '터틀 포지션에서 다리를 앞으로 빼며 회전하여 포지션을 역전시키는 탈출기',
    'ESCAPE',
    'BEGINNER',
    'TURTLE',
    'TURTLE 하위 포지션에서 상대가 내 등 뒤를 타이트하게 장악하지 못하고 측면에 치우쳐 있을 때 사용',
    '싯 아웃 이스케이프는 상대가 내 터틀 가드를 압박할 때 기습적으로 한쪽 다리를 바깥으로 차내며 주저앉아, 상대를 내 앞이나 하위 포지션으로 떨어뜨리는 역동적인 탈출 기술이다.',
    '태클을 방어당해 터틀이 되었거나 사이드 압박을 피해 터틀로 전환했을 때 상체의 베이스가 열린 틈을 타 시도한다.',
    '터틀 포지션에서 양 무릎과 팔꿈치로 매트를 지탱하고 방어하는 상태.',
    '상대 베이스의 반대쪽 팔을 축으로 삼고, 안쪽 다리를 상대 몸통 밑 공간으로 길게 뻗으며 주저앉는다(Sit-out). 이어서 몸을 빠르게 회전시켜 상대를 하위에 깔거나 가드를 회복한다.',
    '다리를 뺄 때 엉덩이를 매트에 낮게 깔아 상대가 내 백을 타거나 올라타지 못하게 공간을 지워야 한다.',
    '다리를 어설프게 빼다가 상체를 들면 상대방에게 백 마운트나 초크 그립을 헌납하게 된다.',
    '실패 시 척추가 노출되어 백 포지션을 완전히 내줄 수 있으므로 민첩하고 확실한 회전이 필요하다.',
    'https://www.youtube.com/watch?v=0kF4A-T2r2c',
    TRUE,
    40,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('turtle-sit-out-escape', currval('training_cards_id_seq'));

-- 스탠딩 가드 브레이크 / standing-guard-break
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '스탠딩 가드 브레이크',
    '클로즈드 가드 안에서 완전히 일어서서 상대의 단단한 다리 잠금을 열어내는 기술',
    'PASS',
    'BEGINNER',
    'CLOSED_GUARD',
    'CLOSED_GUARD 하위 포지션의 상대가 단단하게 다리를 잠그고 서브미션을 노릴 때 탈출하기 위해 사용',
    '스탠딩 가드 브레이크는 무릎을 꿇은 상태에서 열기 힘든 강력한 클로즈드 가드를 중력과 레버리지(지렛대 원리)를 이용해 일어서서 안전하게 깨부수는 필수 패스 셋업 기술이다.',
    '하위 상대의 칼라 초크나 암바 그립을 무력화시키고 베이스를 확보한 뒤 일어설 타이밍을 잡는다.',
    '상대의 클로즈드 가드 안에 갇혀 있으나 양손으로 상대의 겨드랑이나 골반, 소매를 통제한 상태.',
    '상대의 상체 그립을 제압한 채 한 발씩 차례로 일어서서 척추를 곧게 편다. 한 손으로 상대의 한쪽 무릎 내측을 아래로 강하게 밀어내어 잠긴 다리를 오픈시킨다.',
    '일어설 때 허리를 숙이면 역으로 암바나 덤프 스윕을 당하므로, 스쿼트 자세처럼 가슴과 척추를 꼿꼿이 세워야 한다.',
    '고개를 숙인 채 어설프게 일어섰다가 상대에게 언더훅을 파이고 플라워 스윕이나 삼각 조르기를 당한다.',
    '상대가 내 발목을 잡고 뒤로 넘어뜨리는 스윕을 시도할 수 있으므로 발의 베이스 넓이를 잘 유지해야 한다.',
    'https://www.youtube.com/watch?v=kYor41n_D00',
    TRUE,
    41,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('standing-guard-break', currval('training_cards_id_seq'));

-- ezekiel-from-mount -> upa-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'upa-escape'
WHERE source.seed_key = 'ezekiel-from-mount'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- berimbolo -> de-la-riva-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'de-la-riva-sweep'
WHERE source.seed_key = 'berimbolo'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- berimbolo -> rear-naked-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'rear-naked-choke'
WHERE source.seed_key = 'berimbolo'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- gubber-guard-control -> omoplata
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'omoplata'
WHERE source.seed_key = 'gubber-guard-control'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- gubber-guard-control -> triangle-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'triangle-choke'
WHERE source.seed_key = 'gubber-guard-control'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- inverted-armbar -> knee-cut-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'knee-cut-pass'
WHERE source.seed_key = 'inverted-armbar'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- inverted-armbar -> half-guard-underhook-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'half-guard-underhook-sweep'
WHERE source.seed_key = 'inverted-armbar'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- straight-ankle-lock -> single-leg-x-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-x-sweep'
WHERE source.seed_key = 'straight-ankle-lock'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- straight-ankle-lock -> x-guard-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'x-guard-sweep'
WHERE source.seed_key = 'straight-ankle-lock'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- turtle-sit-out-escape -> rear-naked-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'rear-naked-choke'
WHERE source.seed_key = 'turtle-sit-out-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- standing-guard-break -> flower-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'flower-sweep'
WHERE source.seed_key = 'standing-guard-break'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- standing-guard-break -> knee-cut-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'knee-cut-pass'
WHERE source.seed_key = 'standing-guard-break'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- 2026-06-10 5차 적재 데이터
-- 니 컷 패스 / knee-cut-pass
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '니 컷 패스',
    '상대의 오픈 가드 또는 하프 가드 연결을 무릎으로 갈라 지나가며 사이드 컨트롤로 전환하는 패스',
    'PASS',
    'BEGINNER',
    'OPEN_GUARD',
    '상대가 무릎 방패나 다리 프레임으로 거리를 만들 때 압박을 유지하며 지나갈 때 사용',
    '니 컷 패스는 한쪽 무릎을 상대의 허벅지 안쪽 라인으로 넣고, 상체 압박과 언더훅 또는 칼라 그립을 이용해 상대의 골반 회전을 막으면서 통과하는 기본 패스다. 단순히 무릎만 밀고 들어가는 기술이 아니라, 상대의 어깨와 골반을 반대 방향으로 고정해 하체가 따라오지 못하게 만드는 것이 핵심이다. 성공하면 사이드 컨트롤이나 니 온 벨리로 자연스럽게 연결할 수 있다.',
    '상대가 오픈 가드에서 한쪽 다리로 내 골반이나 어깨를 막고 있거나, 하프 가드로 연결하려고 할 때 사용하기 좋다. 특히 상대의 무릎 방패가 높지 않고, 내 무릎이 상대 허벅지 중앙선을 가를 수 있는 각도가 나왔을 때 효과적이다. 상대가 스파이더 가드나 라소 가드처럼 소매를 강하게 잡고 있으면 먼저 그립을 정리한 뒤 진입해야 한다.',
    '상대 앞에서 한쪽 무릎을 세우고, 다른 다리는 뒤로 뻗어 베이스를 만든다. 진입하는 쪽 무릎은 상대의 허벅지 사이 또는 무릎 방패 안쪽으로 들어간다. 상체는 낮게 유지하고, 가능하면 머리는 상대 가슴 또는 턱 아래 방향으로 붙여 상대가 상체를 세우지 못하게 한다.',
    '먼저 상대의 바지나 무릎 라인을 제어해 다리 움직임을 줄인다. 그다음 진입하는 쪽 무릎을 상대 허벅지 안쪽으로 밀어 넣고, 반대쪽 팔로 언더훅이나 머리 컨트롤을 잡는다. 가슴 압박을 유지한 상태에서 엉덩이를 낮추고, 발등이나 정강이를 빼며 상대의 다리 프레임을 통과한다. 마지막에는 급하게 올라가지 말고 어깨 압박을 유지하면서 사이드 컨트롤로 안정화한다.',
    '무릎을 넣는 것보다 상체 압박과 골반 고정이 더 중요하다. 머리 위치가 높으면 상대가 언더훅을 되찾거나 싱글 레그로 연결할 수 있으므로 낮게 유지한다. 지나갈 때는 다리를 크게 들어 넘기지 말고 바닥을 긁듯이 천천히 빼야 상대의 리커버리를 줄일 수 있다.',
    '무릎만 먼저 넣고 상체 압박을 하지 않아 상대가 쉽게 새우기 동작으로 빠져나가는 경우가 많다. 또 머리를 상대 바깥쪽으로 두면 기무라 그립이나 언더훅 스윕을 허용할 수 있다. 패스가 거의 끝났다고 생각하고 손을 풀어버리면 상대가 다시 가드를 회복한다.',
    '상대의 무릎이나 발목을 비틀어 억지로 누르지 말아야 한다. 니 컷 중 상대가 강하게 하프 가드로 감으면 무리하게 다리를 빼지 말고 압박을 재정비한 뒤 진행한다.',
    'https://www.youtube.com/watch?v=3IqCi1GXmOg',
    TRUE,
    42,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('knee-cut-pass', currval('training_cards_id_seq'));

-- 암 트라이앵글 초크 / arm-triangle-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '암 트라이앵글 초크',
    '상대의 한쪽 팔과 목을 함께 압박해 경동맥을 조르는 상체 압박형 초크',
    'SUBMISSION',
    'BEGINNER',
    'SIDE_CONTROL',
    '사이드 컨트롤이나 마운트에서 상대의 한쪽 팔이 목 옆으로 밀려 올라갔을 때 사용',
    '암 트라이앵글 초크는 내 어깨와 팔을 이용해 상대의 목 한쪽을 막고, 상대 자신의 팔이 반대쪽 목을 막게 만들어 압박하는 서브미션이다. 팔 힘으로 조르는 기술이 아니라, 머리 위치와 어깨 압박, 몸 전체의 각도 조절로 완성된다. 마운트에서 세팅한 뒤 사이드로 내려가 마무리하거나, 사이드 컨트롤에서 바로 진입할 수 있다.',
    '상대가 프레임을 만들기 위해 팔을 목 앞에 세우거나, 마운트에서 내 몸을 밀어내려다 팔이 얼굴 옆으로 올라갔을 때 좋은 기회가 생긴다. 상대의 팔꿈치가 몸통에서 떨어져 목 옆으로 넘어오면 초크 구조를 만들 수 있다. 상대가 팔을 다시 빼기 전에 머리를 낮추고 팔을 고립시키는 것이 중요하다.',
    '사이드 컨트롤 또는 마운트에서 상대의 한쪽 팔이 상대 머리 옆으로 넘어간 상태에서 시작한다. 내 머리는 상대의 갇힌 팔 쪽 귀 근처에 낮게 붙이고, 한쪽 팔은 상대 목 아래를 깊게 감는다. 반대 손은 팔꿈치나 손목을 잡아 구조를 잠근다.',
    '상대의 팔을 목 옆으로 밀어 올리고 내 머리를 낮춰 팔이 빠져나오지 못하게 막는다. 목을 감은 팔의 손을 반대쪽 이두박근이나 손에 연결해 잠금 구조를 만든다. 마운트에서 시작했다면 초크를 유지한 채 갇힌 팔 반대쪽 사이드로 내려가 몸을 낮춘다. 가슴을 바닥 쪽으로 떨어뜨리고 어깨를 상대 목에 밀어 넣어 천천히 압박을 완성한다.',
    '팔로 당기는 것이 아니라 어깨를 목에 밀어 넣는 느낌이 중요하다. 내 머리가 높아지면 상대 팔이 빠지므로 귀 옆에 낮게 붙인다. 마무리할 때는 몸을 상대와 평행하게 두기보다 약간 각도를 만들고 엉덩이를 낮춰야 압박이 깊어진다.',
    '손으로만 조이려고 해서 팔이 빨리 지치는 경우가 많다. 상대 팔이 충분히 목 옆으로 넘어가지 않았는데 억지로 잡으면 초크가 아니라 얼굴 압박만 된다. 마운트에서 바로 누르기만 하면 상대가 브릿지로 뒤집을 수 있다.',
    '목과 어깨에 강한 압박이 들어가므로 파트너가 탭하면 즉시 풀어야 한다. 초보자는 얼굴을 비트는 방식이 아니라 목 양쪽 혈류를 막는 구조를 천천히 확인하면서 연습하는 것이 좋다.',
    'https://www.youtube.com/watch?v=XMN6eFY0u-g',
    TRUE,
    43,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('arm-triangle-choke', currval('training_cards_id_seq'));

-- 사이드 컨트롤 아메리카나 / side-control-americana
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '사이드 컨트롤 아메리카나',
    '사이드 컨트롤에서 상대 팔을 ㄱ자 형태로 고정해 어깨 관절을 압박하는 서브미션',
    'SUBMISSION',
    'BEGINNER',
    'SIDE_CONTROL',
    '상대가 아래에서 내 목이나 어깨를 밀어내며 팔꿈치가 벌어졌을 때 사용',
    '아메리카나는 상대 팔꿈치가 몸에서 벌어진 순간을 이용해 손목과 팔꿈치를 고정하고, 어깨를 외회전 방향으로 압박하는 기본 관절기다. 사이드 컨트롤에서 자주 나오며, 상대가 프레임을 만들거나 밀어내는 반응을 역이용한다. 기술 자체는 단순해 보이지만, 상대의 손목을 바닥에 붙이고 팔꿈치 각도를 유지하는 세부가 중요하다.',
    '사이드 컨트롤에서 상대가 내 목을 밀거나 팔꿈치를 열어 공간을 만들려고 할 때 사용할 수 있다. 상대 손이 바닥에 가깝고 팔꿈치가 90도 정도로 접힌 상태라면 좋은 세팅이다. 상대가 팔을 몸에 단단히 붙이고 있으면 무리하게 빼내기보다 압박을 이용해 반응을 유도해야 한다.',
    '사이드 컨트롤에서 가슴을 상대 가슴 위에 붙이고, 상대의 가까운 팔이 바닥 쪽으로 눌린 상태에서 시작한다. 한 손으로 상대 손목을 잡아 바닥에 고정하고, 다른 팔은 상대 팔 아래로 넣어 내 손목을 잡아 피겨포 그립을 만든다.',
    '먼저 상대 손목을 바닥에 붙이고 팔꿈치를 어깨선보다 너무 아래로 내려가지 않게 조절한다. 팔 아래로 내 팔을 넣어 내 손목을 잡고, 가슴 압박을 유지해 상대가 몸을 돌리지 못하게 한다. 이후 상대 손등을 바닥에 붙인 채 팔꿈치를 몸 쪽으로 살짝 당기고, 손목을 상대 엉덩이 방향으로 천천히 이동시킨다. 어깨에 압박이 걸리면 바로 마무리한다.',
    '상대 손목이 바닥에서 뜨면 힘이 새기 쉽다. 팔꿈치를 너무 머리 위로 올리면 압박이 약해지고, 너무 아래로 내리면 상대가 팔을 펼 수 있다. 내 가슴 압박을 유지해야 상대가 몸을 돌려 방어하지 못한다.',
    '손목만 비틀려고 해서 파트너 어깨에 불필요한 통증을 주는 경우가 있다. 또 사이드 컨트롤 압박을 풀고 팔만 잡으려 하면 상대가 언더훅을 만들고 빠져나간다. 마무리 방향을 위로 들어 올리면 기술이 잘 걸리지 않는다.',
    '어깨 관절에 직접 압박이 들어가는 기술이므로 천천히 적용해야 한다. 유연성이 낮거나 어깨 부상이 있는 파트너에게는 특히 강도를 낮춰 연습한다.',
    'https://www.youtube.com/watch?v=ZgMu5o7vzMc',
    TRUE,
    44,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('side-control-americana', currval('training_cards_id_seq'));

-- 코우치 가리 / ko-uchi-gari
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '코우치 가리',
    '상대의 안쪽 발을 작게 걸어 중심을 무너뜨리는 스탠딩 테이크다운',
    'TAKEDOWN',
    'BEGINNER',
    'STANDING',
    '상대가 뒤로 물러나거나 한쪽 발에 체중이 실렸을 때 안쪽 발을 걸어 넘어뜨릴 때 사용',
    '코우치 가리는 상대의 다리 안쪽을 내 발로 걸어 작은 원으로 걷어내며 넘어뜨리는 테이크다운이다. 큰 힘으로 들어 올리는 기술이 아니라, 상체 당김과 발 걸기 타이밍을 맞춰 상대의 체중이 실린 발을 빼앗는 방식이다. 주짓수에서는 단독으로 사용하기도 하고, 더블 레그나 싱글 레그 진입을 만들기 위한 연결 기술로도 자주 쓰인다.',
    '상대가 스탠딩에서 뒤로 빠지거나, 내 전진 압박 때문에 한쪽 발을 무겁게 디딜 때 사용하기 좋다. 상대가 다리를 넓게 벌리고 버티면 안쪽 발목이나 뒤꿈치 라인을 걸어 균형을 깨뜨릴 수 있다. 실패해도 상대의 스탠스를 좁히거나 손을 짚게 만들 수 있어 후속 공격으로 연결하기 좋다.',
    '기본 스탠딩 그립 또는 칼라 앤 슬리브 그립에서 시작한다. 내 머리는 정면에서 너무 숙이지 않고, 상대의 어깨와 팔을 당기며 체중을 한쪽 발에 싣게 만든다. 걸어낼 발은 상대의 발 안쪽 라인으로 들어갈 준비를 한다.',
    '상대의 상체를 살짝 당기거나 밀어 반응을 만든다. 상대가 발을 딛는 순간 내 발을 상대 발 안쪽 뒤꿈치 근처에 넣고, 바닥을 쓸듯이 짧게 걸어낸다. 동시에 손은 상대를 걸리는 발 방향으로 끌고, 내 몸은 앞으로 압박한다. 상대가 넘어지면 바로 상체를 따라가며 사이드 컨트롤이나 하프 가드 상위 포지션으로 안정화한다.',
    '발로 차는 것이 아니라 상대가 체중을 싣는 순간 발을 빼앗는 느낌이어야 한다. 상체 그립의 당김과 발 걸기가 동시에 일어나야 효과가 크다. 실패했을 때 바로 더블 레그나 앵클 픽으로 연결할 수 있도록 자세를 낮게 유지한다.',
    '상대 발이 가벼운데 억지로 걸어 기술이 비는 경우가 많다. 발을 크게 휘두르면 상대에게 싱글 레그를 잡힐 수 있다. 상체 컨트롤 없이 발만 걸면 상대가 균형을 쉽게 회복한다.',
    '연습 중 상대 무릎이 안쪽으로 꺾이지 않게 조심해야 한다. 넘어뜨린 뒤에는 상대가 머리부터 떨어지지 않도록 그립을 유지하고 따라가야 한다.',
    'https://www.youtube.com/watch?v=AIRKLN3SPHQ',
    TRUE,
    45,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('ko-uchi-gari', currval('training_cards_id_seq'));

-- 딥 하프 가드 스윕 / deep-half-guard-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '딥 하프 가드 스윕',
    '상대의 한쪽 다리 아래 깊게 들어가 중심을 띄운 뒤 상위 포지션으로 전환하는 하프 가드 스윕',
    'SWEEP',
    'INTERMEDIATE',
    'HALF_GUARD',
    '하프 가드 아래에서 상대가 강하게 압박할 때 몸을 깊게 넣어 무게중심을 뒤집을 때 사용',
    '딥 하프 가드 스윕은 상대의 다리 아래로 몸을 깊게 넣어 상대의 골반과 무게중심을 내 어깨 위에 올린 뒤, 방향 전환으로 뒤집는 기술이다. 일반 하프 가드 언더훅 싸움과 달리, 상대의 상체를 밀어내기보다 내 몸을 상대 하체 아래로 이동시켜 중심을 빼앗는다. 압박이 강한 상대에게 공간을 만들어 탈출과 스윕을 동시에 노릴 수 있다.',
    '하프 가드에서 상대가 크로스페이스로 강하게 누르거나, 언더훅 싸움에서 밀렸을 때 딥 하프로 전환할 수 있다. 상대의 무릎이 바닥에 고정되어 있고 체중이 앞으로 실려 있으면 몸을 안쪽으로 파고들 기회가 생긴다. 단, 상대가 기무라 그립을 강하게 잡고 있거나 목을 깊게 컨트롤하면 먼저 팔을 보호해야 한다.',
    '하프 가드 아래에서 상대 한쪽 다리를 내 다리로 감고, 내 상체는 상대 다리 아래로 들어갈 준비를 한다. 머리는 상대 골반 아래 또는 허벅지 안쪽에 위치시키고, 팔은 상대 다리나 엉덩이 라인을 감아 상대가 뒤로 빠지지 못하게 한다.',
    '먼저 옆으로 누워 상대 압박을 정면으로 받지 않게 만든다. 내 머리와 어깨를 상대 다리 아래로 깊게 넣고, 상대의 갇힌 다리를 두 팔과 다리로 통제한다. 상대 체중이 내 위로 올라오면 다리 훅과 엉덩이 회전을 이용해 상대의 중심을 한쪽으로 기울인다. 상대가 손을 짚거나 중심이 무너지면 몸을 따라 올라가 상위 하프 가드 또는 사이드 컨트롤로 전환한다.',
    '머리를 너무 바깥에 두면 상대가 다리를 빼거나 백스텝으로 빠질 수 있다. 상대의 골반 아래까지 깊이 들어가야 무게중심을 조절할 수 있다. 팔을 뻗어 버티기보다 팔꿈치를 몸에 붙여 기무라 카운터를 예방해야 한다.',
    '깊이 들어가지 못한 상태에서 스윕만 시도해 상대에게 패스를 허용하는 경우가 많다. 상대 다리를 느슨하게 잡으면 상대가 무릎을 빼고 마운트나 니 컷으로 전환할 수 있다. 목을 눌린 상태에서 억지로 회전하면 목과 허리에 부담이 커진다.',
    '딥 하프는 상대 체중이 내 머리와 목 근처에 실리기 쉬우므로 목을 비틀지 않아야 한다. 파트너가 체중을 과하게 실으면 천천히 멈추고 포지션을 다시 잡는 것이 안전하다.',
    'https://www.youtube.com/watch?v=ojvH99btFYo',
    TRUE,
    46,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('deep-half-guard-sweep', currval('training_cards_id_seq'));

-- 노스 사우스 초크 / north-south-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '노스 사우스 초크',
    '노스 사우스 포지션에서 팔과 몸통 압박으로 상대의 목을 조르는 서브미션',
    'SUBMISSION',
    'INTERMEDIATE',
    'SIDE_CONTROL',
    '사이드 컨트롤에서 상대가 목을 돌리거나 팔 프레임이 약해졌을 때 노스 사우스로 이동해 사용',
    '노스 사우스 초크는 상대 머리 위쪽으로 이동한 뒤 한쪽 팔로 상대 목을 감싸고, 내 갈비뼈와 광배근 쪽 압박으로 반대쪽 혈류를 막는 초크다. 겉보기에는 팔로 목을 감싸는 단순한 기술처럼 보이지만, 실제로는 몸의 위치, 어깨 깊이, 엉덩이 무게가 맞아야 효과가 난다. 팔 힘으로 조르기보다 상대 목 주변 공간을 없애는 방식으로 이해해야 한다.',
    '사이드 컨트롤에서 상대가 내 몸을 밀기 위해 팔을 뻗거나, 얼굴을 반대쪽으로 돌리며 탈출하려 할 때 진입하기 좋다. 상대의 겨드랑이 프레임이 약하고 머리 주변 공간이 열려 있으면 노스 사우스로 이동하면서 목을 감을 수 있다. 기무라 공격을 의식하는 상대에게 반대 옵션으로도 연결된다.',
    '사이드 컨트롤 또는 노스 사우스 상위 포지션에서 시작한다. 한 팔은 상대 목 아래를 깊게 감고, 내 어깨 또는 갈비뼈 쪽이 상대 목 옆에 밀착되어야 한다. 다리는 넓게 벌려 베이스를 만들고 엉덩이는 낮춘다.',
    '사이드 컨트롤에서 상대 목 아래로 팔을 깊게 넣고 머리 위쪽으로 이동한다. 상대의 턱 아래 공간을 확보한 뒤, 내 몸을 노스 사우스 방향으로 정렬한다. 팔로 당기기보다 어깨와 옆구리를 상대 목에 밀착시키고, 엉덩이를 낮춰 상대가 고개를 돌리지 못하게 한다. 압박이 충분히 잠기면 천천히 체중을 떨어뜨려 마무리한다.',
    '팔꿈치가 상대 목에서 멀어지면 초크가 헐거워진다. 상대 목 양쪽 공간을 모두 없애야 하며, 단순히 목 앞을 누르면 턱 압박만 된다. 머리는 너무 높게 두지 말고 상대 어깨 근처로 낮춰 안정적인 베이스를 만든다.',
    '팔 힘으로 목을 당기면서 마무리하려고 하면 초크가 약하고 오래 걸린다. 몸이 너무 상대 머리 위로 지나가면 목 라인을 놓친다. 상대 팔 프레임을 제거하지 않고 진입하면 밀려나거나 가드를 회복당한다.',
    '목 압박이 강하게 들어가므로 파트너가 탭하면 즉시 풀어야 한다. 턱이나 얼굴을 비트는 식으로 억지로 마무리하지 말고, 혈류를 막는 정확한 각도를 연습해야 한다.',
    'https://www.youtube.com/watch?v=RkFHJHC58qc',
    TRUE,
    47,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('north-south-choke', currval('training_cards_id_seq'));

-- 클락 초크 / clock-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '클락 초크',
    '터틀 상태의 상대 옆에서 깃을 깊게 잡고 몸을 시계 방향으로 돌려 압박하는 초크',
    'SUBMISSION',
    'INTERMEDIATE',
    'TURTLE',
    '상대가 터틀로 웅크리며 백을 내주지 않으려 할 때 깃을 이용해 공격',
    '클락 초크는 상대가 터틀 자세로 방어할 때, 한 손으로 목깃을 깊게 잡고 내 몸을 상대 주위로 돌리며 목을 압박하는 기 초크다. 백 컨트롤을 완전히 얻지 못했더라도 상대의 목깃과 어깨 라인을 제어하면 빠르게 위협을 줄 수 있다. 상대의 몸을 억지로 뒤집는 것이 아니라, 상대가 웅크린 구조를 이용해 목 주변을 조여 들어가는 기술이다.',
    '상대가 테이크다운 방어 후 무릎을 꿇고 터틀로 버티거나, 패스를 당하지 않으려고 네발 자세로 웅크릴 때 사용할 수 있다. 상대가 팔꿈치를 몸에 붙이고 백 훅을 방어하는 경우에도 깃이 열려 있다면 클락 초크 진입이 가능하다. 보우 앤 애로우 초크로 바로 연결하기 어려운 상황에서 좋은 대안이 된다.',
    '상대가 터틀 자세이고 나는 상대의 한쪽 옆 또는 약간 대각선 위에 위치한다. 한 손은 상대 목깃 안쪽으로 깊게 들어가고, 반대 손은 상대 가까운 팔이나 바지, 또는 먼쪽 겨드랑이 라인을 제어한다. 가슴은 상대 등 위에 무겁게 붙인다.',
    '목깃을 깊게 잡은 손의 손목을 상대 목 옆에 단단히 고정한다. 반대 손으로 상대 팔이나 하체를 잡아 상대가 굴러 빠지는 것을 막는다. 이후 내 다리를 뒤로 빼며 몸을 상대 머리 방향으로 시계처럼 돌린다. 회전하면서 가슴 압박을 유지하고, 깃을 잡은 팔꿈치를 조여 상대 목에 압박을 완성한다.',
    '깃 그립이 얕으면 목이 아니라 턱이나 얼굴만 누르게 된다. 몸을 돌릴 때 상대 등과의 밀착을 잃지 않아야 한다. 상대가 앞으로 굴러 탈출하지 못하도록 반대 손 컨트롤을 끝까지 유지한다.',
    '깃을 깊게 잡기 전에 몸부터 돌려 초크가 풀리는 경우가 많다. 상대의 팔이나 하체를 통제하지 않아 롤링 이스케이프를 허용하기도 한다. 체중을 상대 등에서 떼고 앉아버리면 압박이 사라진다.',
    '목깃 초크는 압박이 빠르게 들어갈 수 있으므로 천천히 조여야 한다. 상대가 목을 비틀며 버티면 억지로 끌지 말고 포지션을 다시 잡아야 한다.',
    'https://www.youtube.com/watch?v=Hmz6VXltm9E',
    TRUE,
    48,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('clock-choke', currval('training_cards_id_seq'));

-- 트라이포드 스윕 / tripod-sweep
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '트라이포드 스윕',
    '오픈 가드에서 한쪽 발목을 잡고 반대쪽 엉덩이를 밀어 상대를 뒤로 넘어뜨리는 기본 스윕',
    'SWEEP',
    'BEGINNER',
    'OPEN_GUARD',
    '상대가 서서 가드를 열거나 패스를 준비할 때 발목과 엉덩이를 동시에 제어해 사용',
    '트라이포드 스윕은 오픈 가드에서 상대의 한쪽 발목을 잡고, 내 발로 상대 엉덩이를 밀어 중심을 뒤로 무너뜨리는 기본 스윕이다. 상대가 서 있는 상황에서 특히 자주 사용되며, 단순하지만 타이밍과 거리 조절이 중요하다. 성공하면 바로 일어나 상위 포지션을 잡거나, 상대가 버티는 반응에 따라 싱글 레그 엑스나 델라히바 형태로 연결할 수 있다.',
    '상대가 클로즈드 가드를 열고 일어났거나, 오픈 가드 위에서 자세를 세우고 패스를 준비할 때 사용하기 좋다. 상대의 한쪽 발이 내 손이 닿는 위치에 있고, 상대 엉덩이나 허벅지에 발을 댈 수 있는 거리가 유지되어야 한다. 상대가 뒤로 무게를 두지 않고 앞으로 압박하려 할수록 밀어내는 힘이 효과적으로 작용한다.',
    '오픈 가드에서 바닥에 누워 있거나 앉은 상태로 시작한다. 한 손은 상대의 발목이나 바지 끝을 잡고, 같은 쪽 또는 반대쪽 발은 상대 엉덩이에 둔다. 다른 발은 상대 반대쪽 다리 뒤나 무릎 근처에 위치해 중심을 방해할 준비를 한다.',
    '먼저 상대 한쪽 발목을 단단히 잡아 뒤로 물러나지 못하게 한다. 내 발을 상대 엉덩이에 두고 밀어내며, 다른 발은 상대 반대쪽 다리 뒤를 걸거나 무릎을 제어한다. 발목을 잡은 손은 당기고 엉덩이를 미는 발은 밀어 상대를 뒤로 넘어뜨린다. 상대가 넘어지면 손을 놓지 말고 바로 일어나 상위 포지션으로 따라간다.',
    '발목을 잡은 손과 엉덩이를 미는 발이 반대 방향으로 작용해야 한다. 상대를 넘어뜨린 뒤 누워 있으면 스윕 점유가 늦어지므로 즉시 일어나야 한다. 상대가 균형을 회복하려는 방향을 예측해 반대쪽 다리 방해를 유지한다.',
    '엉덩이를 밀기만 하고 발목을 잡아두지 않아 상대가 쉽게 뒤로 빠지는 경우가 많다. 상대가 넘어졌는데도 상체를 세우지 않아 다시 가드 하위 포지션에 머문다. 다리를 너무 뻗어 무릎이 잠기면 상대가 레그 드래그로 전환할 수 있다.',
    '상대가 넘어질 때 발목을 과하게 비틀지 않도록 주의해야 한다. 스윕 후 일어날 때 상대 다리 사이에 머리를 깊게 넣으면 길로틴 초크를 허용할 수 있다.',
    'https://www.youtube.com/watch?v=sb1qqB60GHU',
    TRUE,
    49,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('tripod-sweep', currval('training_cards_id_seq'));

-- 테크니컬 마운트 이스케이프 / technical-mount-escape
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '테크니컬 마운트 이스케이프',
    '상대가 테크니컬 마운트로 전환했을 때 팔과 무릎 프레임을 이용해 하프 가드나 가드로 회복하는 탈출',
    'ESCAPE',
    'INTERMEDIATE',
    'MOUNT',
    '마운트 아래에서 상대가 한쪽 무릎을 세우고 백이나 암바를 노릴 때 사용',
    '테크니컬 마운트 이스케이프는 상대가 일반 마운트에서 옆으로 돌아앉아 백 테이크, 암바, 칼라 초크를 노리는 순간에 필요한 방어 기술이다. 이 포지션에서는 등을 완전히 내주면 백 컨트롤로 연결되고, 팔을 뻗으면 암바를 허용하기 쉽다. 따라서 팔을 몸에 붙이고 상대의 무릎과 발목 사이 공간을 찾아 하프 가드나 클로즈드 가드로 회복하는 것이 목표다.',
    '상대가 마운트에서 내 팔을 고립시키거나, 내가 옆으로 돌아누운 반응을 따라 테크니컬 마운트로 전환했을 때 사용한다. 상대의 앞쪽 무릎이 내 겨드랑이 근처에 있고 뒤쪽 발이 내 엉덩이 근처에 있을 때, 다리 사이 공간을 만드는 것이 중요하다. 상대가 백을 잡기 전 초기에 반응할수록 성공률이 높다.',
    '나는 하위 마운트에서 옆으로 누운 상태이고, 상대는 한쪽 무릎을 세워 내 등 뒤쪽에 위치한 테크니컬 마운트 상태다. 내 팔꿈치는 몸 가까이에 붙이고, 목과 턱은 초크를 방어할 수 있게 낮춘다. 아래쪽 팔은 상대 무릎이나 정강이 라인에 프레임을 만든다.',
    '먼저 목과 팔을 보호하며 상대가 깊은 그립을 잡지 못하게 한다. 아래쪽 팔꿈치와 무릎을 이용해 상대의 앞쪽 무릎 아래에 프레임을 만든다. 엉덩이를 뒤로 빼며 상대 발목과 무릎 사이에 내 무릎을 끼워 넣고, 다리로 상대의 한쪽 다리를 감아 하프 가드로 만든다. 공간이 더 생기면 반대 다리를 끌어와 클로즈드 가드나 오픈 가드로 회복한다.',
    '팔을 뻗지 않고 팔꿈치를 몸에 붙이는 것이 가장 중요하다. 상대가 백으로 넘어가기 전에 무릎을 먼저 끼워 넣어야 한다. 탈출 중에도 목깃과 팔을 방어해야 하며, 하프 가드를 잡은 뒤에는 바로 언더훅 싸움으로 이어가야 한다.',
    '상대 몸을 밀어내려고 팔을 펴다가 암바를 허용하는 경우가 많다. 등을 완전히 돌려 버리면 백 컨트롤을 내준다. 하프 가드를 잡았다고 멈추면 상대가 다시 마운트로 올라오므로 프레임과 언더훅을 계속 만들어야 한다.',
    '목을 비틀어 억지로 빠져나오려고 하면 초크와 경추 부담이 생길 수 있다. 상대가 암바로 전환하는 순간에는 팔을 당기기보다 무릎을 모으고 손을 연결해 방어해야 한다.',
    'https://www.youtube.com/watch?v=9icekBSQUaQ',
    TRUE,
    50,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('technical-mount-escape', currval('training_cards_id_seq'));

-- 니 온 벨리 이스케이프 / knee-on-belly-escape
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '니 온 벨리 이스케이프',
    '상대의 니 온 벨리 압박에서 프레임과 엉덩이 이동을 이용해 가드 또는 하프 가드로 회복하는 탈출',
    'ESCAPE',
    'BEGINNER',
    'SIDE_CONTROL',
    '상대가 사이드 컨트롤에서 무릎을 배 위에 올리고 압박하며 마운트나 서브미션을 노릴 때 사용',
    '니 온 벨리 이스케이프는 상대가 무릎을 내 복부나 명치 라인에 올려 체중을 싣는 상황에서, 무릎을 손으로 밀기보다 프레임과 몸의 각도를 이용해 공간을 만드는 탈출이다. 상대의 무릎 압박을 정면으로 버티면 호흡이 막히고 팔이 벌어져 서브미션을 허용하기 쉽다. 핵심은 상대 무릎과 골반 사이에 프레임을 만들고, 엉덩이를 빼며 내 무릎을 다시 안쪽으로 넣는 것이다.',
    '상대가 사이드 컨트롤에서 니 온 벨리로 올라와 점수를 얻거나, 내 팔 반응을 이용해 암바와 칼라 초크를 노릴 때 사용한다. 상대가 체중을 무겁게 싣는 순간에는 바로 밀기보다 호흡을 안정시키고 프레임을 먼저 만들어야 한다. 상대의 발이 바닥에 넓게 벌어져 있을수록 한쪽 방향으로 엉덩이를 빼 공간을 만들기 좋다.',
    '나는 바닥에 누워 있고 상대의 한쪽 무릎이 내 배 위에 올라와 있다. 한 팔은 상대의 무릎이나 정강이 라인에 프레임을 만들고, 다른 팔은 상대의 상체 또는 어깨 쪽 프레임으로 거리를 조절한다. 턱은 당기고 팔꿈치는 몸에서 과하게 벌어지지 않게 한다.',
    '먼저 양손으로 무릎을 밀어내기보다 팔꿈치와 전완으로 상대 무릎 라인에 프레임을 만든다. 숨을 내쉬며 엉덩이를 상대 무릎이 향한 반대 방향으로 빼고, 동시에 가까운 무릎을 안쪽으로 끼워 넣는다. 내 정강이가 상대와 나 사이에 들어오면 프레임을 유지하며 가드로 회복한다. 상대가 무게를 앞으로 싣는다면 언더훅이나 싱글 레그 형태로 일어나는 선택지도 만들 수 있다.',
    '상대 무릎을 손힘으로만 밀면 팔이 펴져 공격을 허용한다. 엉덩이 이동과 무릎 삽입이 동시에 일어나야 한다. 프레임은 상대를 멀리 밀기 위한 것이 아니라 내가 움직일 공간을 만들기 위한 것이다.',
    '배 위의 압박이 답답해서 양팔로 상대를 밀다가 암바나 기무라를 허용하는 경우가 많다. 엉덩이를 움직이지 않고 상체만 비틀면 공간이 생기지 않는다. 무릎을 넣은 뒤 바로 쉬어버리면 상대가 다시 니 온 벨리나 마운트로 전환한다.',
    '복부 압박이 강하므로 호흡을 참지 말고 짧게 내쉬며 움직여야 한다. 갈비뼈나 복부 부상이 있는 경우 파트너에게 강도를 낮춰달라고 말하고 천천히 드릴해야 한다.',
    'https://www.youtube.com/watch?v=XKKlVjWoVHk',
    TRUE,
    51,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('knee-on-belly-escape', currval('training_cards_id_seq'));

-- 베이스볼 배트 초크 / baseball-bat-choke
INSERT INTO training_cards (
    title,
    summary,
    topic,
    level,
    position,
    situation_summary,
    description,
    situation_description,
    starting_position_description,
    flow_description,
    key_points,
    common_mistakes,
    cautions,
    youtube_url,
    active,
    display_order,
    created_at,
    updated_at
) VALUES (
    '베이스볼 배트 초크',
    '양손을 야구 배트 잡듯이 깃에 배치하고 회전 압박으로 목을 조르는 기 초크',
    'SUBMISSION',
    'INTERMEDIATE',
    'SIDE_CONTROL',
    '사이드 컨트롤, 니 온 벨리, 또는 가드에서 상대의 목깃을 깊게 잡을 수 있을 때 사용',
    '베이스볼 배트 초크는 양손을 야구 배트를 잡는 것처럼 상대의 깃에 배치한 뒤, 몸을 회전시키며 상대 목 양쪽을 압박하는 초크다. 기 주짓수에서 활용도가 높고, 탑 포지션뿐 아니라 하위 포지션에서 기습적으로 사용할 수도 있다. 손목의 방향과 팔꿈치 간격이 정확해야 하며, 그립을 잡은 뒤에는 몸 전체가 회전하며 압박을 만들어야 한다.',
    '니 온 벨리나 사이드 컨트롤에서 상대가 목을 방어하지 않고 팔로 내 몸을 밀 때 사용하기 좋다. 상대의 칼라가 열려 있고 양손을 목 주변에 배치할 수 있으면 세팅할 수 있다. 가드 하위에서 상대가 패스를 강하게 들어올 때 역으로 그립을 숨겨 잡고 초크를 노리는 형태도 가능하지만, 실패하면 포지션을 잃을 수 있으므로 초보자는 탑 포지션 세팅부터 익히는 것이 좋다.',
    '사이드 컨트롤 또는 니 온 벨리에서 상대 목깃을 한 손으로 깊게 잡고, 반대 손은 첫 번째 손과 가까운 방향으로 깃을 잡는다. 두 손은 야구 배트 그립처럼 붙어 있고, 팔꿈치는 너무 넓게 벌어지지 않게 한다. 상체는 상대 가슴 위에 안정적으로 위치한다.',
    '먼저 첫 번째 손을 상대 목 뒤쪽 깃에 깊게 넣는다. 반대 손을 가까운 깃에 추가로 배치해 배트 그립을 만든다. 그립이 완성되면 머리를 낮추고 상대가 팔을 끼워 방어하지 못하게 한다. 이후 몸을 상대 머리 쪽 또는 노스 사우스 방향으로 회전시키며 팔꿈치를 모으고 손목 방향을 유지한다. 목 양쪽 압박이 걸리면 천천히 체중을 실어 마무리한다.',
    '그립이 얕으면 초크가 아니라 목 앞쪽 압박만 된다. 양손 간격이 너무 넓으면 힘이 분산되므로 손을 가깝게 배치한다. 마무리는 손목을 비트는 것이 아니라 몸 회전과 팔꿈치 조임으로 만들어야 한다.',
    '그립을 잡자마자 팔 힘으로만 당겨 상대에게 방어 시간을 주는 경우가 많다. 상체 압박을 풀고 회전하면 상대가 팔을 끼워 방어한다. 하위 포지션에서 무리하게 시도하다가 사이드 컨트롤을 완전히 허용할 수 있다.',
    '깃 초크는 탭이 빠르게 나올 수 있으므로 천천히 압박해야 한다. 손목과 손가락에 부담이 갈 수 있으므로 그립을 무리하게 비틀지 않는다.',
    'https://www.youtube.com/watch?v=s14Iiq41uAc',
    TRUE,
    52,
    NOW(),
    NOW()
);
INSERT INTO tmp_training_card_seed_map (seed_key, card_id)
VALUES ('baseball-bat-choke', currval('training_cards_id_seq'));

-- knee-cut-pass -> toreando-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'toreando-pass'
WHERE source.seed_key = 'knee-cut-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- knee-cut-pass -> over-under-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'over-under-pass'
WHERE source.seed_key = 'knee-cut-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- knee-cut-pass -> smash-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'smash-pass'
WHERE source.seed_key = 'knee-cut-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- knee-cut-pass -> spider-guard-control
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 3, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'spider-guard-control'
WHERE source.seed_key = 'knee-cut-pass'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- arm-triangle-choke -> side-control-americana
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'side-control-americana'
WHERE source.seed_key = 'arm-triangle-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- arm-triangle-choke -> north-south-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'north-south-choke'
WHERE source.seed_key = 'arm-triangle-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- arm-triangle-choke -> ezekiel-from-mount
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'ezekiel-from-mount'
WHERE source.seed_key = 'arm-triangle-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- arm-triangle-choke -> d-arce-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 3, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'd-arce-choke'
WHERE source.seed_key = 'arm-triangle-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- side-control-americana -> kimura-from-closed-guard
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'kimura-from-closed-guard'
WHERE source.seed_key = 'side-control-americana'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- side-control-americana -> arm-triangle-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'arm-triangle-choke'
WHERE source.seed_key = 'side-control-americana'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- side-control-americana -> north-south-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'north-south-choke'
WHERE source.seed_key = 'side-control-americana'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- ko-uchi-gari -> double-leg-takedown
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'double-leg-takedown'
WHERE source.seed_key = 'ko-uchi-gari'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- ko-uchi-gari -> single-leg-takedown
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-takedown'
WHERE source.seed_key = 'ko-uchi-gari'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- ko-uchi-gari -> ankle-pick
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'ankle-pick'
WHERE source.seed_key = 'ko-uchi-gari'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- ko-uchi-gari -> osoto-gari
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 3, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'osoto-gari'
WHERE source.seed_key = 'ko-uchi-gari'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- deep-half-guard-sweep -> half-guard-underhook-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'half-guard-underhook-sweep'
WHERE source.seed_key = 'deep-half-guard-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- deep-half-guard-sweep -> over-under-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'over-under-pass'
WHERE source.seed_key = 'deep-half-guard-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- deep-half-guard-sweep -> smash-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'smash-pass'
WHERE source.seed_key = 'deep-half-guard-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- deep-half-guard-sweep -> knee-cut-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 3, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'knee-cut-pass'
WHERE source.seed_key = 'deep-half-guard-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- north-south-choke -> d-arce-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'd-arce-choke'
WHERE source.seed_key = 'north-south-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- north-south-choke -> arm-triangle-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'arm-triangle-choke'
WHERE source.seed_key = 'north-south-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- north-south-choke -> side-control-americana
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'side-control-americana'
WHERE source.seed_key = 'north-south-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- clock-choke -> turtle-sit-out-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'turtle-sit-out-escape'
WHERE source.seed_key = 'clock-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- clock-choke -> bow-and-arrow-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'bow-and-arrow-choke'
WHERE source.seed_key = 'clock-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- clock-choke -> rear-naked-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'rear-naked-choke'
WHERE source.seed_key = 'clock-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- clock-choke -> d-arce-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 3, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'd-arce-choke'
WHERE source.seed_key = 'clock-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- tripod-sweep -> single-leg-x-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'single-leg-x-sweep'
WHERE source.seed_key = 'tripod-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- tripod-sweep -> de-la-riva-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'de-la-riva-sweep'
WHERE source.seed_key = 'tripod-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- tripod-sweep -> spider-guard-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'spider-guard-sweep'
WHERE source.seed_key = 'tripod-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- tripod-sweep -> x-guard-sweep
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 3, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'x-guard-sweep'
WHERE source.seed_key = 'tripod-sweep'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- technical-mount-escape -> upa-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'upa-escape'
WHERE source.seed_key = 'technical-mount-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- technical-mount-escape -> elbow-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'elbow-escape'
WHERE source.seed_key = 'technical-mount-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- technical-mount-escape -> ezekiel-from-mount
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'ezekiel-from-mount'
WHERE source.seed_key = 'technical-mount-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- technical-mount-escape -> knee-on-belly-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 3, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'knee-on-belly-escape'
WHERE source.seed_key = 'technical-mount-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- knee-on-belly-escape -> elbow-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'elbow-escape'
WHERE source.seed_key = 'knee-on-belly-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- knee-on-belly-escape -> upa-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'upa-escape'
WHERE source.seed_key = 'knee-on-belly-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- knee-on-belly-escape -> toreando-pass
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'toreando-pass'
WHERE source.seed_key = 'knee-on-belly-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- knee-on-belly-escape -> side-control-americana
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 3, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'side-control-americana'
WHERE source.seed_key = 'knee-on-belly-escape'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- baseball-bat-choke -> cross-collar-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 0, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'cross-collar-choke'
WHERE source.seed_key = 'baseball-bat-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- baseball-bat-choke -> bow-and-arrow-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 1, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'bow-and-arrow-choke'
WHERE source.seed_key = 'baseball-bat-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- baseball-bat-choke -> clock-choke
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 2, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'clock-choke'
WHERE source.seed_key = 'baseball-bat-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;

-- baseball-bat-choke -> knee-on-belly-escape
INSERT INTO training_card_relations (
    card_id,
    related_card_id,
    display_order,
    created_at,
    updated_at
)
SELECT source.card_id, related.card_id, 3, NOW(), NOW()
FROM tmp_training_card_seed_map source
JOIN tmp_training_card_seed_map related
  ON related.seed_key = 'knee-on-belly-escape'
WHERE source.seed_key = 'baseball-bat-choke'
ON CONFLICT (card_id, related_card_id) DO NOTHING;


COMMIT;
