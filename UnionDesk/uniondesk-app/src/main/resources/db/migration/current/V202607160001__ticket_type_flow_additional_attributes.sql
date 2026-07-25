-- 工作流转换规则：附加属性配置（转换前补录属性）
ALTER TABLE ticket_type_flow_transition
    ADD COLUMN additional_attributes JSON NULL COMMENT '附加属性配置 JSON 数组' AFTER attribute_updates;

UPDATE ticket_type_flow_transition
SET additional_attributes = JSON_ARRAY()
WHERE additional_attributes IS NULL;

ALTER TABLE ticket_type_flow_transition
    MODIFY COLUMN additional_attributes JSON NOT NULL COMMENT '附加属性配置 JSON 数组';
