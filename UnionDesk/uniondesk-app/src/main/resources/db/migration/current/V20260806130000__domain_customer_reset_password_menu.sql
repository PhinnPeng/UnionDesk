-- 域客户重置密码：业务端按钮菜单 + 平台运营/域管理员菜单授权
-- 前置迁移 V20260806120000 已插入权限行与 iam_role_permission；本迁移补齐菜单快照授权

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'BUSINESS-DOMAIN-CUSTOMERS-RESET-PASSWORD', 'button', 'business', '重置客户密码', 'domain.customer.reset_password',
       (SELECT id FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS' LIMIT 1), 4, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'BUSINESS-DOMAIN-CUSTOMERS-RESET-PASSWORD');

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'BUSINESS-DOMAIN-CUSTOMERS-RESET-PASSWORD' AND m.status = 1
WHERE r.code IN ('domain_admin', 'super_admin', 'platform_admin');
