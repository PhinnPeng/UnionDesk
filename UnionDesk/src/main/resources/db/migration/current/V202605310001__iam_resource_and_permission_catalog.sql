-- US-S1-07：IAM 资源 API 与权限码目录 JWT 路径对齐

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT
    'platform.admin_permission_codes.read',
    '查看权限码目录',
    '查看管理端权限码定义列表',
    'platform',
    'platform.admin_permission_codes',
    'read',
    'GET',
    '/api/v1/iam/admin-permission-codes',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission WHERE code = 'platform.admin_permission_codes.read'
);

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT
    'platform.iam.resource.read',
    '查看 IAM 资源',
    '查看 IAM 资源列表',
    'platform',
    'platform.iam.resource',
    'read',
    'GET',
    '/api/v1/iam/resources',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission WHERE code = 'platform.iam.resource.read'
);

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT
    'platform.iam.resource.create',
    '创建 IAM 资源',
    '创建 IAM 资源',
    'platform',
    'platform.iam.resource',
    'create',
    'POST',
    '/api/v1/iam/resources',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission WHERE code = 'platform.iam.resource.create'
);

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT
    'platform.iam.resource.update',
    '更新 IAM 资源',
    '更新 IAM 资源',
    'platform',
    'platform.iam.resource',
    'update',
    'PUT',
    '/api/v1/iam/resources/*',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission WHERE code = 'platform.iam.resource.update'
);

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT
    'platform.iam.role_resource.read',
    '查看角色 IAM 资源',
    '查看角色绑定的 IAM 资源',
    'platform',
    'platform.iam.role_resource',
    'read',
    'GET',
    '/api/v1/iam/roles/*/resources',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission WHERE code = 'platform.iam.role_resource.read'
);

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT
    'platform.iam.role_resource.update',
    '更新角色 IAM 资源',
    '更新角色绑定的 IAM 资源',
    'platform',
    'platform.iam.role_resource',
    'update',
    'PUT',
    '/api/v1/iam/roles/*/resources',
    1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission WHERE code = 'platform.iam.role_resource.update'
);

UPDATE iam_permission
SET http_method = 'PUT',
    path_pattern = '/api/v1/iam/users/*'
WHERE code = 'domain.user.update'
  AND path_pattern IS NULL;

UPDATE iam_permission
SET http_method = 'DELETE',
    path_pattern = '/api/v1/iam/users/*'
WHERE code = 'domain.user.remove'
  AND path_pattern IS NULL;

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT rp.role_id, catalog_perm.id
FROM iam_role_permission rp
JOIN iam_permission menu_perm ON menu_perm.id = rp.permission_id
    AND menu_perm.code = 'platform.menu.read'
JOIN iam_permission catalog_perm ON catalog_perm.code = 'platform.admin_permission_codes.read'
WHERE NOT EXISTS (
    SELECT 1
    FROM iam_role_permission existing
    WHERE existing.role_id = rp.role_id
      AND existing.permission_id = catalog_perm.id
);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT role_row.id, perm.id
FROM role role_row
JOIN iam_permission perm ON perm.code IN (
    'platform.iam.resource.read',
    'platform.iam.resource.create',
    'platform.iam.resource.update',
    'platform.iam.role_resource.read',
    'platform.iam.role_resource.update',
    'platform.admin_permission_codes.read'
)
WHERE role_row.code = 'super_admin'
  AND NOT EXISTS (
    SELECT 1
    FROM iam_role_permission existing
    WHERE existing.role_id = role_row.id
      AND existing.permission_id = perm.id
);
