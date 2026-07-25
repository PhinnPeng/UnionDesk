-- 事项类型工作流扁平化：status_flow_config JSON → ticket_type_flow_status / ticket_type_flow_transition
-- 数据迁移使用 MySQL 8 JSON_TABLE 解析 states[] / transitions[]；规则表数据优先覆盖 JSON 边

-- ---------------------------------------------------------------------------
-- 1. 新建扁平工作流表
-- ---------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS ticket_type_flow_status (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    domain_id           BIGINT UNSIGNED NOT NULL COMMENT '平台=0',
    ticket_type_id      BIGINT UNSIGNED NOT NULL,
    state_code          VARCHAR(64)     NOT NULL COMMENT 'ticket_status.code',
    name                VARCHAR(128)    NOT NULL,
    state_type          VARCHAR(32)     NOT NULL COMMENT 'in_progress|paused|terminal',
    allow_customer_withdraw TINYINT     NOT NULL DEFAULT 0,
    is_resolved         TINYINT         NOT NULL DEFAULT 0,
    sort_order          INT             NOT NULL DEFAULT 0,
    source_status_id    BIGINT UNSIGNED NULL COMMENT '可选追溯 ticket_status.id',
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_status (domain_id, ticket_type_id, state_code),
    KEY idx_flow_status_type (domain_id, ticket_type_id, sort_order)
) COMMENT='事项类型工作流状态（点）';

CREATE TABLE IF NOT EXISTS ticket_type_flow_transition (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    domain_id           BIGINT UNSIGNED NOT NULL COMMENT '平台=0',
    ticket_type_id      BIGINT UNSIGNED NOT NULL,
    from_state_code     VARCHAR(64)     NOT NULL COMMENT '状态 code 或 *',
    to_state_code       VARCHAR(64)     NOT NULL,
    step_name           VARCHAR(128)    NOT NULL DEFAULT '',
    permission_mode     VARCHAR(16)     NOT NULL DEFAULT 'none',
    member_ids          JSON            NOT NULL,
    role_ids            JSON            NOT NULL,
    required_slot_ids   JSON            NOT NULL,
    attribute_updates   JSON            NOT NULL,
    sort_order          INT             NOT NULL DEFAULT 0,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_flow_transition (domain_id, ticket_type_id, from_state_code, to_state_code),
    KEY idx_flow_transition_type (domain_id, ticket_type_id),
    KEY idx_flow_transition_from (domain_id, ticket_type_id, from_state_code)
) COMMENT='事项类型工作流边（步骤+规则扁平字段）';

-- ---------------------------------------------------------------------------
-- 2. 从 ticket_type.status_flow_config JSON 迁移 states[]
-- ---------------------------------------------------------------------------

INSERT INTO ticket_type_flow_status (
    domain_id,
    ticket_type_id,
    state_code,
    name,
    state_type,
    allow_customer_withdraw,
    is_resolved,
    sort_order,
    source_status_id
)
SELECT
    CASE WHEN tt.scope = 'platform' THEN 0 ELSE tt.business_domain_id END AS domain_id,
    tt.id AS ticket_type_id,
    TRIM(jt.state_code) AS state_code,
    COALESCE(NULLIF(TRIM(jt.name), ''), TRIM(jt.state_code)) AS name,
    TRIM(jt.state_type) AS state_type,
    CASE
        WHEN jt.allow_customer_withdraw IS TRUE THEN 1
        WHEN jt.allow_customer_withdraw = 1 THEN 1
        ELSE 0
    END AS allow_customer_withdraw,
    CASE
        WHEN jt.is_resolved IS TRUE THEN 1
        WHEN jt.is_resolved = 1 THEN 1
        ELSE 0
    END AS is_resolved,
    jt.array_index - 1 AS sort_order,
    jt.source_status_id
FROM ticket_type tt
CROSS JOIN JSON_TABLE(
    JSON_EXTRACT(tt.status_flow_config, '$.states'),
    '$[*]' COLUMNS (
        array_index FOR ORDINALITY,
        state_code VARCHAR(64) PATH '$.code',
        name VARCHAR(128) PATH '$.name',
        state_type VARCHAR(32) PATH '$.state_type',
        allow_customer_withdraw JSON PATH '$.allow_customer_withdraw',
        is_resolved JSON PATH '$.is_resolved',
        source_status_id BIGINT UNSIGNED PATH '$.source_status_id'
    )
) AS jt
WHERE tt.status_flow_config IS NOT NULL
  AND JSON_TYPE(JSON_EXTRACT(tt.status_flow_config, '$.states')) = 'ARRAY'
  AND JSON_LENGTH(JSON_EXTRACT(tt.status_flow_config, '$.states')) > 0
  AND jt.state_code IS NOT NULL
  AND TRIM(jt.state_code) <> ''
  AND TRIM(jt.state_code) <> '*'
  AND jt.state_type IS NOT NULL
  AND TRIM(jt.state_type) <> '';

