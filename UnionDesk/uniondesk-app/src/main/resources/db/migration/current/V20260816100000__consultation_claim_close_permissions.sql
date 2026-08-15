-- E-EMP1 在线咨询接待闭环（后端）
-- 1) 新增权限码：consultation.claim（客服接入/领取会话）、consultation.close（客服结束会话）
-- 2) 授权：agent/domain_admin 授 claim+close（与 consultation.view/convert 同角色）
-- 3) consultation_message 增加撤回列 retracted_at / retracted_by（2 分钟内本人消息撤回）

-- A) 权限码（幂等）
INSERT INTO iam_permission (
    code,
    name,
    description,
    permission_scope,
    resource_code,
    action_code,
    http_method,
    path_pattern,
    status
)
VALUES
    ('consultation.claim', '接入咨询会话', '客服从队列接入/领取咨询会话', 'domain', 'consultation', 'claim', 'POST', '/api/v1/admin/domains/*/consultations/*/claim', 1),
    ('consultation.close', '结束咨询会话', '客服结束咨询会话（未转工单可手动关闭）', 'domain', 'consultation', 'close', 'POST', '/api/v1/admin/domains/*/consultations/*/end', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    permission_scope = VALUES(permission_scope),
    resource_code = VALUES(resource_code),
    action_code = VALUES(action_code),
    http_method = VALUES(http_method),
    path_pattern = VALUES(path_pattern),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

-- B) 角色授权（幂等，agent/domain_admin 全量）
INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE p.code IN ('consultation.claim', 'consultation.close')
  AND r.scope = 'domain'
  AND r.code IN ('domain_admin', 'agent')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );

-- C) 咨询消息撤回列（幂等，MySQL 8 无 ADD COLUMN IF NOT EXISTS，靠 Flyway 版本表保证只执行一次）
ALTER TABLE consultation_message
    ADD COLUMN retracted_at DATETIME(3) NULL COMMENT '撤回时间' AFTER created_at,
    ADD COLUMN retracted_by BIGINT UNSIGNED NULL COMMENT '撤回人（staff_account id）' AFTER retracted_at;
