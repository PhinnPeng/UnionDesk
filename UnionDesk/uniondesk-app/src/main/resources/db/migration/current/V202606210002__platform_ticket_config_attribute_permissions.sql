-- S1 事项属性权限：platform.ticket_config.* + platform.domain.control.ticket_attribute.*

INSERT INTO iam_permission (code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status)
SELECT v.code, v.name, v.name, 'platform', v.code, v.code, NULL, NULL, 1
FROM (
    SELECT 'platform.ticket_config.attr.read' AS code, '全局事项属性-查看' AS name
    UNION ALL SELECT 'platform.ticket_config.attr.create', '全局事项属性-创建'
    UNION ALL SELECT 'platform.ticket_config.attr.update', '全局事项属性-更新'
    UNION ALL SELECT 'platform.ticket_config.attr.delete', '全局事项属性-删除'
    UNION ALL SELECT 'platform.domain.control.ticket_attribute.read', '域管控-事项属性-查看'
    UNION ALL SELECT 'platform.domain.control.ticket_attribute.create', '域管控-事项属性-创建'
    UNION ALL SELECT 'platform.domain.control.ticket_attribute.update', '域管控-事项属性-更新'
    UNION ALL SELECT 'platform.domain.control.ticket_attribute.delete', '域管控-事项属性-删除'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission p WHERE p.code = v.code
);

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.ticket_config.attr.read',
    'platform.ticket_config.attr.create',
    'platform.ticket_config.attr.update',
    'platform.ticket_config.attr.delete',
    'platform.domain.control.ticket_attribute.read',
    'platform.domain.control.ticket_attribute.create',
    'platform.domain.control.ticket_attribute.update',
    'platform.domain.control.ticket_attribute.delete'
)
WHERE r.code = 'super_admin'
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
