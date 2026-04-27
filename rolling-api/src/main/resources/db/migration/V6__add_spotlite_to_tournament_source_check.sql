ALTER TABLE tournaments
    DROP CONSTRAINT IF EXISTS tournaments_source_check;

ALTER TABLE tournaments
    ADD CONSTRAINT tournaments_source_check
        CHECK (
            source IS NULL
            OR source IN (
                'STREET_JIU_JITSU',
                'KOREA_JIU',
                'HEROES_OF_JIU_JITSU',
                'SPOTLITE',
                'MANUAL'
            )
        );
