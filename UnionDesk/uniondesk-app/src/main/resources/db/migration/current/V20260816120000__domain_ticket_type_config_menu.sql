-- 补业务域端「事项类型配置」隐藏菜单（/domain/ticket-config/types/:typeId）
-- 根因：generateRoutesFromBackend 由后端菜单树驱动，缺菜单节点则前端路由 404（与工单详情 404 同类，见 BUSINESS-DOMAIN-TICKET-DETAIL 先例）
-- 幂等：以 code 为键；parent_id 置 NULL 顶层隐藏（避免「有页面菜单挂子路由」致列表页白屏）

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required, created_at, updated_at)
SELECT 'BUSINESS-DOMAIN-TICKET-TYPE-CONFIG', 'menu', 'business', '事项类型配置', '/domain/ticket-config/types/:typeId', '/src/pages/domain/ticket-config/type-config/index.tsx', NULL, NULL, 110, NULL, 1, 1, 0, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-TICKET-TYPE-CONFIG');

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, NOW()
FROM role r
JOIN iam_admin_menu m ON m.code = 'BUSINESS-DOMAIN-TICKET-TYPE-CONFIG'
WHERE r.code IN ('agent', 'domain_admin', 'super_admin');
