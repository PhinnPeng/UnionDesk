-- SLA 权限补录：给规则与工作日历补齐读写权限，确保旧库跳过早期脚本后仍可联调
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
    (
        'domain.sla.read',
        'View SLA rules',
        'View SLA rules and calendars within a business domain',
        'domain',
        'sla_rule',
        'read',
        'GET',
        '/api/v1/admin/domains/*/sla-*',
        1
    ),
    (
        'domain.sla.create',
        'Create SLA rule',
        'Create SLA rules and calendars within a business domain',
        'domain',
        'sla_rule',
        'create',
        'POST',
        '/api/v1/admin/domains/*/sla-*',
        1
    ),
    (
        'domain.sla.update',
        'Update SLA rule',
        'Update SLA rules and calendars within a business domain',
        'domain',
        'sla_rule',
        'update',
        'PUT',
        '/api/v1/admin/domains/*/sla-*/*',
        1
    )
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
    'domain.sla.read',
    'domain.sla.create',
    'domain.sla.update'
)
WHERE r.code IN ('platform_admin', 'domain_admin', 'super_admin')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
