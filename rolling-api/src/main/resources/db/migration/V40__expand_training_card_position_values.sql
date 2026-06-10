-- 작성일시: 2026-06-10 15:10:00 +09:00
ALTER TABLE training_cards
    DROP CONSTRAINT training_cards_position_check;

ALTER TABLE training_cards
    ADD CONSTRAINT training_cards_position_check CHECK (
        position IN (
            'STANDING',
            'GUARD',
            'CLOSED_GUARD',
            'OPEN_GUARD',
            'HALF_GUARD',
            'SIDE_CONTROL',
            'MOUNT',
            'BACK',
            'TURTLE',
            'LEG_ENTANGLEMENT'
        )
    );
