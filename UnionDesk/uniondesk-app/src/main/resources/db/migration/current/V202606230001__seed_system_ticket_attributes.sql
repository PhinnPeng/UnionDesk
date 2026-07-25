-- S1 种入平台级系统属性：标题、描述
-- 注意：此迁移依赖于 V202606210001 创建的 ticket_attribute 表

INSERT IGNORE INTO ticket_attribute (
    scope, business_domain_id, name, description, field_type, type_config,
    status, sort_order, is_system, source_attribute_id, created_at, updated_at
)
VALUES
    ('platform', NULL, '标题', '事项标题，用于快速识别事项内容', 'input', '{"format":"text"}', 'active', 0, 1, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    ('platform', NULL, '描述', '事项详细描述，用于补充事项的详细信息', 'input', '{"format":"text","multiline":true}', 'active', 1, 1, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));
