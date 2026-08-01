-- 双端账号（平台角色 + 域成员）进业务端时，角色码常为 super_admin / platform_admin，
-- 而业务菜单此前只绑 domain_admin/agent，导致登录跳 /home 无路由 → 404。
-- 将业务端工作台与域治理菜单同步授权给平台运营角色（菜单仍按 snapshot scope 过滤，不会污染平台侧栏）。

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.status = 1 AND m.scope = 'business'
WHERE r.code IN ('super_admin', 'platform_admin')
  AND (
      m.code LIKE 'BUSINESS-HOME%'
      OR m.code LIKE 'BUSINESS-DOMAIN-%'
  );

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE r.code IN ('super_admin', 'platform_admin')
  AND (
      p.code = 'domain.home.read'
      OR p.code LIKE 'domain.overview.%'
      OR p.code LIKE 'domain.general.%'
      OR p.code LIKE 'domain.member.%'
      OR p.code LIKE 'domain.role.%'
      OR p.code LIKE 'domain.customer.%'
      OR p.code LIKE 'domain.invitation_code.%'
      OR p.code LIKE 'domain.ticket_%'
      OR p.code LIKE 'domain.blocked_word.%'
      OR p.code LIKE 'domain.config.%'
      OR p.code LIKE 'domain.notification_template.%'
      OR p.code LIKE 'domain.audit_log.%'
      OR p.code LIKE 'domain.login_log.%'
  )
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );
