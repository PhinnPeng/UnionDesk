-- 平台级系统预置状态：未开始 / 处理中 / 已完成 / 已取消

INSERT IGNORE INTO ticket_status (
    scope, business_domain_id, code, name, description, category, state_type,
    config_json, status, sort_order, is_system, source_status_id, created_at, updated_at
)
VALUES
    ('platform', NULL, 'not_started', '未开始', '', 'not_started', 'paused', '{}', 'active', 0, 1, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    ('platform', NULL, 'in_progress', '处理中', '', 'in_progress', 'in_progress', '{}', 'active', 1, 1, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    ('platform', NULL, 'completed', '已完成', '', 'completed', 'terminal', '{}', 'active', 2, 1, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)),
    ('platform', NULL, 'cancelled', '已取消', '', 'completed', 'terminal', '{}', 'active', 3, 1, NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3));
