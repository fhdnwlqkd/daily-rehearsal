CREATE TABLE rehearsal_session (
    session_id VARCHAR(36) NOT NULL,
    situation_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    context_status VARCHAR(50) NOT NULL,
    follow_up_attempt INT NOT NULL DEFAULT 0,
    selected_outfit_id VARCHAR(100) NULL,
    current_turn INT NOT NULL DEFAULT 0,
    max_turn INT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    completed_at DATETIME(6) NULL,
    PRIMARY KEY (session_id),
    INDEX idx_rehearsal_session_status (status),
    INDEX idx_rehearsal_session_context_status (context_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE session_context_value (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(36) NOT NULL,
    context_key VARCHAR(100) NOT NULL,
    context_value TEXT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_session_context_value_session_key UNIQUE (session_id, context_key),
    CONSTRAINT fk_session_context_value_session
        FOREIGN KEY (session_id) REFERENCES rehearsal_session (session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE simulation_turn (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(36) NOT NULL,
    turn_no INT NOT NULL,
    opponent_line_status VARCHAR(30) NOT NULL,
    opponent_line TEXT NULL,
    opponent_line_failure_reason TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_simulation_turn_session_turn UNIQUE (session_id, turn_no),
    CONSTRAINT fk_simulation_turn_session
        FOREIGN KEY (session_id) REFERENCES rehearsal_session (session_id) ON DELETE CASCADE,
    INDEX idx_simulation_turn_opponent_line_status (opponent_line_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE simulation_turn_attempt (
    id BIGINT NOT NULL AUTO_INCREMENT,
    simulation_turn_id BIGINT NOT NULL,
    attempt_no INT NOT NULL,
    user_transcript TEXT NOT NULL,
    evaluation_status VARCHAR(30) NOT NULL,
    success BOOLEAN NULL,
    feedback TEXT NULL,
    fallback BOOLEAN NULL,
    evaluation_failure_reason TEXT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_simulation_turn_attempt_turn_attempt UNIQUE (simulation_turn_id, attempt_no),
    CONSTRAINT fk_simulation_turn_attempt_turn
        FOREIGN KEY (simulation_turn_id) REFERENCES simulation_turn (id) ON DELETE CASCADE,
    INDEX idx_simulation_turn_attempt_evaluation_status (evaluation_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE rehearsal_result (
    id BIGINT NOT NULL AUTO_INCREMENT,
    session_id VARCHAR(36) NOT NULL,
    video_url VARCHAR(500) NULL,
    ticket_summary TEXT NULL,
    download_url VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_rehearsal_result_session UNIQUE (session_id),
    CONSTRAINT fk_rehearsal_result_session
        FOREIGN KEY (session_id) REFERENCES rehearsal_session (session_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
