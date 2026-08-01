-- 修复 Flyway 迁移失败问题
-- 执行此脚本前请确保备份数据库

-- 1. 检查 flyway_schema_history 表中 202606230001 的状态
SELECT * FROM flyway_schema_history WHERE version = '202606230001';

-- 2. 如果状态为 FAILED，删除该记录（Flyway 会重新执行该迁移）
-- DELETE FROM flyway_schema_history WHERE version = '202606230001' AND success = 0;

-- 3. 或者，如果希望手动修复数据，可以先检查 ticket_attribute 表是否已有系统属性
SELECT * FROM ticket_attribute WHERE scope = 'platform' AND is_system = 1;

-- 4. 如果已有数据，可以手动插入缺失的系统属性（标题、描述）
-- 如果表已存在但数据不完整，可以手动执行以下插入：

INSERT INTO ticket_attribute (
    scope, business_domain_id, name, description, field_type, type_config,
    status, sort_order, is_system, source_attribute_id, created_at, updated_at
)
SELECT
    'platform', NULL, v.name, v.description, v.field_type, v.type_config,
    'active', v.sort_order, 1, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
FROM (
    SELECT
        '标题' AS name,
        '事项标题，用于快速识别事项内容' AS description,
        'input' AS field_type,
        '{"format":"text"}' AS type_config,
        0 AS sort_order
    UNION ALL
    SELECT
        '描述' AS name,
        '事项详细描述，用于补充事项的详细信息' AS description,
        'input' AS field_type,
        '{"format":"text","multiline":true}' AS type_config,
        1 AS sort_order
) AS v
WHERE NOT EXISTS (
    SELECT 1 FROM ticket_attribute
    WHERE scope = 'platform' AND name = v.name AND is_system = 1
);

-- 5. 然后手动标记迁移为成功（如果确定数据已正确插入）
-- INSERT INTO flyway_schema_history (version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
-- VALUES ('202606230001', 'seed system ticket attributes', 'SQL', 'V202606230001__seed_system_ticket_attributes.sql', -1, 'root', NOW(), 0, 1);
