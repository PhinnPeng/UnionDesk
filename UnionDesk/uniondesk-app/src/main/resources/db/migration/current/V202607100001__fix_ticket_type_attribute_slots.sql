-- 修复事项类型属性关联，确保 feedback、suggestion 仅关联描述属性
-- 描述为必填，采用简单事项的约束方式

-- 获取系统属性ID
SET @title_attr_id = (SELECT id FROM ticket_attribute WHERE name = '标题' AND is_system = 1 LIMIT 1);
SET @desc_attr_id = (SELECT id FROM ticket_attribute WHERE name = '描述' AND is_system = 1 LIMIT 1);

-- 获取平台级事项类型ID
SET @feedback_type_id = (SELECT id FROM ticket_type WHERE scope = 'platform' AND code = 'feedback' LIMIT 1);
SET @suggestion_type_id = (SELECT id FROM ticket_type WHERE scope = 'platform' AND code = 'suggestion' LIMIT 1);

-- 清理 feedback 和 suggestion 的现有属性关联（保留标题和描述，删除其他）
DELETE FROM ticket_type_attribute
WHERE ticket_type_id IN (@feedback_type_id, @suggestion_type_id)
  AND attribute_id NOT IN (@title_attr_id, @desc_attr_id);

-- 为 feedback 关联描述属性（如果不存在）
INSERT IGNORE INTO ticket_type_attribute (
    ticket_type_id, attribute_id, sort_order, slot_config, status, created_at, updated_at
)
SELECT @feedback_type_id, @desc_attr_id, 0,
       JSON_OBJECT('required', true, 'visible_to_customer', true, 'default_value', NULL),
       'enabled', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE @feedback_type_id IS NOT NULL AND @desc_attr_id IS NOT NULL;

-- 为 suggestion 关联描述属性（如果不存在）
INSERT IGNORE INTO ticket_type_attribute (
    ticket_type_id, attribute_id, sort_order, slot_config, status, created_at, updated_at
)
SELECT @suggestion_type_id, @desc_attr_id, 0,
       JSON_OBJECT('required', true, 'visible_to_customer', true, 'default_value', NULL),
       'enabled', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE @suggestion_type_id IS NOT NULL AND @desc_attr_id IS NOT NULL;

-- 更新现有描述属性的 slot_config，确保描述为必填
UPDATE ticket_type_attribute
SET slot_config = JSON_OBJECT('required', true, 'visible_to_customer', true, 'default_value', NULL)
WHERE ticket_type_id IN (@feedback_type_id, @suggestion_type_id)
  AND attribute_id = @desc_attr_id;
