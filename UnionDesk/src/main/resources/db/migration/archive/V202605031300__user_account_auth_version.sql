-- 为 user_account 补齐 auth_version，供会话失效和权限变更场景使用
SET @exist := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'user_account'
      AND COLUMN_NAME = 'auth_version'
);
SET @sql := IF(
    @exist = 0,
    'ALTER TABLE user_account ADD COLUMN auth_version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''权限版本号'' AFTER account_type',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
