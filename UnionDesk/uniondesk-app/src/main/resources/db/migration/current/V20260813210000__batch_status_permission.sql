-- P1-2 跨域批量停用（任务 08-11-group-role-management，design §4/§5）
-- 平台权限码 platform.user.domain_batch_status 注册并授权 super_admin / platform_admin
-- （跨域批量停用为高危操作，需 step-up 二次认证；域级角色不授予）

INSERT INTO iam_permission (code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status)
SELECT v.code, v.name, v.name, 'platform', v.code, v.code, 'POST', '/api/v1/admin/staff/*/domain-members/batch-status', 1
FROM (
    SELECT 'platform.user.domain_batch_status' AS code, '跨域批量停用成员' AS name
) v
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission p WHERE p.code = v.code
);

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code = 'platform.user.domain_batch_status'
WHERE r.code IN ('super_admin', 'platform_admin')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
