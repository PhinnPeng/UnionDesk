-- 回填默认 admin 账号的 platform_admin 绑定，确保 /api/v1/auth/me 可返回 platform_roles
INSERT IGNORE INTO staff_account_platform_role (staff_account_id, platform_role_id, created_at)
SELECT sa.id, pr.id, CURRENT_TIMESTAMP(3)
FROM staff_account sa
JOIN platform_role pr ON pr.code = 'platform_admin'
WHERE sa.login_name = 'admin';
