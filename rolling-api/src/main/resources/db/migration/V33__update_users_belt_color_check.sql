ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_belt_color_check;

ALTER TABLE users
    ADD CONSTRAINT users_belt_color_check CHECK (
        belt_color IN (
            'WHITE',
            'GRAY_WHITE',
            'GRAY',
            'GRAY_BLACK',
            'YELLOW_WHITE',
            'YELLOW',
            'YELLOW_BLACK',
            'ORANGE_WHITE',
            'ORANGE',
            'ORANGE_BLACK',
            'GREEN_WHITE',
            'GREEN',
            'GREEN_BLACK',
            'BLUE',
            'PURPLE',
            'BROWN',
            'BLACK'
        )
    );
