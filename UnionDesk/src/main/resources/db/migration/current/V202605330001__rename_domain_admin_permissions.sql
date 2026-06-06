-- 重命名 domain.admin.list.read / domain.admin.create + 新增 platform.domain.control.read

-- 1. 重命名 iam_permission
UPDATE iam_permission
SET code = 'platform.domain.list.read',
    name = '查看业务域列表',
    action_code = 'platform_domain_list_read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.admin.list.read';

UPDATE iam_permission
SET code = 'platform.domain.create',
    name = '新建业务域',
    action_code = 'platform_domain_create',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.admin.create';

-- 2. 新增 platform.domain.control.read（进入业务域控制台，UI 按钮权限，无后端 API）
INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.read', '进入业务域控制台', '进入业务域控制台页面',
       'platform', 'domain', 'platform_domain_control_read', NULL, NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.read');

-- 3. 重命名 iam_admin_menu 中的按钮 permission_code
UPDATE iam_admin_menu
SET permission_code = 'platform.domain.list.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'domain.admin.list.read';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.create',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'domain.admin.create';

-- 4. 新增"进入业务域控制台"按钮（挂在"业务域列表"菜单下，order_no=0）
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-CONTROL', 'button', 'platform', '进入业务域控制台', 'platform.domain.control.read',
       (SELECT id FROM iam_admin_menu WHERE route_path = '/platform/domains' AND node_type = 'menu' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.read' AND node_type = 'button');

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code = 'TMP-BTN-DOMAIN-CONTROL';

-- 5. 更新 iam_role_permission：将旧权限迁移到新权限
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT rp.role_id, p_new.id, CURRENT_TIMESTAMP(3)
FROM iam_role_permission rp
JOIN iam_permission p_old ON p_old.id = rp.permission_id
    AND p_old.code IN ('domain.admin.list.read', 'domain.admin.create')
JOIN iam_permission p_new ON p_new.code IN ('platform.domain.list.read', 'platform.domain.create')
    AND p_new.status = 1;

-- 6. super_admin 绑定 platform.domain.control.read
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code = 'platform.domain.control.read' AND p.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.permission_code = 'platform.domain.control.read' AND btn.status = 1
WHERE r.code = 'super_admin';
