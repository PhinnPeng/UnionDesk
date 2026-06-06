-- 客户管理：domain.customer.* → platform.domain.customer.* + 菜单按钮

UPDATE iam_permission
SET code = 'platform.domain.customer.read',
    name = '查看客户',
    description = '查看业务域客户列表',
    permission_scope = 'platform',
    action_code = 'platform_domain_customer_read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.customer.read';

UPDATE iam_permission
SET code = 'platform.domain.customer.create',
    name = '添加客户',
    description = '添加客户入域（含手动与员工导入）',
    permission_scope = 'platform',
    action_code = 'platform_domain_customer_create',
    path_pattern = '/api/v1/admin/domains/*/customers/**',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.customer.create';

UPDATE iam_permission
SET code = 'platform.domain.customer.update',
    name = '更新客户',
    description = '更新业务域客户状态',
    permission_scope = 'platform',
    action_code = 'platform_domain_customer_update',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'domain.customer.update_status';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.customer.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'domain.customer.read';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.customer.create',
    name = '添加客户',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'domain.customer.create';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.customer.update',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'domain.customer.update_status';

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-CUSTOMERS', 'catalog', 'platform', '客户管理', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       10, NULL, 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CUSTOMERS');

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-CUSTOMERS'
SET btn.parent_id = catalog.id,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code IN (
      'platform.domain.customer.read',
      'platform.domain.customer.create',
      'platform.domain.customer.update'
  );

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-CUSTOMER-READ', 'button', 'platform', '查看客户', 'platform.domain.customer.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CUSTOMERS' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.customer.read' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-CUSTOMER-CREATE', 'button', 'platform', '添加客户', 'platform.domain.customer.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CUSTOMERS' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.customer.create' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-CUSTOMER-UPDATE', 'button', 'platform', '更新客户', 'platform.domain.customer.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CUSTOMERS' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.customer.update' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN ('TMP-BTN-DOMAIN-CUSTOMER-READ', 'TMP-BTN-DOMAIN-CUSTOMER-CREATE', 'TMP-BTN-DOMAIN-CUSTOMER-UPDATE');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.customer.read',
    'platform.domain.customer.create',
    'platform.domain.customer.update'
) AND p.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'PLATFORM-DOMAIN-CUSTOMERS' AND m.status = 1
WHERE r.code = 'super_admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button'
    AND btn.permission_code IN (
        'platform.domain.customer.read',
        'platform.domain.customer.create',
        'platform.domain.customer.update'
    )
    AND btn.status = 1
WHERE r.code = 'super_admin';
