-- 业务域控制台：权限码统一 + 概览/通用 catalog 与按钮

-- 1. iam_permission 重命名
UPDATE iam_permission
SET code = 'platform.domain.control.entry',
    name = '进入控制台',
    description = '进入业务域控制台并查看详情',
    action_code = 'platform_domain_control_entry',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.admin.detail.read';

UPDATE iam_permission
SET code = 'platform.domain.control.general.update',
    name = '更新基础信息',
    description = '更新业务域基础信息',
    action_code = 'platform_domain_control_general_update',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.admin.update';

UPDATE iam_permission
SET code = 'platform.domain.control.general.delete',
    name = '删除业务域',
    description = '软删除业务域',
    action_code = 'platform_domain_control_general_delete',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'platform.domain.control.deleted';

-- 2. 新增 platform.domain.control.overview（UI 门控，无 API）
INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.overview', '查看概览数据', '查看业务域控制台概览 Tab',
       'platform', 'domain', 'platform_domain_control_overview', NULL, NULL, 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.overview');

-- 3. iam_admin_menu permission_code 同步
UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.entry',
    name = '进入控制台',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'domain.admin.detail.read';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.general.update',
    name = '更新基础信息',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'domain.admin.update';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.general.delete',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'platform.domain.control.deleted';

-- 4. 详情 menu 改名
UPDATE iam_admin_menu
SET name = '业务域控制台',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-DOMAIN-DETAIL'
   OR (route_path = '/platform/domains/detail' AND node_type = 'menu');

-- 5. 新增隐藏 catalog：概览 / 通用
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-OVERVIEW', 'catalog', 'platform', '概览', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       1, NULL, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-OVERVIEW');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-GENERAL', 'catalog', 'platform', '通用', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       2, NULL, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-GENERAL');

-- 6. 概览按钮
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-OVERVIEW', 'button', 'platform', '查看概览数据', 'platform.domain.control.overview',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-OVERVIEW' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.overview' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code = 'TMP-BTN-DOMAIN-OVERVIEW';

-- 6b. 通用下按钮（INSERT 兜底 + reparent）
INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-GENERAL-UPDATE', 'button', 'platform', '更新基础信息', 'platform.domain.control.general.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-GENERAL' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu
    WHERE permission_code = 'platform.domain.control.general.update' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-GENERAL-DELETE', 'button', 'platform', '删除业务域', 'platform.domain.control.general.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-GENERAL' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu
    WHERE permission_code = 'platform.domain.control.general.delete' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN ('TMP-BTN-DOMAIN-GENERAL-UPDATE', 'TMP-BTN-DOMAIN-GENERAL-DELETE');

-- 7. 更新/删除按钮 reparent 到「通用」catalog
UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-GENERAL'
SET btn.parent_id = catalog.id,
    btn.name = CASE
        WHEN btn.permission_code = 'platform.domain.control.general.update' THEN '更新基础信息'
        WHEN btn.permission_code = 'platform.domain.control.general.delete' THEN '删除业务域'
        ELSE btn.name
    END,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code IN (
      'platform.domain.control.general.update',
      'platform.domain.control.general.delete'
  );

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-GENERAL'
SET btn.permission_code = 'platform.domain.control.general.update',
    btn.name = '更新基础信息',
    btn.parent_id = catalog.id,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code IN ('domain.admin.update');

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-GENERAL'
SET btn.permission_code = 'platform.domain.control.general.delete',
    btn.name = '删除业务域',
    btn.parent_id = catalog.id,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code IN ('domain.admin.delete', 'platform.domain.control.deleted');

-- 8. 角色权限迁移：旧 permission 持有者绑定新 permission
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT rp.role_id, p_new.id, CURRENT_TIMESTAMP(3)
FROM iam_role_permission rp
JOIN iam_permission p_old ON p_old.id = rp.permission_id
    AND p_old.code IN (
        'domain.admin.detail.read',
        'domain.admin.update',
        'platform.domain.control.deleted'
    )
JOIN iam_permission p_new ON p_new.code IN (
        'platform.domain.control.entry',
        'platform.domain.control.general.update',
        'platform.domain.control.general.delete'
    ) AND p_new.status = 1
WHERE (
    (p_old.code = 'domain.admin.detail.read' AND p_new.code = 'platform.domain.control.entry')
    OR (p_old.code = 'domain.admin.update' AND p_new.code = 'platform.domain.control.general.update')
    OR (p_old.code = 'platform.domain.control.deleted' AND p_new.code = 'platform.domain.control.general.delete')
);

-- 9. super_admin 绑定 overview + 新 menu
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code = 'platform.domain.control.overview' AND p.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code IN ('PLATFORM-DOMAIN-OVERVIEW', 'PLATFORM-DOMAIN-GENERAL')
    OR m.permission_code = 'platform.domain.control.overview'
WHERE r.code = 'super_admin'
  AND m.status = 1;

-- 10. 已为业务域控制台授权的角色，补齐概览/通用 catalog + 按钮
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_admin_menu m ON (
    m.code IN ('PLATFORM-DOMAIN-OVERVIEW', 'PLATFORM-DOMAIN-GENERAL')
    OR m.permission_code IN (
        'platform.domain.control.overview',
        'platform.domain.control.general.update',
        'platform.domain.control.general.delete'
    )
)
WHERE m.status = 1;

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT DISTINCT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_permission p ON p.code = 'platform.domain.control.overview' AND p.status = 1;
