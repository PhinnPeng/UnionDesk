-- 业务端：系统设置 / 客户管理 中枢菜单
-- 侧栏一级：概览、事项配置、客户管理、系统设置；配置类叶子收入中枢二级导航（前端 Hub）

-- A) 新增「系统设置」一级菜单
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-SETTINGS', 'menu', 'business', '系统设置', '/domain/settings', './domain/settings', NULL,
       NULL, 50, 'SettingOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS');

-- B) 「客户管理」改为中枢入口
UPDATE iam_admin_menu
SET route_path = '/domain/customers',
    component_key = './domain/customers/hub',
    name = '客户管理',
    order_no = 40,
    icon = 'SolutionOutlined',
    hidden = 0,
    status = 1,
    parent_id = NULL,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS';

-- C) 事项配置顺序靠前（作业类）
UPDATE iam_admin_menu
SET order_no = 30,
    parent_id = NULL,
    hidden = 0,
    status = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-TICKET-CONFIG';

UPDATE iam_admin_menu
SET order_no = 20,
    parent_id = NULL,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'BUSINESS-DOMAIN-OVERVIEW';

-- D) 配置类叶子从侧栏隐藏（路由由前端静态表 + redirect 承接；按钮权限挂在原菜单上保留）
UPDATE iam_admin_menu
SET hidden = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'business'
  AND node_type = 'menu'
  AND code IN (
      'BUSINESS-DOMAIN-BASIC',
      'BUSINESS-DOMAIN-MEMBERS',
      'BUSINESS-DOMAIN-ROLES',
      'BUSINESS-DOMAIN-ONBOARDING',
      'BUSINESS-DOMAIN-BLOCKWORDS',
      'BUSINESS-DOMAIN-NOTIFICATIONS',
      'BUSINESS-DOMAIN-CONFIG',
      'BUSINESS-DOMAIN-AUDIT-LOGS',
      'BUSINESS-DOMAIN-LOGIN-LOGS'
  );

-- E) 凡已绑定任一配置类叶子菜单的角色，自动获得「系统设置」入口
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, settings.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu leaf ON leaf.id = rmr.menu_id
JOIN iam_admin_menu settings ON settings.code = 'BUSINESS-DOMAIN-SETTINGS'
WHERE leaf.scope = 'business'
  AND leaf.code IN (
      'BUSINESS-DOMAIN-BASIC',
      'BUSINESS-DOMAIN-MEMBERS',
      'BUSINESS-DOMAIN-ROLES',
      'BUSINESS-DOMAIN-ONBOARDING',
      'BUSINESS-DOMAIN-BLOCKWORDS',
      'BUSINESS-DOMAIN-NOTIFICATIONS',
      'BUSINESS-DOMAIN-CONFIG',
      'BUSINESS-DOMAIN-AUDIT-LOGS',
      'BUSINESS-DOMAIN-LOGIN-LOGS',
      'BUSINESS-DOMAIN-SETTINGS'
  );

-- F) 兜底：domain_admin 必绑系统设置
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'BUSINESS-DOMAIN-SETTINGS'
WHERE r.code = 'domain_admin';
