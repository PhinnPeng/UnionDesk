-- 确保 login_log 具备统一登录审计所需列（兼容未执行 V202605230001 或 ALTER 未生效的环境）

SET @table_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'login_log'
);

SET @ddl := IF(@table_exists = 0,
    'CREATE TABLE login_log (
        id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
        subject_id BIGINT UNSIGNED DEFAULT NULL,
        portal_type VARCHAR(16) NOT NULL,
        client_code VARCHAR(64) NULL,
        sid CHAR(36) NULL,
        event_type VARCHAR(32) NOT NULL DEFAULT ''LOGIN'',
        business_domain_id BIGINT UNSIGNED DEFAULT NULL,
        login_name VARCHAR(128) NOT NULL,
        login_identifier_masked VARCHAR(128) NULL,
        login_identifier_type VARCHAR(16) NULL,
        ip VARCHAR(64) DEFAULT NULL,
        user_agent VARCHAR(255) DEFAULT NULL,
        result VARCHAR(16) NOT NULL,
        fail_reason VARCHAR(255) DEFAULT NULL,
        created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
        PRIMARY KEY (id),
        KEY idx_login_log_subject_created (subject_id, created_at),
        KEY idx_login_log_name_created (login_name, created_at),
        KEY idx_login_log_portal_created (portal_type, created_at),
        KEY idx_login_log_domain_created (business_domain_id, created_at),
        KEY idx_login_log_client_created (client_code, created_at),
        KEY idx_login_log_sid (sid),
        CONSTRAINT fk_login_log_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id),
        CONSTRAINT fk_login_log_subject FOREIGN KEY (subject_id) REFERENCES identity_subject (id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'login_log'
      AND COLUMN_NAME = 'client_code'
);
SET @ddl := IF(@table_exists > 0 AND @col_exists = 0,
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
