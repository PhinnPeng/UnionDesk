DROP TRIGGER IF EXISTS trg_user_global_role_account_type_insert;
DROP TRIGGER IF EXISTS trg_user_global_role_account_type_update;
DROP TRIGGER IF EXISTS trg_user_domain_role_account_type_insert;
DROP TRIGGER IF EXISTS trg_user_domain_role_account_type_update;
DROP TRIGGER IF EXISTS trg_user_account_account_type_update;

-- 先删除约束，再更新数据
ALTER TABLE iam_resource
    DROP CHECK chk_iam_resource_type;

ALTER TABLE iam_resource
    DROP CHECK chk_iam_resource_api_fields;

-- 现在可以安全地更新数据
UPDATE iam_resource
SET resource_type = 'action'
WHERE resource_type = 'api';

ALTER TABLE iam_resource
    ADD CONSTRAINT chk_iam_resource_type CHECK (resource_type IN ('menu', 'action'));

ALTER TABLE iam_resource
    ADD CONSTRAINT chk_iam_resource_action_fields CHECK (
        (resource_type = 'action' AND http_method IS NOT NULL AND path_pattern IS NOT NULL)
        OR (resource_type = 'menu' AND http_method IS NULL)
    );

INSERT INTO iam_resource (resource_type, resource_code, resource_name, client_scope, http_method, path_pattern, status)
VALUES
    ('action', 'action.iam.me.permission_snapshot.read', 'Read Permission Snapshot', 'all', 'GET', '/api/v1/iam/me/permission-snapshot', 1)
ON DUPLICATE KEY UPDATE
    resource_name = VALUES(resource_name),
    client_scope = VALUES(client_scope),
    http_method = VALUES(http_method),
    path_pattern = VALUES(path_pattern),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO iam_role_resource (role_id, resource_id)
SELECT r.id, ir.id
FROM role r
JOIN iam_resource ir ON ir.resource_code = 'action.iam.me.permission_snapshot.read'
WHERE r.code IN ('super_admin', 'domain_admin', 'agent', 'customer')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    resource_id = VALUES(resource_id);
