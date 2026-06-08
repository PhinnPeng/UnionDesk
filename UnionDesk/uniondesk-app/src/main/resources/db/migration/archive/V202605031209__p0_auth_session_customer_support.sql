SET @exist := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'auth_login_session'
      AND COLUMN_NAME = 'session_type'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE auth_login_session ADD COLUMN session_type VARCHAR(32) NOT NULL DEFAULT ''login'' AFTER sid',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'auth_login_session'
      AND CONSTRAINT_NAME = 'fk_auth_login_session_user'
);
SET @sql := IF(
    @exist > 0,
    'ALTER TABLE auth_login_session DROP FOREIGN KEY fk_auth_login_session_user',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'auth_login_session'
      AND INDEX_NAME = 'idx_auth_login_session_type_status_expires'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE auth_login_session ADD KEY idx_auth_login_session_type_status_expires (session_type, session_status, expires_at)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE auth_login_session
SET session_type = 'login'
WHERE session_type IS NULL;
