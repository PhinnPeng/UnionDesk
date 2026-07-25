-- 步骤转换规则表：存储工作流中每个 from→to 转换的详细规则配置

CREATE TABLE ticket_transition_rule (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    domain_id           BIGINT UNSIGNED NOT NULL COMMENT '业务域 ID，平台级为 0',
    ticket_type_id      BIGINT UNSIGNED NOT NULL COMMENT '事项类型 ID',
    from_state_code     VARCHAR(64)     NOT NULL COMMENT '源状态 code（关联 status_flow.states[].code）',
    to_state_code       VARCHAR(64)     NOT NULL COMMENT '目标状态 code',
    step_name           VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '步骤显示名称',

    -- 规则配置
    permission_mode     VARCHAR(16)     NOT NULL DEFAULT 'none' COMMENT 'none | members | roles',
    member_ids          JSON            NOT NULL COMMENT '允许的成员 ID 列表 []',
    role_ids            JSON            NOT NULL COMMENT '允许的角色 ID 列表 []',
    required_slot_ids   JSON            NOT NULL COMMENT '附加属性槽位 ID 列表 []',
    attribute_updates   JSON            NOT NULL COMMENT '属性值变更 [{slot_id, value, value_type}]',

    sort_order          INT             NOT NULL DEFAULT 0,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_transition_unique (domain_id, ticket_type_id, from_state_code, to_state_code),
    KEY idx_transition_type (domain_id, ticket_type_id),
    KEY idx_transition_from (domain_id, ticket_type_id, from_state_code)
) COMMENT='步骤转换规则';
