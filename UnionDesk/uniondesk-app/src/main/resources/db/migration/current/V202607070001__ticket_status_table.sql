-- 事项状态字典（平台/域 scope，MVP 仅平台 CRUD）

CREATE TABLE ticket_status (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scope               VARCHAR(16)     NOT NULL COMMENT 'platform | domain',
    business_domain_id  BIGINT UNSIGNED NULL COMMENT 'domain 必填；platform 为 NULL',
    code                VARCHAR(64)     NOT NULL,
    name                VARCHAR(128)    NOT NULL,
    description         VARCHAR(500)    NOT NULL DEFAULT '',
    category            VARCHAR(32)     NOT NULL COMMENT 'not_started | in_progress | completed',
    state_type          VARCHAR(32)     NOT NULL COMMENT 'in_progress | paused | terminal',
    config_json         JSON            NOT NULL,
    status              VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT 'active | disabled',
    sort_order          INT             NOT NULL DEFAULT 0,
    is_system           TINYINT         NOT NULL DEFAULT 0,
    source_status_id    BIGINT UNSIGNED NULL,
    created_by          BIGINT UNSIGNED NULL,
    updated_by          BIGINT UNSIGNED NULL,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    scope_domain_key    BIGINT UNSIGNED GENERATED ALWAYS AS (
        CASE WHEN scope = 'platform' THEN 0 ELSE business_domain_id END
    ) STORED,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_status_scope_domain_code (scope, scope_domain_key, code),
    UNIQUE KEY uk_ticket_status_scope_domain_name (scope, scope_domain_key, name),
    KEY idx_ticket_status_scope_domain_sort (scope, scope_domain_key, sort_order)
) COMMENT='事项状态字典';
