-- US-S2-06：日志权限迁移 platform.log.* / platform.domain.control.* + 域详情 catalog

-- 1. 平台 permission 重命名
UPDATE iam_permission
SET code = 'platform.log.audit.read',
    name = '查看平台操作日志',
    description = '查看平台级操作审计日志列表',
    permission_scope = 'platform',
    resource_code = 'log',
    action_code = 'platform_log_audit_read',
    path_pattern = '/api/v1/admin/audit-logs',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'platform.audit_log.read';

UPDATE iam_permission
SET code = 'platform.log.login.read',
    name = '查看平台登录日志',
    description = '查看平台级登录日志列表',
    permission_scope = 'platform',
    resource_code = 'log',
    action_code = 'platform_log_login_read',
    path_pattern = '/api/v1/admin/login-logs',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'platform.login_log.read';

-- 2. 域内 permission 重命名
UPDATE iam_permission
SET code = 'platform.domain.control.audit_log.read',
    name = '查看操作日志',
    description = '查看业务域内操作审计日志列表',
    permission_scope = 'platform',
    resource_code = 'domain_control_audit_log',
    action_code = 'platform_domain_control_audit_log_read',
    path_pattern = '/api/v1/admin/domains/*/audit-logs',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.audit_log.read';

UPDATE iam_permission
SET code = 'platform.domain.control.login_log.read',
    name = '查看登录日志',
    description = '查看业务域内登录日志列表',
    permission_scope = 'platform',
    resource_code = 'domain_control_login_log',
    action_code = 'platform_domain_control_login_log_read',
    path_pattern = '/api/v1/admin/domains/*/login-logs',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.login_log.read';

-- 3. 平台菜单按钮 permission_code 同步
UPDATE iam_admin_menu
SET permission_code = 'platform.log.audit.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'platform.audit_log.read' AND node_type = 'button';

UPDATE iam_admin_menu
SET permission_code = 'platform.log.login.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'platform.login_log.read' AND node_type = 'button';

-- 4. 域详情 hidden catalog「操作日志」+ 按钮
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-CONTROL-AUDIT-LOG', 'catalog', 'platform', '操作日志', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       14, NULL, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-AUDIT-LOG');

UPDATE iam_admin_menu
SET name = '操作日志',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1) AS detail_menu),
    scope = 'platform',
    hidden = 1,
    order_no = 14,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-DOMAIN-CONTROL-AUDIT-LOG';

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-AUDIT-LOG-READ', 'button', 'platform', '查看操作日志', 'platform.domain.control.audit_log.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-AUDIT-LOG' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.audit_log.read' AND node_type = 'button'
);

-- 5. 域详情 hidden catalog「登录日志」+ 按钮
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-CONTROL-LOGIN-LOG', 'catalog', 'platform', '登录日志', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       15, NULL, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-LOGIN-LOG');

UPDATE iam_admin_menu
SET name = '登录日志',
    parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1) AS detail_menu),
    scope = 'platform',
    hidden = 1,
    order_no = 15,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-DOMAIN-CONTROL-LOGIN-LOG';

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-LOGIN-LOG-READ', 'button', 'platform', '查看登录日志', 'platform.domain.control.login_log.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-LOGIN-LOG' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.login_log.read' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN ('TMP-BTN-DOMAIN-AUDIT-LOG-READ', 'TMP-BTN-DOMAIN-LOGIN-LOG-READ');

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-CONTROL-AUDIT-LOG'
SET btn.parent_id = catalog.id,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code = 'platform.domain.control.audit_log.read';

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-CONTROL-LOGIN-LOG'
SET btn.parent_id = catalog.id,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code = 'platform.domain.control.login_log.read';

-- 6. super_admin 权限与菜单
INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.log.audit.read',
    'platform.log.login.read',
    'platform.domain.control.audit_log.read',
    'platform.domain.control.login_log.read'
) AND p.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code IN (
    'PLATFORM-DOMAIN-CONTROL-AUDIT-LOG',
    'PLATFORM-DOMAIN-CONTROL-LOGIN-LOG'
) AND m.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button' AND btn.status = 1
    AND btn.permission_code IN (
        'platform.log.audit.read',
        'platform.log.login.read',
        'platform.domain.control.audit_log.read',
        'platform.domain.control.login_log.read'
    )
WHERE r.code = 'super_admin';

-- 7. 已有域详情菜单权限的角色补绑
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_admin_menu m ON (
    m.code IN ('PLATFORM-DOMAIN-CONTROL-AUDIT-LOG', 'PLATFORM-DOMAIN-CONTROL-LOGIN-LOG')
    OR m.permission_code IN (
        'platform.domain.control.audit_log.read',
        'platform.domain.control.login_log.read'
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
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.audit_log.read',
    'platform.domain.control.login_log.read'
) AND p.status = 1;
