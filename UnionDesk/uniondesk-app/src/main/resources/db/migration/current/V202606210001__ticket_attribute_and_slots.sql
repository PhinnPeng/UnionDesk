-- S1 事项属性：属性字典 + 类型插槽；form_schema 增加 plugin_revision

CREATE TABLE ticket_attribute (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scope               VARCHAR(16)     NOT NULL COMMENT 'platform | domain',
    business_domain_id  BIGINT UNSIGNED NULL COMMENT 'domain 必填；platform 为 NULL',
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(500)    NOT NULL,
    field_type          VARCHAR(32)     NOT NULL COMMENT 'input | select | switch | date',
    type_config         JSON            NOT NULL,
    status              VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT 'active | disabled',
    sort_order          INT             NOT NULL DEFAULT 0,
    is_system           TINYINT         NOT NULL DEFAULT 0,
    source_attribute_id BIGINT UNSIGNED NULL,
    created_by          BIGINT UNSIGNED NULL,
    updated_by          BIGINT UNSIGNED NULL,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    scope_domain_key    BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN scope = 'platform' THEN 0 ELSE business_domain_id END
    ) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_attribute_scope_domain_name (scope, scope_domain_key, name),
    KEY idx_ticket_attribute_scope_domain_sort (scope, scope_domain_key, sort_order)
) COMMENT='事项属性字典';

CREATE TABLE ticket_type_attribute (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_type_id  BIGINT UNSIGNED NOT NULL,
    attribute_id    BIGINT UNSIGNED NOT NULL,
    sort_order      INT             NOT NULL DEFAULT 0,
    slot_config     JSON            NOT NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'enabled' COMMENT 'enabled | disabled',
    created_by      BIGINT UNSIGNED NULL,
    updated_by      BIGINT UNSIGNED NULL,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_type_attribute_type_attr (ticket_type_id, attribute_id),
    KEY idx_ticket_type_attribute_type_sort (ticket_type_id, sort_order)
) COMMENT='事项类型属性插槽';

ALTER TABLE ticket_form_schema
    ADD COLUMN plugin_revision VARCHAR(64) NULL COMMENT '插槽集合 hash，用于 has_unpublished' AFTER form_schema;

ALTER TABLE ticket_type
    ADD COLUMN category VARCHAR(32) NOT NULL DEFAULT 'transaction' COMMENT 'transaction | feedback' AFTER icon;
