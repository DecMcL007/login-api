-- If a unique constraint was created implicitly, drop it.
-- Replace constraint/index names with the actual ones from \d users
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_password_hash_key;
DROP INDEX IF EXISTS users_password_hash_idx;

-- Optional: add useful indexes
CREATE INDEX IF NOT EXISTS idx_users_username ON users (username);
CREATE INDEX IF NOT EXISTS idx_users_email ON users (email);