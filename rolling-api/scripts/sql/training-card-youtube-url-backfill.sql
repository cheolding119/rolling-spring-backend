-- 훈련카드 YouTube URL 보정용 SQL
-- 목적:
-- 1. 기존 DB에 이미 적재된 placeholder youtube_url 값을 실제 재생 가능한 URL로 교체한다.
-- 2. 현재는 재생 불가가 확인된 카드만 선별해서 보정한다.

BEGIN;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=pzgrGCdYl_4',
    updated_at = NOW()
WHERE title = '클로즈드 가드 암바'
  AND youtube_url = 'https://www.youtube.com/watch?v=armbar123';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=k54daRsX3co',
    updated_at = NOW()
WHERE title = '트라이앵글 초크'
  AND youtube_url = 'https://www.youtube.com/watch?v=triangle123';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=4q_TMA7usDw',
    updated_at = NOW()
WHERE title = '크로스 칼라 초크'
  AND youtube_url = 'https://www.youtube.com/watch?v=crosscollar';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=YvJ-PVhKiWI',
    updated_at = NOW()
WHERE title = '힙 범프 스윕'
  AND youtube_url = 'https://www.youtube.com/watch?v=hipbump';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=avrcVQ2mQIw',
    updated_at = NOW()
WHERE title = '라소 가드 스윕'
  AND youtube_url = 'https://www.youtube.com/watch?v=lasso123';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=KjYuOdMKZfs',
    updated_at = NOW()
WHERE title = '싱글 레그 엑스 스윕'
  AND youtube_url = 'https://www.youtube.com/watch?v=slx123';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=iUqtvbW3mJQ',
    updated_at = NOW()
WHERE title = '우파 이스케이프'
  AND youtube_url = 'https://www.youtube.com/watch?v=upa123';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=wxNAEByjOoA',
    updated_at = NOW()
WHERE title = '더블 레그 테이크다운'
  AND youtube_url = 'https://www.youtube.com/watch?v=doubleleg';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=JDJYWBbzspQ',
    updated_at = NOW()
WHERE title = '가드 풀'
  AND youtube_url = 'https://www.youtube.com/watch?v=guardpull';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=4hAY12ghrGk',
    updated_at = NOW()
WHERE title = '오버 언더 패스'
  AND youtube_url = 'https://www.youtube.com/results?search_query=over+under+pass+bjj';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=BZGvAiMzUAM',
    updated_at = NOW()
WHERE title = '스파이더 가드 컨트롤'
  AND youtube_url = 'https://www.youtube.com/results?search_query=spider+guard+control+bjj';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=dpuKJPsq84g',
    updated_at = NOW()
WHERE title = '오모플라타'
  AND youtube_url = 'https://www.youtube.com/results?search_query=omoplata+bjj';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=ZZ15qbqTcTA',
    updated_at = NOW()
WHERE title = '하프 가드 언더훅 스윕'
  AND youtube_url = 'https://www.youtube.com/results?search_query=half+guard+underhook+sweep+bjj';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=1B_mWDcIybw',
    updated_at = NOW()
WHERE title = '라소 가드 컨트롤'
  AND youtube_url = 'https://www.youtube.com/results?search_query=lasso+guard+bjj';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=F0Qz-DcqxJw',
    updated_at = NOW()
WHERE title = '펜듈럼 스윕'
  AND youtube_url = 'https://www.youtube.com/results?search_query=pendulum+sweep+bjj';

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=mVkKOPNGvjA',
    updated_at = NOW()
WHERE title = '클로즈드 가드 기무라'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=QYJxHtTuOAw',
    updated_at = NOW()
WHERE title = '시저스 스윕'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=LU7bEi96ink',
    updated_at = NOW()
WHERE title = '플라워 스윕'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=EM4xc2XSBrE',
    updated_at = NOW()
WHERE title = '싱글 레그 테이크다운'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=G499sHSEk3o',
    updated_at = NOW()
WHERE title = '오소토 가리 (큰바깥후리기)'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=8huFMFJPsZM',
    updated_at = NOW()
WHERE title = '엑스 패스'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=p9iBzTpXGD4',
    updated_at = NOW()
WHERE title = '스매시 패스'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=zoabs7H2ZN8',
    updated_at = NOW()
WHERE title = '스파이더 가드 스윕'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=ZBLTVlH3Rb0',
    updated_at = NOW()
WHERE title = '델라히바 스윕'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=oe-xUmRLcUg',
    updated_at = NOW()
WHERE title = '엑스 가드 스윕'
  AND youtube_url IS NULL;

UPDATE training_cards
SET youtube_url = 'https://www.youtube.com/watch?v=FyqaWxyMtLc',
    updated_at = NOW()
WHERE title = '엘보우 이스케이프'
  AND youtube_url IS NULL;

COMMIT;
