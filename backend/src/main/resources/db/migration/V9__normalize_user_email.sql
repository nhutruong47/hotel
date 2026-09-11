-- Canonical account emails are lowercase and trimmed. Empty legacy values
-- become NULL; H2's existing unique email index then enforces the invariant.
UPDATE users
SET email = NULLIF(LOWER(TRIM(email)), '')
WHERE email IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_normalized ON users(email);
