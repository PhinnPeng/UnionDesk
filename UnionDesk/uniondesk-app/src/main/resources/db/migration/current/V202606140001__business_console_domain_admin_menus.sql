-- US-S2-E2-00: business scope 按钮 domain.* 化 + 收敛 domain_admin 平台菜单绑定

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.menu.read', '查看业务域菜单', '查看 business scope 菜单树', 'domain', 'domain.menu', 'domain_menu_read', 'GET', '/api/v1/iam/menus/tree', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.menu.read');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.menu.create', '创建业务域菜单', '创建 business scope 菜单或按钮', 'domain', 'domain.menu', 'domain_menu_create', 'POST', '/api/v1/iam/menus', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.menu.create');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.menu.update', '更新业务域菜单', '更新 business scope 菜单或按钮', 'domain', 'domain.menu', 'domain_menu_update', 'PUT', '/api/v1/iam/menus/*', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.menu.update');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'domain.menu.delete', '删除业务域菜单', '删除 business scope 菜单或按钮', 'domain', 'domain.menu', 'domain_menu_delete', 'DELETE', '/api/v1/iam/menus/*', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'domain.menu.delete');

UPDATE iam_admin_menu
SET permission_code = 'domain.menu.read', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.menu.read';

UPDATE iam_admin_menu
SET permission_code = 'domain.menu.create', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.menu.create';

UPDATE iam_admin_menu
SET permission_code = 'domain.menu.update', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.menu.update';

UPDATE iam_admin_menu
SET permission_code = 'domain.menu.delete', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.menu.delete';

UPDATE iam_admin_menu
SET permission_code = 'domain.role.read', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.role.read';

UPDATE iam_admin_menu
SET permission_code = 'domain.role.create', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.role.create';

UPDATE iam_admin_menu
SET permission_code = 'domain.role.update', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.role.update';

UPDATE iam_admin_menu
SET permission_code = 'domain.role.delete', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.role.delete';

UPDATE iam_admin_menu
SET permission_code = 'domain.role.permission.read', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.role_permission.read';

UPDATE iam_admin_menu
SET permission_code = 'domain.role.permission.update', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business' AND node_type = 'button' AND permission_code = 'platform.role_permission.update';

DELETE rmr
FROM iam_admin_role_menu_relation rmr
INNER JOIN iam_admin_menu m ON m.id = rmr.menu_id
INNER JOIN role r ON r.id = rmr.role_id
WHERE r.code = 'domain_admin' AND m.scope = 'platform';

DELETE rp
FROM iam_role_permission rp
INNER JOIN iam_permission p ON p.id = rp.permission_id
INNER JOIN role r ON r.id = rp.role_id
WHERE r.code = 'domain_admin' AND p.permission_scope = 'platform';

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
CROSS JOIN iam_permission p
WHERE r.code = 'domain_admin'
  AND p.code IN ('domain.menu.read', 'domain.menu.create', 'domain.menu.update', 'domain.menu.delete')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );
