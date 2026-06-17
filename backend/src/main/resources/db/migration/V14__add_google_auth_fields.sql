ALTER TABLE user_account ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20) DEFAULT 'LOCAL' NOT NULL;
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);
ALTER TABLE user_account ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
ALTER TABLE user_account ALTER COLUMN password DROP NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_user_account_google_id
    ON user_account(google_id)
    WHERE google_id IS NOT NULL;
