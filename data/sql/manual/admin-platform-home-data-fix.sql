-- Admin 登录默认进 /platform/home：仅数据修复（非 Flyway）
-- 原因：global 角色（platform_admin）绑定了 domain.member.* 平台按钮，快照 actions 混入非 platform.* → E2-00 规则落 /home
-- 幂等：可重复执行

-- 1) admin 仅保留 platform_admin，移除域表上的 super_admin 误绑
DELETE udr
FROM user_domain_role udr
JOIN user_account ua ON ua.id = udr.user_id
JOIN role r ON r.id = udr.role_id
WHERE ua.username = 'admin';

-- 2) global 角色：移除 domain.member.* 菜单按钮绑定
DELETE rmr
FROM iam_admin_role_menu_relation rmr
JOIN role r ON r.id = rmr.role_id
JOIN iam_admin_menu btn ON btn.id = rmr.menu_id
WHERE r.scope = 'global'
  AND btn.node_type = 'button'
  AND btn.permission_code IN (
      'domain.member.read',
      'domain.member.create',
      'domain.member.update_roles',
      'domain.member.update_status',
      'domain.member.delete'
  );

-- 3) global 角色：移除 domain.member.* 直授（若迁移遗漏）
DELETE rp
FROM iam_role_permission rp
JOIN role r ON r.id = rp.role_id
JOIN iam_permission p ON p.id = rp.permission_id
WHERE r.scope = 'global'
  AND p.code IN (
      'domain.member.read',
      'domain.member.create',
      'domain.member.update_roles',
      'domain.member.update_status',
      'domain.member.delete'
  );

-- 4) global 角色：移除「员工管理」catalog 绑定（其下按钮均为 domain.member.*）
DELETE rmr
FROM iam_admin_role_menu_relation rmr
JOIN role r ON r.id = rmr.role_id
JOIN iam_admin_menu m ON m.id = rmr.menu_id
WHERE r.scope = 'global'
  AND m.code = 'PLATFORM-DOMAIN-MEMBERS';
