ALTER TABLE users
    ADD COLUMN IF NOT EXISTS provider VARCHAR(20) NOT NULL DEFAULT 'LOCAL';

ALTER TABLE users
    ADD CONSTRAINT chk_users_provider CHECK (provider IN ('LOCAL', 'GOOGLE'));

-- 기존 데이터는 모두 LOCAL로 설정 (이미 DEFAULT 'LOCAL'로 처리됨)
COMMENT ON COLUMN users.provider IS '가입 경로: LOCAL(이메일/비밀번호), GOOGLE(구글 OAuth2)';