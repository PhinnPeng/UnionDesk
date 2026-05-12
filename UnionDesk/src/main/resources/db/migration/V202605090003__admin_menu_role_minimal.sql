-- ============================================================
-- 1. 菜单命名与角色命名收敛
-- ============================================================
UPDATE role
SET name = '超级管理员'
WHERE code = 'super_admin'
  AND name <> '超级管理员';

UPDATE iam_admin_menu
SET name = '角色管理'
WHERE route_path = '/platform/role'
  AND name <> '角色管理';

UPDATE iam_admin_menu
SET name = '菜单管理'
WHERE route_path = '/platform/menu'
  AND name <> '菜单管理';

-- ============================================================
-- 2. 清空 super_admin 现有平台菜单授权，仅保留目标菜单
-- ============================================================
DELETE rmr
FROM iam_admin_role_menu_relation rmr
JOIN role r ON r.id = rmr.role_id
JOIN iam_admin_menu m ON m.id = rmr.menu_id
WHERE r.code = 'super_admin'
  AND m.scope = 'platform';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m
WHERE r.code = 'super_admin'
  AND m.node_type = 'menu'
  AND m.route_path IN ('/platform/permission', '/platform/role', '/platform/menu');

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu parent ON parent.route_path IN ('/platform/permission', '/platform/role', '/platform/menu')
JOIN iam_admin_menu btn ON btn.parent_id = parent.id
WHERE r.code = 'super_admin'
  AND btn.node_type = 'button'
  AND btn.required = 1;

-- ============================================================
-- 3. admin 账号仅保留 super_admin，全量清理多余角色绑定
-- ============================================================
INSERT IGNORE INTO user_global_role (user_id, role_id)
SELECT ua.id, r.id
FROM user_account ua
JOIN role r ON r.code = 'super_admin'
WHERE ua.username = 'admin';

DELETE ugr
FROM user_global_role ugr
JOIN user_account ua ON ua.id = ugr.user_id
JOIN role r ON r.id = ugr.role_id
WHERE ua.username = 'admin'
  AND r.code <> 'super_admin';

DELETE udr
FROM user_domain_role udr
JOIN user_account ua ON ua.id = udr.user_id
WHERE ua.username = 'admin';

DELETE sapr
FROM staff_account_platform_role sapr
JOIN staff_account sa ON sa.id = sapr.staff_account_id
WHERE sa.login_name = 'admin';
