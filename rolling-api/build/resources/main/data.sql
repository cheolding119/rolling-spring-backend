-- Mock 테스트 데이터 (삭제 시 이 파일만 제거하면 됩니다)

-- 테스트용 사용자 (암장 owner)
INSERT INTO users (id, nickname, phone, social_provider, social_id, created_at, updated_at)
SELECT 1, '테스트관리자', '010-1234-5678', 'KAKAO', 'test_owner_001', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM users WHERE id = 1);

-- 서울 지역 클라이밍 짐 데이터
INSERT INTO gyms (id, owner_id, name, address, latitude, longitude, phone, description, price_info, is_visible, created_at, updated_at)
SELECT 1, 1, '더클라임 강남점', '서울 강남구 테헤란로 123', 37.5012, 127.0396, '02-1234-5678', '강남 최대 규모의 클라이밍 짐입니다. 초보자부터 고급자까지 다양한 루트를 제공합니다.', '일일권 20,000원 / 월정액 150,000원', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM gyms WHERE id = 1);

INSERT INTO gyms (id, owner_id, name, address, latitude, longitude, phone, description, price_info, is_visible, created_at, updated_at)
SELECT 2, 1, '클라이밍파크 홍대점', '서울 마포구 홍익로 45', 37.5563, 126.9220, '02-2345-6789', '홍대 중심부에 위치한 클라이밍 짐. 볼더링 전문.', '일일권 18,000원 / 월정액 130,000원', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM gyms WHERE id = 2);

INSERT INTO gyms (id, owner_id, name, address, latitude, longitude, phone, description, price_info, is_visible, created_at, updated_at)
SELECT 3, 1, '락앤락 클라이밍', '서울 송파구 올림픽로 300', 37.5145, 127.1050, '02-3456-7890', '송파구 대표 클라이밍 센터. 리드/볼더링 겸용.', '일일권 22,000원 / 월정액 160,000원', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM gyms WHERE id = 3);

INSERT INTO gyms (id, owner_id, name, address, latitude, longitude, phone, description, price_info, is_visible, created_at, updated_at)
SELECT 4, 1, '정상클라이밍 신촌점', '서울 서대문구 연세로 50', 37.5598, 126.9425, '02-4567-8901', '신촌역 3분 거리. 초보자 강습 전문.', '일일권 15,000원 / 월정액 120,000원', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM gyms WHERE id = 4);

INSERT INTO gyms (id, owner_id, name, address, latitude, longitude, phone, description, price_info, is_visible, created_at, updated_at)
SELECT 5, 1, '버티컬월드 성수점', '서울 성동구 성수일로 77', 37.5445, 127.0560, '02-5678-9012', '성수동 핫플레이스! 카페형 클라이밍 짐.', '일일권 25,000원 / 월정액 180,000원', true, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM gyms WHERE id = 5);

-- 운영 스케줄 데이터
INSERT INTO gym_schedules (gym_id, day_of_week, start_time, end_time, class_name)
SELECT 1, 'MONDAY', '10:00:00', '12:00:00', '초급반'
WHERE NOT EXISTS (SELECT 1 FROM gym_schedules WHERE gym_id = 1 AND day_of_week = 'MONDAY' AND class_name = '초급반');

INSERT INTO gym_schedules (gym_id, day_of_week, start_time, end_time, class_name)
SELECT 1, 'MONDAY', '14:00:00', '16:00:00', '중급반'
WHERE NOT EXISTS (SELECT 1 FROM gym_schedules WHERE gym_id = 1 AND day_of_week = 'MONDAY' AND class_name = '중급반');

INSERT INTO gym_schedules (gym_id, day_of_week, start_time, end_time, class_name)
SELECT 1, 'WEDNESDAY', '19:00:00', '21:00:00', '직장인반'
WHERE NOT EXISTS (SELECT 1 FROM gym_schedules WHERE gym_id = 1 AND day_of_week = 'WEDNESDAY' AND class_name = '직장인반');

INSERT INTO gym_schedules (gym_id, day_of_week, start_time, end_time, class_name)
SELECT 2, 'TUESDAY', '10:00:00', '12:00:00', '입문반'
WHERE NOT EXISTS (SELECT 1 FROM gym_schedules WHERE gym_id = 2 AND day_of_week = 'TUESDAY' AND class_name = '입문반');

INSERT INTO gym_schedules (gym_id, day_of_week, start_time, end_time, class_name)
SELECT 2, 'SATURDAY', '14:00:00', '17:00:00', '주말 집중반'
WHERE NOT EXISTS (SELECT 1 FROM gym_schedules WHERE gym_id = 2 AND day_of_week = 'SATURDAY' AND class_name = '주말 집중반');

-- 이미지 데이터
INSERT INTO gym_images (gym_id, image_url, sort_order)
SELECT 1, 'https://example.com/images/gym1_main.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM gym_images WHERE gym_id = 1 AND sort_order = 1);

INSERT INTO gym_images (gym_id, image_url, sort_order)
SELECT 1, 'https://example.com/images/gym1_interior.jpg', 2
WHERE NOT EXISTS (SELECT 1 FROM gym_images WHERE gym_id = 1 AND sort_order = 2);

INSERT INTO gym_images (gym_id, image_url, sort_order)
SELECT 2, 'https://example.com/images/gym2_main.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM gym_images WHERE gym_id = 2 AND sort_order = 1);

INSERT INTO gym_images (gym_id, image_url, sort_order)
SELECT 3, 'https://example.com/images/gym3_main.jpg', 1
WHERE NOT EXISTS (SELECT 1 FROM gym_images WHERE gym_id = 3 AND sort_order = 1);

-- 편의시설 데이터
INSERT INTO gym_amenities (gym_id, amenity_type, available)
SELECT 1, 'PARKING', true
WHERE NOT EXISTS (SELECT 1 FROM gym_amenities WHERE gym_id = 1 AND amenity_type = 'PARKING');

INSERT INTO gym_amenities (gym_id, amenity_type, available)
SELECT 1, 'SHOWER', true
WHERE NOT EXISTS (SELECT 1 FROM gym_amenities WHERE gym_id = 1 AND amenity_type = 'SHOWER');

INSERT INTO gym_amenities (gym_id, amenity_type, available)
SELECT 1, 'LOCKER', true
WHERE NOT EXISTS (SELECT 1 FROM gym_amenities WHERE gym_id = 1 AND amenity_type = 'LOCKER');

INSERT INTO gym_amenities (gym_id, amenity_type, available)
SELECT 2, 'SHOWER', true
WHERE NOT EXISTS (SELECT 1 FROM gym_amenities WHERE gym_id = 2 AND amenity_type = 'SHOWER');

INSERT INTO gym_amenities (gym_id, amenity_type, available)
SELECT 2, 'WIFI', true
WHERE NOT EXISTS (SELECT 1 FROM gym_amenities WHERE gym_id = 2 AND amenity_type = 'WIFI');

INSERT INTO gym_amenities (gym_id, amenity_type, available)
SELECT 3, 'PARKING', true
WHERE NOT EXISTS (SELECT 1 FROM gym_amenities WHERE gym_id = 3 AND amenity_type = 'PARKING');

INSERT INTO gym_amenities (gym_id, amenity_type, available)
SELECT 3, 'CAFE', true
WHERE NOT EXISTS (SELECT 1 FROM gym_amenities WHERE gym_id = 3 AND amenity_type = 'CAFE');