-- Generated rebaseline from current UnionDesk schema and stable seeds
-- Source database: uniondesk @ 127.0.0.1:30306
-- Active Flyway location: classpath:db/migration/current
-- Old versioned migrations live under db/migration/archive

CREATE TABLE IF NOT EXISTS `access_ip_policy` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `scope_type` varchar(16) NOT NULL,
  `scope_id` bigint unsigned NOT NULL,
  `portal_type` varchar(16) NOT NULL,
  `enabled` tinyint NOT NULL DEFAULT '0',
  `allowlist_json` json NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_access_ip_policy` (`scope_type`,`scope_id`,`portal_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='IP 白名单策略';

CREATE TABLE IF NOT EXISTS `attachment_policy` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `scope_type` varchar(16) NOT NULL,
  `scope_id` bigint unsigned NOT NULL,
  `allowed_types_json` json DEFAULT NULL,
  `max_single_size_mb` int unsigned DEFAULT NULL,
  `max_total_size_mb` int unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attachment_policy_scope` (`scope_type`,`scope_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='附件策略';

CREATE TABLE IF NOT EXISTS `auth_client` (
  `client_code` varchar(64) NOT NULL,
  `client_name` varchar(128) NOT NULL,
  `allowed_account_type` varchar(16) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`client_code`),
  KEY `idx_auth_client_status` (`status`),
  CONSTRAINT `chk_auth_client_account_type` CHECK ((`allowed_account_type` in (_utf8mb4'admin',_utf8mb4'customer')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `auth_login_config` (
  `config_key` varchar(64) NOT NULL,
  `config_value` varchar(255) NOT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `business_domain` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `visibility_policy` varchar(32) NOT NULL DEFAULT 'global',
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `registration_policy` varchar(32) NOT NULL DEFAULT 'open' COMMENT 'open|invitation_only|admin_only',
  `visibility_policy_codes` json DEFAULT NULL COMMENT 'PRD: public|domain_customer_only|channel_only',
  `deleted_at` datetime(3) DEFAULT NULL,
  `logo` varchar(512) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_business_domain_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `auth_login_session` (
  `sid` char(36) NOT NULL,
  `session_type` varchar(32) NOT NULL DEFAULT 'login',
  `user_id` bigint unsigned NOT NULL,
  `client_code` varchar(64) NOT NULL,
  `account_type` varchar(16) NOT NULL,
  `role_code` varchar(32) NOT NULL,
  `business_domain_id` bigint unsigned DEFAULT NULL,
  `login_identifier_masked` varchar(128) NOT NULL,
  `session_status` varchar(16) NOT NULL DEFAULT 'active',
  `issued_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `expires_at` datetime(3) NOT NULL,
  `last_seen_at` datetime(3) DEFAULT NULL,
  `revoked_at` datetime(3) DEFAULT NULL,
  `revoked_reason` varchar(255) DEFAULT NULL,
  `refresh_token_hash` char(64) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`sid`),
  KEY `idx_auth_login_session_user_status` (`user_id`,`session_status`,`expires_at`),
  KEY `idx_auth_login_session_status_expires` (`session_status`,`expires_at`),
  KEY `idx_auth_login_session_last_seen` (`last_seen_at`),
  KEY `fk_auth_login_session_domain` (`business_domain_id`),
  KEY `idx_auth_login_session_client_status` (`client_code`,`session_status`,`expires_at`),
  KEY `idx_auth_login_session_type_status_expires` (`session_type`,`session_status`,`expires_at`),
  CONSTRAINT `fk_auth_login_session_client` FOREIGN KEY (`client_code`) REFERENCES `auth_client` (`client_code`),
  CONSTRAINT `fk_auth_login_session_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `chk_auth_login_session_account_type` CHECK ((`account_type` in (_utf8mb4'staff',_utf8mb4'customer'))),
  CONSTRAINT `chk_auth_login_session_session_type` CHECK ((`session_type` in (_utf8mb4'login',_utf8mb4'password_reset')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `blocked_word` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned DEFAULT NULL,
  `word` varchar(128) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_blocked_word_domain` (`business_domain_id`),
  CONSTRAINT `fk_blocked_word_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='屏蔽词';

CREATE TABLE IF NOT EXISTS `domain_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `config_key` varchar(128) NOT NULL,
  `config_value` text,
  `value_type` varchar(16) NOT NULL DEFAULT 'string',
  `description` varchar(255) DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_config` (`business_domain_id`,`config_key`),
  CONSTRAINT `fk_domain_config_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务域配置';

CREATE TABLE IF NOT EXISTS `domain_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `preset` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_role_domain_code` (`business_domain_id`,`code`),
  CONSTRAINT `fk_domain_role_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务域内角色';

CREATE TABLE IF NOT EXISTS `domain_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `source_domain_id` bigint unsigned DEFAULT NULL,
  `template_type` varchar(32) NOT NULL,
  `name` varchar(128) NOT NULL,
  `snapshot_json` json NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'draft',
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_domain_template_source` (`source_domain_id`),
  CONSTRAINT `fk_domain_template_source` FOREIGN KEY (`source_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='业务域模板';

CREATE TABLE IF NOT EXISTS `health_marker` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `name` varchar(32) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `iam_admin_menu` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `node_type` varchar(16) NOT NULL,
  `scope` varchar(16) NOT NULL DEFAULT 'business',
  `name` varchar(128) NOT NULL,
  `route_path` varchar(255) DEFAULT NULL,
  `component_key` varchar(255) DEFAULT NULL,
  `permission_code` varchar(128) DEFAULT NULL,
  `parent_id` bigint unsigned DEFAULT NULL,
  `order_no` int NOT NULL DEFAULT '0',
  `icon` varchar(64) DEFAULT NULL,
  `hidden` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `required` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_admin_menu_code` (`code`),
  UNIQUE KEY `uk_iam_admin_menu_route_path` (`route_path`),
  UNIQUE KEY `uk_iam_admin_menu_permission_code` (`permission_code`),
  KEY `idx_iam_admin_menu_parent_order` (`parent_id`,`order_no`,`id`),
  KEY `idx_iam_admin_menu_type_status` (`node_type`,`status`),
  KEY `idx_iam_admin_menu_scope` (`scope`),
  CONSTRAINT `fk_iam_admin_menu_parent` FOREIGN KEY (`parent_id`) REFERENCES `iam_admin_menu` (`id`),
  CONSTRAINT `chk_iam_admin_menu_button_fields` CHECK (((`node_type` <> _utf8mb4'button') or ((`route_path` is null) and (`component_key` is null) and (`permission_code` is not null)))),
  CONSTRAINT `chk_iam_admin_menu_catalog_fields` CHECK (((`node_type` <> _utf8mb4'catalog') or ((`route_path` is null) and (`component_key` is null) and (`permission_code` is null) and (`required` = 0)))),
  CONSTRAINT `chk_iam_admin_menu_menu_fields` CHECK (((`node_type` <> _utf8mb4'menu') or ((`route_path` is not null) and (`component_key` is not null) and (`permission_code` is null)))),
  CONSTRAINT `chk_iam_admin_menu_node_type` CHECK ((`node_type` in (_utf8mb4'catalog',_utf8mb4'menu',_utf8mb4'button'))),
  CONSTRAINT `chk_iam_admin_menu_scope` CHECK ((`scope` in (_utf8mb4'platform',_utf8mb4'business')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Admin 菜单树';

CREATE TABLE IF NOT EXISTS `iam_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(128) NOT NULL,
  `name` varchar(128) NOT NULL,
  `description` varchar(255) NOT NULL,
  `permission_scope` varchar(16) NOT NULL,
  `resource_code` varchar(128) NOT NULL,
  `action_code` varchar(64) NOT NULL,
  `http_method` varchar(16) DEFAULT NULL,
  `path_pattern` varchar(255) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_permission_code` (`code`),
  KEY `idx_iam_permission_scope_status` (`permission_scope`,`status`),
  KEY `idx_iam_permission_route` (`http_method`,`path_pattern`),
  CONSTRAINT `chk_iam_permission_route` CHECK ((((`http_method` is null) and (`path_pattern` is null)) or ((`http_method` is not null) and (`path_pattern` is not null)))),
  CONSTRAINT `chk_iam_permission_scope` CHECK ((`permission_scope` in (_utf8mb4'platform',_utf8mb4'domain')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限元数据';

CREATE TABLE IF NOT EXISTS `iam_resource` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `resource_type` varchar(16) NOT NULL,
  `resource_code` varchar(128) NOT NULL,
  `resource_name` varchar(128) NOT NULL,
  `client_scope` varchar(32) NOT NULL,
  `http_method` varchar(16) DEFAULT NULL,
  `path_pattern` varchar(255) DEFAULT NULL,
  `parent_id` bigint unsigned DEFAULT NULL,
  `order_no` int NOT NULL DEFAULT '0',
  `icon` varchar(64) DEFAULT NULL,
  `component` varchar(255) DEFAULT NULL,
  `hidden` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_resource_code` (`resource_code`),
  KEY `idx_iam_resource_scope_type` (`client_scope`,`resource_type`,`status`),
  KEY `idx_iam_resource_api_route` (`http_method`,`path_pattern`),
  KEY `idx_iam_resource_parent_order` (`parent_id`,`order_no`,`id`),
  CONSTRAINT `fk_iam_resource_parent` FOREIGN KEY (`parent_id`) REFERENCES `iam_resource` (`id`),
  CONSTRAINT `chk_iam_resource_action_fields` CHECK ((((`resource_type` = _utf8mb4'action') and (`http_method` is not null) and (`path_pattern` is not null)) or ((`resource_type` = _utf8mb4'menu') and (`http_method` is null)))),
  CONSTRAINT `chk_iam_resource_scope` CHECK ((`client_scope` in (_utf8mb4'ud-admin-web',_utf8mb4'ud-customer-web',_utf8mb4'all'))),
  CONSTRAINT `chk_iam_resource_type` CHECK ((`resource_type` in (_utf8mb4'menu',_utf8mb4'action')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `identity_subject` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `subject_type` varchar(16) NOT NULL DEFAULT 'person',
  `phone` varchar(32) NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'active',
  `merged_into_id` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_identity_subject_phone` (`phone`),
  KEY `idx_identity_subject_merged` (`merged_into_id`),
  CONSTRAINT `fk_identity_subject_merged` FOREIGN KEY (`merged_into_id`) REFERENCES `identity_subject` (`id`),
  CONSTRAINT `chk_identity_subject_type` CHECK ((`subject_type` in (_utf8mb4'person',_utf8mb4'system')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一身份主体';

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned DEFAULT NULL,
  `operator_subject_id` bigint unsigned DEFAULT NULL,
  `operator_actor_type` varchar(16) DEFAULT NULL,
  `target` varchar(255) NOT NULL,
  `action` varchar(128) NOT NULL,
  `detail` json DEFAULT NULL,
  `result` varchar(32) DEFAULT NULL,
  `occurred_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `request_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_domain_time` (`business_domain_id`,`occurred_at`),
  KEY `idx_audit_operator_time` (`operator_subject_id`,`occurred_at`),
  KEY `idx_audit_request` (`request_id`),
  CONSTRAINT `fk_audit_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_audit_operator` FOREIGN KEY (`operator_subject_id`) REFERENCES `identity_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='审计日志（PRD）';

CREATE TABLE IF NOT EXISTS `customer_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `subject_id` bigint unsigned NOT NULL,
  `login_name` varchar(64) NOT NULL,
  `display_name` varchar(128) DEFAULT NULL,
  `avatar_url` varchar(512) DEFAULT NULL,
  `phone` varchar(32) NOT NULL,
  `email` varchar(128) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `must_change_password` tinyint NOT NULL DEFAULT '0',
  `status` varchar(16) NOT NULL DEFAULT 'active',
  `source` varchar(32) NOT NULL DEFAULT 'local',
  `auth_version` int unsigned NOT NULL DEFAULT '1',
  `password_changed_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_account_login` (`login_name`),
  UNIQUE KEY `uk_customer_account_subject` (`subject_id`),
  CONSTRAINT `fk_customer_account_subject` FOREIGN KEY (`subject_id`) REFERENCES `identity_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户账号';

CREATE TABLE IF NOT EXISTS `customer_cancel_request` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `customer_account_id` bigint unsigned NOT NULL,
  `subject_id` bigint unsigned NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'pending',
  `recoverable_until` datetime(3) DEFAULT NULL,
  `reason_code` varchar(64) DEFAULT NULL,
  `reason_text` varchar(512) DEFAULT NULL,
  `snapshot_json` json DEFAULT NULL,
  `reviewed_by` bigint unsigned DEFAULT NULL,
  `reviewed_at` datetime(3) DEFAULT NULL,
  `recovered_at` datetime(3) DEFAULT NULL,
  `finalized_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_cancel_customer` (`customer_account_id`,`status`),
  KEY `fk_cancel_subject` (`subject_id`),
  CONSTRAINT `fk_cancel_customer` FOREIGN KEY (`customer_account_id`) REFERENCES `customer_account` (`id`),
  CONSTRAINT `fk_cancel_subject` FOREIGN KEY (`subject_id`) REFERENCES `identity_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户注销申请';

CREATE TABLE IF NOT EXISTS `domain_customer` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `customer_account_id` bigint unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'pending',
  `source` varchar(32) NOT NULL DEFAULT 'self_register',
  `activated_at` datetime(3) DEFAULT NULL,
  `disabled_at` datetime(3) DEFAULT NULL,
  `deleted_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_customer_account_domain` (`customer_account_id`,`business_domain_id`),
  KEY `idx_domain_customer_domain_status` (`business_domain_id`,`status`),
  CONSTRAINT `fk_domain_customer_account` FOREIGN KEY (`customer_account_id`) REFERENCES `customer_account` (`id`),
  CONSTRAINT `fk_domain_customer_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客户入域关系';

CREATE TABLE IF NOT EXISTS `file_attachment` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned DEFAULT NULL,
  `uploader_subject_id` bigint unsigned DEFAULT NULL,
  `portal_type` varchar(16) DEFAULT NULL,
  `file_name` varchar(512) NOT NULL,
  `mime_type` varchar(128) NOT NULL,
  `file_size` bigint unsigned NOT NULL,
  `storage_type` varchar(16) NOT NULL,
  `storage_key` varchar(512) NOT NULL,
  `checksum` varchar(128) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'confirmed',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `deleted_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_file_attachment_domain_created` (`business_domain_id`,`created_at`),
  KEY `idx_file_attachment_subject` (`uploader_subject_id`),
  CONSTRAINT `fk_file_attachment_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_file_attachment_subject` FOREIGN KEY (`uploader_subject_id`) REFERENCES `identity_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一附件主表';

CREATE TABLE IF NOT EXISTS `attachment_ref` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `attachment_id` bigint unsigned NOT NULL,
  `target_type` varchar(32) NOT NULL,
  `target_id` bigint unsigned NOT NULL,
  `relation_type` varchar(32) NOT NULL DEFAULT 'primary',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_attachment_ref_target` (`target_type`,`target_id`),
  KEY `fk_attachment_ref_file` (`attachment_id`),
  CONSTRAINT `fk_attachment_ref_file` FOREIGN KEY (`attachment_id`) REFERENCES `file_attachment` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='附件业务关联';

CREATE TABLE IF NOT EXISTS `identity_binding` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `subject_id` bigint unsigned NOT NULL,
  `provider` varchar(32) NOT NULL,
  `provider_uid` varchar(128) NOT NULL,
  `provider_profile` json DEFAULT NULL,
  `bound_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_identity_binding_provider_uid` (`provider`,`provider_uid`),
  KEY `idx_identity_binding_subject` (`subject_id`),
  CONSTRAINT `fk_identity_binding_subject` FOREIGN KEY (`subject_id`) REFERENCES `identity_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='第三方身份绑定';

CREATE TABLE IF NOT EXISTS `invitation_code` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `code` varchar(64) NOT NULL,
  `channel` varchar(64) DEFAULT NULL,
  `expires_at` datetime(3) DEFAULT NULL,
  `max_uses` int unsigned DEFAULT NULL,
  `used_count` int unsigned NOT NULL DEFAULT '0',
  `status` varchar(16) NOT NULL DEFAULT 'active',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invitation_domain_code` (`business_domain_id`,`code`),
  CONSTRAINT `fk_invitation_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='邀请码';

CREATE TABLE IF NOT EXISTS `login_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `subject_id` bigint unsigned DEFAULT NULL,
  `portal_type` varchar(16) NOT NULL,
  `business_domain_id` bigint unsigned DEFAULT NULL,
  `login_name` varchar(128) NOT NULL,
  `ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `result` varchar(16) NOT NULL,
  `fail_reason` varchar(255) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_login_log_subject_created` (`subject_id`,`created_at`),
  KEY `idx_login_log_name_created` (`login_name`,`created_at`),
  KEY `fk_login_log_domain` (`business_domain_id`),
  CONSTRAINT `fk_login_log_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_login_log_subject` FOREIGN KEY (`subject_id`) REFERENCES `identity_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='登录日志（PRD，可与 auth_login_log 并存）';

CREATE TABLE IF NOT EXISTS `notification_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned DEFAULT NULL,
  `source_type` varchar(32) DEFAULT NULL,
  `source_id` bigint unsigned DEFAULT NULL,
  `channel` varchar(16) NOT NULL,
  `recipient_subject_id` bigint unsigned DEFAULT NULL,
  `portal_type` varchar(16) DEFAULT NULL,
  `template_code` varchar(64) DEFAULT NULL,
  `payload_json` json DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'pending',
  `retry_count` int unsigned NOT NULL DEFAULT '0',
  `last_error` varchar(512) DEFAULT NULL,
  `next_retry_at` datetime(3) DEFAULT NULL,
  `sent_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_notification_log_domain_created` (`business_domain_id`,`created_at`),
  KEY `idx_notification_log_recipient` (`recipient_subject_id`,`status`),
  CONSTRAINT `fk_notification_log_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_notification_log_recipient` FOREIGN KEY (`recipient_subject_id`) REFERENCES `identity_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知发送日志';

CREATE TABLE IF NOT EXISTS `inbox_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `notification_log_id` bigint unsigned DEFAULT NULL,
  `recipient_subject_id` bigint unsigned NOT NULL,
  `portal_type` varchar(16) NOT NULL,
  `business_domain_id` bigint unsigned DEFAULT NULL,
  `title` varchar(255) NOT NULL,
  `content` text NOT NULL,
  `jump_url` varchar(512) DEFAULT NULL,
  `is_read` tinyint NOT NULL DEFAULT '0',
  `read_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `deleted_at` datetime(3) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_inbox_recipient_read` (`recipient_subject_id`,`is_read`,`created_at`),
  KEY `fk_inbox_notification` (`notification_log_id`),
  KEY `fk_inbox_domain` (`business_domain_id`),
  CONSTRAINT `fk_inbox_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_inbox_notification` FOREIGN KEY (`notification_log_id`) REFERENCES `notification_log` (`id`),
  CONSTRAINT `fk_inbox_recipient` FOREIGN KEY (`recipient_subject_id`) REFERENCES `identity_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='站内信';

CREATE TABLE IF NOT EXISTS `notification_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `scope_type` varchar(16) NOT NULL,
  `scope_id` bigint unsigned NOT NULL,
  `event_category` varchar(64) NOT NULL,
  `channel` varchar(16) NOT NULL,
  `code` varchar(64) NOT NULL,
  `title_template` varchar(512) NOT NULL,
  `content_template` text NOT NULL,
  `is_security` tinyint NOT NULL DEFAULT '0',
  `is_unsubscribable` tinyint NOT NULL DEFAULT '0',
  `status` varchar(16) NOT NULL DEFAULT 'active',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_template_scope_code` (`scope_type`,`scope_id`,`channel`,`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通知模板';

CREATE TABLE IF NOT EXISTS `permission_item` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(128) NOT NULL,
  `name` varchar(128) NOT NULL,
  `module` varchar(64) NOT NULL DEFAULT '',
  `type` varchar(16) NOT NULL DEFAULT 'api',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_permission_item_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='权限项（目标态，可与 iam_permission 并存）';

CREATE TABLE IF NOT EXISTS `domain_role_permission` (
  `domain_role_id` bigint unsigned NOT NULL,
  `permission_item_id` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`domain_role_id`,`permission_item_id`),
  KEY `fk_drp_item` (`permission_item_id`),
  CONSTRAINT `fk_drp_item` FOREIGN KEY (`permission_item_id`) REFERENCES `permission_item` (`id`),
  CONSTRAINT `fk_drp_role` FOREIGN KEY (`domain_role_id`) REFERENCES `domain_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='域角色与权限项';

CREATE TABLE IF NOT EXISTS `platform_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `preset` tinyint NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台角色';

CREATE TABLE IF NOT EXISTS `quick_reply_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `scope_type` varchar(32) NOT NULL DEFAULT 'ticket',
  `title` varchar(128) NOT NULL,
  `content` text NOT NULL,
  `category` varchar(64) DEFAULT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'active',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_by` bigint unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_quick_reply_domain` (`business_domain_id`,`status`),
  CONSTRAINT `fk_quick_reply_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='快捷回复模板';

CREATE TABLE IF NOT EXISTS `role` (
  `id` int unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `scope` varchar(16) NOT NULL,
  `is_system` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `iam_admin_role_menu_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `role_id` int unsigned NOT NULL,
  `menu_id` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_admin_role_menu` (`role_id`,`menu_id`),
  KEY `idx_iam_admin_role_menu_menu_id` (`menu_id`),
  CONSTRAINT `fk_iam_admin_role_menu_menu` FOREIGN KEY (`menu_id`) REFERENCES `iam_admin_menu` (`id`),
  CONSTRAINT `fk_iam_admin_role_menu_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='Admin 角色菜单授权关系';

CREATE TABLE IF NOT EXISTS `iam_role_permission` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `role_id` int unsigned NOT NULL,
  `permission_id` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_role_permission` (`role_id`,`permission_id`),
  KEY `idx_iam_role_permission_permission` (`permission_id`),
  CONSTRAINT `fk_iam_role_permission_permission` FOREIGN KEY (`permission_id`) REFERENCES `iam_permission` (`id`),
  CONSTRAINT `fk_iam_role_permission_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关系';

CREATE TABLE IF NOT EXISTS `iam_role_resource` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `role_id` int unsigned NOT NULL,
  `resource_id` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_role_resource` (`role_id`,`resource_id`),
  KEY `idx_iam_role_resource_resource` (`resource_id`),
  CONSTRAINT `fk_iam_role_resource_resource` FOREIGN KEY (`resource_id`) REFERENCES `iam_resource` (`id`),
  CONSTRAINT `fk_iam_role_resource_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `sla_calendar` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `name` varchar(128) NOT NULL,
  `config` json NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_sla_calendar_domain` (`business_domain_id`),
  CONSTRAINT `fk_sla_calendar_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SLA 工作日历';

CREATE TABLE IF NOT EXISTS `staff_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `subject_id` bigint unsigned NOT NULL,
  `login_name` varchar(64) NOT NULL,
  `phone` varchar(32) NOT NULL,
  `email` varchar(128) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `must_change_password` tinyint NOT NULL DEFAULT '0',
  `status` varchar(16) NOT NULL DEFAULT 'active',
  `source` varchar(32) NOT NULL DEFAULT 'local',
  `auth_version` int unsigned NOT NULL DEFAULT '1',
  `password_changed_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_account_login` (`login_name`),
  UNIQUE KEY `uk_staff_account_subject` (`subject_id`),
  CONSTRAINT `fk_staff_account_subject` FOREIGN KEY (`subject_id`) REFERENCES `identity_subject` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工账号';

CREATE TABLE IF NOT EXISTS `domain_member` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `staff_account_id` bigint unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'active',
  `source` varchar(32) NOT NULL DEFAULT 'invite',
  `activated_at` datetime(3) DEFAULT NULL,
  `disabled_at` datetime(3) DEFAULT NULL,
  `deleted_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_domain_member_staff_domain` (`staff_account_id`,`business_domain_id`),
  KEY `idx_domain_member_domain_status` (`business_domain_id`,`status`),
  CONSTRAINT `fk_domain_member_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_domain_member_staff` FOREIGN KEY (`staff_account_id`) REFERENCES `staff_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='域成员（员工）';

CREATE TABLE IF NOT EXISTS `domain_member_role` (
  `domain_member_id` bigint unsigned NOT NULL,
  `domain_role_id` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`domain_member_id`,`domain_role_id`),
  KEY `fk_dmr_role` (`domain_role_id`),
  CONSTRAINT `fk_dmr_member` FOREIGN KEY (`domain_member_id`) REFERENCES `domain_member` (`id`),
  CONSTRAINT `fk_dmr_role` FOREIGN KEY (`domain_role_id`) REFERENCES `domain_role` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='域成员角色';

CREATE TABLE IF NOT EXISTS `staff_account_platform_role` (
  `staff_account_id` bigint unsigned NOT NULL,
  `platform_role_id` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`staff_account_id`,`platform_role_id`),
  KEY `fk_sapr_role` (`platform_role_id`),
  CONSTRAINT `fk_sapr_role` FOREIGN KEY (`platform_role_id`) REFERENCES `platform_role` (`id`),
  CONSTRAINT `fk_sapr_staff` FOREIGN KEY (`staff_account_id`) REFERENCES `staff_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工平台角色';

CREATE TABLE IF NOT EXISTS `staff_presence` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `staff_account_id` bigint unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'offline',
  `current_session_count` int unsigned NOT NULL DEFAULT '0',
  `max_concurrent_sessions` int unsigned NOT NULL DEFAULT '3',
  `last_heartbeat_at` datetime(3) DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_presence_staff_domain` (`staff_account_id`,`business_domain_id`),
  KEY `fk_staff_presence_domain` (`business_domain_id`),
  CONSTRAINT `fk_staff_presence_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_staff_presence_staff` FOREIGN KEY (`staff_account_id`) REFERENCES `staff_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='客服在线态';

CREATE TABLE IF NOT EXISTS `system_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `config_key` varchar(128) NOT NULL,
  `config_value` text,
  `value_type` varchar(16) NOT NULL DEFAULT 'string',
  `description` varchar(255) DEFAULT NULL,
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_system_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台系统配置';

CREATE TABLE IF NOT EXISTS `ticket_priority_level` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `code` varchar(32) NOT NULL,
  `name` varchar(64) NOT NULL,
  `sort_order` int NOT NULL DEFAULT '0',
  `is_default` tinyint NOT NULL DEFAULT '0',
  `status` varchar(16) NOT NULL DEFAULT 'active',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_priority_domain_code` (`business_domain_id`,`code`),
  CONSTRAINT `fk_ticket_priority_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单优先级';

CREATE TABLE IF NOT EXISTS `ticket_type` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `code` varchar(64) NOT NULL,
  `name` varchar(64) NOT NULL,
  `sla_first_response_minutes` int unsigned DEFAULT NULL,
  `sla_resolve_minutes` int unsigned DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `status_flow_config` json DEFAULT NULL COMMENT '状态机 JSON',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_type_domain_code` (`business_domain_id`,`code`),
  CONSTRAINT `fk_ticket_type_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `dynamic_field_config` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `ticket_type_id` bigint unsigned NOT NULL,
  `field_code` varchar(64) NOT NULL,
  `field_type` varchar(32) NOT NULL,
  `config_json` json NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dynamic_field_domain_type_code` (`business_domain_id`,`ticket_type_id`,`field_code`),
  KEY `fk_dynamic_field_type` (`ticket_type_id`),
  CONSTRAINT `fk_dynamic_field_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_dynamic_field_type` FOREIGN KEY (`ticket_type_id`) REFERENCES `ticket_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单动态字段定义';

CREATE TABLE IF NOT EXISTS `sla_rule` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `name` varchar(128) NOT NULL DEFAULT '',
  `ticket_type_id` bigint unsigned DEFAULT NULL,
  `priority_level_id` bigint unsigned DEFAULT NULL,
  `calendar_id` bigint unsigned DEFAULT NULL,
  `first_response_minutes` int unsigned DEFAULT NULL,
  `resolution_minutes` int unsigned DEFAULT NULL,
  `is_urgent_config` tinyint NOT NULL DEFAULT '0',
  `breach_action_json` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_sla_rule_domain_type` (`business_domain_id`,`ticket_type_id`),
  KEY `fk_sla_rule_type` (`ticket_type_id`),
  KEY `fk_sla_rule_calendar` (`calendar_id`),
  KEY `fk_sla_rule_priority_level` (`priority_level_id`),
  CONSTRAINT `fk_sla_rule_calendar` FOREIGN KEY (`calendar_id`) REFERENCES `sla_calendar` (`id`),
  CONSTRAINT `fk_sla_rule_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_sla_rule_priority_level` FOREIGN KEY (`priority_level_id`) REFERENCES `ticket_priority_level` (`id`),
  CONSTRAINT `fk_sla_rule_type` FOREIGN KEY (`ticket_type_id`) REFERENCES `ticket_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='SLA 规则';

CREATE TABLE IF NOT EXISTS `ticket_template` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `ticket_type_id` bigint unsigned NOT NULL,
  `scope` varchar(16) NOT NULL,
  `name` varchar(128) NOT NULL,
  `content_json` json NOT NULL,
  `status` varchar(16) NOT NULL DEFAULT 'active',
  `sort_order` int NOT NULL DEFAULT '0',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ticket_template_domain_type` (`business_domain_id`,`ticket_type_id`),
  KEY `fk_ticket_template_type` (`ticket_type_id`),
  CONSTRAINT `fk_ticket_template_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_ticket_template_type` FOREIGN KEY (`ticket_type_id`) REFERENCES `ticket_type` (`id`),
  CONSTRAINT `chk_ticket_template_scope` CHECK ((`scope` in (_utf8mb4'internal',_utf8mb4'customer')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单模板';

CREATE TABLE IF NOT EXISTS `user_account` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL,
  `nickname` varchar(64) DEFAULT NULL,
  `mobile` varchar(20) NOT NULL,
  `email` varchar(128) DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `password_hash` varchar(255) NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `account_type` varchar(16) NOT NULL,
  `auth_version` int unsigned NOT NULL DEFAULT '1' COMMENT '权限版本号',
  `employment_status` varchar(16) NOT NULL DEFAULT 'active',
  `offboarded_at` datetime(3) DEFAULT NULL,
  `offboarded_by` bigint unsigned DEFAULT NULL,
  `offboard_reason` varchar(255) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_mobile` (`mobile`),
  UNIQUE KEY `uk_user_username` (`username`),
  UNIQUE KEY `uk_user_email` (`email`),
  KEY `idx_user_account_employment_status` (`employment_status`,`status`),
  KEY `fk_user_account_offboarded_by` (`offboarded_by`),
  CONSTRAINT `fk_user_account_offboarded_by` FOREIGN KEY (`offboarded_by`) REFERENCES `user_account` (`id`),
  CONSTRAINT `chk_user_account_type` CHECK ((`account_type` in (_utf8mb4'admin',_utf8mb4'customer')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `auth_login_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `sid` char(36) DEFAULT NULL,
  `user_id` bigint unsigned DEFAULT NULL,
  `username` varchar(64) DEFAULT NULL,
  `login_identifier_masked` varchar(128) NOT NULL,
  `login_identifier_type` varchar(16) NOT NULL,
  `event_type` varchar(32) NOT NULL,
  `result` varchar(16) NOT NULL,
  `reason` varchar(255) DEFAULT NULL,
  `client_ip` varchar(64) DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_auth_login_log_sid_created` (`sid`,`created_at`),
  KEY `idx_auth_login_log_user_created` (`user_id`,`created_at`),
  KEY `idx_auth_login_log_event_created` (`event_type`,`created_at`),
  CONSTRAINT `fk_auth_login_log_session` FOREIGN KEY (`sid`) REFERENCES `auth_login_session` (`sid`),
  CONSTRAINT `fk_auth_login_log_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `consultation_session` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `session_no` varchar(32) NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `customer_id` bigint unsigned NOT NULL,
  `session_status` varchar(32) NOT NULL DEFAULT 'open',
  `assigned_to` bigint unsigned DEFAULT NULL,
  `last_message_at` datetime(3) DEFAULT NULL,
  `closed_at` datetime(3) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `customer_account_id` bigint unsigned DEFAULT NULL,
  `agent_staff_account_id` bigint unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_session_no` (`session_no`),
  KEY `idx_consultation_domain_status_updated` (`business_domain_id`,`session_status`,`updated_at`),
  KEY `idx_consultation_customer_updated` (`customer_id`,`updated_at`),
  KEY `fk_consultation_session_assigned_to` (`assigned_to`),
  KEY `fk_consultation_customer_account` (`customer_account_id`),
  KEY `fk_consultation_agent_staff` (`agent_staff_account_id`),
  CONSTRAINT `fk_consultation_agent_staff` FOREIGN KEY (`agent_staff_account_id`) REFERENCES `staff_account` (`id`),
  CONSTRAINT `fk_consultation_customer_account` FOREIGN KEY (`customer_account_id`) REFERENCES `customer_account` (`id`),
  CONSTRAINT `fk_consultation_session_assigned_to` FOREIGN KEY (`assigned_to`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_consultation_session_customer` FOREIGN KEY (`customer_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_consultation_session_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `consultation_message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `consultation_session_id` bigint unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `seq_no` int unsigned NOT NULL,
  `sender_user_id` bigint unsigned DEFAULT NULL,
  `sender_role` varchar(16) NOT NULL,
  `message_type` varchar(16) NOT NULL DEFAULT 'text',
  `content` text,
  `payload` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_message_seq` (`consultation_session_id`,`seq_no`),
  KEY `idx_consultation_message_session_created` (`consultation_session_id`,`created_at`),
  KEY `fk_consultation_message_domain` (`business_domain_id`),
  KEY `fk_consultation_message_sender` (`sender_user_id`),
  CONSTRAINT `fk_consultation_message_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_consultation_message_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_consultation_message_session` FOREIGN KEY (`consultation_session_id`) REFERENCES `consultation_session` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `customer_business_domain_access` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `customer_id` bigint unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `access_status` varchar(16) NOT NULL DEFAULT 'pending',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_customer_domain_access` (`customer_id`,`business_domain_id`),
  KEY `idx_cda_domain_status` (`business_domain_id`,`access_status`),
  CONSTRAINT `fk_cda_customer` FOREIGN KEY (`customer_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_cda_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `feedback` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned NOT NULL,
  `customer_id` bigint unsigned NOT NULL,
  `feedback_type` varchar(16) NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `status` varchar(32) NOT NULL DEFAULT 'pending',
  `internal_notes` text,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_feedback_domain_status_created` (`business_domain_id`,`status`,`created_at`),
  KEY `fk_feedback_customer` (`customer_id`),
  CONSTRAINT `fk_feedback_customer` FOREIGN KEY (`customer_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_feedback_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `iam_role_binding` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `role_id` int unsigned NOT NULL,
  `binding_scope` varchar(16) NOT NULL,
  `business_domain_id` bigint unsigned DEFAULT NULL,
  `business_domain_key` bigint unsigned GENERATED ALWAYS AS (coalesce(`business_domain_id`,0)) STORED,
  `granted_by` bigint unsigned DEFAULT NULL,
  `effective_from` datetime(3) DEFAULT NULL,
  `effective_to` datetime(3) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '1',
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_iam_role_binding_scope` (`user_id`,`role_id`,`binding_scope`,`business_domain_key`),
  KEY `idx_iam_role_binding_user_status` (`user_id`,`status`),
  KEY `idx_iam_role_binding_role` (`role_id`),
  KEY `idx_iam_role_binding_domain` (`business_domain_id`),
  KEY `fk_iam_role_binding_granted_by` (`granted_by`),
  CONSTRAINT `fk_iam_role_binding_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_iam_role_binding_granted_by` FOREIGN KEY (`granted_by`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_iam_role_binding_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `fk_iam_role_binding_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `chk_iam_role_binding_domain` CHECK ((((`binding_scope` = _utf8mb4'global') and (`business_domain_id` is null)) or ((`binding_scope` = _utf8mb4'domain') and (`business_domain_id` is not null)))),
  CONSTRAINT `chk_iam_role_binding_scope` CHECK ((`binding_scope` in (_utf8mb4'global',_utf8mb4'domain')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='统一角色绑定';

CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `business_domain_id` bigint unsigned DEFAULT NULL,
  `operator_user_id` bigint unsigned DEFAULT NULL,
  `module` varchar(64) NOT NULL,
  `action` varchar(64) NOT NULL,
  `request_id` varchar(64) DEFAULT NULL,
  `before_data` json DEFAULT NULL,
  `after_data` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_operation_domain_module_created` (`business_domain_id`,`module`,`created_at`),
  KEY `idx_operation_operator_created` (`operator_user_id`,`created_at`),
  KEY `idx_operation_request_id` (`request_id`),
  CONSTRAINT `fk_operation_log_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_operation_log_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `platform_organization` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `code` varchar(64) NOT NULL,
  `name` varchar(128) NOT NULL,
  `parent_id` bigint unsigned DEFAULT NULL,
  `leader_user_id` bigint unsigned DEFAULT NULL,
  `order_no` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `remark` varchar(255) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_platform_organization_code` (`code`),
  KEY `idx_platform_organization_parent_order` (`parent_id`,`order_no`,`id`),
  KEY `idx_platform_organization_status` (`status`),
  KEY `fk_platform_organization_leader` (`leader_user_id`),
  CONSTRAINT `fk_platform_organization_leader` FOREIGN KEY (`leader_user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_platform_organization_parent` FOREIGN KEY (`parent_id`) REFERENCES `platform_organization` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='平台内部组织树';

CREATE TABLE IF NOT EXISTS `ticket` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticket_no` varchar(32) NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `customer_id` bigint unsigned NOT NULL,
  `ticket_type_id` bigint unsigned NOT NULL,
  `title` varchar(255) NOT NULL,
  `description` text,
  `status` varchar(32) NOT NULL,
  `priority` varchar(16) NOT NULL DEFAULT 'normal',
  `source` varchar(16) NOT NULL DEFAULT 'web',
  `assigned_to` bigint unsigned DEFAULT NULL,
  `custom_fields` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `version` int unsigned NOT NULL DEFAULT '1' COMMENT '乐观锁',
  `result` varchar(32) DEFAULT NULL COMMENT '反馈/建议内部结论',
  `sla_first_response_deadline` datetime(3) DEFAULT NULL,
  `sla_resolution_deadline` datetime(3) DEFAULT NULL,
  `sla_first_responded_at` datetime(3) DEFAULT NULL,
  `sla_resolved_at` datetime(3) DEFAULT NULL,
  `sla_status` varchar(32) DEFAULT NULL,
  `sla_paused_duration` int unsigned NOT NULL DEFAULT '0',
  `sla_pause_started_at` datetime(3) DEFAULT NULL,
  `assignee_staff_account_id` bigint unsigned DEFAULT NULL,
  `customer_account_id` bigint unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ticket_no` (`ticket_no`),
  KEY `idx_ticket_domain_status_priority` (`business_domain_id`,`status`,`priority`,`created_at`),
  KEY `idx_ticket_customer_created` (`customer_id`,`created_at`),
  KEY `idx_ticket_assigned_status` (`assigned_to`,`status`,`updated_at`),
  KEY `fk_ticket_type` (`ticket_type_id`),
  KEY `idx_ticket_assignee_staff` (`assignee_staff_account_id`,`status`,`updated_at`),
  KEY `fk_ticket_customer_account` (`customer_account_id`),
  CONSTRAINT `fk_ticket_assigned_to` FOREIGN KEY (`assigned_to`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_ticket_assignee_staff` FOREIGN KEY (`assignee_staff_account_id`) REFERENCES `staff_account` (`id`),
  CONSTRAINT `fk_ticket_customer` FOREIGN KEY (`customer_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_ticket_customer_account` FOREIGN KEY (`customer_account_id`) REFERENCES `customer_account` (`id`),
  CONSTRAINT `fk_ticket_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_ticket_type` FOREIGN KEY (`ticket_type_id`) REFERENCES `ticket_type` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `consultation_ticket_link` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `consultation_session_id` bigint unsigned NOT NULL,
  `ticket_id` bigint unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `converted_by` bigint unsigned DEFAULT NULL,
  `converted_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_consultation_ticket_link_session` (`consultation_session_id`),
  UNIQUE KEY `uk_consultation_ticket_link_ticket` (`ticket_id`),
  KEY `fk_consultation_ticket_link_domain` (`business_domain_id`),
  KEY `fk_consultation_ticket_link_converted_by` (`converted_by`),
  CONSTRAINT `fk_consultation_ticket_link_converted_by` FOREIGN KEY (`converted_by`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_consultation_ticket_link_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_consultation_ticket_link_session` FOREIGN KEY (`consultation_session_id`) REFERENCES `consultation_session` (`id`),
  CONSTRAINT `fk_consultation_ticket_link_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ticket_event_log` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `event_type` varchar(32) NOT NULL,
  `operator_user_id` bigint unsigned DEFAULT NULL,
  `payload` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ticket_event_ticket_created` (`ticket_id`,`created_at`),
  KEY `fk_ticket_event_domain` (`business_domain_id`),
  KEY `fk_ticket_event_operator` (`operator_user_id`),
  CONSTRAINT `fk_ticket_event_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_ticket_event_operator` FOREIGN KEY (`operator_user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_ticket_event_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `ticket_history` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `action` varchar(64) NOT NULL,
  `from_value` varchar(255) DEFAULT NULL,
  `to_value` varchar(255) DEFAULT NULL,
  `operator_subject_id` bigint unsigned DEFAULT NULL,
  `operator_actor_type` varchar(16) DEFAULT NULL,
  `payload` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ticket_history_ticket_created` (`ticket_id`,`created_at`),
  KEY `fk_ticket_history_domain` (`business_domain_id`),
  KEY `fk_ticket_history_operator` (`operator_subject_id`),
  CONSTRAINT `fk_ticket_history_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_ticket_history_operator` FOREIGN KEY (`operator_subject_id`) REFERENCES `identity_subject` (`id`),
  CONSTRAINT `fk_ticket_history_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单操作流（PRD ticket_history）';

CREATE TABLE IF NOT EXISTS `ticket_relation` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `source_ticket_id` bigint unsigned NOT NULL,
  `target_ticket_id` bigint unsigned NOT NULL,
  `relation_type` varchar(32) NOT NULL,
  `created_by_staff_account_id` bigint unsigned DEFAULT NULL,
  `note` varchar(512) DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_ticket_relation_source` (`source_ticket_id`),
  KEY `idx_ticket_relation_target` (`target_ticket_id`),
  KEY `fk_ticket_relation_creator` (`created_by_staff_account_id`),
  CONSTRAINT `fk_ticket_relation_creator` FOREIGN KEY (`created_by_staff_account_id`) REFERENCES `staff_account` (`id`),
  CONSTRAINT `fk_ticket_relation_source` FOREIGN KEY (`source_ticket_id`) REFERENCES `ticket` (`id`),
  CONSTRAINT `fk_ticket_relation_target` FOREIGN KEY (`target_ticket_id`) REFERENCES `ticket` (`id`),
  CONSTRAINT `chk_ticket_relation_type` CHECK ((`relation_type` in (_utf8mb4'merge',_utf8mb4'follow_up')))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工单关联/合并';

CREATE TABLE IF NOT EXISTS `ticket_reply` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `ticket_id` bigint unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `sender_user_id` bigint unsigned DEFAULT NULL,
  `sender_role` varchar(16) NOT NULL,
  `reply_type` varchar(16) NOT NULL,
  `content` text,
  `attachment_urls` json DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `sender_type` varchar(16) DEFAULT NULL,
  `staff_account_id` bigint unsigned DEFAULT NULL,
  `customer_account_id` bigint unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_ticket_reply_ticket_created` (`ticket_id`,`created_at`),
  KEY `fk_ticket_reply_domain` (`business_domain_id`),
  KEY `fk_ticket_reply_sender` (`sender_user_id`),
  KEY `fk_ticket_reply_staff_account` (`staff_account_id`),
  KEY `fk_ticket_reply_customer_account` (`customer_account_id`),
  CONSTRAINT `fk_ticket_reply_customer_account` FOREIGN KEY (`customer_account_id`) REFERENCES `customer_account` (`id`),
  CONSTRAINT `fk_ticket_reply_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_ticket_reply_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `user_account` (`id`),
  CONSTRAINT `fk_ticket_reply_staff_account` FOREIGN KEY (`staff_account_id`) REFERENCES `staff_account` (`id`),
  CONSTRAINT `fk_ticket_reply_ticket` FOREIGN KEY (`ticket_id`) REFERENCES `ticket` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_domain_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `role_id` int unsigned NOT NULL,
  `business_domain_id` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_domain_role` (`user_id`,`role_id`,`business_domain_id`),
  KEY `idx_udr_domain_role` (`business_domain_id`,`role_id`),
  KEY `fk_udr_role` (`role_id`),
  CONSTRAINT `fk_udr_domain` FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`),
  CONSTRAINT `fk_udr_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `fk_udr_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_global_role` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `role_id` int unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_global_role` (`user_id`,`role_id`),
  KEY `fk_ugr_role` (`role_id`),
  CONSTRAINT `fk_ugr_role` FOREIGN KEY (`role_id`) REFERENCES `role` (`id`),
  CONSTRAINT `fk_ugr_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_organization` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT,
  `user_id` bigint unsigned NOT NULL,
  `organization_id` bigint unsigned NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_organization` (`user_id`,`organization_id`),
  KEY `fk_user_org_org` (`organization_id`),
  CONSTRAINT `fk_user_org_org` FOREIGN KEY (`organization_id`) REFERENCES `platform_organization` (`id`),
  CONSTRAINT `fk_user_org_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户-部门关系表';

-- Seed data for `attachment_policy`
INSERT INTO `attachment_policy` (`id`, `scope_type`, `scope_id`, `allowed_types_json`, `max_single_size_mb`, `max_total_size_mb`, `created_at`, `updated_at`)
VALUES
(1, 'platform', 0, '["pdf", "png", "jpg", "jpeg", "gif", "webp", "txt", "zip"]', 20, 200, '2026-05-18 16:56:44.813', '2026-05-18 16:56:44.813')
ON DUPLICATE KEY UPDATE
`scope_type` = VALUES(`scope_type`),
`scope_id` = VALUES(`scope_id`),
`allowed_types_json` = VALUES(`allowed_types_json`),
`max_single_size_mb` = VALUES(`max_single_size_mb`),
`max_total_size_mb` = VALUES(`max_total_size_mb`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `auth_client`
INSERT INTO `auth_client` (`client_code`, `client_name`, `allowed_account_type`, `status`, `created_at`, `updated_at`)
VALUES
('ud-admin-web', 'UnionDesk Admin Web', 'admin', 1, '2026-05-18 16:56:22.083', '2026-05-18 16:56:22.083'),
('ud-customer-web', 'UnionDesk Customer Web', 'customer', 1, '2026-05-18 16:56:22.083', '2026-05-18 16:56:22.083')
ON DUPLICATE KEY UPDATE
`client_name` = VALUES(`client_name`),
`allowed_account_type` = VALUES(`allowed_account_type`),
`status` = VALUES(`status`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `auth_login_config`
INSERT INTO `auth_login_config` (`config_key`, `config_value`, `updated_at`)
VALUES
('captcha_enabled', 'true', '2026-05-18 16:56:30.189'),
('captcha_hint', '请按住滑块，拖动到最右边', '2026-05-18 16:56:30.189'),
('email_login_enabled', 'true', '2026-05-18 16:56:19.933'),
('max_active_sessions_per_user', '10', '2026-05-18 16:56:19.933'),
('mobile_login_enabled', 'true', '2026-05-18 16:56:19.933'),
('password_login_enabled', 'true', '2026-05-18 16:56:19.933'),
('session_ttl_seconds', '604800', '2026-05-18 16:56:19.933'),
('username_login_enabled', 'true', '2026-05-18 16:56:19.933'),
('wechat_hint', '', '2026-05-18 16:56:20.809'),
('wechat_login_enabled', 'false', '2026-05-18 16:56:20.809')
ON DUPLICATE KEY UPDATE
`config_value` = VALUES(`config_value`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `business_domain`
INSERT INTO `business_domain` (`id`, `code`, `name`, `visibility_policy`, `status`, `created_at`, `updated_at`, `registration_policy`, `visibility_policy_codes`, `deleted_at`, `logo`)
VALUES
(1, 'default', 'Default Domain', 'global', 1, '2026-05-18 16:56:16.876', '2026-05-18 16:56:31.975', 'open', '["public"]', NULL, NULL)
ON DUPLICATE KEY UPDATE
`code` = VALUES(`code`),
`name` = VALUES(`name`),
`visibility_policy` = VALUES(`visibility_policy`),
`status` = VALUES(`status`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`),
`registration_policy` = VALUES(`registration_policy`),
`visibility_policy_codes` = VALUES(`visibility_policy_codes`),
`deleted_at` = VALUES(`deleted_at`),
`logo` = VALUES(`logo`);

-- Seed data for `domain_role`
INSERT INTO `domain_role` (`id`, `business_domain_id`, `code`, `name`, `preset`, `created_at`, `updated_at`)
VALUES
(1, 1, 'super_admin', '业务域超级管理员', 1, '2026-05-18 16:56:46.833', '2026-05-18 16:56:46.833'),
(2, 1, 'domain_admin', '业务域管理员', 1, '2026-05-18 16:56:46.864', '2026-05-18 16:56:46.864'),
(3, 1, 'agent', '客服', 1, '2026-05-18 16:56:46.895', '2026-05-18 16:56:46.895')
ON DUPLICATE KEY UPDATE
`business_domain_id` = VALUES(`business_domain_id`),
`code` = VALUES(`code`),
`name` = VALUES(`name`),
`preset` = VALUES(`preset`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `iam_admin_menu`
INSERT INTO `iam_admin_menu` (`id`, `code`, `node_type`, `scope`, `name`, `route_path`, `component_key`, `permission_code`, `parent_id`, `order_no`, `icon`, `hidden`, `status`, `required`, `created_at`, `updated_at`)
VALUES
(1, 'ADM0000000001', 'catalog', 'business', '系统管理', NULL, NULL, NULL, NULL, 100, 'SettingOutlined', 0, 1, 0, '2026-05-18 16:56:27.969', '2026-05-18 16:56:28.155'),
(2, 'ADM0000000002', 'menu', 'business', '菜单管理', '/system/menus', './system/menus', NULL, 1, 101, 'MenuOutlined', 0, 1, 0, '2026-05-18 16:56:28.000', '2026-05-18 16:56:28.155'),
(3, 'ADM0000000003', 'menu', 'business', '角色管理', '/system/roles', './system/roles', NULL, 1, 102, 'TeamOutlined', 0, 1, 0, '2026-05-18 16:56:28.031', '2026-05-18 16:56:28.155'),
(10, 'ADM0000000010', 'button', 'business', '查看角色', NULL, NULL, 'platform.role.read', 3, 0, NULL, 0, 1, 1, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.784'),
(11, 'ADM0000000011', 'button', 'business', '创建角色', NULL, NULL, 'platform.role.create', 3, 0, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.784'),
(12, 'ADM0000000012', 'button', 'business', '更新角色', NULL, NULL, 'platform.role.update', 3, 0, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.784'),
(13, 'ADM0000000013', 'button', 'business', '删除角色', NULL, NULL, 'platform.role.delete', 3, 0, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.752'),
(14, 'ADM0000000014', 'button', 'business', '查看角色授权', NULL, NULL, 'platform.role_permission.read', 3, 0, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.784'),
(15, 'ADM0000000015', 'button', 'business', '更新角色授权', NULL, NULL, 'platform.role_permission.update', 3, 0, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.784'),
(38, 'ADM0000000038', 'menu', 'platform', '平台首页', '/platform/home', './platform/home', NULL, NULL, 1, 'DashboardOutlined', 0, 1, 1, '2026-05-18 16:56:57.611', '2026-05-18 16:57:02.908'),
(4, 'ADM0000000004', 'menu', 'business', '用户管理', '/system/users', './system/users', NULL, 1, 103, 'UserOutlined', 0, 1, 0, '2026-05-18 16:56:28.062', '2026-05-18 16:56:28.155'),
(17, 'ADM0000000017', 'button', 'business', '创建域用户', NULL, NULL, 'domain.user.create', 4, 0, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.784'),
(42, 'ADM0000000042', 'menu', 'platform', '导入导出', '/platform/import-export', './platform/import-export', NULL, NULL, 5, 'ImportOutlined', 0, 1, 0, '2026-05-18 16:56:57.752', '2026-05-18 16:57:02.908'),
(43, 'ADM0000000043', 'menu', 'platform', '业务域管理', '/platform/domains', './platform/domains', NULL, NULL, 6, 'GlobalOutlined', 0, 1, 0, '2026-05-18 16:56:57.782', '2026-05-18 16:57:02.908'),
(44, 'ADM0000000044', 'menu', 'platform', '客户入域', '/platform/domain-onboarding', './platform/domain-onboarding', NULL, NULL, 7, 'UserAddOutlined', 0, 1, 0, '2026-05-18 16:56:57.813', '2026-05-18 16:57:02.908'),
(45, 'ADM0000000045', 'menu', 'platform', '工单池', '/platform/ticket-pool', './platform/ticket-pool', NULL, NULL, 8, 'SolutionOutlined', 0, 1, 0, '2026-05-18 16:56:57.862', '2026-05-18 16:57:02.908'),
(46, 'ADM0000000046', 'menu', 'platform', '站内信', '/platform/inbox', './platform/inbox', NULL, NULL, 9, 'MailOutlined', 0, 1, 0, '2026-05-18 16:56:57.890', '2026-05-18 16:57:02.908'),
(47, 'ADM0000000047', 'menu', 'platform', '附件', '/platform/attachments', './platform/attachments', NULL, NULL, 10, 'PaperClipOutlined', 0, 1, 0, '2026-05-18 16:56:57.924', '2026-05-18 16:57:02.908'),
(48, 'ADM0000000048', 'menu', 'platform', '权限管理', '/platform/permission', './platform/permission', NULL, NULL, 11, 'SafetyCertificateOutlined', 0, 1, 0, '2026-05-18 16:56:57.952', '2026-05-18 16:57:02.908'),
(49, 'ADM0000000049', 'menu', 'platform', '角色管理', '/platform/role', './system/role', NULL, 48, 11, 'TeamOutlined', 0, 1, 0, '2026-05-18 16:56:57.984', '2026-05-18 16:56:59.942'),
(5, 'ADM0000000005', 'menu', 'business', '离职池', '/system/users/offboard-pool', './system/users/offboard-pool', NULL, 4, 104, 'DeleteOutlined', 0, 1, 0, '2026-05-18 16:56:28.093', '2026-05-18 16:56:28.155'),
(50, 'ADM0000000050', 'menu', 'platform', '菜单管理', '/platform/menu', './platform/system/menu', NULL, 48, 12, 'MenuOutlined', 0, 1, 0, '2026-05-18 16:56:58.015', '2026-05-18 16:56:59.974'),
(51, 'ADM0000000051', 'menu', 'platform', '审计日志', '/platform/audit-logs', './platform/audit-logs', NULL, NULL, 13, 'FileSearchOutlined', 0, 1, 0, '2026-05-18 16:56:58.046', '2026-05-18 16:57:02.908'),
(52, 'ADM0000000052', 'menu', 'platform', '域配置', '/platform/domain-config', './platform/domain-config', NULL, NULL, 14, 'SettingOutlined', 0, 1, 0, '2026-05-18 16:56:58.077', '2026-05-18 16:57:02.908'),
(53, 'ADM0000000053', 'menu', 'platform', 'SLA管理', '/platform/sla-management', './platform/sla-management', NULL, NULL, 15, 'ClockCircleOutlined', 0, 1, 0, '2026-05-18 16:56:58.108', '2026-05-18 16:57:02.908'),
(54, 'ADM0000000054', 'menu', 'platform', '系统设置', '/platform/system-settings', './platform/system-settings', NULL, NULL, 16, 'ToolOutlined', 0, 1, 0, '2026-05-18 16:56:58.140', '2026-05-18 16:57:02.908'),
(55, 'ADM0000000055', 'menu', 'platform', '工单详情', '/platform/ticket-detail', './platform/ticket-detail', NULL, NULL, 17, 'ProfileOutlined', 1, 1, 0, '2026-05-18 16:56:58.171', '2026-05-18 16:57:02.908'),
(56, 'ADM0000000056', 'button', 'platform', '查询平台首页', NULL, NULL, 'platform.login_log.read', 38, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.201', '2026-05-18 16:56:58.760'),
(58, 'ADM0000000058', 'button', 'platform', '查询业务域管理', NULL, NULL, 'domain.read', 43, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.356', '2026-05-18 16:56:58.760'),
(59, 'ADM0000000059', 'button', 'platform', '客户入域', NULL, NULL, 'domain.customer.create', 44, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.386', '2026-05-18 16:56:58.760'),
(6, 'ADM0000000006', 'button', 'business', '查看菜单', NULL, NULL, 'platform.menu.read', 2, 0, NULL, 0, 1, 1, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.784'),
(60, 'ADM0000000060', 'button', 'platform', '查询工单池', NULL, NULL, 'ticket.read', 45, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.418', '2026-05-18 16:56:58.760'),
(61, 'ADM0000000061', 'button', 'platform', '查询站内信', NULL, NULL, 'inbox.read', 46, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.449', '2026-05-18 16:56:58.760'),
(62, 'ADM0000000062', 'button', 'platform', '查询附件', NULL, NULL, 'attachment.download', 47, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.480', '2026-05-18 16:56:58.760'),
(63, 'ADM0000000063', 'button', 'platform', '查询权限管理', NULL, NULL, 'platform.permission.manage', 48, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.511', '2026-05-18 16:56:58.760'),
(64, 'ADM0000000064', 'button', 'platform', '查询审计日志', NULL, NULL, 'platform.audit_log.read', 51, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.605', '2026-05-18 16:56:58.760'),
(65, 'ADM0000000065', 'button', 'platform', '查询域配置', NULL, NULL, 'domain.config.read', 52, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.635', '2026-05-18 16:56:58.760'),
(66, 'ADM0000000066', 'button', 'platform', '查询SLA管理', NULL, NULL, 'domain.sla.read', 53, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.666', '2026-05-18 16:56:58.760'),
(67, 'ADM0000000067', 'button', 'platform', '查询系统设置', NULL, NULL, 'platform.system_config.read', 54, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.698', '2026-05-18 16:56:58.760'),
(68, 'ADM0000000068', 'button', 'platform', '查询工单详情', NULL, NULL, 'ticket.reply', 55, 0, NULL, 0, 1, 1, '2026-05-18 16:56:58.729', '2026-05-18 16:56:58.760'),
(69, 'ADM0000000069', 'button', 'platform', '绑定平台角色', NULL, NULL, 'platform.role.bind', 49, 6, NULL, 0, 1, 0, '2026-05-18 16:57:02.471', '2026-05-18 16:57:02.501'),
(7, 'ADM0000000007', 'button', 'business', '创建菜单', NULL, NULL, 'platform.menu.create', 2, 0, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.784'),
(70, 'ADM0000000070', 'catalog', 'platform', '组织管理', NULL, NULL, NULL, NULL, 2, 'ApartmentOutlined', 0, 1, 0, '2026-05-18 16:57:03.436', '2026-05-18 16:57:04.460'),
(39, 'ADM0000000039', 'menu', 'platform', '用户管理', '/platform/user', './platform/user', NULL, 70, 1, 'UserOutlined', 0, 1, 0, '2026-05-18 16:56:57.642', '2026-05-18 16:57:03.467'),
(16, 'ADM0000000016', 'button', 'platform', '查询平台用户', NULL, NULL, 'platform.user.read', 39, 0, NULL, 0, 1, 1, '2026-05-18 16:56:28.124', '2026-05-18 16:57:03.716'),
(18, 'ADM0000000018', 'button', 'platform', '编辑用户', NULL, NULL, 'platform.user.update', 39, 3, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:57:03.902'),
(19, 'ADM0000000019', 'button', 'platform', '离职用户', NULL, NULL, 'platform.user.disable', 39, 4, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:57:03.964'),
(40, 'ADM0000000040', 'menu', 'platform', '组织架构', '/platform/dept', './platform/dept', NULL, 70, 2, 'ApartmentOutlined', 0, 1, 0, '2026-05-18 16:56:57.689', '2026-05-18 16:57:03.529'),
(41, 'ADM0000000041', 'menu', 'platform', '离职池', '/platform/offboard-pool', './platform/offboard-pool', NULL, 70, 3, 'UserDeleteOutlined', 0, 1, 0, '2026-05-18 16:56:57.721', '2026-05-18 16:57:03.593'),
(20, 'ADM0000000020', 'button', 'platform', '查询离职池', NULL, NULL, 'platform.user.offboard_pool.read', 41, 0, NULL, 0, 1, 1, '2026-05-18 16:56:28.124', '2026-05-18 16:57:04.151'),
(21, 'ADM0000000021', 'button', 'platform', '恢复用户', NULL, NULL, 'platform.user.restore', 41, 3, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:57:04.338'),
(22, 'ADM0000000022', 'button', 'platform', '删除用户', NULL, NULL, 'platform.user.delete', 41, 4, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:57:04.400'),
(57, 'ADM0000000057', 'button', 'platform', '新增用户', NULL, NULL, 'platform.user.create', 39, 1, NULL, 0, 1, 0, '2026-05-18 16:56:58.262', '2026-05-18 16:57:03.777'),
(71, 'ADM0000000071', 'menu', 'platform', '组织配置', '/platform/org-config', './platform/org-config', NULL, 70, 4, 'SettingOutlined', 0, 1, 0, '2026-05-18 16:57:03.685', '2026-05-18 16:57:04.460'),
(72, 'ADM0000000072', 'button', 'platform', '导入用户', NULL, NULL, 'platform.user.import', 39, 2, NULL, 0, 1, 0, '2026-05-18 16:57:03.872', '2026-05-18 16:57:04.460'),
(73, 'ADM0000000073', 'button', 'platform', '查看组织架构', NULL, NULL, 'platform.organization.read', 40, 0, NULL, 0, 1, 1, '2026-05-18 16:57:04.058', '2026-05-18 16:57:05.053'),
(74, 'ADM0000000074', 'button', 'platform', '新增组织', NULL, NULL, 'platform.organization.create', 40, 1, NULL, 0, 1, 0, '2026-05-18 16:57:04.120', '2026-05-18 16:57:04.460'),
(75, 'ADM0000000075', 'button', 'platform', '导出离职池', NULL, NULL, 'platform.user.offboard_pool.export', 41, 1, NULL, 0, 1, 0, '2026-05-18 16:57:04.245', '2026-05-18 16:57:04.460'),
(76, 'ADM0000000076', 'button', 'platform', '批量恢复', NULL, NULL, 'platform.user.offboard_pool.batch_restore', 41, 2, NULL, 0, 1, 0, '2026-05-18 16:57:04.307', '2026-05-18 16:57:04.460'),
(77, 'TMP-BUTTON-PLATFORM-ORG-UPDATE', 'button', 'platform', '编辑组织', NULL, NULL, 'platform.organization.update', 40, 2, NULL, 0, 1, 0, '2026-05-18 16:57:05.210', '2026-05-18 16:57:05.210'),
(78, 'TMP-BUTTON-PLATFORM-ORG-DELETE', 'button', 'platform', '删除组织', NULL, NULL, 'platform.organization.delete', 40, 3, NULL, 0, 1, 0, '2026-05-18 16:57:05.272', '2026-05-18 16:57:05.272'),
(79, 'TMP-BUTTON-PLATFORM-USER-RESET-PASSWORD', 'button', 'platform', '重置密码', NULL, NULL, 'platform.user.reset_password', 39, 45, NULL, 0, 1, 0, '2026-05-18 16:57:05.843', '2026-05-18 16:57:05.843'),
(8, 'ADM0000000008', 'button', 'business', '更新菜单', NULL, NULL, 'platform.menu.update', 2, 0, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.784'),
(9, 'ADM0000000009', 'button', 'business', '删除菜单', NULL, NULL, 'platform.menu.delete', 2, 0, NULL, 0, 1, 0, '2026-05-18 16:56:28.124', '2026-05-18 16:56:29.752')
ON DUPLICATE KEY UPDATE
`code` = VALUES(`code`),
`node_type` = VALUES(`node_type`),
`scope` = VALUES(`scope`),
`name` = VALUES(`name`),
`route_path` = VALUES(`route_path`),
`component_key` = VALUES(`component_key`),
`permission_code` = VALUES(`permission_code`),
`parent_id` = VALUES(`parent_id`),
`order_no` = VALUES(`order_no`),
`icon` = VALUES(`icon`),
`hidden` = VALUES(`hidden`),
`status` = VALUES(`status`),
`required` = VALUES(`required`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `iam_permission`
INSERT INTO `iam_permission` (`id`, `code`, `name`, `description`, `permission_scope`, `resource_code`, `action_code`, `http_method`, `path_pattern`, `status`, `created_at`, `updated_at`)
VALUES
(1, 'platform.menu.read', '查看菜单', '查看管理端菜单树', 'platform', 'platform.menu', 'read', 'GET', '/api/v1/iam/menus/tree', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(2, 'platform.menu.create', '创建菜单', '创建管理端菜单或按钮', 'platform', 'platform.menu', 'create', 'POST', '/api/v1/iam/menus', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(3, 'platform.menu.update', '更新菜单', '更新管理端菜单或按钮', 'platform', 'platform.menu', 'update', 'PUT', '/api/v1/iam/menus/*', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(4, 'platform.menu.delete', '删除菜单', '删除管理端菜单或按钮', 'platform', 'platform.menu', 'delete', 'DELETE', '/api/v1/iam/menus/*', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(5, 'platform.role.read', '查看角色', '查看平台角色列表', 'platform', 'platform.role', 'read', 'GET', '/api/v1/iam/roles', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(6, 'platform.role.create', '创建角色', '创建平台或业务域角色', 'platform', 'platform.role', 'create', 'POST', '/api/v1/iam/roles', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(7, 'platform.role.update', '更新角色', '更新角色基础信息', 'platform', 'platform.role', 'update', 'PUT', '/api/v1/iam/roles/*', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(8, 'platform.role.delete', '删除角色', '删除非系统角色', 'platform', 'platform.role', 'delete', 'DELETE', '/api/v1/iam/roles/*', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(9, 'platform.role_permission.read', '查看角色授权', '查看角色拥有的菜单与按钮权限', 'platform', 'platform.role_permission', 'read', 'GET', '/api/v1/iam/roles/*/permissions', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(10, 'platform.role_permission.update', '更新角色授权', '更新角色拥有的菜单与按钮权限', 'platform', 'platform.role_permission', 'update', 'PUT', '/api/v1/iam/roles/*/permissions', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(11, 'platform.role.bind', '绑定平台角色', '为用户绑定平台级角色', 'platform', 'platform.role', 'bind', NULL, NULL, 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(12, 'platform.user.read', '查看平台用户', '查看平台用户与系统用户', 'platform', 'platform.user', 'read', 'GET', '/api/v1/iam/users', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(13, 'platform.user.create', '创建平台用户', '创建带有平台级角色的用户', 'platform', 'platform.user', 'create', 'POST', '/api/v1/iam/users', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(14, 'platform.user.update', '更新平台用户', '更新平台用户资料与状态', 'platform', 'platform.user', 'update', 'PUT', '/api/v1/iam/users/*', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(15, 'platform.user.disable', '停用平台用户', '办理平台用户离职或停用', 'platform', 'platform.user', 'disable', 'POST', '/api/v1/iam/users/*/offboard', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(16, 'platform.user.restore', '恢复平台用户', '恢复已离职或停用的平台用户', 'platform', 'platform.user', 'restore', 'POST', '/api/v1/iam/users/*/restore', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(17, 'platform.user.offboard_pool.read', '查看离职池', '查看离职用户池', 'platform', 'platform.user', 'read_offboard_pool', 'GET', '/api/v1/iam/users/offboard-pool', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(18, 'platform.user.delete', '删除平台用户', '彻底删除离职用户', 'platform', 'platform.user', 'delete', 'DELETE', '/api/v1/iam/users/*', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(19, 'platform.permission.manage', '管理权限目录', '维护权限目录与授权基础数据', 'platform', 'platform.permission', 'manage', NULL, NULL, 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(20, 'domain.user.read', '查看域成员', '查看业务域成员', 'domain', 'domain.user', 'read', 'GET', '/api/v1/iam/users', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(21, 'domain.user.create', '创建域用户', '在业务域内创建或邀请成员', 'domain', 'domain.user', 'create', 'POST', '/api/v1/iam/users', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(22, 'domain.user.update', '更新域成员', '更新业务域成员资料与域内角色', 'domain', 'domain.user', 'update', NULL, NULL, 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(23, 'domain.user.remove', '移除域成员', '从业务域内移除成员', 'domain', 'domain.user', 'remove', NULL, NULL, 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(24, 'domain.sla.update', 'Update SLA rule', 'Update SLA rules and calendars within a business domain', 'domain', 'sla_rule', 'update', 'PUT', '/api/v1/admin/domains/*/sla-*/*', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:55.708'),
(25, 'domain.notification_template.update', 'Update notification template', 'Update notification template', 'domain', 'notification_template', 'update', 'PUT', '/api/v1/admin/domains/*/notification-templates/*', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:50.377'),
(26, 'ticket.read', '查看工单', '查看业务域内工单', 'domain', 'ticket', 'read', 'GET', '/api/v1/tickets', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(27, 'ticket.assign', '分配工单', '客服分配工单', 'domain', 'ticket', 'assign', 'POST', '/api/v1/admin/domains/*/tickets/*/assign', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:45.202'),
(28, 'ticket.close', '关闭工单', '关闭业务域内工单', 'domain', 'ticket', 'close', 'PATCH', '/api/v1/admin/domains/*/tickets/*/status', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:45.202'),
(29, 'consultation.reply', '回复咨询', '回复业务域内咨询会话', 'domain', 'consultation', 'reply', 'POST', '/api/v1/consultations/*/messages', 1, '2026-05-18 16:56:29.302', '2026-05-18 16:56:29.302'),
(30, 'ticket.create', '创建工单', '客户提交新的工单', 'domain', 'ticket', 'create', 'POST', '/api/v1/domains/*/tickets', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(31, 'ticket.view.self', '查看我的工单', '查看客户自己的工单列表与详情', 'domain', 'ticket', 'view_self', 'GET', '/api/v1/domains/*/tickets/my/**', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(32, 'ticket.view.domain_all', '查看域内工单', '查看业务域内所有工单', 'domain', 'ticket', 'view_domain_all', 'GET', '/api/v1/admin/domains/*/tickets/**', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(33, 'ticket.claim', '领取工单', '客服领取工单', 'domain', 'ticket', 'claim', 'POST', '/api/v1/admin/domains/*/tickets/*/claim', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(34, 'ticket.reply.self', '回复我的工单', '客户回复自己的工单', 'domain', 'ticket', 'reply_self', 'POST', '/api/v1/domains/*/tickets/my/**/replies', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(35, 'ticket.reply', '回复工单', '客服回复工单', 'domain', 'ticket', 'reply', 'POST', '/api/v1/admin/domains/*/tickets/*/replies', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(36, 'ticket.withdraw.self', '撤回我的工单', '客户撤回自己创建的工单', 'domain', 'ticket', 'withdraw_self', 'POST', '/api/v1/domains/*/tickets/my/**/withdraw', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(37, 'ticket.merge', '合并工单', '将工单合并到目标工单', 'domain', 'ticket', 'merge', 'POST', '/api/v1/admin/domains/*/tickets/*/merge', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(38, 'attachment.upload', '上传附件', '上传工单附件', 'domain', 'attachment', 'upload', 'POST', '/api/v1/attachments/upload', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(39, 'attachment.download', '下载附件', '下载工单附件', 'domain', 'attachment', 'download', 'GET', '/api/v1/attachments/*/download', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(40, 'inbox.read', '查看站内信', '查看站内信列表和未读数', 'domain', 'inbox', 'read', 'GET', '/api/v1/inbox/**', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(41, 'inbox.mark_read', '标记站内信已读', '将站内信标记为已读', 'domain', 'inbox', 'mark_read', 'POST', '/api/v1/inbox/**', 1, '2026-05-18 16:56:45.202', '2026-05-18 16:56:45.202'),
(44, 'domain.read', '查看业务域', '查看客户可见业务域列表和详情', 'domain', 'domain', 'read', 'GET', '/api/v1/domains/**', 1, '2026-05-18 16:56:45.789', '2026-05-18 16:56:45.789'),
(45, 'domain.admin.read', '查看业务域管理', '查看后台业务域列表和详情', 'platform', 'domain', 'admin_read', 'GET', '/api/v1/admin/domains/**', 1, '2026-05-18 16:56:45.789', '2026-05-18 16:56:45.789'),
(46, 'domain.admin.create', '创建业务域', '创建新的业务域', 'platform', 'domain', 'admin_create', 'POST', '/api/v1/admin/domains', 1, '2026-05-18 16:56:45.789', '2026-05-18 16:56:45.789'),
(47, 'domain.admin.update', '更新业务域', '更新业务域信息', 'platform', 'domain', 'admin_update', 'PUT', '/api/v1/admin/domains/*', 1, '2026-05-18 16:56:45.789', '2026-05-18 16:56:45.789'),
(48, 'domain.admin.delete', '删除业务域', '删除业务域', 'platform', 'domain', 'admin_delete', 'DELETE', '/api/v1/admin/domains/*', 1, '2026-05-18 16:56:45.789', '2026-05-18 16:56:45.789'),
(49, 'domain.customer.read', '查看域客户', '查看域客户列表', 'domain', 'domain.customer', 'read', 'GET', '/api/v1/admin/domains/*/customers', 1, '2026-05-18 16:56:46.319', '2026-05-18 16:56:46.319'),
(50, 'domain.customer.create', '创建域客户', '创建域客户入域关系', 'domain', 'domain.customer', 'create', 'POST', '/api/v1/admin/domains/*/customers', 1, '2026-05-18 16:56:46.319', '2026-05-18 16:56:46.319'),
(51, 'domain.customer.update_status', '更新域客户状态', '更新域客户状态', 'domain', 'domain.customer', 'update_status', 'PATCH', '/api/v1/admin/domains/*/customers/*/status', 1, '2026-05-18 16:56:46.319', '2026-05-18 16:56:46.319'),
(52, 'domain.invitation_code.read', '查看邀请码', '查看邀请码列表', 'domain', 'domain.invitation_code', 'read', 'GET', '/api/v1/admin/domains/*/invitation-codes', 1, '2026-05-18 16:56:46.319', '2026-05-18 16:56:46.319'),
(53, 'domain.invitation_code.create', '创建邀请码', '创建邀请码', 'domain', 'domain.invitation_code', 'create', 'POST', '/api/v1/admin/domains/*/invitation-codes', 1, '2026-05-18 16:56:46.319', '2026-05-18 16:56:46.319'),
(54, 'domain.invitation_code.delete', '删除邀请码', '删除邀请码', 'domain', 'domain.invitation_code', 'delete', 'DELETE', '/api/v1/admin/domains/*/invitation-codes/*', 1, '2026-05-18 16:56:46.319', '2026-05-18 16:56:46.319'),
(55, 'domain.ticket_type.read', 'View ticket types', 'View ticket type configuration within a business domain', 'domain', 'ticket_type', 'read', 'GET', '/api/v1/admin/domains/*/ticket-types', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(56, 'domain.ticket_type.create', 'Create ticket type', 'Create ticket type configuration within a business domain', 'domain', 'ticket_type', 'create', 'POST', '/api/v1/admin/domains/*/ticket-types', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(57, 'domain.ticket_type.update', 'Update ticket type', 'Update ticket type configuration within a business domain', 'domain', 'ticket_type', 'update', 'PUT', '/api/v1/admin/domains/*/ticket-types/*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(58, 'domain.ticket_type.delete', 'Delete ticket type', 'Delete ticket type configuration within a business domain', 'domain', 'ticket_type', 'delete', 'DELETE', '/api/v1/admin/domains/*/ticket-types/*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(59, 'domain.ticket_template.read', 'View ticket templates', 'View ticket template configuration within a business domain', 'domain', 'ticket_template', 'read', 'GET', '/api/v1/admin/domains/*/ticket-templates', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(60, 'domain.ticket_template.create', 'Create ticket template', 'Create ticket template configuration within a business domain', 'domain', 'ticket_template', 'create', 'POST', '/api/v1/admin/domains/*/ticket-templates', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(61, 'domain.ticket_template.update', 'Update ticket template', 'Update ticket template configuration within a business domain', 'domain', 'ticket_template', 'update', 'PUT', '/api/v1/admin/domains/*/ticket-templates/*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(62, 'domain.ticket_template.delete', 'Delete ticket template', 'Delete ticket template configuration within a business domain', 'domain', 'ticket_template', 'delete', 'DELETE', '/api/v1/admin/domains/*/ticket-templates/*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(63, 'domain.quick_reply.read', 'View quick replies', 'View quick reply configuration within a business domain', 'domain', 'quick_reply', 'read', 'GET', '/api/v1/admin/domains/*/quick-reply*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(64, 'domain.quick_reply.create', 'Create quick reply', 'Create quick reply configuration within a business domain', 'domain', 'quick_reply', 'create', 'POST', '/api/v1/admin/domains/*/quick-reply*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(65, 'domain.quick_reply.update', 'Update quick reply', 'Update quick reply configuration within a business domain', 'domain', 'quick_reply', 'update', 'PUT', '/api/v1/admin/domains/*/quick-reply*/*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(66, 'domain.quick_reply.delete', 'Delete quick reply', 'Delete quick reply configuration within a business domain', 'domain', 'quick_reply', 'delete', 'DELETE', '/api/v1/admin/domains/*/quick-reply*/*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(67, 'domain.priority_level.read', 'View priority levels', 'View priority level configuration within a business domain', 'domain', 'priority_level', 'read', 'GET', '/api/v1/admin/domains/*/priority-levels', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(68, 'domain.priority_level.create', 'Create priority level', 'Create priority level configuration within a business domain', 'domain', 'priority_level', 'create', 'POST', '/api/v1/admin/domains/*/priority-levels', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(69, 'domain.priority_level.update', 'Update priority level', 'Update priority level configuration within a business domain', 'domain', 'priority_level', 'update', 'PUT', '/api/v1/admin/domains/*/priority-levels/*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(70, 'domain.priority_level.delete', 'Delete priority level', 'Delete priority level configuration within a business domain', 'domain', 'priority_level', 'delete', 'DELETE', '/api/v1/admin/domains/*/priority-levels/*', 1, '2026-05-18 16:56:47.299', '2026-05-18 16:56:47.299'),
(71, 'platform.audit_log.read', '查看平台审计日志', '查看平台级审计日志列表', 'platform', 'audit_log', 'read', 'GET', '/api/v1/admin/audit-logs', 1, '2026-05-18 16:56:47.782', '2026-05-18 16:56:47.782'),
(72, 'domain.audit_log.read', '查看域审计日志', '查看业务域内审计日志列表', 'domain', 'audit_log', 'read', 'GET', '/api/v1/admin/domains/*/audit-logs', 1, '2026-05-18 16:56:47.782', '2026-05-18 16:56:47.782'),
(73, 'domain.login_log.read', '查看域登录日志', '查看业务域内登录日志列表', 'domain', 'login_log', 'read', 'GET', '/api/v1/admin/domains/*/login-logs', 1, '2026-05-18 16:56:47.782', '2026-05-18 16:56:47.782'),
(74, 'domain.blocked_word.read', '查看屏蔽词', '查看业务域内屏蔽词列表', 'domain', 'blocked_word', 'read', 'GET', '/api/v1/admin/domains/*/blocked-words', 1, '2026-05-18 16:56:48.357', '2026-05-18 16:56:48.357'),
(75, 'domain.blocked_word.create', '创建屏蔽词', '为业务域新增屏蔽词', 'domain', 'blocked_word', 'create', 'POST', '/api/v1/admin/domains/*/blocked-words', 1, '2026-05-18 16:56:48.357', '2026-05-18 16:56:48.357'),
(76, 'domain.blocked_word.delete', '删除屏蔽词', '删除业务域内屏蔽词', 'domain', 'blocked_word', 'delete', 'DELETE', '/api/v1/admin/domains/*/blocked-words/*', 1, '2026-05-18 16:56:48.357', '2026-05-18 16:56:48.357'),
(77, 'platform.login_log.read', 'View platform login logs', 'View platform login logs', 'platform', 'login_log', 'read', 'GET', '/api/v1/admin/login-logs', 1, '2026-05-18 16:56:50.377', '2026-05-18 16:56:50.377'),
(78, 'domain.config.read', 'View domain config', 'View business domain config', 'domain', 'domain_config', 'read', 'GET', '/api/v1/admin/domains/*/config', 1, '2026-05-18 16:56:50.377', '2026-05-18 16:56:50.377'),
(79, 'domain.config.update', 'Update domain config', 'Update business domain config', 'domain', 'domain_config', 'update', 'PUT', '/api/v1/admin/domains/*/config', 1, '2026-05-18 16:56:50.377', '2026-05-18 16:56:50.377'),
(80, 'platform.system_config.read', 'View system config', 'View platform system config', 'platform', 'system_config', 'read', 'GET', '/api/v1/admin/system-config', 1, '2026-05-18 16:56:50.377', '2026-05-18 16:56:50.377'),
(81, 'platform.system_config.update', 'Update system config', 'Update platform system config', 'platform', 'system_config', 'update', 'PUT', '/api/v1/admin/system-config', 1, '2026-05-18 16:56:50.377', '2026-05-18 16:56:50.377'),
(82, 'domain.sla.read', 'View SLA rules', 'View SLA rules and calendars within a business domain', 'domain', 'sla_rule', 'read', 'GET', '/api/v1/admin/domains/*/sla-*', 1, '2026-05-18 16:56:50.377', '2026-05-18 16:56:55.708'),
(83, 'domain.sla.create', 'Create SLA rule', 'Create SLA rules and calendars within a business domain', 'domain', 'sla_rule', 'create', 'POST', '/api/v1/admin/domains/*/sla-*', 1, '2026-05-18 16:56:50.377', '2026-05-18 16:56:55.708'),
(84, 'domain.notification_template.read', 'View notification templates', 'View notification templates', 'domain', 'notification_template', 'read', 'GET', '/api/v1/admin/domains/*/notification-templates', 1, '2026-05-18 16:56:50.377', '2026-05-18 16:56:50.377'),
(90, 'platform.organization.read', '查看组织架构', '查看平台组织架构', 'platform', 'platform.organization', 'read', 'GET', '/api/v1/iam/organizations', 1, '2026-05-18 16:57:03.343', '2026-05-18 16:57:03.343'),
(91, 'platform.organization.create', '新增组织', '创建平台组织节点', 'platform', 'platform.organization', 'create', 'POST', '/api/v1/iam/organizations', 1, '2026-05-18 16:57:03.343', '2026-05-18 16:57:05.007'),
(92, 'platform.user.import', '导入用户', '导入平台用户', 'platform', 'platform.user', 'import', NULL, NULL, 1, '2026-05-18 16:57:03.343', '2026-05-18 16:57:03.343'),
(93, 'platform.user.offboard_pool.export', '导出离职池', '导出离职池数据', 'platform', 'platform.user.offboard_pool', 'export', NULL, NULL, 1, '2026-05-18 16:57:03.343', '2026-05-18 16:57:03.343'),
(94, 'platform.user.offboard_pool.batch_restore', '批量恢复', '批量恢复离职池用户', 'platform', 'platform.user.offboard_pool', 'batch_restore', NULL, NULL, 1, '2026-05-18 16:57:03.343', '2026-05-18 16:57:03.343'),
(95, 'platform.organization.update', '编辑组织', '编辑平台组织节点', 'platform', 'platform.organization', 'update', 'PUT', '/api/v1/iam/organizations/*', 1, '2026-05-18 16:57:05.007', '2026-05-18 16:57:05.007'),
(96, 'platform.organization.delete', '删除组织', '删除平台组织节点', 'platform', 'platform.organization', 'delete', 'DELETE', '/api/v1/iam/organizations/*', 1, '2026-05-18 16:57:05.007', '2026-05-18 16:57:05.007'),
(99, 'platform.user.reset_password', '重置平台用户密码', '重置平台用户登录密码', 'platform', 'platform.user', 'reset_password', NULL, NULL, 1, '2026-05-18 16:57:05.813', '2026-05-18 16:57:05.813')
ON DUPLICATE KEY UPDATE
`code` = VALUES(`code`),
`name` = VALUES(`name`),
`description` = VALUES(`description`),
`permission_scope` = VALUES(`permission_scope`),
`resource_code` = VALUES(`resource_code`),
`action_code` = VALUES(`action_code`),
`http_method` = VALUES(`http_method`),
`path_pattern` = VALUES(`path_pattern`),
`status` = VALUES(`status`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `iam_resource`
INSERT INTO `iam_resource` (`id`, `resource_type`, `resource_code`, `resource_name`, `client_scope`, `http_method`, `path_pattern`, `parent_id`, `order_no`, `icon`, `component`, `hidden`, `status`, `created_at`, `updated_at`)
VALUES
(1, 'menu', 'menu.admin.dashboard', 'Admin Dashboard', 'ud-admin-web', NULL, '/dashboard/analysis', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:23.822'),
(10, 'action', 'api.auth.users.revoke_sessions', 'Revoke User Sessions', 'ud-admin-web', 'POST', '/api/v1/auth/users/*/revoke-sessions', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(11, 'action', 'api.auth.login_logs.read', 'Read Login Logs', 'ud-admin-web', 'GET', '/api/v1/auth/login-logs', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(12, 'action', 'api.auth.logout', 'Logout', 'all', 'POST', '/api/v1/auth/logout', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(13, 'action', 'api.dashboard.read', 'Read Dashboard', 'ud-admin-web', 'GET', '/api/v1/dashboard', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(14, 'action', 'api.domains.read', 'Read Domains', 'all', 'GET', '/api/v1/domains', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(15, 'action', 'api.tickets.list', 'List Tickets', 'all', 'GET', '/api/v1/tickets', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(16, 'action', 'api.tickets.create', 'Create Ticket', 'all', 'POST', '/api/v1/tickets', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(17, 'action', 'api.tickets.detail', 'Get Ticket Detail', 'all', 'GET', '/api/v1/tickets/*', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(18, 'action', 'api.tickets.detail_by_id', 'Get Ticket Detail By Id', 'all', 'GET', '/api/v1/tickets/id/*', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(19, 'action', 'api.tickets.status.update', 'Update Ticket Status', 'ud-admin-web', 'PATCH', '/api/v1/tickets/*/status', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(2, 'menu', 'menu.admin.home', 'Admin Workspace', 'ud-admin-web', NULL, '/home', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:23.822'),
(20, 'action', 'api.tickets.processing', 'Mark Ticket Processing', 'ud-admin-web', 'POST', '/api/v1/tickets/*/processing', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(21, 'action', 'api.tickets.resolved', 'Mark Ticket Resolved', 'ud-admin-web', 'POST', '/api/v1/tickets/*/resolved', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(22, 'action', 'api.consultations.list', 'List Consultations', 'all', 'GET', '/api/v1/consultations', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(23, 'action', 'api.consultations.messages.list', 'List Consultation Messages', 'all', 'GET', '/api/v1/consultations/*/messages', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(24, 'action', 'api.consultations.messages.create', 'Create Consultation Message', 'all', 'POST', '/api/v1/consultations/messages', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(25, 'action', 'api.consultations.messages.create_by_session', 'Create Consultation Message By Session', 'all', 'POST', '/api/v1/consultations/*/messages', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(26, 'action', 'api.consultations.convert_ticket', 'Convert Consultation To Ticket', 'ud-admin-web', 'POST', '/api/v1/consultations/*/ticket', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(27, 'action', 'api.iam.resources.read', 'Read IAM Resources', 'ud-admin-web', 'GET', '/api/v1/iam/resources', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(28, 'action', 'api.iam.resources.create', 'Create IAM Resource', 'ud-admin-web', 'POST', '/api/v1/iam/resources', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(29, 'action', 'api.iam.resources.update', 'Update IAM Resource', 'ud-admin-web', 'PUT', '/api/v1/iam/resources/*', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(3, 'menu', 'menu.admin.system.login_governance', 'Login Governance', 'ud-admin-web', NULL, '/system/login-governance', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:23.822'),
(30, 'action', 'api.iam.role_resources.read', 'Read Role Resources', 'ud-admin-web', 'GET', '/api/v1/iam/roles/*/resources', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(31, 'action', 'api.iam.role_resources.update', 'Update Role Resources', 'ud-admin-web', 'PUT', '/api/v1/iam/roles/*/resources', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(32, 'action', 'api.iam.me.menu_resources.read', 'Read Current Menu Resources', 'all', 'GET', '/api/v1/iam/me/menu-resources', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(33, 'action', 'action.iam.me.permission_snapshot.read', 'Read Permission Snapshot', 'all', 'GET', '/api/v1/iam/me/permission-snapshot', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:25.671', '2026-05-18 16:56:25.671'),
(34, 'menu', 'menu.admin.system', '系统管理', 'ud-admin-web', NULL, '/system', NULL, 100, 'SettingOutlined', NULL, 0, 1, '2026-05-18 16:56:27.067', '2026-05-18 16:56:29.815'),
(35, 'menu', 'menu.admin.system.menus', '菜单管理', 'ud-admin-web', NULL, '/system/menus', 34, 101, 'MenuOutlined', './system/menus', 0, 1, '2026-05-18 16:56:27.067', '2026-05-18 16:56:29.815'),
(36, 'menu', 'menu.admin.system.roles', '角色管理', 'ud-admin-web', NULL, '/system/roles', 34, 102, 'TeamOutlined', './system/roles', 0, 1, '2026-05-18 16:56:27.067', '2026-05-18 16:56:29.815'),
(37, 'menu', 'menu.admin.system.users', '用户管理', 'ud-admin-web', NULL, '/system/users', 34, 103, 'UserOutlined', './system/users', 0, 1, '2026-05-18 16:56:27.067', '2026-05-18 16:56:29.815'),
(38, 'menu', 'menu.admin.system.users.offboard_pool', '离职池', 'ud-admin-web', NULL, '/system/users/offboard-pool', 34, 104, 'DeleteOutlined', './system/users/offboard-pool', 0, 1, '2026-05-18 16:56:27.067', '2026-05-18 16:56:29.815'),
(39, 'action', 'action.iam.menus.read', 'Read Menu Tree', 'ud-admin-web', 'GET', '/api/v1/iam/menus/tree', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(4, 'menu', 'menu.customer.workspace', 'Customer Workspace', 'ud-customer-web', NULL, '/', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:23.822'),
(40, 'action', 'action.iam.menus.create', 'Create Menu', 'ud-admin-web', 'POST', '/api/v1/iam/menus', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(41, 'action', 'action.iam.menus.update', 'Update Menu', 'ud-admin-web', 'PUT', '/api/v1/iam/menus/*', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(42, 'action', 'action.iam.menus.delete', 'Delete Menu', 'ud-admin-web', 'DELETE', '/api/v1/iam/menus/*', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(43, 'action', 'action.iam.roles.read', 'Read Roles', 'ud-admin-web', 'GET', '/api/v1/iam/roles', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(44, 'action', 'action.iam.roles.create', 'Create Role', 'ud-admin-web', 'POST', '/api/v1/iam/roles', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(45, 'action', 'action.iam.roles.update', 'Update Role', 'ud-admin-web', 'PUT', '/api/v1/iam/roles/*', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(46, 'action', 'action.iam.roles.delete', 'Delete Role', 'ud-admin-web', 'DELETE', '/api/v1/iam/roles/*', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(47, 'action', 'action.iam.role_permissions.read', 'Read Role Permissions', 'ud-admin-web', 'GET', '/api/v1/iam/roles/*/permissions', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(48, 'action', 'action.iam.role_permissions.update', 'Update Role Permissions', 'ud-admin-web', 'PUT', '/api/v1/iam/roles/*/permissions', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(49, 'action', 'action.iam.users.read', 'Read Users', 'ud-admin-web', 'GET', '/api/v1/iam/users', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(5, 'action', 'api.auth.session.read', 'Read Current Session', 'all', 'GET', '/api/v1/auth/session', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(50, 'action', 'action.iam.users.create', '创建域用户', 'ud-admin-web', 'POST', '/api/v1/iam/users', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:29.815'),
(51, 'action', 'action.iam.users.update', 'Update User', 'ud-admin-web', 'PUT', '/api/v1/iam/users/*', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(52, 'action', 'action.iam.users.offboard', 'Offboard User', 'ud-admin-web', 'POST', '/api/v1/iam/users/*/offboard', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(53, 'action', 'action.iam.users.restore', 'Restore User', 'ud-admin-web', 'POST', '/api/v1/iam/users/*/restore', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(54, 'action', 'action.iam.users.offboard_pool.read', 'Read Offboard Pool', 'ud-admin-web', 'GET', '/api/v1/iam/users/offboard-pool', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(55, 'action', 'action.iam.users.delete', 'Delete User Permanently', 'ud-admin-web', 'DELETE', '/api/v1/iam/users/*', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:27.146', '2026-05-18 16:56:27.146'),
(6, 'action', 'api.auth.login_config.read', 'Read Login Config', 'all', 'GET', '/api/v1/auth/login-config', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(7, 'action', 'api.auth.login_config.update', 'Update Login Config', 'ud-admin-web', 'PUT', '/api/v1/auth/login-config', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(8, 'action', 'api.auth.online_sessions.read', 'Read Online Sessions', 'ud-admin-web', 'GET', '/api/v1/auth/online-sessions', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755'),
(9, 'action', 'api.auth.online_sessions.revoke', 'Revoke Session', 'ud-admin-web', 'POST', '/api/v1/auth/online-sessions/*/revoke', NULL, 0, NULL, NULL, 0, 1, '2026-05-18 16:56:23.822', '2026-05-18 16:56:24.755')
ON DUPLICATE KEY UPDATE
`resource_type` = VALUES(`resource_type`),
`resource_code` = VALUES(`resource_code`),
`resource_name` = VALUES(`resource_name`),
`client_scope` = VALUES(`client_scope`),
`http_method` = VALUES(`http_method`),
`path_pattern` = VALUES(`path_pattern`),
`parent_id` = VALUES(`parent_id`),
`order_no` = VALUES(`order_no`),
`icon` = VALUES(`icon`),
`component` = VALUES(`component`),
`hidden` = VALUES(`hidden`),
`status` = VALUES(`status`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `identity_subject`
INSERT INTO `identity_subject` (`id`, `subject_type`, `phone`, `status`, `merged_into_id`, `created_at`, `updated_at`)
VALUES
(1, 'person', '13800000000', 'active', NULL, '2026-05-18 16:56:56.145', '2026-05-18 16:56:56.145'),
(2, 'person', '13900000000', 'active', NULL, '2026-05-18 16:56:56.145', '2026-05-18 16:56:56.145')
ON DUPLICATE KEY UPDATE
`subject_type` = VALUES(`subject_type`),
`phone` = VALUES(`phone`),
`status` = VALUES(`status`),
`merged_into_id` = VALUES(`merged_into_id`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `permission_item`
INSERT INTO `permission_item` (`id`, `code`, `name`, `module`, `type`, `created_at`, `updated_at`)
VALUES
(1, 'user_manage:menu', '域成员管理菜单', 'domain', 'menu', '2026-05-18 16:56:46.802', '2026-05-18 16:56:46.802'),
(2, 'domain_config:menu', '域配置菜单', 'domain', 'menu', '2026-05-18 16:56:46.802', '2026-05-18 16:56:46.802'),
(3, 'log:menu', '日志查看菜单', 'domain', 'menu', '2026-05-18 16:56:46.802', '2026-05-18 16:56:46.802'),
(4, 'word_filter:menu', '屏蔽词菜单', 'domain', 'menu', '2026-05-18 16:56:46.802', '2026-05-18 16:56:46.802'),
(5, 'domain:user_manage', '域成员管理', 'domain', 'button', '2026-05-18 16:56:46.802', '2026-05-18 16:56:46.802'),
(6, 'domain:config', '域配置管理', 'domain', 'button', '2026-05-18 16:56:46.802', '2026-05-18 16:56:46.802'),
(7, 'domain:log_view', '域日志查看', 'domain', 'button', '2026-05-18 16:56:46.802', '2026-05-18 16:56:46.802'),
(8, 'domain:word_filter', '域屏蔽词管理', 'domain', 'button', '2026-05-18 16:56:46.802', '2026-05-18 16:56:46.802')
ON DUPLICATE KEY UPDATE
`code` = VALUES(`code`),
`name` = VALUES(`name`),
`module` = VALUES(`module`),
`type` = VALUES(`type`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `domain_role_permission`
INSERT INTO `domain_role_permission` (`domain_role_id`, `permission_item_id`, `created_at`)
VALUES
(1, 1, '2026-05-18 16:56:46.927'),
(1, 2, '2026-05-18 16:56:46.927'),
(1, 3, '2026-05-18 16:56:46.927'),
(1, 4, '2026-05-18 16:56:46.927'),
(1, 5, '2026-05-18 16:56:46.927'),
(1, 6, '2026-05-18 16:56:46.927'),
(1, 7, '2026-05-18 16:56:46.927'),
(1, 8, '2026-05-18 16:56:46.927'),
(2, 1, '2026-05-18 16:56:46.927'),
(2, 2, '2026-05-18 16:56:46.927'),
(2, 3, '2026-05-18 16:56:46.927'),
(2, 4, '2026-05-18 16:56:46.927'),
(2, 5, '2026-05-18 16:56:46.927'),
(2, 6, '2026-05-18 16:56:46.927'),
(2, 7, '2026-05-18 16:56:46.927'),
(2, 8, '2026-05-18 16:56:46.927')
ON DUPLICATE KEY UPDATE
`created_at` = VALUES(`created_at`);

-- Seed data for `platform_role`
INSERT INTO `platform_role` (`id`, `code`, `name`, `preset`, `created_at`, `updated_at`)
VALUES
(1, 'platform_admin', '平台管理员', 1, '2026-05-18 16:56:44.578', '2026-05-18 16:56:44.578'),
(2, 'security_auditor', '安全审计员', 1, '2026-05-18 16:56:44.628', '2026-05-18 16:56:44.628')
ON DUPLICATE KEY UPDATE
`code` = VALUES(`code`),
`name` = VALUES(`name`),
`preset` = VALUES(`preset`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `role`
INSERT INTO `role` (`id`, `code`, `name`, `scope`, `is_system`, `created_at`, `updated_at`)
VALUES
(1, 'customer', '客户用户', 'domain', 1, '2026-05-18 16:56:16.473', '2026-05-18 16:56:28.635'),
(2, 'agent', '客服专员', 'domain', 1, '2026-05-18 16:56:16.473', '2026-05-18 16:56:28.635'),
(3, 'domain_admin', '业务域管理员', 'domain', 1, '2026-05-18 16:56:16.473', '2026-05-18 16:56:28.635'),
(4, 'super_admin', '超级管理员', 'global', 1, '2026-05-18 16:56:16.473', '2026-05-18 16:56:28.635'),
(5, 'platform_admin', '平台管理员', 'global', 1, '2026-05-18 16:56:28.666', '2026-05-18 16:56:28.666'),
(6, 'security_auditor', '安全审计员', 'global', 1, '2026-05-18 16:56:28.666', '2026-05-18 16:56:28.666')
ON DUPLICATE KEY UPDATE
`code` = VALUES(`code`),
`name` = VALUES(`name`),
`scope` = VALUES(`scope`),
`is_system` = VALUES(`is_system`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `iam_admin_role_menu_relation`
INSERT INTO `iam_admin_role_menu_relation` (`id`, `role_id`, `menu_id`, `created_at`)
VALUES
(1, 4, 2, '2026-05-18 16:56:28.186'),
(2, 4, 3, '2026-05-18 16:56:28.186'),
(3, 4, 4, '2026-05-18 16:56:28.186'),
(4, 4, 5, '2026-05-18 16:56:28.186'),
(8, 4, 7, '2026-05-18 16:56:28.217'),
(9, 4, 9, '2026-05-18 16:56:28.217'),
(10, 4, 6, '2026-05-18 16:56:28.217'),
(11, 4, 8, '2026-05-18 16:56:28.217'),
(12, 4, 14, '2026-05-18 16:56:28.217'),
(13, 4, 15, '2026-05-18 16:56:28.217'),
(14, 4, 11, '2026-05-18 16:56:28.217'),
(15, 4, 13, '2026-05-18 16:56:28.217'),
(16, 4, 10, '2026-05-18 16:56:28.217'),
(17, 4, 12, '2026-05-18 16:56:28.217'),
(18, 4, 17, '2026-05-18 16:56:28.217'),
(19, 4, 22, '2026-05-18 16:56:28.217'),
(20, 4, 19, '2026-05-18 16:56:28.217'),
(21, 4, 20, '2026-05-18 16:56:28.217'),
(22, 4, 16, '2026-05-18 16:56:28.217'),
(23, 4, 21, '2026-05-18 16:56:28.217'),
(24, 4, 18, '2026-05-18 16:56:28.217'),
(71, 4, 50, '2026-05-18 16:57:00.036'),
(72, 4, 48, '2026-05-18 16:57:00.036'),
(73, 4, 49, '2026-05-18 16:57:00.036'),
(74, 4, 63, '2026-05-18 16:57:00.066'),
(76, 4, 38, '2026-05-18 16:57:00.999'),
(77, 4, 56, '2026-05-18 16:57:01.029'),
(78, 4, 39, '2026-05-18 16:57:01.777'),
(79, 4, 40, '2026-05-18 16:57:01.777'),
(80, 4, 41, '2026-05-18 16:57:01.777'),
(81, 4, 42, '2026-05-18 16:57:01.777'),
(82, 4, 43, '2026-05-18 16:57:01.777'),
(83, 4, 44, '2026-05-18 16:57:01.777'),
(84, 4, 45, '2026-05-18 16:57:01.777'),
(85, 4, 46, '2026-05-18 16:57:01.777'),
(86, 4, 47, '2026-05-18 16:57:01.777'),
(87, 4, 51, '2026-05-18 16:57:01.777'),
(88, 4, 52, '2026-05-18 16:57:01.777'),
(89, 4, 53, '2026-05-18 16:57:01.777'),
(90, 4, 54, '2026-05-18 16:57:01.777'),
(91, 4, 55, '2026-05-18 16:57:01.777'),
(93, 4, 69, '2026-05-18 16:57:02.533'),
(94, 4, 71, '2026-05-18 16:57:04.491'),
(97, 4, 73, '2026-05-18 16:57:04.538'),
(98, 4, 74, '2026-05-18 16:57:04.538'),
(99, 4, 75, '2026-05-18 16:57:04.538'),
(100, 4, 76, '2026-05-18 16:57:04.538'),
(101, 4, 57, '2026-05-18 16:57:04.538'),
(102, 4, 72, '2026-05-18 16:57:04.538'),
(104, 5, 78, '2026-05-18 16:57:05.302'),
(105, 5, 77, '2026-05-18 16:57:05.302'),
(106, 5, 74, '2026-05-18 16:57:05.302'),
(107, 5, 73, '2026-05-18 16:57:05.302'),
(108, 4, 78, '2026-05-18 16:57:05.302'),
(109, 4, 77, '2026-05-18 16:57:05.302'),
(111, 5, 79, '2026-05-18 16:57:07.240'),
(112, 4, 79, '2026-05-18 16:57:07.240')
ON DUPLICATE KEY UPDATE
`role_id` = VALUES(`role_id`),
`menu_id` = VALUES(`menu_id`),
`created_at` = VALUES(`created_at`);

-- Seed data for `iam_role_permission`
INSERT INTO `iam_role_permission` (`id`, `role_id`, `permission_id`, `created_at`)
VALUES
(1, 4, 20, '2026-05-18 16:56:29.442'),
(2, 4, 21, '2026-05-18 16:56:29.442'),
(3, 4, 22, '2026-05-18 16:56:29.442'),
(4, 4, 23, '2026-05-18 16:56:29.442'),
(5, 4, 24, '2026-05-18 16:56:29.442'),
(6, 4, 25, '2026-05-18 16:56:29.442'),
(7, 4, 26, '2026-05-18 16:56:29.442'),
(8, 4, 27, '2026-05-18 16:56:29.442'),
(9, 4, 28, '2026-05-18 16:56:29.442'),
(10, 4, 29, '2026-05-18 16:56:29.442'),
(11, 4, 1, '2026-05-18 16:56:29.442'),
(12, 4, 2, '2026-05-18 16:56:29.442'),
(13, 4, 3, '2026-05-18 16:56:29.442'),
(14, 4, 4, '2026-05-18 16:56:29.442'),
(15, 4, 5, '2026-05-18 16:56:29.442'),
(16, 4, 6, '2026-05-18 16:56:29.442'),
(17, 4, 7, '2026-05-18 16:56:29.442'),
(18, 4, 8, '2026-05-18 16:56:29.442'),
(19, 4, 9, '2026-05-18 16:56:29.442'),
(20, 4, 10, '2026-05-18 16:56:29.442'),
(21, 4, 11, '2026-05-18 16:56:29.442'),
(22, 4, 12, '2026-05-18 16:56:29.442'),
(23, 4, 13, '2026-05-18 16:56:29.442'),
(24, 4, 14, '2026-05-18 16:56:29.442'),
(25, 4, 15, '2026-05-18 16:56:29.442'),
(26, 4, 16, '2026-05-18 16:56:29.442'),
(27, 4, 17, '2026-05-18 16:56:29.442'),
(28, 4, 18, '2026-05-18 16:56:29.442'),
(29, 4, 19, '2026-05-18 16:56:29.442'),
(32, 5, 1, '2026-05-18 16:56:29.490'),
(33, 5, 2, '2026-05-18 16:56:29.490'),
(34, 5, 3, '2026-05-18 16:56:29.490'),
(35, 5, 4, '2026-05-18 16:56:29.490'),
(36, 5, 5, '2026-05-18 16:56:29.490'),
(37, 5, 6, '2026-05-18 16:56:29.490'),
(38, 5, 7, '2026-05-18 16:56:29.490'),
(39, 5, 8, '2026-05-18 16:56:29.490'),
(40, 5, 9, '2026-05-18 16:56:29.490'),
(41, 5, 10, '2026-05-18 16:56:29.490'),
(42, 5, 11, '2026-05-18 16:56:29.490'),
(43, 5, 12, '2026-05-18 16:56:29.490'),
(44, 5, 13, '2026-05-18 16:56:29.490'),
(45, 5, 14, '2026-05-18 16:56:29.490'),
(46, 5, 15, '2026-05-18 16:56:29.490'),
(47, 5, 16, '2026-05-18 16:56:29.490'),
(48, 5, 17, '2026-05-18 16:56:29.490'),
(49, 5, 18, '2026-05-18 16:56:29.490'),
(50, 5, 19, '2026-05-18 16:56:29.490'),
(63, 5, 21, '2026-05-18 16:56:29.536'),
(64, 5, 20, '2026-05-18 16:56:29.536'),
(65, 5, 23, '2026-05-18 16:56:29.536'),
(66, 5, 22, '2026-05-18 16:56:29.536'),
(70, 6, 1, '2026-05-18 16:56:29.582'),
(71, 6, 9, '2026-05-18 16:56:29.582'),
(72, 6, 5, '2026-05-18 16:56:29.582'),
(73, 6, 17, '2026-05-18 16:56:29.582'),
(74, 6, 12, '2026-05-18 16:56:29.582'),
(77, 3, 29, '2026-05-18 16:56:29.629'),
(78, 3, 25, '2026-05-18 16:56:29.629'),
(79, 3, 24, '2026-05-18 16:56:29.629'),
(80, 3, 21, '2026-05-18 16:56:29.629'),
(81, 3, 20, '2026-05-18 16:56:29.629'),
(82, 3, 23, '2026-05-18 16:56:29.629'),
(83, 3, 22, '2026-05-18 16:56:29.629'),
(84, 3, 27, '2026-05-18 16:56:29.629'),
(85, 3, 28, '2026-05-18 16:56:29.629'),
(86, 3, 26, '2026-05-18 16:56:29.629'),
(92, 2, 29, '2026-05-18 16:56:29.674'),
(93, 2, 27, '2026-05-18 16:56:29.674'),
(94, 2, 26, '2026-05-18 16:56:29.674'),
(95, 1, 39, '2026-05-18 16:56:45.248'),
(96, 1, 38, '2026-05-18 16:56:45.248'),
(97, 1, 41, '2026-05-18 16:56:45.248'),
(98, 1, 40, '2026-05-18 16:56:45.248'),
(99, 1, 30, '2026-05-18 16:56:45.248'),
(100, 1, 34, '2026-05-18 16:56:45.248'),
(101, 1, 31, '2026-05-18 16:56:45.248'),
(102, 1, 36, '2026-05-18 16:56:45.248'),
(110, 2, 39, '2026-05-18 16:56:45.303'),
(111, 2, 38, '2026-05-18 16:56:45.303'),
(112, 2, 41, '2026-05-18 16:56:45.303'),
(113, 2, 40, '2026-05-18 16:56:45.303'),
(114, 2, 33, '2026-05-18 16:56:45.303'),
(115, 2, 28, '2026-05-18 16:56:45.303'),
(116, 2, 35, '2026-05-18 16:56:45.303'),
(117, 2, 32, '2026-05-18 16:56:45.303'),
(125, 3, 39, '2026-05-18 16:56:45.356'),
(126, 3, 38, '2026-05-18 16:56:45.356'),
(127, 3, 41, '2026-05-18 16:56:45.356'),
(128, 3, 40, '2026-05-18 16:56:45.356'),
(129, 3, 33, '2026-05-18 16:56:45.356'),
(130, 3, 37, '2026-05-18 16:56:45.356'),
(131, 3, 35, '2026-05-18 16:56:45.356'),
(132, 3, 32, '2026-05-18 16:56:45.356'),
(140, 4, 30, '2026-05-18 16:56:45.403'),
(141, 4, 31, '2026-05-18 16:56:45.403'),
(142, 4, 32, '2026-05-18 16:56:45.403'),
(143, 4, 33, '2026-05-18 16:56:45.403'),
(144, 4, 34, '2026-05-18 16:56:45.403'),
(145, 4, 35, '2026-05-18 16:56:45.403'),
(146, 4, 36, '2026-05-18 16:56:45.403'),
(147, 4, 37, '2026-05-18 16:56:45.403'),
(148, 4, 38, '2026-05-18 16:56:45.403'),
(149, 4, 39, '2026-05-18 16:56:45.403'),
(150, 4, 40, '2026-05-18 16:56:45.403'),
(151, 4, 41, '2026-05-18 16:56:45.403'),
(155, 1, 44, '2026-05-18 16:56:45.836'),
(156, 5, 46, '2026-05-18 16:56:45.882'),
(157, 5, 48, '2026-05-18 16:56:45.882'),
(158, 5, 45, '2026-05-18 16:56:45.882'),
(159, 5, 47, '2026-05-18 16:56:45.882'),
(160, 5, 44, '2026-05-18 16:56:45.882'),
(163, 4, 46, '2026-05-18 16:56:45.928'),
(164, 4, 48, '2026-05-18 16:56:45.928'),
(165, 4, 45, '2026-05-18 16:56:45.928'),
(166, 4, 47, '2026-05-18 16:56:45.928'),
(167, 4, 44, '2026-05-18 16:56:45.928'),
(170, 4, 50, '2026-05-18 16:56:46.365'),
(171, 3, 50, '2026-05-18 16:56:46.365'),
(172, 4, 49, '2026-05-18 16:56:46.365'),
(173, 3, 49, '2026-05-18 16:56:46.365'),
(174, 4, 51, '2026-05-18 16:56:46.365'),
(175, 3, 51, '2026-05-18 16:56:46.365'),
(176, 4, 53, '2026-05-18 16:56:46.365'),
(177, 3, 53, '2026-05-18 16:56:46.365'),
(178, 4, 54, '2026-05-18 16:56:46.365'),
(179, 3, 54, '2026-05-18 16:56:46.365'),
(180, 4, 52, '2026-05-18 16:56:46.365'),
(181, 3, 52, '2026-05-18 16:56:46.365'),
(185, 2, 49, '2026-05-18 16:56:46.412'),
(186, 3, 68, '2026-05-18 16:56:47.347'),
(187, 3, 70, '2026-05-18 16:56:47.347'),
(188, 3, 67, '2026-05-18 16:56:47.347'),
(189, 3, 69, '2026-05-18 16:56:47.347'),
(190, 3, 64, '2026-05-18 16:56:47.347'),
(191, 3, 66, '2026-05-18 16:56:47.347'),
(192, 3, 63, '2026-05-18 16:56:47.347'),
(193, 3, 65, '2026-05-18 16:56:47.347'),
(194, 3, 60, '2026-05-18 16:56:47.347'),
(195, 3, 62, '2026-05-18 16:56:47.347'),
(196, 3, 59, '2026-05-18 16:56:47.347'),
(197, 3, 61, '2026-05-18 16:56:47.347'),
(198, 3, 56, '2026-05-18 16:56:47.347'),
(199, 3, 58, '2026-05-18 16:56:47.347'),
(200, 3, 55, '2026-05-18 16:56:47.347'),
(201, 3, 57, '2026-05-18 16:56:47.347'),
(217, 4, 68, '2026-05-18 16:56:47.393'),
(218, 4, 70, '2026-05-18 16:56:47.393'),
(219, 4, 67, '2026-05-18 16:56:47.393'),
(220, 4, 69, '2026-05-18 16:56:47.393'),
(221, 4, 64, '2026-05-18 16:56:47.393'),
(222, 4, 66, '2026-05-18 16:56:47.393'),
(223, 4, 63, '2026-05-18 16:56:47.393'),
(224, 4, 65, '2026-05-18 16:56:47.393'),
(225, 4, 60, '2026-05-18 16:56:47.393'),
(226, 4, 62, '2026-05-18 16:56:47.393'),
(227, 4, 59, '2026-05-18 16:56:47.393'),
(228, 4, 61, '2026-05-18 16:56:47.393'),
(229, 4, 56, '2026-05-18 16:56:47.393'),
(230, 4, 58, '2026-05-18 16:56:47.393'),
(231, 4, 55, '2026-05-18 16:56:47.393'),
(232, 4, 57, '2026-05-18 16:56:47.393'),
(248, 5, 71, '2026-05-18 16:56:47.829'),
(249, 4, 71, '2026-05-18 16:56:47.875'),
(250, 3, 72, '2026-05-18 16:56:47.922'),
(251, 3, 73, '2026-05-18 16:56:47.922'),
(253, 4, 72, '2026-05-18 16:56:47.968'),
(254, 4, 73, '2026-05-18 16:56:47.968'),
(256, 3, 75, '2026-05-18 16:56:48.403'),
(257, 3, 76, '2026-05-18 16:56:48.403'),
(258, 3, 74, '2026-05-18 16:56:48.403'),
(259, 4, 75, '2026-05-18 16:56:48.449'),
(260, 4, 76, '2026-05-18 16:56:48.449'),
(261, 4, 74, '2026-05-18 16:56:48.449'),
(262, 4, 78, '2026-05-18 16:56:50.423'),
(263, 5, 78, '2026-05-18 16:56:50.423'),
(264, 3, 78, '2026-05-18 16:56:50.423'),
(265, 4, 79, '2026-05-18 16:56:50.423'),
(266, 5, 79, '2026-05-18 16:56:50.423'),
(267, 3, 79, '2026-05-18 16:56:50.423'),
(268, 4, 84, '2026-05-18 16:56:50.423'),
(269, 5, 84, '2026-05-18 16:56:50.423'),
(270, 3, 84, '2026-05-18 16:56:50.423'),
(271, 5, 25, '2026-05-18 16:56:50.423'),
(272, 4, 83, '2026-05-18 16:56:50.423'),
(273, 5, 83, '2026-05-18 16:56:50.423'),
(274, 3, 83, '2026-05-18 16:56:50.423'),
(275, 4, 82, '2026-05-18 16:56:50.423'),
(276, 5, 82, '2026-05-18 16:56:50.423'),
(277, 3, 82, '2026-05-18 16:56:50.423'),
(278, 5, 24, '2026-05-18 16:56:50.423'),
(279, 4, 77, '2026-05-18 16:56:50.423'),
(280, 5, 77, '2026-05-18 16:56:50.423'),
(281, 3, 77, '2026-05-18 16:56:50.423'),
(282, 4, 80, '2026-05-18 16:56:50.423'),
(283, 5, 80, '2026-05-18 16:56:50.423'),
(284, 3, 80, '2026-05-18 16:56:50.423'),
(285, 4, 81, '2026-05-18 16:56:50.423'),
(286, 5, 81, '2026-05-18 16:56:50.423'),
(287, 3, 81, '2026-05-18 16:56:50.423'),
(294, 4, 91, '2026-05-18 16:57:04.583'),
(295, 5, 91, '2026-05-18 16:57:04.583'),
(296, 4, 90, '2026-05-18 16:57:04.583'),
(297, 5, 90, '2026-05-18 16:57:04.583'),
(298, 4, 92, '2026-05-18 16:57:04.583'),
(299, 5, 92, '2026-05-18 16:57:04.583'),
(300, 4, 94, '2026-05-18 16:57:04.583'),
(301, 5, 94, '2026-05-18 16:57:04.583'),
(302, 4, 93, '2026-05-18 16:57:04.583'),
(303, 5, 93, '2026-05-18 16:57:04.583'),
(309, 4, 96, '2026-05-18 16:57:05.350'),
(310, 5, 96, '2026-05-18 16:57:05.350'),
(311, 4, 95, '2026-05-18 16:57:05.350'),
(312, 5, 95, '2026-05-18 16:57:05.350')
ON DUPLICATE KEY UPDATE
`role_id` = VALUES(`role_id`),
`permission_id` = VALUES(`permission_id`),
`created_at` = VALUES(`created_at`);

-- Seed data for `iam_role_resource`
INSERT INTO `iam_role_resource` (`id`, `role_id`, `resource_id`, `created_at`)
VALUES
(1, 4, 5, '2026-05-18 16:56:23.868'),
(2, 4, 6, '2026-05-18 16:56:23.868'),
(3, 4, 12, '2026-05-18 16:56:23.868'),
(4, 4, 14, '2026-05-18 16:56:23.868'),
(5, 4, 15, '2026-05-18 16:56:23.868'),
(6, 4, 16, '2026-05-18 16:56:23.868'),
(7, 4, 17, '2026-05-18 16:56:23.868'),
(8, 4, 18, '2026-05-18 16:56:23.868'),
(9, 4, 22, '2026-05-18 16:56:23.868'),
(10, 4, 23, '2026-05-18 16:56:23.868'),
(11, 4, 24, '2026-05-18 16:56:23.868'),
(12, 4, 25, '2026-05-18 16:56:23.868'),
(13, 4, 32, '2026-05-18 16:56:23.868'),
(14, 4, 7, '2026-05-18 16:56:23.868'),
(15, 4, 8, '2026-05-18 16:56:23.868'),
(16, 4, 9, '2026-05-18 16:56:23.868'),
(17, 4, 10, '2026-05-18 16:56:23.868'),
(18, 4, 11, '2026-05-18 16:56:23.868'),
(19, 4, 13, '2026-05-18 16:56:23.868'),
(20, 4, 19, '2026-05-18 16:56:23.868'),
(21, 4, 20, '2026-05-18 16:56:23.868'),
(22, 4, 21, '2026-05-18 16:56:23.868'),
(23, 4, 26, '2026-05-18 16:56:23.868'),
(24, 4, 27, '2026-05-18 16:56:23.868'),
(25, 4, 28, '2026-05-18 16:56:23.868'),
(26, 4, 29, '2026-05-18 16:56:23.868'),
(27, 4, 30, '2026-05-18 16:56:23.868'),
(28, 4, 31, '2026-05-18 16:56:23.868'),
(29, 4, 1, '2026-05-18 16:56:23.868'),
(30, 4, 2, '2026-05-18 16:56:23.868'),
(31, 4, 3, '2026-05-18 16:56:23.868'),
(32, 4, 4, '2026-05-18 16:56:23.868'),
(64, 3, 6, '2026-05-18 16:56:23.916'),
(65, 2, 6, '2026-05-18 16:56:23.916'),
(66, 3, 12, '2026-05-18 16:56:23.916'),
(67, 2, 12, '2026-05-18 16:56:23.916'),
(68, 3, 5, '2026-05-18 16:56:23.916'),
(69, 2, 5, '2026-05-18 16:56:23.916'),
(70, 3, 26, '2026-05-18 16:56:23.916'),
(71, 2, 26, '2026-05-18 16:56:23.916'),
(72, 3, 22, '2026-05-18 16:56:23.916'),
(73, 2, 22, '2026-05-18 16:56:23.916'),
(74, 3, 24, '2026-05-18 16:56:23.916'),
(75, 2, 24, '2026-05-18 16:56:23.916'),
(76, 3, 25, '2026-05-18 16:56:23.916'),
(77, 2, 25, '2026-05-18 16:56:23.916'),
(78, 3, 23, '2026-05-18 16:56:23.916'),
(79, 2, 23, '2026-05-18 16:56:23.916'),
(80, 3, 13, '2026-05-18 16:56:23.916'),
(81, 2, 13, '2026-05-18 16:56:23.916'),
(82, 3, 14, '2026-05-18 16:56:23.916'),
(83, 2, 14, '2026-05-18 16:56:23.916'),
(84, 3, 32, '2026-05-18 16:56:23.916'),
(85, 2, 32, '2026-05-18 16:56:23.916'),
(86, 3, 17, '2026-05-18 16:56:23.916'),
(87, 2, 17, '2026-05-18 16:56:23.916'),
(88, 3, 18, '2026-05-18 16:56:23.916'),
(89, 2, 18, '2026-05-18 16:56:23.916'),
(90, 3, 15, '2026-05-18 16:56:23.916'),
(91, 2, 15, '2026-05-18 16:56:23.916'),
(92, 3, 20, '2026-05-18 16:56:23.916'),
(93, 2, 20, '2026-05-18 16:56:23.916'),
(94, 3, 21, '2026-05-18 16:56:23.916'),
(95, 2, 21, '2026-05-18 16:56:23.916'),
(96, 3, 19, '2026-05-18 16:56:23.916'),
(97, 2, 19, '2026-05-18 16:56:23.916'),
(98, 3, 1, '2026-05-18 16:56:23.916'),
(99, 2, 1, '2026-05-18 16:56:23.916'),
(100, 3, 2, '2026-05-18 16:56:23.916'),
(101, 2, 2, '2026-05-18 16:56:23.916'),
(127, 1, 6, '2026-05-18 16:56:23.961'),
(128, 1, 12, '2026-05-18 16:56:23.961'),
(129, 1, 5, '2026-05-18 16:56:23.961'),
(130, 1, 22, '2026-05-18 16:56:23.961'),
(131, 1, 24, '2026-05-18 16:56:23.961'),
(132, 1, 25, '2026-05-18 16:56:23.961'),
(133, 1, 23, '2026-05-18 16:56:23.961'),
(134, 1, 14, '2026-05-18 16:56:23.961'),
(135, 1, 32, '2026-05-18 16:56:23.961'),
(136, 1, 16, '2026-05-18 16:56:23.961'),
(137, 1, 17, '2026-05-18 16:56:23.961'),
(138, 1, 15, '2026-05-18 16:56:23.961'),
(139, 1, 4, '2026-05-18 16:56:23.961'),
(142, 2, 33, '2026-05-18 16:56:25.718'),
(143, 1, 33, '2026-05-18 16:56:25.718'),
(144, 3, 33, '2026-05-18 16:56:25.718'),
(145, 4, 33, '2026-05-18 16:56:25.718'),
(149, 4, 40, '2026-05-18 16:56:27.194'),
(150, 4, 42, '2026-05-18 16:56:27.194'),
(151, 4, 39, '2026-05-18 16:56:27.194'),
(152, 4, 41, '2026-05-18 16:56:27.194'),
(153, 4, 47, '2026-05-18 16:56:27.194'),
(154, 4, 48, '2026-05-18 16:56:27.194'),
(155, 4, 44, '2026-05-18 16:56:27.194'),
(156, 4, 46, '2026-05-18 16:56:27.194'),
(157, 4, 43, '2026-05-18 16:56:27.194'),
(158, 4, 45, '2026-05-18 16:56:27.194'),
(159, 4, 50, '2026-05-18 16:56:27.194'),
(160, 4, 55, '2026-05-18 16:56:27.194'),
(161, 4, 52, '2026-05-18 16:56:27.194'),
(162, 4, 54, '2026-05-18 16:56:27.194'),
(163, 4, 49, '2026-05-18 16:56:27.194'),
(164, 4, 53, '2026-05-18 16:56:27.194'),
(165, 4, 51, '2026-05-18 16:56:27.194'),
(166, 4, 34, '2026-05-18 16:56:27.194'),
(167, 4, 35, '2026-05-18 16:56:27.194'),
(168, 4, 36, '2026-05-18 16:56:27.194'),
(169, 4, 37, '2026-05-18 16:56:27.194'),
(170, 4, 38, '2026-05-18 16:56:27.194')
ON DUPLICATE KEY UPDATE
`role_id` = VALUES(`role_id`),
`resource_id` = VALUES(`resource_id`),
`created_at` = VALUES(`created_at`);

-- Seed data for `staff_account`
INSERT INTO `staff_account` (`id`, `subject_id`, `login_name`, `phone`, `email`, `password_hash`, `must_change_password`, `status`, `source`, `auth_version`, `password_changed_at`, `created_at`, `updated_at`)
VALUES
(2, 2, 'admin', '13900000000', 'agent@uniondesk.local', '{noop}admin123', 0, 'active', 'local', 1, NULL, '2026-05-18 16:56:56.537', '2026-05-18 16:56:56.537')
ON DUPLICATE KEY UPDATE
`subject_id` = VALUES(`subject_id`),
`login_name` = VALUES(`login_name`),
`phone` = VALUES(`phone`),
`email` = VALUES(`email`),
`password_hash` = VALUES(`password_hash`),
`must_change_password` = VALUES(`must_change_password`),
`status` = VALUES(`status`),
`source` = VALUES(`source`),
`auth_version` = VALUES(`auth_version`),
`password_changed_at` = VALUES(`password_changed_at`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `staff_account_platform_role`
INSERT INTO `staff_account_platform_role` (`staff_account_id`, `platform_role_id`, `created_at`)
VALUES
(2, 1, '2026-05-18 16:57:01.404')
ON DUPLICATE KEY UPDATE
`created_at` = VALUES(`created_at`);

-- Seed data for `ticket_priority_level`
INSERT INTO `ticket_priority_level` (`id`, `business_domain_id`, `code`, `name`, `sort_order`, `is_default`, `status`, `created_at`, `updated_at`)
VALUES
(1, 1, 'low', '低', 10, 0, 'active', '2026-05-18 16:56:44.672', '2026-05-18 16:56:44.672'),
(2, 1, 'normal', '普通', 20, 1, 'active', '2026-05-18 16:56:44.719', '2026-05-18 16:56:44.719'),
(3, 1, 'high', '高', 30, 0, 'active', '2026-05-18 16:56:44.767', '2026-05-18 16:56:44.767')
ON DUPLICATE KEY UPDATE
`business_domain_id` = VALUES(`business_domain_id`),
`code` = VALUES(`code`),
`name` = VALUES(`name`),
`sort_order` = VALUES(`sort_order`),
`is_default` = VALUES(`is_default`),
`status` = VALUES(`status`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `ticket_type`
INSERT INTO `ticket_type` (`id`, `business_domain_id`, `code`, `name`, `sla_first_response_minutes`, `sla_resolve_minutes`, `created_at`, `updated_at`, `status_flow_config`)
VALUES
(1, 1, 'general', 'General Ticket', 60, 1440, '2026-05-18 16:56:16.969', '2026-05-18 16:56:16.969', NULL)
ON DUPLICATE KEY UPDATE
`business_domain_id` = VALUES(`business_domain_id`),
`code` = VALUES(`code`),
`name` = VALUES(`name`),
`sla_first_response_minutes` = VALUES(`sla_first_response_minutes`),
`sla_resolve_minutes` = VALUES(`sla_resolve_minutes`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`),
`status_flow_config` = VALUES(`status_flow_config`);

-- Seed data for `user_account`
INSERT INTO `user_account` (`id`, `username`, `mobile`, `email`, `remark`, `password_hash`, `status`, `account_type`, `auth_version`, `employment_status`, `offboarded_at`, `offboarded_by`, `offboard_reason`, `created_at`, `updated_at`)
VALUES
(1, 'customer', '13800000000', 'admin@uniondesk.local', NULL, '{noop}customer123', 1, 'customer', 1, 'active', NULL, NULL, NULL, '2026-05-18 16:56:16.923', '2026-05-18 16:56:21.431'),
(2, 'admin', '13900000000', 'agent@uniondesk.local', NULL, '{noop}admin123', 1, 'admin', 1, 'active', NULL, NULL, NULL, '2026-05-18 16:56:18.309', '2026-05-18 16:56:21.431')
ON DUPLICATE KEY UPDATE
`username` = VALUES(`username`),
`mobile` = VALUES(`mobile`),
`email` = VALUES(`email`),
`remark` = VALUES(`remark`),
`password_hash` = VALUES(`password_hash`),
`status` = VALUES(`status`),
`account_type` = VALUES(`account_type`),
`auth_version` = VALUES(`auth_version`),
`employment_status` = VALUES(`employment_status`),
`offboarded_at` = VALUES(`offboarded_at`),
`offboarded_by` = VALUES(`offboarded_by`),
`offboard_reason` = VALUES(`offboard_reason`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `consultation_session`
INSERT INTO `consultation_session` (`id`, `session_no`, `business_domain_id`, `customer_id`, `session_status`, `assigned_to`, `last_message_at`, `closed_at`, `created_at`, `updated_at`, `customer_account_id`, `agent_staff_account_id`)
VALUES
(1, 'CS202604190001', 1, 1, 'open', 1, '2026-05-18 16:56:17.155', NULL, '2026-05-18 16:56:17.155', '2026-05-18 16:56:17.155', NULL, NULL),
(2, 'CS202604190002', 1, 1, 'closed', 1, '2026-05-18 16:56:17.201', '2026-05-18 16:56:17.201', '2026-05-18 16:56:17.201', '2026-05-18 16:56:17.201', NULL, NULL),
(3, 'C202604200001', 1, 1, 'open', 2, '2026-05-18 16:56:18.464', NULL, '2026-05-18 16:56:18.464', '2026-05-18 16:56:18.464', NULL, NULL)
ON DUPLICATE KEY UPDATE
`session_no` = VALUES(`session_no`),
`business_domain_id` = VALUES(`business_domain_id`),
`customer_id` = VALUES(`customer_id`),
`session_status` = VALUES(`session_status`),
`assigned_to` = VALUES(`assigned_to`),
`last_message_at` = VALUES(`last_message_at`),
`closed_at` = VALUES(`closed_at`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`),
`customer_account_id` = VALUES(`customer_account_id`),
`agent_staff_account_id` = VALUES(`agent_staff_account_id`);

-- Seed data for `consultation_message`
INSERT INTO `consultation_message` (`id`, `consultation_session_id`, `business_domain_id`, `seq_no`, `sender_user_id`, `sender_role`, `message_type`, `content`, `payload`, `created_at`)
VALUES
(1, 1, 1, 1, 1, 'customer', 'text', 'The ticket list seems inconsistent.', NULL, '2026-05-18 16:56:17.247'),
(2, 1, 1, 2, 1, 'agent', 'text', 'I will check whether another app changed the workflow.', NULL, '2026-05-18 16:56:17.294'),
(3, 2, 1, 1, 1, 'customer', 'text', 'Please convert this chat into a ticket.', NULL, '2026-05-18 16:56:17.341'),
(4, 3, 1, 1, 1, 'customer', 'text', '我需要一个可演示的初步工单系统。', NULL, '2026-05-18 16:56:18.510'),
(5, 3, 1, 2, 2, 'agent', 'text', '已收到，我们先演示提单、分派和关闭流程。', NULL, '2026-05-18 16:56:18.557')
ON DUPLICATE KEY UPDATE
`consultation_session_id` = VALUES(`consultation_session_id`),
`business_domain_id` = VALUES(`business_domain_id`),
`seq_no` = VALUES(`seq_no`),
`sender_user_id` = VALUES(`sender_user_id`),
`sender_role` = VALUES(`sender_role`),
`message_type` = VALUES(`message_type`),
`content` = VALUES(`content`),
`payload` = VALUES(`payload`),
`created_at` = VALUES(`created_at`);

-- Seed data for `iam_role_binding`
INSERT INTO `iam_role_binding` (`id`, `user_id`, `role_id`, `binding_scope`, `business_domain_id`, `granted_by`, `effective_from`, `effective_to`, `status`, `created_at`, `updated_at`)
VALUES
(1, 2, 4, 'global', NULL, NULL, NULL, NULL, 1, '2026-05-18 16:56:29.348', '2026-05-18 16:56:29.348'),
(2, 1, 1, 'domain', 1, NULL, NULL, NULL, 1, '2026-05-18 16:56:29.395', '2026-05-18 16:56:29.395'),
(3, 2, 2, 'domain', 1, NULL, NULL, NULL, 1, '2026-05-18 16:56:29.395', '2026-05-18 16:56:29.395'),
(4, 2, 3, 'domain', 1, NULL, NULL, NULL, 1, '2026-05-18 16:56:29.395', '2026-05-18 16:56:29.395')
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`role_id` = VALUES(`role_id`),
`binding_scope` = VALUES(`binding_scope`),
`business_domain_id` = VALUES(`business_domain_id`),
`granted_by` = VALUES(`granted_by`),
`effective_from` = VALUES(`effective_from`),
`effective_to` = VALUES(`effective_to`),
`status` = VALUES(`status`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `platform_organization`
INSERT INTO `platform_organization` (`id`, `code`, `name`, `parent_id`, `leader_user_id`, `order_no`, `status`, `remark`, `created_at`, `updated_at`)
VALUES
(1, 'platform-root', '平台组织', NULL, 1, 10, 1, '平台组织根节点', '2026-05-18 16:56:30.779', '2026-05-18 16:56:30.779'),
(2, 'platform-ops', '平台运营部', 1, 1, 20, 1, '负责平台账号与角色治理', '2026-05-18 16:56:30.825', '2026-05-18 16:56:30.825'),
(3, 'security-audit', '安全审计组', 1, 1, 30, 1, '负责平台审计日志与安全策略核查', '2026-05-18 16:56:30.873', '2026-05-18 16:56:30.873')
ON DUPLICATE KEY UPDATE
`code` = VALUES(`code`),
`name` = VALUES(`name`),
`parent_id` = VALUES(`parent_id`),
`leader_user_id` = VALUES(`leader_user_id`),
`order_no` = VALUES(`order_no`),
`status` = VALUES(`status`),
`remark` = VALUES(`remark`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`);

-- Seed data for `ticket`
INSERT INTO `ticket` (`id`, `ticket_no`, `business_domain_id`, `customer_id`, `ticket_type_id`, `title`, `description`, `status`, `priority`, `source`, `assigned_to`, `custom_fields`, `created_at`, `updated_at`, `version`, `result`, `sla_first_response_deadline`, `sla_resolution_deadline`, `sla_first_responded_at`, `sla_resolved_at`, `sla_status`, `sla_paused_duration`, `sla_pause_started_at`, `assignee_staff_account_id`, `customer_account_id`)
VALUES
(1, 'T202604190001', 1, 1, 1, 'Login page cannot submit tickets', 'Demo ticket used to show the open queue.', 'open', 'normal', 'web', NULL, NULL, '2026-05-18 16:56:17.015', '2026-05-18 16:56:17.015', 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL),
(2, 'T202604190002', 1, 1, 1, 'Billing data loads slowly', 'Demo ticket used to show the processing queue.', 'processing', 'high', 'web', NULL, NULL, '2026-05-18 16:56:17.063', '2026-05-18 16:56:17.063', 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL),
(3, 'T202604190003', 1, 1, 1, 'Resolved ticket sample', 'Demo ticket used to show the resolved queue.', 'resolved', 'normal', 'web', NULL, NULL, '2026-05-18 16:56:17.111', '2026-05-18 16:56:17.111', 1, NULL, NULL, NULL, NULL, NULL, NULL, 0, NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
`ticket_no` = VALUES(`ticket_no`),
`business_domain_id` = VALUES(`business_domain_id`),
`customer_id` = VALUES(`customer_id`),
`ticket_type_id` = VALUES(`ticket_type_id`),
`title` = VALUES(`title`),
`description` = VALUES(`description`),
`status` = VALUES(`status`),
`priority` = VALUES(`priority`),
`source` = VALUES(`source`),
`assigned_to` = VALUES(`assigned_to`),
`custom_fields` = VALUES(`custom_fields`),
`created_at` = VALUES(`created_at`),
`updated_at` = VALUES(`updated_at`),
`version` = VALUES(`version`),
`result` = VALUES(`result`),
`sla_first_response_deadline` = VALUES(`sla_first_response_deadline`),
`sla_resolution_deadline` = VALUES(`sla_resolution_deadline`),
`sla_first_responded_at` = VALUES(`sla_first_responded_at`),
`sla_resolved_at` = VALUES(`sla_resolved_at`),
`sla_status` = VALUES(`sla_status`),
`sla_paused_duration` = VALUES(`sla_paused_duration`),
`sla_pause_started_at` = VALUES(`sla_pause_started_at`),
`assignee_staff_account_id` = VALUES(`assignee_staff_account_id`),
`customer_account_id` = VALUES(`customer_account_id`);

-- Seed data for `consultation_ticket_link`
INSERT INTO `consultation_ticket_link` (`id`, `consultation_session_id`, `ticket_id`, `business_domain_id`, `converted_by`, `converted_at`)
VALUES
(1, 2, 3, 1, 1, '2026-05-18 16:56:17.436')
ON DUPLICATE KEY UPDATE
`consultation_session_id` = VALUES(`consultation_session_id`),
`ticket_id` = VALUES(`ticket_id`),
`business_domain_id` = VALUES(`business_domain_id`),
`converted_by` = VALUES(`converted_by`),
`converted_at` = VALUES(`converted_at`);

-- Seed data for `ticket_reply`
INSERT INTO `ticket_reply` (`id`, `ticket_id`, `business_domain_id`, `sender_user_id`, `sender_role`, `reply_type`, `content`, `attachment_urls`, `created_at`, `sender_type`, `staff_account_id`, `customer_account_id`)
VALUES
(1, 2, 1, 1, 'agent', 'text', 'The ticket has been accepted by support.', NULL, '2026-05-18 16:56:17.389', NULL, NULL, NULL)
ON DUPLICATE KEY UPDATE
`ticket_id` = VALUES(`ticket_id`),
`business_domain_id` = VALUES(`business_domain_id`),
`sender_user_id` = VALUES(`sender_user_id`),
`sender_role` = VALUES(`sender_role`),
`reply_type` = VALUES(`reply_type`),
`content` = VALUES(`content`),
`attachment_urls` = VALUES(`attachment_urls`),
`created_at` = VALUES(`created_at`),
`sender_type` = VALUES(`sender_type`),
`staff_account_id` = VALUES(`staff_account_id`),
`customer_account_id` = VALUES(`customer_account_id`);

-- Seed data for `user_domain_role`
INSERT INTO `user_domain_role` (`id`, `user_id`, `role_id`, `business_domain_id`, `created_at`)
VALUES
(4, 1, 1, 1, '2026-05-18 16:56:20.423')
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`role_id` = VALUES(`role_id`),
`business_domain_id` = VALUES(`business_domain_id`),
`created_at` = VALUES(`created_at`);

-- Seed data for `user_global_role`
INSERT INTO `user_global_role` (`id`, `user_id`, `role_id`, `created_at`)
VALUES
(1, 2, 4, '2026-05-18 16:56:18.370')
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`role_id` = VALUES(`role_id`),
`created_at` = VALUES(`created_at`);

-- Seed data for `user_organization`
INSERT INTO `user_organization` (`id`, `user_id`, `organization_id`, `created_at`)
VALUES
(1, 2, 2, '2026-05-18 16:57:07.692'),
(2, 1, 1, '2026-05-18 16:57:07.739')
ON DUPLICATE KEY UPDATE
`user_id` = VALUES(`user_id`),
`organization_id` = VALUES(`organization_id`),
`created_at` = VALUES(`created_at`);

