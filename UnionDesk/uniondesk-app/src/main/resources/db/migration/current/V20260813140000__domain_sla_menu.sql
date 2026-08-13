-- 业务域端「SLA 管理」菜单：挂「系统设置 → 功能配置」下（域治理 catalog 已废弃禁用）
-- 路由 /domain/sla，组件 ./domain/sla；domain_admin/agent 菜单绑定 + agent 只读授权

-- A) SLA 管理菜单行
INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'BUSINESS-DOMAIN-SLA', 'menu', 'business', 'SLA 管理', '/domain/sla', './domain/sla', NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SETTINGS-FEATURES' LIMIT 1),
       40, NULL, 0, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-SLA');

-- B) domain_admin / agent 菜单绑定（仿 V20260728170000 内建业务角色授权）
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'BUSINESS-DOMAIN-SLA'
WHERE r.scope = 'domain'
  AND r.code IN ('domain_admin', 'agent');

-- C) 父目录传播：绑定 SLA 叶子的角色补绑「功能配置」「系统设置」
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT rmr.role_id, cat.id, CURRENT_TIMESTAMP(3)
FROM iam_admin_role_menu_relation rmr
JOIN iam_admin_menu leaf ON leaf.id = rmr.menu_id AND leaf.code = 'BUSINESS-DOMAIN-SLA'
JOIN iam_admin_menu cat ON cat.code IN ('BUSINESS-DOMAIN-SETTINGS-FEATURES', 'BUSINESS-DOMAIN-SETTINGS');

-- D) 权限：domain_admin 已有 domain.sla.* （历史迁移），幂等补全；agent 仅授只读避免死菜单
INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE r.scope = 'domain'
  AND r.code = 'domain_admin'
  AND p.code IN ('domain.sla.read', 'domain.sla.create', 'domain.sla.update')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE r.scope = 'domain'
  AND r.code = 'agent'
  AND p.code = 'domain.sla.read'
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );
