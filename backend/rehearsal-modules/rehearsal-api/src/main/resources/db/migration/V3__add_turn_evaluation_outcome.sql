ALTER TABLE simulation_turn_attempt
    ADD COLUMN evaluation_outcome VARCHAR(30) NULL AFTER evaluation_status;

UPDATE simulation_turn_attempt
SET evaluation_outcome = CASE
    WHEN success = TRUE THEN 'ACCEPTED'
    WHEN success = FALSE THEN 'RETRY_REQUIRED'
    ELSE NULL
END;
