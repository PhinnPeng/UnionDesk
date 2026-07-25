-- 全局事项类型：ticket_type scope 扩展 + 平台种子 + 权限

ALTER TABLE ticket_type
    DROP FOREIGN KEY fk_ticket_type_domain;

ALTER TABLE ticket_type
    DROP INDEX uk_ticket_type_domain_code;

ALTER TABLE ticket_type
    DROP INDEX uk_ticket_type_domain_name;

ALTER TABLE ticket_type
    MODIFY COLUMN business_domain_id BIGINT UNSIGNED NULL COMMENT 'domain 必填；platform 为 NULL';

ALTER TABLE ticket_type
    ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'domain' COMMENT 'platform | domain' AFTER id,
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0 AFTER status,
    ADD COLUMN is_system TINYINT NOT NULL DEFAULT 0 AFTER sort_order,
    ADD COLUMN source_global_type_id BIGINT UNSIGNED NULL COMMENT '域内副本指向全局类型' AFTER is_system;

ALTER TABLE ticket_type
    ADD COLUMN scope_domain_key BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN scope = 'platform' THEN 0 ELSE business_domain_id END
    ) STORED AFTER source_global_type_id;

UPDATE ticket_type
SET scope = 'domain'
WHERE scope IS NULL OR scope = '';

CREATE UNIQUE INDEX uk_ticket_type_scope_domain_code ON ticket_type (scope, scope_domain_key, code);
CREATE UNIQUE INDEX uk_ticket_type_scope_domain_name ON ticket_type (scope, scope_domain_key, name);
CREATE INDEX idx_ticket_type_scope_domain_sort ON ticket_type (scope, scope_domain_key, sort_order);

-- 平台系统预置类型
INSERT INTO ticket_type (
    scope, business_domain_id, code, name, description, icon, category, status, status_flow_config,
    sort_order, is_system, created_at, updated_at
)
SELECT
    'platform', NULL, 'feedback', '问题反馈', '用于收集用户反馈与问题报告', 'mdi:comment-alert-outline', 'feedback', 'active',
    JSON_OBJECT(
        'states', JSON_ARRAY(
            JSON_OBJECT('code', 'pending', 'name', '待处理', 'state_type', 'in_progress', 'allow_customer_withdraw', true, 'is_resolved', false),
            JSON_OBJECT('code', 'processing', 'name', '处理中', 'state_type', 'in_progress', 'allow_customer_withdraw', false, 'is_resolved', false),
            JSON_OBJECT('code', 'closed', 'name', '已关闭', 'state_type', 'terminal', 'allow_customer_withdraw', false, 'is_resolved', false)
        ),
        'transitions', JSON_ARRAY(
            JSON_OBJECT('from', 'pending', 'to', 'processing'),
            JSON_OBJECT('from', 'processing', 'to', 'closed')
        )
    ),
    0, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (SELECT 1 FROM ticket_type WHERE scope = 'platform' AND code = 'feedback');

INSERT INTO ticket_type (
    scope, business_domain_id, code, name, description, icon, category, status, status_flow_config,
    sort_order, is_system, created_at, updated_at
)
SELECT
    'platform', NULL, 'suggestion', '建议', '用于收集产品改进与功能建议', 'mdi:lightbulb-on-outline', 'transaction', 'active',
    JSON_OBJECT(
        'states', JSON_ARRAY(
            JSON_OBJECT('code', 'pending', 'name', '待处理', 'state_type', 'in_progress', 'allow_customer_withdraw', true, 'is_resolved', false),
            JSON_OBJECT('code', 'processing', 'name', '处理中', 'state_type', 'in_progress', 'allow_customer_withdraw', false, 'is_resolved', false),
            JSON_OBJECT('code', 'closed', 'name', '已关闭', 'state_type', 'terminal', 'allow_customer_withdraw', false, 'is_resolved', false)
        ),
        'transitions', JSON_ARRAY(
            JSON_OBJECT('from', 'pending', 'to', 'processing'),
            JSON_OBJECT('from', 'processing', 'to', 'closed')
        )
    ),
    1, 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (SELECT 1 FROM ticket_type WHERE scope = 'platform' AND code = 'suggestion');

-- 平台类型表单 schema（business_domain_id=0 表示 platform）
INSERT INTO ticket_form_schema (business_domain_id, ticket_type_id, record_type, version_no, form_schema, published_at)
SELECT
    0,
    tt.id,
    'published',
    1,
    JSON_OBJECT(
        'type', 'object',
        'properties', JSON_OBJECT(
            'title', JSON_OBJECT('type', 'string', 'title', '标题', 'x-component', 'Input', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true),
            'description', JSON_OBJECT('type', 'string', 'title', '详细描述', 'x-component', 'Input.TextArea', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true)
        )
    ),
    CURRENT_TIMESTAMP(3)
FROM ticket_type tt
WHERE tt.scope = 'platform'
  AND NOT EXISTS (
      SELECT 1 FROM ticket_form_schema fs
      WHERE fs.ticket_type_id = tt.id AND fs.business_domain_id = 0 AND fs.record_type = 'published'
  );

INSERT INTO ticket_form_schema (business_domain_id, ticket_type_id, record_type, version_no, form_schema)
SELECT
    0,
    tt.id,
    'draft',
    0,
    JSON_OBJECT(
        'type', 'object',
        'properties', JSON_OBJECT(
            'title', JSON_OBJECT('type', 'string', 'title', '标题', 'x-component', 'Input', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true),
            'description', JSON_OBJECT('type', 'string', 'title', '详细描述', 'x-component', 'Input.TextArea', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true)
        )
    )
FROM ticket_type tt
WHERE tt.scope = 'platform'
  AND NOT EXISTS (
      SELECT 1 FROM ticket_form_schema fs
      WHERE fs.ticket_type_id = tt.id AND fs.business_domain_id = 0 AND fs.record_type = 'draft'
  );

INSERT INTO iam_permission (code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status)
SELECT v.code, v.name, v.name, 'platform', v.code, v.code, NULL, NULL, 1
FROM (
    SELECT 'platform.ticket_config.type.read' AS code, '全局事项类型-查看' AS name
    UNION ALL SELECT 'platform.ticket_config.type.create', '全局事项类型-创建'
    UNION ALL SELECT 'platform.ticket_config.type.update', '全局事项类型-更新'
    UNION ALL SELECT 'platform.ticket_config.type.delete', '全局事项类型-删除'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission p WHERE p.code = v.code
);

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.ticket_config.type.read',
    'platform.ticket_config.type.create',
    'platform.ticket_config.type.update',
    'platform.ticket_config.type.delete'
)
WHERE r.code IN ('super_admin', 'platform_admin')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );
