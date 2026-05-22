ALTER TABLE training_log_entries
    ADD COLUMN gym_attendance BOOLEAN NULL,
    ADD COLUMN condition INT NULL;

ALTER TABLE training_log_entries
    ADD CONSTRAINT training_log_entries_condition_check
        CHECK (condition IS NULL OR (condition >= 1 AND condition <= 5));
