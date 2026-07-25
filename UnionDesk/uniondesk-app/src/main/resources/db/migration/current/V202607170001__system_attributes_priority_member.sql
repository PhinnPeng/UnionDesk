-- P0/P1/P2/P3: 系统属性 system_key、优先级彩色、处理人/关注人种子、关注人表

-- ========== P0: system_key ==========
ALTER TABLE ticket_attribute
    ADD COLUMN system_key VARCHAR(32) NULL COMMENT '系统字段键 title/description/priority/assignee/watchers' AFTER is_system;

UPDATE ticket_attribute
SET system_key = 'title', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'platform' AND business_domain_id IS NULL AND name = '标题' AND is_system = 1;

UPDATE ticket_attribute
SET system_key = 'description', updated_at = CURRENT_TIMESTAMP(3)
WHERE scope = 'platform' AND business_domain_id IS NULL AND name = '描述' AND is_system = 1;

CREATE UNIQUE INDEX uk_ticket_attribute_system_key
    ON ticket_attribute (system_key);

-- ========== P1: 优先级 color/icon ==========
ALTER TABLE ticket_priority_level
    ADD COLUMN color VARCHAR(16) NULL COMMENT '展示色 #RRGGBB' AFTER name,
    ADD COLUMN icon VARCHAR(32) NULL COMMENT '图标键 urgent/high/normal/low' AFTER color;

-- 标准化已有档位
UPDATE ticket_priority_level
SET code = 'low', name = '低', color = '#8c8c8c', icon = 'low',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code IN ('low', '低') OR name IN ('低', 'low');

UPDATE ticket_priority_level
SET code = 'normal', name = '中', color = '#1677ff', icon = 'normal', is_default = 1,
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code IN ('normal', '中', 'medium') OR name IN ('中', 'normal', '普通');

UPDATE ticket_priority_level
SET code = 'high', name = '高', color = '#fa8c16', icon = 'high',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code IN ('high', '高') OR name IN ('高', 'high');

UPDATE ticket_priority_level
SET code = 'urgent', name = '紧急', color = '#f5222d', icon = 'urgent',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE code IN ('urgent', '紧急', 'critical') OR name IN ('紧急', 'urgent');

-- 回填缺省 color/icon
UPDATE ticket_priority_level SET color = '#8c8c8c', icon = 'low' WHERE code = 'low' AND (color IS NULL OR icon IS NULL);
UPDATE ticket_priority_level SET color = '#1677ff', icon = 'normal' WHERE code = 'normal' AND (color IS NULL OR icon IS NULL);
UPDATE ticket_priority_level SET color = '#fa8c16', icon = 'high' WHERE code = 'high' AND (color IS NULL OR icon IS NULL);
UPDATE ticket_priority_level SET color = '#f5222d', icon = 'urgent' WHERE code = 'urgent' AND (color IS NULL OR icon IS NULL);

-- 每个域补 urgent（若不存在）
INSERT INTO ticket_priority_level (business_domain_id, code, name, color, icon, sort_order, is_default, status, created_at, updated_at)
SELECT d.id, 'urgent', '紧急', '#f5222d', 'urgent', 0, 0, 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
FROM business_domain d
WHERE NOT EXISTS (
    SELECT 1 FROM ticket_priority_level p
    WHERE p.business_domain_id = d.id AND p.code = 'urgent'
);

-- 确保每域有 low/normal/high
INSERT INTO ticket_priority_level (business_domain_id, code, name, color, icon, sort_order, is_default, status, created_at, updated_at)
SELECT d.id, 'low', '低', '#8c8c8c', 'low', 30, 0, 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
FROM business_domain d
WHERE NOT EXISTS (SELECT 1 FROM ticket_priority_level p WHERE p.business_domain_id = d.id AND p.code = 'low');

INSERT INTO ticket_priority_level (business_domain_id, code, name, color, icon, sort_order, is_default, status, created_at, updated_at)
SELECT d.id, 'normal', '中', '#1677ff', 'normal', 20, 1, 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
FROM business_domain d
WHERE NOT EXISTS (SELECT 1 FROM ticket_priority_level p WHERE p.business_domain_id = d.id AND p.code = 'normal');

INSERT INTO ticket_priority_level (business_domain_id, code, name, color, icon, sort_order, is_default, status, created_at, updated_at)
SELECT d.id, 'high', '高', '#fa8c16', 'high', 10, 0, 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
FROM business_domain d
WHERE NOT EXISTS (SELECT 1 FROM ticket_priority_level p WHERE p.business_domain_id = d.id AND p.code = 'high');

ALTER TABLE ticket_priority_level
    MODIFY COLUMN color VARCHAR(16) NOT NULL DEFAULT '#1677ff',
    MODIFY COLUMN icon VARCHAR(32) NOT NULL DEFAULT 'normal';

-- 种子系统属性：优先级 / 处理人 / 关注人
INSERT INTO ticket_attribute (
    scope, business_domain_id, name, description, field_type, type_config,
    status, sort_order, is_system, system_key, source_attribute_id, created_at, updated_at
)
SELECT 'platform', NULL, '优先级', '事项优先级，对应域内标准优先级档位', 'select', '{"options_source":"priority_levels"}',
       'active', 2, 1, 'priority', NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
    SELECT 1 FROM ticket_attribute
    WHERE scope = 'platform' AND business_domain_id IS NULL AND system_key = 'priority'
);

INSERT INTO ticket_attribute (
    scope, business_domain_id, name, description, field_type, type_config,
    status, sort_order, is_system, system_key, source_attribute_id, created_at, updated_at
)
SELECT 'platform', NULL, '处理人', '事项当前处理人（域内或平台员工）', 'member', '{"multiple":false,"scope_mode":"auto"}',
       'active', 3, 1, 'assignee', NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
    SELECT 1 FROM ticket_attribute
    WHERE scope = 'platform' AND business_domain_id IS NULL AND system_key = 'assignee'
);

INSERT INTO ticket_attribute (
    scope, business_domain_id, name, description, field_type, type_config,
    status, sort_order, is_system, system_key, source_attribute_id, created_at, updated_at
)
SELECT 'platform', NULL, '关注人', '关注该事项的员工（域内或平台）', 'member', '{"multiple":true,"scope_mode":"auto"}',
       'active', 4, 1, 'watchers', NULL, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (
    SELECT 1 FROM ticket_attribute
    WHERE scope = 'platform' AND business_domain_id IS NULL AND system_key = 'watchers'
);

-- ========== P3: 关注人表 ==========
CREATE TABLE IF NOT EXISTS ticket_watcher (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    ticket_id BIGINT UNSIGNED NOT NULL,
    staff_account_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_ticket_watcher (ticket_id, staff_account_id),
    KEY idx_ticket_watcher_staff (staff_account_id),
    CONSTRAINT fk_ticket_watcher_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id)
) COMMENT='工单关注人';
