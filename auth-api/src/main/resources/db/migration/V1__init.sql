-- Flyway applies this once, on the first start of auth-api, and records it in flyway_schema_history.
-- To change the schema later, add V2__<description>.sql - never edit a migration that has already run.

CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL
);

CREATE TABLE processing_log (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL REFERENCES users (id),
    input_text  TEXT        NOT NULL,
    output_text TEXT        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- PostgreSQL doesn't index foreign key columns automatically; this keeps "logs of user X" lookups fast.
CREATE INDEX idx_processing_log_user_id ON processing_log (user_id);
