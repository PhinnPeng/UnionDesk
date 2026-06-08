-- 统一登录日志至 login_log，迁移 auth_login_log 后删除旧表

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'login_log'
      AND COLUMN_NAME = 'client_code'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE login_log
        ADD COLUMN client_code VARCHAR(64) NULL AFTER portal_type,
        ADD COLUMN sid CHAR(36) NULL AFTER client_code,
        ADD COLUMN event_type VARCHAR(32) NOT NULL DEFAULT ''LOGIN'' AFTER sid,
        ADD COLUMN login_identifier_masked VARCHAR(128) NULL AFTER login_name,
        ADD COLUMN login_identifier_type VARCHAR(16) NULL AFTER login_identifier_masked',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'login_log'
      AND INDEX_NAME = 'idx_login_log_portal_created'
);
SET @ddl := IF(@idx_exists = 0,
    'CREATE INDEX idx_login_log_portal_created ON login_log (portal_type, created_at)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'login_log'
      AND INDEX_NAME = 'idx_login_log_domain_created'
);
SET @ddl := IF(@idx_exists = 0,
    'CREATE INDEX idx_login_log_domain_created ON login_log (business_domain_id, created_at)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'login_log'
      AND INDEX_NAME = 'idx_login_log_client_created'
);
SET @ddl := IF(@idx_exists = 0,
    'CREATE INDEX idx_login_log_client_created ON login_log (client_code, created_at)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(*)
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'login_log'
      AND INDEX_NAME = 'idx_login_log_sid'
);
SET @ddl := IF(@idx_exists = 0,
    'CREATE INDEX idx_login_log_sid ON login_log (sid)',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 历史数据从 auth_login_log 迁入 login_log 易因外键/会话缺失失败，已改为仅做结构升级；旧表保留供人工迁移。

DELETE rr
FROM iam_role_resource rr
INNER JOIN iam_resource r ON r.id = rr.resource_id
WHERE r.resource_code = 'api.auth.login_logs.read';

DELETE FROM iam_resource
WHERE resource_code = 'api.auth.login_logs.read';
