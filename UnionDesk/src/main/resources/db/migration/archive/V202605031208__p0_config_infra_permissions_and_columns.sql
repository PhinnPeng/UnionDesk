SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sla_rule' AND COLUMN_NAME = 'name');
SET @sql := IF(@exist = 0, 'ALTER TABLE sla_rule ADD COLUMN name VARCHAR(128) NOT NULL DEFAULT '''' AFTER business_domain_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sla_rule' AND COLUMN_NAME = 'priority_level_id');
SET @sql := IF(@exist = 0, 'ALTER TABLE sla_rule ADD COLUMN priority_level_id BIGINT UNSIGNED DEFAULT NULL AFTER ticket_type_id', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'sla_rule' AND CONSTRAINT_NAME = 'fk_sla_rule_priority_level');
SET @sql := IF(@exist = 0, 'ALTER TABLE sla_rule ADD CONSTRAINT fk_sla_rule_priority_level FOREIGN KEY (priority_level_id) REFERENCES ticket_priority_level (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'file_attachment' AND COLUMN_NAME = 'status');
SET @sql := IF(@exist = 0, 'ALTER TABLE file_attachment ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT ''confirmed'' AFTER checksum', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

INSERT INTO iam_permission (
    code,
    name,
    description,
    permission_scope,
    resource_code,
    action_code,
    http_method,
    path_pattern,
    status
)
VALUES
    ('platform.login_log.read', 'View platform login logs', 'View platform login logs', 'platform', 'login_log', 'read', 'GET', '/api/v1/admin/login-logs', 1),
    ('domain.config.read', 'View domain config', 'View business domain config', 'domain', 'domain_config', 'read', 'GET', '/api/v1/admin/domains/*/config', 1),
    ('domain.config.update', 'Update domain config', 'Update business domain config', 'domain', 'domain_config', 'update', 'PUT', '/api/v1/admin/domains/*/config', 1),
    ('platform.system_config.read', 'View system config', 'View platform system config', 'platform', 'system_config', 'read', 'GET', '/api/v1/admin/system-config', 1),
    ('platform.system_config.update', 'Update system config', 'Update platform system config', 'platform', 'system_config', 'update', 'PUT', '/api/v1/admin/system-config', 1),
    ('domain.sla.read', 'View SLA rules', 'View SLA rules', 'domain', 'sla_rule', 'read', 'GET', '/api/v1/admin/domains/*/sla-rules', 1),
    ('domain.sla.create', 'Create SLA rule', 'Create SLA rule', 'domain', 'sla_rule', 'create', 'POST', '/api/v1/admin/domains/*/sla-rules', 1),
    ('domain.sla.update', 'Update SLA rule', 'Update SLA rule', 'domain', 'sla_rule', 'update', 'PUT', '/api/v1/admin/domains/*/sla-rules/*', 1),
    ('domain.notification_template.read', 'View notification templates', 'View notification templates', 'domain', 'notification_template', 'read', 'GET', '/api/v1/admin/domains/*/notification-templates', 1),
    ('domain.notification_template.update', 'Update notification template', 'Update notification template', 'domain', 'notification_template', 'update', 'PUT', '/api/v1/admin/domains/*/notification-templates/*', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    permission_scope = VALUES(permission_scope),
    resource_code = VALUES(resource_code),
    action_code = VALUES(action_code),
    http_method = VALUES(http_method),
    path_pattern = VALUES(path_pattern),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.login_log.read',
    'domain.config.read',
    'domain.config.update',
    'platform.system_config.read',
    'platform.system_config.update',
    'domain.sla.read',
    'domain.sla.create',
    'domain.sla.update',
    'domain.notification_template.read',
    'domain.notification_template.update'
)
WHERE r.code IN ('platform_admin', 'domain_admin', 'super_admin')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
