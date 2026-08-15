-- 员工端工作台聚合（菜单数据部分）：
-- 1) 新增一级菜单「工作台」（/domain/workbench），聚合工单队列 + 在线咨询
-- 2) 工单队列 / 在线咨询 菜单 hidden=1（隐藏显示，路由仍由菜单树生成，按钮不动）
-- 3) 事项配置挂入「系统设置 → 功能配置」目录（SLA 管理之后），路径/组件/权限码不变
-- 4) 父目录角色绑定传播核查补绑（照 SLA 迁移 V20260813140000 C 段先例）

-- A) 新增「工作台」一级菜单（业务端，置于运营概览之后，order 25）
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-WORKBENCH', 'menu', 'business', '工作台', '/domain/workbench', './domain/workbench', NULL,
       NULL, 25, 'DashboardOutlined', 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-WORKBENCH')
  AND NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE route_path = '/domain/workbench' AND node_type = 'menu' AND scope = 'business');

-- B) 「工作台」角色绑定：domain_admin / agent / super_admin（幂等；super_admin 为 global 域，按 code 绑定）
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'BUSINESS-DOMAIN-WORKBENCH'
WHERE r.code IN ('domain_admin', 'agent', 'super_admin');

-- C) 工单队列 / 在线咨询 隐藏为 hidden=1（按钮节点不动，路由仍由后端菜单树生成）
UPDATE iam_admin_menu
SET hidden = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code IN ('BUSINESS-DOMAIN-TICKET-QUEUE', 'BUSINESS-DOMAIN-CONSULTATION')
  AND hidden = 0;

-- D) 事项配置挂入「功能配置」目录（SLA 管理 order 40 之后），路径/组件/权限码不变
UPDATE iam_admin_menu AS tc
JOIN iam_admin_menu AS features ON features.code = 'BUSINESS-DOMAIN-SETTINGS-FEATURES'
SET tc.parent_id = features.id,
    tc.order_no = 50,
    tc.updated_at = CURRENT_TIMESTAMP(3)
WHERE tc.code = 'BUSINESS-DOMAIN-TICKET-CONFIG';

-- E) 父目录角色传播核查补绑：绑定过事项配置的角色，补绑「功能配置」「系统设置」父目录（避免死菜单，幂等）
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, cat.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu leaf ON leaf.id = rmr.menu_id AND leaf.code = 'BUSINESS-DOMAIN-TICKET-CONFIG'
JOIN iam_admin_menu cat ON cat.code IN ('BUSINESS-DOMAIN-SETTINGS-FEATURES', 'BUSINESS-DOMAIN-SETTINGS');
