-- 客户管理：platform.domain.customer.* → platform.domain.control.customer.*

-- 1. iam_permission 重命名
UPDATE iam_permission
SET code = 'platform.domain.control.customer.read',
    name = '查看客户',
    description = '查看业务域客户列表与详情',
    action_code = 'platform_domain_control_customer_read',
    path_pattern = '/api/v1/admin/domains/*/customers/**',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'platform.domain.customer.read';

UPDATE iam_permission
SET code = 'platform.domain.control.customer.create',
    name = '添加客户',
    description = '添加客户入域（含手动与员工导入）',
    action_code = 'platform_domain_control_customer_create',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'platform.domain.customer.create';

UPDATE iam_permission
SET code = 'platform.domain.control.customer.update-status',
    name = '启停客户',
    description = '启用或禁用业务域客户',
    action_code = 'platform_domain_control_customer_update_status',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'platform.domain.customer.update';

-- 2. catalog rename
UPDATE iam_admin_menu
SET code = 'PLATFORM-DOMAIN-CONTROL-CUSTOMER',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code = 'PLATFORM-DOMAIN-CUSTOMERS';

-- 3. iam_admin_menu permission_code 同步
UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.customer.read',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'platform.domain.customer.read';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.customer.create',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'platform.domain.customer.create';

UPDATE iam_admin_menu
SET permission_code = 'platform.domain.control.customer.update-status',
    name = '启停客户',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE permission_code = 'platform.domain.customer.update';

UPDATE iam_admin_menu btn
JOIN iam_admin_menu catalog ON catalog.code = 'PLATFORM-DOMAIN-CONTROL-CUSTOMER'
SET btn.parent_id = catalog.id,
    btn.scope = 'platform',
    btn.hidden = 0,
    btn.updated_at = CURRENT_TIMESTAMP(3)
WHERE btn.node_type = 'button'
  AND btn.permission_code IN (
      'platform.domain.control.customer.read',
      'platform.domain.control.customer.create',
      'platform.domain.control.customer.update-status'
  );

-- 4. 已授权业务域控制台的角色，补齐客户 button 菜单关系（迁移后 permission_code 已变）
INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_admin_menu btn ON btn.node_type = 'button'
    AND btn.permission_code IN (
        'platform.domain.control.customer.read',
        'platform.domain.control.customer.create',
        'platform.domain.control.customer.update-status'
    )
    AND btn.status = 1;