-- ---------------------------------------------------------------------------
-- 3. 从 ticket_type.status_flow_config JSON 迁移 transitions[]（空规则默认值）
--    跳过 to='*'；from 须在已迁移 states 中，除非 from='*'
-- ---------------------------------------------------------------------------

INSERT INTO ticket_type_flow_transition (
    domain_id,
    ticket_type_id,
    from_state_code,
    to_state_code,
    step_name,
    permission_mode,
    member_ids,
    role_ids,
    required_slot_ids,
    attribute_updates,
    sort_order
)
SELECT
    CASE WHEN tt.scope = 'platform' THEN 0 ELSE tt.business_domain_id END AS domain_id,
    tt.id AS ticket_type_id,
    TRIM(jt.from_code) AS from_state_code,
    TRIM(jt.to_code) AS to_state_code,
    '' AS step_name,
    'none' AS permission_mode,
    JSON_ARRAY() AS member_ids,
    JSON_ARRAY() AS role_ids,
    JSON_ARRAY() AS required_slot_ids,
    JSON_ARRAY() AS attribute_updates,
    jt.array_index - 1 AS sort_order
FROM ticket_type tt
CROSS JOIN JSON_TABLE(
    JSON_EXTRACT(tt.status_flow_config, '$.transitions'),
    '$[*]' COLUMNS (
        array_index FOR ORDINALITY,
        from_code VARCHAR(64) PATH '$.from',
        to_code VARCHAR(64) PATH '$.to'
    )
) AS jt
LEFT JOIN ticket_type_flow_status fs_from
    ON fs_from.domain_id = CASE WHEN tt.scope = 'platform' THEN 0 ELSE tt.business_domain_id END
   AND fs_from.ticket_type_id = tt.id
   AND fs_from.state_code = TRIM(jt.from_code)
LEFT JOIN ticket_type_flow_status fs_to
    ON fs_to.domain_id = CASE WHEN tt.scope = 'platform' THEN 0 ELSE tt.business_domain_id END
   AND fs_to.ticket_type_id = tt.id
   AND fs_to.state_code = TRIM(jt.to_code)
WHERE tt.status_flow_config IS NOT NULL
  AND JSON_TYPE(JSON_EXTRACT(tt.status_flow_config, '$.transitions')) = 'ARRAY'
  AND JSON_LENGTH(JSON_EXTRACT(tt.status_flow_config, '$.transitions')) > 0
  AND jt.from_code IS NOT NULL
  AND TRIM(jt.from_code) <> ''
  AND jt.to_code IS NOT NULL
  AND TRIM(jt.to_code) <> ''
  AND TRIM(jt.to_code) <> '*'
  AND (TRIM(jt.from_code) = '*' OR fs_from.id IS NOT NULL)
  AND fs_to.id IS NOT NULL;

-- ---------------------------------------------------------------------------
-- 4. 复制 ticket_transition_rule → ticket_type_flow_transition（规则优先）
--     注：ticket_transition_rule 表可能尚未创建（上游迁移未执行），跳过
-- ---------------------------------------------------------------------------

-- ---------------------------------------------------------------------------
-- 5. 清空平台 feedback / suggestion 工作流（按 scope+code，非硬编码 id）
-- ---------------------------------------------------------------------------

DELETE t
FROM ticket_type_flow_transition t
INNER JOIN ticket_type tt ON tt.id = t.ticket_type_id
WHERE tt.scope = 'platform'
  AND tt.code IN ('feedback', 'suggestion');

DELETE s
FROM ticket_type_flow_status s
INNER JOIN ticket_type tt ON tt.id = s.ticket_type_id
WHERE tt.scope = 'platform'
  AND tt.code IN ('feedback', 'suggestion');

-- ---------------------------------------------------------------------------
-- 6. 移除旧 JSON 列与规则表
-- ---------------------------------------------------------------------------

ALTER TABLE ticket_type DROP COLUMN status_flow_config;
