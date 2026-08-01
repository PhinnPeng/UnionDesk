-- 域管控事项状态权限：platform.domain.control.ticket_status.*

INSERT INTO iam_permission (code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status)
SELECT v.code, v.name, v.name, 'platform', v.code, v.code, NULL, NULL, 1
FROM (
    SELECT 'platform.domain.control.ticket_status.read' AS code, '域管控-事项状态-查看' AS name
    UNION ALL SELECT 'platform.domain.control.ticket_status.create', '域管控-事项状态-创建'
    UNION ALL SELECT 'platform.domain.control.ticket_status.update', '域管控-事项状态-更新'
    UNION ALL SELECT 'platform.domain.control.ticket_status.delete', '域管控-事项状态-删除'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission p WHERE p.code = v.code
);

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.ticket_status.read',
    'platform.domain.control.ticket_status.create',
    'platform.domain.control.ticket_status.update',
    'platform.domain.control.ticket_status.delete'
)
WHERE r.code = 'super_admin'
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.ticket_status.read',
    'platform.domain.control.ticket_status.create',
    'platform.domain.control.ticket_status.update',
    'platform.domain.control.ticket_status.delete'
)
WHERE r.code = 'platform_admin'
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
