-- IAM 角色—控制台绑定对齐：收敛 global/domain 权限包 + admin bootstrap

DELETE rp
FROM iam_role_permission rp
JOIN role r ON r.id = rp.role_id
JOIN iam_permission p ON p.id = rp.permission_id
WHERE r.scope = 'global'
  AND (p.permission_scope = 'domain' OR p.code NOT LIKE 'platform.%');

DELETE rmr
FROM iam_admin_role_menu_relation rmr
JOIN role r ON r.id = rmr.role_id
JOIN iam_admin_menu m ON m.id = rmr.menu_id
WHERE r.scope = 'global'
  AND m.scope = 'business';

DELETE ugr
FROM user_global_role ugr
JOIN user_account ua ON ua.id = ugr.user_id
JOIN role r ON r.id = ugr.role_id
WHERE ua.username = 'admin'
  AND r.code = 'super_admin';

INSERT IGNORE INTO user_global_role (user_id, role_id, created_at)
SELECT ua.id, r.id, CURRENT_TIMESTAMP(3)
FROM user_account ua
JOIN role r ON r.code = 'platform_admin'
WHERE ua.username = 'admin';

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.scope = 'platform' AND m.status = 1
WHERE r.code = 'platform_admin';

DELETE rmr
FROM iam_admin_role_menu_relation rmr
JOIN role r ON r.id = rmr.role_id
JOIN iam_admin_menu m ON m.id = rmr.menu_id
WHERE r.code = 'agent'
  AND m.scope = 'platform';

DELETE rp
FROM iam_role_permission rp
JOIN role r ON r.id = rp.role_id
JOIN iam_permission p ON p.id = rp.permission_id
WHERE r.code = 'agent'
  AND (p.permission_scope = 'platform' OR p.code LIKE 'platform.%');

DELETE sapr
FROM staff_account_platform_role sapr
JOIN staff_account sa ON sa.id = sapr.staff_account_id
WHERE sa.username = 'admin';
