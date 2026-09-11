-- Fail safely if legacy case variants would collapse to the same canonical
-- address. Operators must resolve those accounts before rerunning Flyway.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM users
        WHERE email IS NOT NULL AND BTRIM(email) <> ''
        GROUP BY LOWER(BTRIM(email))
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate normalized user emails must be resolved before migration';
    END IF;
END $$;

UPDATE users
SET email = NULLIF(LOWER(BTRIM(email)), '')
WHERE email IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_users_email_normalized ON users(email);
