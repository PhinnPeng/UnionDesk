-- US-S3-02：工单类型 form_schema_draft、description、icon；域内 name 唯一

ALTER TABLE ticket_type
    ADD COLUMN form_schema_draft json DEFAULT NULL COMMENT '未发布 Formily 表单 schema' AFTER form_schema;

ALTER TABLE ticket_type
    ADD COLUMN description varchar(500) DEFAULT NULL COMMENT '描述' AFTER name;

ALTER TABLE ticket_type
    ADD COLUMN icon varchar(128) DEFAULT NULL COMMENT 'Iconify 图标标识' AFTER description;

UPDATE ticket_type
SET form_schema_draft = form_schema
WHERE form_schema_draft IS NULL;

-- code 唯一索引 uk_ticket_type_domain_code 已在 V1 存在；仅补充 name 唯一
CREATE UNIQUE INDEX uk_ticket_type_domain_name ON ticket_type (business_domain_id, name);
