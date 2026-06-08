-- 释放老版本中因笔误或旧结构占用的权限代码，确保新建的按钮唯一键不冲突，且不违反按钮必须有权限代码的 CHECK 约束
UPDATE iam_admin_menu SET permission_code = 'platform.home.query' WHERE id = 56 AND permission_code = 'platform.login_log.read';
UPDATE iam_admin_menu SET permission_code = 'platform.audit_log.query_legacy' WHERE id = 64 AND permission_code = 'platform.audit_log.read';

-- 隐藏旧的独立“审计日志”菜单（使其 hidden=1 且保持 status=1），以防与新功能“日志审计”混淆且不破坏权限快照断言
UPDATE iam_admin_menu SET status = 1, hidden = 1 WHERE id = 51;

-- Insert log audit catalog
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, order_no, icon, status, required)
SELECT 'PLATFORM-AUDIT-CATALOG', 'catalog', 'platform', '日志审计', NULL, 7, 'SafetyCertificateOutlined', 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-AUDIT-CATALOG');

-- Insert login log menu
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-LOGIN-LOG-MENU', 'menu', 'platform', '登录日志', '/platform/log/login-log', './platform/log/login-log', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-AUDIT-CATALOG' LIMIT 1),
       1, 'LoginOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-LOGIN-LOG-MENU');

-- Insert operation log menu
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-OP-LOG-MENU', 'menu', 'platform', '操作日志', '/platform/log/operation-log', './platform/log/operation-log', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-AUDIT-CATALOG' LIMIT 1),
       2, 'ProfileOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-OP-LOG-MENU');

-- Map platform.login_log.read button
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-LOGIN-LOG-QUERY-BTN', 'button', 'platform', '查询登录日志', NULL, NULL, 'platform.login_log.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-LOGIN-LOG-MENU' LIMIT 1),
       1, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-LOGIN-LOG-QUERY-BTN');

-- Map platform.audit_log.read button
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-OP-LOG-QUERY-BTN', 'button', 'platform', '查询操作日志', NULL, NULL, 'platform.audit_log.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-OP-LOG-MENU' LIMIT 1),
       1, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-OP-LOG-QUERY-BTN');

-- Standardize codes
UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN ('PLATFORM-LOGIN-LOG-QUERY-BTN', 'PLATFORM-OP-LOG-QUERY-BTN');

-- Bind menus to super_admin, platform_admin, security_auditor roles
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code IN ('PLATFORM-AUDIT-CATALOG', 'PLATFORM-LOGIN-LOG-MENU', 'PLATFORM-OP-LOG-MENU')
WHERE r.code IN ('super_admin', 'platform_admin', 'security_auditor');

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.permission_code IN ('platform.login_log.read', 'platform.audit_log.read')
WHERE r.code IN ('super_admin', 'platform_admin', 'security_auditor');
