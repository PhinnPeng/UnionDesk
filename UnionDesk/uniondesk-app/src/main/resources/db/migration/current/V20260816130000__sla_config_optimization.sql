-- SLA 配置优化：全局规则（business_domain_id 可空）+ 违约动作幂等列 + SLA 状态归一 + 平台权限
-- 1) sla_rule 去外键 + business_domain_id 可空（NULL = 平台全局默认规则，对齐全局屏蔽词先例）
-- 2) ticket.sla_breach_actioned 违约动作一次性标志（幂等守卫）
-- 3) ticket.sla_status 取消 resolved，唯一最终态 stopped（旧数据归一）
-- 4) 平台权限码 platform.sla.{read,create,update,delete} + platform_admin/super_admin 授权（幂等，code 为键）

-- A) sla_rule 去外键（遗留外键，若存在）
SET @fk_exists := (
    SELECT COUNT(*)
    FROM information_schema.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sla_rule'
      AND CONSTRAINT_NAME = 'fk_sla_rule_domain'
      AND CONSTRAINT_TYPE = 'FOREIGN KEY'
);
SET @ddl := IF(@fk_exists > 0, 'ALTER TABLE sla_rule DROP FOREIGN KEY fk_sla_rule_domain', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- B) sla_rule.business_domain_id 改可空（NULL = 全局规则）
ALTER TABLE sla_rule
    MODIFY business_domain_id bigint unsigned NULL COMMENT '业务域ID，NULL=平台全局默认规则';

-- C) ticket.sla_breach_actioned 新列（先查列是否存在防重复）
SET @col_exists := (
    SELECT COUNT(*)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'ticket'
      AND COLUMN_NAME = 'sla_breach_actioned'
);
SET @ddl := IF(@col_exists = 0,
    'ALTER TABLE ticket ADD COLUMN sla_breach_actioned tinyint NOT NULL DEFAULT 0 COMMENT ''违约动作已执行（每工单一次）''',
    'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- D) SLA 状态归一：取消 resolved，唯一最终态 stopped
UPDATE ticket
SET sla_status = 'stopped'
WHERE sla_status = 'resolved';

-- E) 平台权限码（幂等：uk_iam_permission_code 冲突即更新）
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
    ('platform.sla.read', '查看全局 SLA 规则', '查看平台全局默认 SLA 规则', 'platform', 'sla_rule', 'read', 'GET', '/api/v1/admin/platform/sla-rules', 1),
    ('platform.sla.create', '新建全局 SLA 规则', '新建平台全局默认 SLA 规则', 'platform', 'sla_rule', 'create', 'POST', '/api/v1/admin/platform/sla-rules', 1),
    ('platform.sla.update', '编辑全局 SLA 规则', '编辑平台全局默认 SLA 规则', 'platform', 'sla_rule', 'update', 'PUT', '/api/v1/admin/platform/sla-rules/*', 1),
    ('platform.sla.delete', '删除全局 SLA 规则', '删除平台全局默认 SLA 规则', 'platform', 'sla_rule', 'delete', 'DELETE', '/api/v1/admin/platform/sla-rules/*', 1)
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

-- F) 角色授权（幂等）：platform_admin / super_admin 全量
INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE p.code IN ('platform.sla.read', 'platform.sla.create', 'platform.sla.update', 'platform.sla.delete')
  AND r.scope = 'platform'
  AND r.code IN ('platform_admin', 'super_admin')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );
