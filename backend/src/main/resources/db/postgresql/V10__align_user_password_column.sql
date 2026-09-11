-- The application has always mapped the password hash to users.password.
-- Preserve a legacy value if password was left blank, then remove the
-- redundant NOT NULL password_hash column that otherwise breaks new inserts.
UPDATE users
SET password = password_hash
WHERE (password IS NULL OR BTRIM(password) = '')
  AND password_hash IS NOT NULL
  AND BTRIM(password_hash) <> '';

ALTER TABLE users DROP COLUMN IF EXISTS password_hash;
