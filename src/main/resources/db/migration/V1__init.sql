CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL       PRIMARY KEY,
    email       VARCHAR(255)    NOT NULL,
    password    VARCHAR(255)    NOT NULL,
    username    VARCHAR(50)     NOT NULL,
    role        VARCHAR(20)     NOT NULL DEFAULT 'USER',
    created_at  TIMESTAMP       DEFAULT NOW(),
    updated_at  TIMESTAMP,
    CONSTRAINT uk_users_email    UNIQUE (email),
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT chk_users_role    CHECK (role IN ('USER', 'ADMIN'))
    );

CREATE TABLE IF NOT EXISTS refresh_token (
    id          BIGSERIAL       PRIMARY KEY,
    key_email   VARCHAR(255)    NOT NULL,
    value       VARCHAR(500)    NOT NULL,
    CONSTRAINT fk_refresh_token_user_email
    FOREIGN KEY (key_email) REFERENCES users (email)
    ON DELETE CASCADE ON UPDATE CASCADE
    );

CREATE INDEX IF NOT EXISTS idx_refresh_token_value ON refresh_token (value);
CREATE INDEX IF NOT EXISTS idx_refresh_token_key   ON refresh_token (key_email);

INSERT INTO users (email, password, username, role, created_at)
VALUES
    ('admin@example.com', '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt6aBECnMkKrCtRDbCtCICnw/Bm', '관리자', 'ADMIN', NOW()),
    ('user@example.com',  '$2a$10$N.zmdr9k7uOCQb376NoUnutj8iAt6aBECnMkKrCtRDbCtCICnw/Bm', '테스트유저', 'USER',  NOW())
    ON CONFLICT (email) DO NOTHING;
