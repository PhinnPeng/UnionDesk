-- P0 PRD 数据模型扩展：与《产品需求文档 v1.0》§5 及 P0 接口契约对齐。
-- 原则：仅追加表/列；保留 legacy 表（user_account、ticket_event_log、operation_log、auth_login_log 等）供过渡期代码使用。
-- 幂等：新表使用 IF NOT EXISTS；列通过 information_schema 条件追加，便于手工重放或修复环境。

-- ---------------------------------------------------------------------------
-- business_domain 扩展（P0 接口：visibility_policy_codes、registration_policy）
-- ---------------------------------------------------------------------------
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'business_domain' AND COLUMN_NAME = 'registration_policy');
SET @sql := IF(@exist = 0, 'ALTER TABLE business_domain ADD COLUMN registration_policy VARCHAR(32) NOT NULL DEFAULT ''open'' COMMENT ''open|invitation_only|admin_only''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'business_domain' AND COLUMN_NAME = 'visibility_policy_codes');
SET @sql := IF(@exist = 0, 'ALTER TABLE business_domain ADD COLUMN visibility_policy_codes JSON NULL COMMENT ''PRD: public|domain_customer_only|channel_only''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'business_domain' AND COLUMN_NAME = 'deleted_at');
SET @sql := IF(@exist = 0, 'ALTER TABLE business_domain ADD COLUMN deleted_at DATETIME(3) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'business_domain' AND COLUMN_NAME = 'logo');
SET @sql := IF(@exist = 0, 'ALTER TABLE business_domain ADD COLUMN logo VARCHAR(512) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE business_domain
SET visibility_policy_codes = JSON_ARRAY('public')
WHERE visibility_policy_codes IS NULL;

-- ---------------------------------------------------------------------------
-- 统一身份与账号（PRD：identity_subject + staff_account + customer_account）
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS identity_subject (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_type VARCHAR(16) NOT NULL DEFAULT 'person',
    phone VARCHAR(32) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    merged_into_id BIGINT UNSIGNED DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_identity_subject_phone (phone),
    KEY idx_identity_subject_merged (merged_into_id),
    CONSTRAINT fk_identity_subject_merged FOREIGN KEY (merged_into_id) REFERENCES identity_subject (id),
    CONSTRAINT chk_identity_subject_type CHECK (subject_type IN ('person', 'system'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一身份主体';

CREATE TABLE IF NOT EXISTS staff_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_id BIGINT UNSIGNED NOT NULL,
    login_name VARCHAR(64) NOT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(128) DEFAULT NULL,
    password_hash VARCHAR(255) NOT NULL,
    must_change_password TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    source VARCHAR(32) NOT NULL DEFAULT 'local',
    auth_version INT UNSIGNED NOT NULL DEFAULT 1,
    password_changed_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_account_login (login_name),
    UNIQUE KEY uk_staff_account_subject (subject_id),
    CONSTRAINT fk_staff_account_subject FOREIGN KEY (subject_id) REFERENCES identity_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工账号';

CREATE TABLE IF NOT EXISTS customer_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_id BIGINT UNSIGNED NOT NULL,
    login_name VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) DEFAULT NULL,
    avatar_url VARCHAR(512) DEFAULT NULL,
    phone VARCHAR(32) NOT NULL,
    email VARCHAR(128) DEFAULT NULL,
    password_hash VARCHAR(255) NOT NULL,
    must_change_password TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    source VARCHAR(32) NOT NULL DEFAULT 'local',
    auth_version INT UNSIGNED NOT NULL DEFAULT 1,
    password_changed_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_account_login (login_name),
    UNIQUE KEY uk_customer_account_subject (subject_id),
    CONSTRAINT fk_customer_account_subject FOREIGN KEY (subject_id) REFERENCES identity_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户账号';

CREATE TABLE IF NOT EXISTS identity_binding (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_id BIGINT UNSIGNED NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_uid VARCHAR(128) NOT NULL,
    provider_profile JSON DEFAULT NULL,
    bound_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_identity_binding_provider_uid (provider, provider_uid),
    KEY idx_identity_binding_subject (subject_id),
    CONSTRAINT fk_identity_binding_subject FOREIGN KEY (subject_id) REFERENCES identity_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='第三方身份绑定';

CREATE TABLE IF NOT EXISTS platform_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    preset TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_platform_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台角色';

CREATE TABLE IF NOT EXISTS staff_account_platform_role (
    staff_account_id BIGINT UNSIGNED NOT NULL,
    platform_role_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (staff_account_id, platform_role_id),
    CONSTRAINT fk_sapr_staff FOREIGN KEY (staff_account_id) REFERENCES staff_account (id),
    CONSTRAINT fk_sapr_role FOREIGN KEY (platform_role_id) REFERENCES platform_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工平台角色';

CREATE TABLE IF NOT EXISTS domain_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    preset TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_domain_role_domain_code (business_domain_id, code),
    CONSTRAINT fk_domain_role_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务域内角色';

CREATE TABLE IF NOT EXISTS permission_item (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(128) NOT NULL,
    name VARCHAR(128) NOT NULL,
    module VARCHAR(64) NOT NULL DEFAULT '',
    type VARCHAR(16) NOT NULL DEFAULT 'api',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_permission_item_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限项（目标态，可与 iam_permission 并存）';

CREATE TABLE IF NOT EXISTS domain_role_permission (
    domain_role_id BIGINT UNSIGNED NOT NULL,
    permission_item_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (domain_role_id, permission_item_id),
    CONSTRAINT fk_drp_role FOREIGN KEY (domain_role_id) REFERENCES domain_role (id),
    CONSTRAINT fk_drp_item FOREIGN KEY (permission_item_id) REFERENCES permission_item (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='域角色与权限项';

CREATE TABLE IF NOT EXISTS domain_member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    staff_account_id BIGINT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    source VARCHAR(32) NOT NULL DEFAULT 'invite',
    activated_at DATETIME(3) DEFAULT NULL,
    disabled_at DATETIME(3) DEFAULT NULL,
    deleted_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_domain_member_staff_domain (staff_account_id, business_domain_id),
    KEY idx_domain_member_domain_status (business_domain_id, status),
    CONSTRAINT fk_domain_member_staff FOREIGN KEY (staff_account_id) REFERENCES staff_account (id),
    CONSTRAINT fk_domain_member_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='域成员（员工）';

CREATE TABLE IF NOT EXISTS domain_member_role (
    domain_member_id BIGINT UNSIGNED NOT NULL,
    domain_role_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (domain_member_id, domain_role_id),
    CONSTRAINT fk_dmr_member FOREIGN KEY (domain_member_id) REFERENCES domain_member (id),
    CONSTRAINT fk_dmr_role FOREIGN KEY (domain_role_id) REFERENCES domain_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='域成员角色';

CREATE TABLE IF NOT EXISTS domain_customer (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_account_id BIGINT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    source VARCHAR(32) NOT NULL DEFAULT 'self_register',
    activated_at DATETIME(3) DEFAULT NULL,
    disabled_at DATETIME(3) DEFAULT NULL,
    deleted_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_domain_customer_account_domain (customer_account_id, business_domain_id),
    KEY idx_domain_customer_domain_status (business_domain_id, status),
    CONSTRAINT fk_domain_customer_account FOREIGN KEY (customer_account_id) REFERENCES customer_account (id),
    CONSTRAINT fk_domain_customer_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户入域关系';

CREATE TABLE IF NOT EXISTS invitation_code (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(64) NOT NULL,
    channel VARCHAR(64) DEFAULT NULL,
    expires_at DATETIME(3) DEFAULT NULL,
    max_uses INT UNSIGNED DEFAULT NULL,
    used_count INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_invitation_domain_code (business_domain_id, code),
    CONSTRAINT fk_invitation_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='邀请码';

CREATE TABLE IF NOT EXISTS domain_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,
    value_type VARCHAR(16) NOT NULL DEFAULT 'string',
    description VARCHAR(255) DEFAULT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_domain_config (business_domain_id, config_key),
    CONSTRAINT fk_domain_config_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务域配置';

CREATE TABLE IF NOT EXISTS domain_template (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_domain_id BIGINT UNSIGNED DEFAULT NULL,
    template_type VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    snapshot_json JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'draft',
    created_by BIGINT UNSIGNED DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_domain_template_source (source_domain_id),
    CONSTRAINT fk_domain_template_source FOREIGN KEY (source_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务域模板';

CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(128) NOT NULL,
    config_value TEXT,
    value_type VARCHAR(16) NOT NULL DEFAULT 'string',
    description VARCHAR(255) DEFAULT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_system_config_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='平台系统配置';

-- ---------------------------------------------------------------------------
-- 附件、SLA、工单扩展表
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS file_attachment (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED DEFAULT NULL,
    uploader_subject_id BIGINT UNSIGNED DEFAULT NULL,
    portal_type VARCHAR(16) DEFAULT NULL,
    file_name VARCHAR(512) NOT NULL,
    mime_type VARCHAR(128) NOT NULL,
    file_size BIGINT UNSIGNED NOT NULL,
    storage_type VARCHAR(16) NOT NULL,
    storage_key VARCHAR(512) NOT NULL,
    checksum VARCHAR(128) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_file_attachment_domain_created (business_domain_id, created_at),
    KEY idx_file_attachment_subject (uploader_subject_id),
    CONSTRAINT fk_file_attachment_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id),
    CONSTRAINT fk_file_attachment_subject FOREIGN KEY (uploader_subject_id) REFERENCES identity_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='统一附件主表';

CREATE TABLE IF NOT EXISTS attachment_ref (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    attachment_id BIGINT UNSIGNED NOT NULL,
    target_type VARCHAR(32) NOT NULL,
    target_id BIGINT UNSIGNED NOT NULL,
    relation_type VARCHAR(32) NOT NULL DEFAULT 'primary',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_attachment_ref_target (target_type, target_id),
    CONSTRAINT fk_attachment_ref_file FOREIGN KEY (attachment_id) REFERENCES file_attachment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件业务关联';

CREATE TABLE IF NOT EXISTS attachment_policy (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scope_type VARCHAR(16) NOT NULL,
    scope_id BIGINT UNSIGNED NOT NULL,
    allowed_types_json JSON DEFAULT NULL,
    max_single_size_mb INT UNSIGNED DEFAULT NULL,
    max_total_size_mb INT UNSIGNED DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_attachment_policy_scope (scope_type, scope_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件策略';

CREATE TABLE IF NOT EXISTS sla_calendar (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    name VARCHAR(128) NOT NULL,
    config JSON NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sla_calendar_domain (business_domain_id),
    CONSTRAINT fk_sla_calendar_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SLA 工作日历';

CREATE TABLE IF NOT EXISTS sla_rule (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    ticket_type_id BIGINT UNSIGNED DEFAULT NULL,
    calendar_id BIGINT UNSIGNED DEFAULT NULL,
    first_response_minutes INT UNSIGNED DEFAULT NULL,
    resolution_minutes INT UNSIGNED DEFAULT NULL,
    is_urgent_config TINYINT NOT NULL DEFAULT 0,
    breach_action_json JSON DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_sla_rule_domain_type (business_domain_id, ticket_type_id),
    CONSTRAINT fk_sla_rule_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id),
    CONSTRAINT fk_sla_rule_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_type (id),
    CONSTRAINT fk_sla_rule_calendar FOREIGN KEY (calendar_id) REFERENCES sla_calendar (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SLA 规则';

CREATE TABLE IF NOT EXISTS ticket_priority_level (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    is_default TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_priority_domain_code (business_domain_id, code),
    CONSTRAINT fk_ticket_priority_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单优先级';

CREATE TABLE IF NOT EXISTS ticket_template (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    ticket_type_id BIGINT UNSIGNED NOT NULL,
    scope VARCHAR(16) NOT NULL,
    name VARCHAR(128) NOT NULL,
    content_json JSON NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    sort_order INT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ticket_template_domain_type (business_domain_id, ticket_type_id),
    CONSTRAINT fk_ticket_template_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id),
    CONSTRAINT fk_ticket_template_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_type (id),
    CONSTRAINT chk_ticket_template_scope CHECK (scope IN ('internal', 'customer'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单模板';

CREATE TABLE IF NOT EXISTS ticket_relation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    source_ticket_id BIGINT UNSIGNED NOT NULL,
    target_ticket_id BIGINT UNSIGNED NOT NULL,
    relation_type VARCHAR(32) NOT NULL,
    created_by_staff_account_id BIGINT UNSIGNED DEFAULT NULL,
    note VARCHAR(512) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ticket_relation_source (source_ticket_id),
    KEY idx_ticket_relation_target (target_ticket_id),
    CONSTRAINT fk_ticket_relation_source FOREIGN KEY (source_ticket_id) REFERENCES ticket (id),
    CONSTRAINT fk_ticket_relation_target FOREIGN KEY (target_ticket_id) REFERENCES ticket (id),
    CONSTRAINT fk_ticket_relation_creator FOREIGN KEY (created_by_staff_account_id) REFERENCES staff_account (id),
    CONSTRAINT chk_ticket_relation_type CHECK (relation_type IN ('merge', 'follow_up'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单关联/合并';

CREATE TABLE IF NOT EXISTS ticket_history (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    action VARCHAR(64) NOT NULL,
    from_value VARCHAR(255) DEFAULT NULL,
    to_value VARCHAR(255) DEFAULT NULL,
    operator_subject_id BIGINT UNSIGNED DEFAULT NULL,
    operator_actor_type VARCHAR(16) DEFAULT NULL,
    payload JSON DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ticket_history_ticket_created (ticket_id, created_at),
    CONSTRAINT fk_ticket_history_ticket FOREIGN KEY (ticket_id) REFERENCES ticket (id),
    CONSTRAINT fk_ticket_history_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id),
    CONSTRAINT fk_ticket_history_operator FOREIGN KEY (operator_subject_id) REFERENCES identity_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单操作流（PRD ticket_history）';

CREATE TABLE IF NOT EXISTS dynamic_field_config (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    ticket_type_id BIGINT UNSIGNED NOT NULL,
    field_code VARCHAR(64) NOT NULL,
    field_type VARCHAR(32) NOT NULL,
    config_json JSON NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_dynamic_field_domain_type_code (business_domain_id, ticket_type_id, field_code),
    CONSTRAINT fk_dynamic_field_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id),
    CONSTRAINT fk_dynamic_field_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_type (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单动态字段定义';

CREATE TABLE IF NOT EXISTS quick_reply_template (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    scope_type VARCHAR(32) NOT NULL DEFAULT 'ticket',
    title VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    category VARCHAR(64) DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    sort_order INT NOT NULL DEFAULT 0,
    created_by BIGINT UNSIGNED DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_quick_reply_domain (business_domain_id, status),
    CONSTRAINT fk_quick_reply_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='快捷回复模板';

CREATE TABLE IF NOT EXISTS staff_presence (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    staff_account_id BIGINT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'offline',
    current_session_count INT UNSIGNED NOT NULL DEFAULT 0,
    max_concurrent_sessions INT UNSIGNED NOT NULL DEFAULT 3,
    last_heartbeat_at DATETIME(3) DEFAULT NULL,
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_presence_staff_domain (staff_account_id, business_domain_id),
    CONSTRAINT fk_staff_presence_staff FOREIGN KEY (staff_account_id) REFERENCES staff_account (id),
    CONSTRAINT fk_staff_presence_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服在线态';

-- ticket / ticket_type / ticket_reply / consultation_session 扩展列
SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket_type' AND COLUMN_NAME = 'status_flow_config');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket_type ADD COLUMN status_flow_config JSON NULL COMMENT ''状态机 JSON''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'version');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN version INT UNSIGNED NOT NULL DEFAULT 1 COMMENT ''乐观锁''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'result');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN result VARCHAR(32) DEFAULT NULL COMMENT ''反馈/建议内部结论''', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'sla_first_response_deadline');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN sla_first_response_deadline DATETIME(3) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'sla_resolution_deadline');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN sla_resolution_deadline DATETIME(3) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'sla_first_responded_at');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN sla_first_responded_at DATETIME(3) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'sla_resolved_at');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN sla_resolved_at DATETIME(3) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'sla_status');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN sla_status VARCHAR(32) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'sla_paused_duration');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN sla_paused_duration INT UNSIGNED NOT NULL DEFAULT 0', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'sla_pause_started_at');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN sla_pause_started_at DATETIME(3) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'assignee_staff_account_id');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN assignee_staff_account_id BIGINT UNSIGNED DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND COLUMN_NAME = 'customer_account_id');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD COLUMN customer_account_id BIGINT UNSIGNED DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND INDEX_NAME = 'idx_ticket_assignee_staff');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD KEY idx_ticket_assignee_staff (assignee_staff_account_id, status, updated_at)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND CONSTRAINT_NAME = 'fk_ticket_assignee_staff');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD CONSTRAINT fk_ticket_assignee_staff FOREIGN KEY (assignee_staff_account_id) REFERENCES staff_account (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket' AND CONSTRAINT_NAME = 'fk_ticket_customer_account');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket ADD CONSTRAINT fk_ticket_customer_account FOREIGN KEY (customer_account_id) REFERENCES customer_account (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket_reply' AND COLUMN_NAME = 'sender_type');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket_reply ADD COLUMN sender_type VARCHAR(16) DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket_reply' AND COLUMN_NAME = 'staff_account_id');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket_reply ADD COLUMN staff_account_id BIGINT UNSIGNED DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket_reply' AND COLUMN_NAME = 'customer_account_id');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket_reply ADD COLUMN customer_account_id BIGINT UNSIGNED DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket_reply' AND CONSTRAINT_NAME = 'fk_ticket_reply_staff_account');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket_reply ADD CONSTRAINT fk_ticket_reply_staff_account FOREIGN KEY (staff_account_id) REFERENCES staff_account (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ticket_reply' AND CONSTRAINT_NAME = 'fk_ticket_reply_customer_account');
SET @sql := IF(@exist = 0, 'ALTER TABLE ticket_reply ADD CONSTRAINT fk_ticket_reply_customer_account FOREIGN KEY (customer_account_id) REFERENCES customer_account (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consultation_session' AND COLUMN_NAME = 'customer_account_id');
SET @sql := IF(@exist = 0, 'ALTER TABLE consultation_session ADD COLUMN customer_account_id BIGINT UNSIGNED DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consultation_session' AND COLUMN_NAME = 'agent_staff_account_id');
SET @sql := IF(@exist = 0, 'ALTER TABLE consultation_session ADD COLUMN agent_staff_account_id BIGINT UNSIGNED DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consultation_session' AND CONSTRAINT_NAME = 'fk_consultation_customer_account');
SET @sql := IF(@exist = 0, 'ALTER TABLE consultation_session ADD CONSTRAINT fk_consultation_customer_account FOREIGN KEY (customer_account_id) REFERENCES customer_account (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @exist := (SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'consultation_session' AND CONSTRAINT_NAME = 'fk_consultation_agent_staff');
SET @sql := IF(@exist = 0, 'ALTER TABLE consultation_session ADD CONSTRAINT fk_consultation_agent_staff FOREIGN KEY (agent_staff_account_id) REFERENCES staff_account (id)', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ---------------------------------------------------------------------------
-- 通知、审计、登录日志、屏蔽词
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS notification_template (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scope_type VARCHAR(16) NOT NULL,
    scope_id BIGINT UNSIGNED NOT NULL,
    event_category VARCHAR(64) NOT NULL,
    channel VARCHAR(16) NOT NULL,
    code VARCHAR(64) NOT NULL,
    title_template VARCHAR(512) NOT NULL,
    content_template TEXT NOT NULL,
    is_security TINYINT NOT NULL DEFAULT 0,
    is_unsubscribable TINYINT NOT NULL DEFAULT 0,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_notification_template_scope_code (scope_type, scope_id, channel, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知模板';

CREATE TABLE IF NOT EXISTS notification_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED DEFAULT NULL,
    source_type VARCHAR(32) DEFAULT NULL,
    source_id BIGINT UNSIGNED DEFAULT NULL,
    channel VARCHAR(16) NOT NULL,
    recipient_subject_id BIGINT UNSIGNED DEFAULT NULL,
    portal_type VARCHAR(16) DEFAULT NULL,
    template_code VARCHAR(64) DEFAULT NULL,
    payload_json JSON DEFAULT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    retry_count INT UNSIGNED NOT NULL DEFAULT 0,
    last_error VARCHAR(512) DEFAULT NULL,
    next_retry_at DATETIME(3) DEFAULT NULL,
    sent_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_notification_log_domain_created (business_domain_id, created_at),
    KEY idx_notification_log_recipient (recipient_subject_id, status),
    CONSTRAINT fk_notification_log_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id),
    CONSTRAINT fk_notification_log_recipient FOREIGN KEY (recipient_subject_id) REFERENCES identity_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知发送日志';

CREATE TABLE IF NOT EXISTS inbox_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    notification_log_id BIGINT UNSIGNED DEFAULT NULL,
    recipient_subject_id BIGINT UNSIGNED NOT NULL,
    portal_type VARCHAR(16) NOT NULL,
    business_domain_id BIGINT UNSIGNED DEFAULT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    jump_url VARCHAR(512) DEFAULT NULL,
    is_read TINYINT NOT NULL DEFAULT 0,
    read_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    deleted_at DATETIME(3) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_inbox_recipient_read (recipient_subject_id, is_read, created_at),
    CONSTRAINT fk_inbox_notification FOREIGN KEY (notification_log_id) REFERENCES notification_log (id),
    CONSTRAINT fk_inbox_recipient FOREIGN KEY (recipient_subject_id) REFERENCES identity_subject (id),
    CONSTRAINT fk_inbox_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信';

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED DEFAULT NULL,
    operator_subject_id BIGINT UNSIGNED DEFAULT NULL,
    operator_actor_type VARCHAR(16) DEFAULT NULL,
    target VARCHAR(255) NOT NULL,
    action VARCHAR(128) NOT NULL,
    detail JSON DEFAULT NULL,
    result VARCHAR(32) DEFAULT NULL,
    occurred_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    request_id VARCHAR(64) DEFAULT NULL,
    PRIMARY KEY (id),
    KEY idx_audit_domain_time (business_domain_id, occurred_at),
    KEY idx_audit_operator_time (operator_subject_id, occurred_at),
    KEY idx_audit_request (request_id),
    CONSTRAINT fk_audit_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id),
    CONSTRAINT fk_audit_operator FOREIGN KEY (operator_subject_id) REFERENCES identity_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志（PRD）';

CREATE TABLE IF NOT EXISTS login_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    subject_id BIGINT UNSIGNED DEFAULT NULL,
    portal_type VARCHAR(16) NOT NULL,
    business_domain_id BIGINT UNSIGNED DEFAULT NULL,
    login_name VARCHAR(128) NOT NULL,
    ip VARCHAR(64) DEFAULT NULL,
    user_agent VARCHAR(255) DEFAULT NULL,
    result VARCHAR(16) NOT NULL,
    fail_reason VARCHAR(255) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_login_log_subject_created (subject_id, created_at),
    KEY idx_login_log_name_created (login_name, created_at),
    CONSTRAINT fk_login_log_subject FOREIGN KEY (subject_id) REFERENCES identity_subject (id),
    CONSTRAINT fk_login_log_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='登录日志（PRD，可与 auth_login_log 并存）';

CREATE TABLE IF NOT EXISTS blocked_word (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED DEFAULT NULL,
    word VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_blocked_word_domain (business_domain_id),
    CONSTRAINT fk_blocked_word_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='屏蔽词';

CREATE TABLE IF NOT EXISTS access_ip_policy (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    scope_type VARCHAR(16) NOT NULL,
    scope_id BIGINT UNSIGNED NOT NULL,
    portal_type VARCHAR(16) NOT NULL,
    enabled TINYINT NOT NULL DEFAULT 0,
    allowlist_json JSON NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_access_ip_policy (scope_type, scope_id, portal_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='IP 白名单策略';

CREATE TABLE IF NOT EXISTS customer_cancel_request (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_account_id BIGINT UNSIGNED NOT NULL,
    subject_id BIGINT UNSIGNED NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'pending',
    recoverable_until DATETIME(3) DEFAULT NULL,
    reason_code VARCHAR(64) DEFAULT NULL,
    reason_text VARCHAR(512) DEFAULT NULL,
    snapshot_json JSON DEFAULT NULL,
    reviewed_by BIGINT UNSIGNED DEFAULT NULL,
    reviewed_at DATETIME(3) DEFAULT NULL,
    recovered_at DATETIME(3) DEFAULT NULL,
    finalized_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_cancel_customer (customer_account_id, status),
    CONSTRAINT fk_cancel_customer FOREIGN KEY (customer_account_id) REFERENCES customer_account (id),
    CONSTRAINT fk_cancel_subject FOREIGN KEY (subject_id) REFERENCES identity_subject (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户注销申请';

-- 平台角色种子（幂等）
INSERT INTO platform_role (code, name, preset)
VALUES ('platform_admin', '平台管理员', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), preset = VALUES(preset), updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO platform_role (code, name, preset)
VALUES ('security_auditor', '安全审计员', 1)
ON DUPLICATE KEY UPDATE name = VALUES(name), preset = VALUES(preset), updated_at = CURRENT_TIMESTAMP(3);

-- 默认工单优先级（与现有 ticket.priority 字符串兼容）
INSERT INTO ticket_priority_level (business_domain_id, code, name, sort_order, is_default, status)
SELECT d.id, 'low', '低', 10, 0, 'active' FROM business_domain d WHERE d.code = 'default'
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO ticket_priority_level (business_domain_id, code, name, sort_order, is_default, status)
SELECT d.id, 'normal', '普通', 20, 1, 'active' FROM business_domain d WHERE d.code = 'default'
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO ticket_priority_level (business_domain_id, code, name, sort_order, is_default, status)
SELECT d.id, 'high', '高', 30, 0, 'active' FROM business_domain d WHERE d.code = 'default'
ON DUPLICATE KEY UPDATE name = VALUES(name), sort_order = VALUES(sort_order), status = VALUES(status);

INSERT INTO attachment_policy (scope_type, scope_id, allowed_types_json, max_single_size_mb, max_total_size_mb)
VALUES ('platform', 0, JSON_ARRAY('pdf', 'png', 'jpg', 'jpeg', 'gif', 'webp', 'txt', 'zip'), 20, 200)
ON DUPLICATE KEY UPDATE
    allowed_types_json = VALUES(allowed_types_json),
    max_single_size_mb = VALUES(max_single_size_mb),
    max_total_size_mb = VALUES(max_total_size_mb),
    updated_at = CURRENT_TIMESTAMP(3);
