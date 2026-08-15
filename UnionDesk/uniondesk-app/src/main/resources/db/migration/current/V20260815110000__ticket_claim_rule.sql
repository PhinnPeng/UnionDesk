-- 工单领取规则：域级配置客户提单后的自动领取策略（least_loaded / fixed）
-- 1) 规则表 ticket_claim_rule（域/类型/优先级/指定人外键，匹配口径与 sla_rule 同构）
-- 2) 新增权限码 4 个：domain.ticket_claim_rule.read/create/update/delete
-- 3) 角色绑定：domain_admin 全量 + agent 只读（照 V20260813140000 SLA 先例幂等）

-- A) 规则表
CREATE TABLE IF NOT EXISTS ticket_claim_rule (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL DEFAULT '',
    enabled TINYINT NOT NULL DEFAULT 1 COMMENT '总开关 1=启用 0=停用',
    match_ticket_type_id BIGINT UNSIGNED DEFAULT NULL COMMENT '匹配事项类型，NULL=全部类型',
    match_priority_level_id BIGINT UNSIGNED DEFAULT NULL COMMENT '匹配优先级（join ticket_priority_level.code 匹配 ticket.priority），NULL=全部优先级',
    strategy VARCHAR(32) NOT NULL DEFAULT 'least_loaded' COMMENT 'least_loaded=受理最少 / fixed=指定人',
    assignee_staff_account_id BIGINT UNSIGNED DEFAULT NULL COMMENT 'strategy=fixed 时的指定受理人',
    grace_minutes INT NOT NULL DEFAULT 0 COMMENT '定时兜底延迟分钟（阶段二用，MVP 仅落库）',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ticket_claim_rule_domain_type (business_domain_id, match_ticket_type_id),
    CONSTRAINT fk_ticket_claim_rule_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id),
    CONSTRAINT fk_ticket_claim_rule_type FOREIGN KEY (match_ticket_type_id) REFERENCES ticket_type (id),
    CONSTRAINT fk_ticket_claim_rule_priority FOREIGN KEY (match_priority_level_id) REFERENCES ticket_priority_level (id),
    CONSTRAINT fk_ticket_claim_rule_assignee FOREIGN KEY (assignee_staff_account_id) REFERENCES staff_account (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单领取规则';

-- B) 权限码（幂等：uk_iam_permission_code 冲突即更新）
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
    ('domain.ticket_claim_rule.read', '查看领取规则', '查看业务域内工单领取规则', 'domain', 'ticket_claim_rule', 'read', 'GET', '/api/v1/admin/domains/*/ticket-claim-rules', 1),
    ('domain.ticket_claim_rule.create', '新建领取规则', '新建业务域内工单领取规则', 'domain', 'ticket_claim_rule', 'create', 'POST', '/api/v1/admin/domains/*/ticket-claim-rules', 1),
    ('domain.ticket_claim_rule.update', '编辑领取规则', '编辑/启停业务域内工单领取规则', 'domain', 'ticket_claim_rule', 'update', 'PUT', '/api/v1/admin/domains/*/ticket-claim-rules/*', 1),
    ('domain.ticket_claim_rule.delete', '删除领取规则', '删除业务域内工单领取规则', 'domain', 'ticket_claim_rule', 'delete', 'DELETE', '/api/v1/admin/domains/*/ticket-claim-rules/*', 1)
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

-- C) 角色授权（幂等）：domain_admin 全量 4 码 + agent 只读（read 码）
INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE r.scope = 'domain'
  AND r.code = 'domain_admin'
  AND p.code IN (
      'domain.ticket_claim_rule.read',
      'domain.ticket_claim_rule.create',
      'domain.ticket_claim_rule.update',
      'domain.ticket_claim_rule.delete'
  )
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.status = 1
WHERE r.scope = 'domain'
  AND r.code = 'agent'
  AND p.code = 'domain.ticket_claim_rule.read'
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission existing
      WHERE existing.role_id = r.id AND existing.permission_id = p.id
  );
