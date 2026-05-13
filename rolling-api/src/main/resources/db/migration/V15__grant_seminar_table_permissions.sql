GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE seminars TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE seminar_applications TO rolling_admin;

GRANT USAGE, SELECT, UPDATE ON SEQUENCE seminars_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE seminar_applications_id_seq TO rolling_admin;
