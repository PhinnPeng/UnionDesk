-- 业务域控制台「通用」：新增业务域状态更新权限与按钮

-- 1. iam_permission
INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.general.update-status', '业务域状态更新', '启用或禁用业务域',
       'platform', 'domain', 'platform_domain_control_general_update_status', NULL, NULL, 1
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.general.update-status'
);

-- 2. 通用 catalog 下按钮（order_no：更新=0，状态=1，删除=2）
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-GENERAL-UPDATE-STATUS', 'button', 'platform', '业务域状态更新',
       'platform.domain.control.general.update-status',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-GENERAL' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu
    WHERE permission_code = 'platform.domain.control.general.update-status' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code = 'TMP-BTN-DOMAIN-GENERAL-UPDATE-STATUS';

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-GENERAL'
SET btn.order_no = 2,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code = 'platform.domain.control.general.delete'
  AND btn.parent_id = catalog.id;

-- 3. super_admin 绑定新 permission + button
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code = 'platform.domain.control.general.update-status' AND p.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.permission_code = 'platform.domain.control.general.update-status'
    AND m.node_type = 'button'
    AND m.status = 1
WHERE r.code = 'super_admin';

-- 4. 兼容：已持有 general.update 的角色同步绑定 update-status
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT rp.role_id, p_new.id, CURRENT_TIMESTAMP(3)
FROM iam_role_permission rp
JOIN iam_permission p_old ON p_old.id = rp.permission_id
    AND p_old.code = 'platform.domain.control.general.update'
JOIN iam_permission p_new ON p_new.code = 'platform.domain.control.general.update-status'
    AND p_new.status = 1;

-- 5. 已授权业务域控制台的角色，补齐新 button 菜单关系
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_admin_menu m ON m.permission_code = 'platform.domain.control.general.update-status'
    AND m.node_type = 'button'
    AND m.status = 1;
