SET @exist := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'auth_login_session'
      AND COLUMN_NAME = 'account_type'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE auth_login_session ADD COLUMN account_type VARCHAR(16) NULL AFTER client_code',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'auth_login_session'
      AND COLUMN_NAME = 'session_type'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE auth_login_session ADD COLUMN session_type VARCHAR(32) NOT NULL DEFAULT ''login'' AFTER account_type',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE auth_login_session s
LEFT JOIN user_account u ON u.id = s.user_id
SET s.account_type = CASE
    WHEN u.account_type = 'customer' THEN 'customer'
    ELSE 'staff'
END
WHERE s.account_type IS NULL;

SET @exist := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'auth_login_session'
      AND CONSTRAINT_NAME = 'fk_auth_login_session_user'
);
SET @sql := IF(
    @exist = 1,
    'ALTER TABLE auth_login_session DROP FOREIGN KEY fk_auth_login_session_user',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @exist := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'auth_login_session'
      AND COLUMN_NAME = 'account_type'
);
SET @sql := IF(
    @exist = 1,
    'ALTER TABLE auth_login_session MODIFY account_type VARCHAR(16) NOT NULL',
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
      AND CONSTRAINT_NAME = 'chk_auth_login_session_account_type'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE auth_login_session ADD CONSTRAINT chk_auth_login_session_account_type CHECK (account_type IN (''staff'', ''customer''))',
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
      AND CONSTRAINT_NAME = 'chk_auth_login_session_session_type'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE auth_login_session ADD CONSTRAINT chk_auth_login_session_session_type CHECK (session_type IN (''login'', ''password_reset''))',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
