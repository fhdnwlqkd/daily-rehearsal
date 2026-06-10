CREATE TABLE context_slot (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slot_key VARCHAR(64) NOT NULL,
    label VARCHAR(100) NOT NULL,
    slot_type VARCHAR(30) NOT NULL,
    extraction_hint VARCHAR(1000),
    follow_up_hint VARCHAR(300),
    default_literal_value VARCHAR(500),
    default_context_slot_option_id BIGINT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_context_slot_slot_key UNIQUE (slot_key)
);

CREATE TABLE context_slot_schema (
    id BIGINT NOT NULL AUTO_INCREMENT,
    schema_key VARCHAR(64) NOT NULL,
    name VARCHAR(100) NOT NULL,
    max_follow_up_attempt INT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_context_slot_schema_schema_key UNIQUE (schema_key)
);

CREATE INDEX idx_context_slot_schema_active
    ON context_slot_schema (active);

CREATE TABLE context_slot_option (
    id BIGINT NOT NULL AUTO_INCREMENT,
    context_slot_id BIGINT NOT NULL,
    option_key VARCHAR(64) NOT NULL,
    label VARCHAR(100) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_context_slot_option_slot_option_key UNIQUE (context_slot_id, option_key),
    CONSTRAINT fk_context_slot_option_context_slot
        FOREIGN KEY (context_slot_id) REFERENCES context_slot (id)
);

CREATE TABLE context_slot_schema_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    context_slot_schema_id BIGINT NOT NULL,
    context_slot_id BIGINT NOT NULL,
    required_level VARCHAR(30) NOT NULL,
    priority INT NOT NULL,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_context_slot_schema_item_schema_slot UNIQUE (
        context_slot_schema_id,
        context_slot_id
    ),
    CONSTRAINT fk_context_slot_schema_item_schema
        FOREIGN KEY (context_slot_schema_id) REFERENCES context_slot_schema (id),
    CONSTRAINT fk_context_slot_schema_item_slot
        FOREIGN KEY (context_slot_id) REFERENCES context_slot (id)
);

CREATE INDEX idx_context_slot_schema_item_schema_active_priority
    ON context_slot_schema_item (context_slot_schema_id, active, priority);

ALTER TABLE context_slot
    ADD CONSTRAINT fk_context_slot_default_option
        FOREIGN KEY (default_context_slot_option_id) REFERENCES context_slot_option (id);
