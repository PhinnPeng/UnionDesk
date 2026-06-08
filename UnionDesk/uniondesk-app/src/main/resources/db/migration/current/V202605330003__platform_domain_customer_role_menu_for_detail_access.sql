-- 已为业务域详情菜单授权的角色，补齐客户管理 catalog + 三按钮的菜单关系

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_admin_menu m ON (
    m.code = 'PLATFORM-DOMAIN-CUSTOMERS'
    OR m.permission_code IN (
        'platform.domain.customer.read',
        'platform.domain.customer.create',
        'platform.domain.customer.update'
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
    'platform.domain.customer.read',
    'platform.domain.customer.create',
    'platform.domain.customer.update'
) AND p.status = 1;
