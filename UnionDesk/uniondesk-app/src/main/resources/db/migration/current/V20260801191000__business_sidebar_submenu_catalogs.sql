-- 业务端：系统设置 / 客户管理 改回侧栏父级展开子菜单（catalog + children）
-- 页面路由仍走 /domain/settings/*、/domain/customers/*；不再依赖页内 Hub 二级栏

-- A) 「系统设置」改为目录
UPDATE iam_admin_menu
SET node_type = 'catalog',
    route_path = NULL,
    component_key = NULL,
    name = '系统设置',
    icon = 'SettingOutlined',
    parent_id = NULL,
    order_no = 50,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-SETTINGS';

-- B) 系统设置下分组目录
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-SETTINGS-ORG', 'catalog', 'business', '组织成员', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS' LIMIT 1),
       20, 'TeamOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-ORG');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-SETTINGS-FEATURES', 'catalog', 'business', '功能配置', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS' LIMIT 1),
       30, 'ControlOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-FEATURES');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-SETTINGS-SECURITY', 'catalog', 'business', '安全与审计', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS' LIMIT 1),
       40, 'SafetyCertificateOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-SECURITY');

-- C) 通用设置挂到系统设置下（域信息置顶：直接作为系统设置第一项）
UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS' LIMIT 1) t),
    route_path = '/domain/settings/basic',
    component_key = './domain/basic',
    name = '通用设置',
    order_no = 10,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-BASIC';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-ORG' LIMIT 1) t),
    route_path = '/domain/settings/members',
    component_key = './domain/members',
    name = '员工管理',
    order_no = 10,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-MEMBERS';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-ORG' LIMIT 1) t),
    route_path = '/domain/settings/roles',
    component_key = './domain/roles',
    name = '角色管理',
    order_no = 20,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-ROLES';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-ORG' LIMIT 1) t),
    route_path = '/domain/settings/onboarding',
    component_key = './domain/onboarding',
    name = '入域管理',
    order_no = 30,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-ONBOARDING';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-FEATURES' LIMIT 1) t),
    route_path = '/domain/settings/config',
    component_key = './domain/config',
    name = '参数配置',
    order_no = 10,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-CONFIG';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-FEATURES' LIMIT 1) t),
    route_path = '/domain/settings/blockwords',
    component_key = './domain/blockwords',
    name = '屏蔽词库',
    order_no = 20,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-BLOCKWORDS';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-FEATURES' LIMIT 1) t),
    route_path = '/domain/settings/notifications',
    component_key = './domain/notifications',
    name = '通知配置',
    order_no = 30,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-NOTIFICATIONS';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-SECURITY' LIMIT 1) t),
    route_path = '/domain/settings/audit-logs',
    component_key = './domain/audit-logs',
    name = '操作日志',
    order_no = 10,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-AUDIT-LOGS';

UPDATE iam_admin_menu
SET parent_id = (SELECT id FROM (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-SECURITY' LIMIT 1) t),
    route_path = '/domain/settings/login-logs',
    component_key = './domain/login-logs',
    name = '登录日志',
    order_no = 20,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-LOGIN-LOGS';

-- D) 「客户管理」改为目录 + 客户列表 / 入域配置
UPDATE iam_admin_menu
SET node_type = 'catalog',
    route_path = NULL,
    component_key = NULL,
    name = '客户管理',
    icon = 'SolutionOutlined',
    parent_id = NULL,
    order_no = 40,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS';

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-CUSTOMERS-LIST', 'menu', 'business', '客户列表', '/domain/customers/list', './domain/customers', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS' LIMIT 1),
       10, 'SolutionOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS-LIST');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-CUSTOMERS-ONBOARDING', 'menu', 'business', '入域配置', '/domain/customers/onboarding', './domain/onboarding', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS' LIMIT 1),
       20, 'UserAddOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS-ONBOARDING');

-- 原挂在「客户管理」菜单下的按钮，挪到「客户列表」
UPDATE iam_admin_menu AS btn
INNER JOIN iam_admin_menu AS old_parent
        ON old_parent.id = btn.parent_id AND old_parent.code = 'BUSINESS-DOMAIN-CUSTOMERS'
INNER JOIN iam_admin_menu AS list_menu
        ON list_menu.code = 'BUSINESS-DOMAIN-CUSTOMERS-LIST'
SET btn.parent_id = list_menu.id,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.scope = 'business';

-- E) 角色绑定：分组目录 + 客户子菜单
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, cat.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu settings ON settings.id = rmr.menu_id AND settings.code = 'BUSINESS-DOMAIN-SETTINGS'
JOIN iam_admin_menu cat ON cat.code IN (
    'BUSINESS-DOMAIN-SETTINGS-ORG',
    'BUSINESS-DOMAIN-SETTINGS-FEATURES',
    'BUSINESS-DOMAIN-SETTINGS-SECURITY'
);

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, list_menu.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu cust ON cust.id = rmr.menu_id AND cust.code = 'BUSINESS-DOMAIN-CUSTOMERS'
JOIN iam_admin_menu list_menu ON list_menu.code = 'BUSINESS-DOMAIN-CUSTOMERS-LIST';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, onb.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu src ON src.id = rmr.menu_id AND src.code = 'BUSINESS-DOMAIN-ONBOARDING'
JOIN iam_admin_menu onb ON onb.code = 'BUSINESS-DOMAIN-CUSTOMERS-ONBOARDING';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, cat.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu leaf ON leaf.id = rmr.menu_id
JOIN iam_admin_menu cat ON cat.code = 'BUSINESS-DOMAIN-SETTINGS-ORG'
WHERE leaf.code IN ('BUSINESS-DOMAIN-MEMBERS', 'BUSINESS-DOMAIN-ROLES', 'BUSINESS-DOMAIN-ONBOARDING');

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, cat.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu leaf ON leaf.id = rmr.menu_id
JOIN iam_admin_menu cat ON cat.code = 'BUSINESS-DOMAIN-SETTINGS-FEATURES'
WHERE leaf.code IN ('BUSINESS-DOMAIN-CONFIG', 'BUSINESS-DOMAIN-BLOCKWORDS', 'BUSINESS-DOMAIN-NOTIFICATIONS');

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, cat.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu leaf ON leaf.id = rmr.menu_id
JOIN iam_admin_menu cat ON cat.code = 'BUSINESS-DOMAIN-SETTINGS-SECURITY'
WHERE leaf.code IN ('BUSINESS-DOMAIN-AUDIT-LOGS', 'BUSINESS-DOMAIN-LOGIN-LOGS');

-- 有任意系统设置叶子的角色，补绑「系统设置」父目录
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, settings.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu leaf ON leaf.id = rmr.menu_id
JOIN iam_admin_menu settings ON settings.code = 'BUSINESS-DOMAIN-SETTINGS'
WHERE leaf.code IN (
    'BUSINESS-DOMAIN-BASIC',
    'BUSINESS-DOMAIN-MEMBERS',
    'BUSINESS-DOMAIN-ROLES',
    'BUSINESS-DOMAIN-ONBOARDING',
    'BUSINESS-DOMAIN-CONFIG',
    'BUSINESS-DOMAIN-BLOCKWORDS',
    'BUSINESS-DOMAIN-NOTIFICATIONS',
    'BUSINESS-DOMAIN-AUDIT-LOGS',
    'BUSINESS-DOMAIN-LOGIN-LOGS',
    'BUSINESS-DOMAIN-SETTINGS-ORG',
    'BUSINESS-DOMAIN-SETTINGS-FEATURES',
    'BUSINESS-DOMAIN-SETTINGS-SECURITY'
);
