ALTER TABLE rehearsal_session
    ADD COLUMN video_url VARCHAR(500) NULL AFTER max_turn,
    ADD COLUMN video_upload_status VARCHAR(30) NOT NULL DEFAULT 'NONE' AFTER video_url,
    ADD COLUMN video_upload_failure_reason TEXT NULL AFTER video_upload_status;
