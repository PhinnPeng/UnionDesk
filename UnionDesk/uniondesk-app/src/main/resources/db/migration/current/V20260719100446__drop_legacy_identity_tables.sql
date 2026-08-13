-- W4: 拆除遗留身份表（仅 DROP 当前库仍存在的、指向/挂在旧表上的 FK）
-- 说明：联调库 schema 与 rebaseline 不完全一致（如 auth_login_log 已不存在、business_domain FK 已卸），
-- 禁止对不存在的表/约束执行 ALTER，否则 Flyway 失败。

-- 1) 卸掉仍指向 user_account 的外键
ALTER TABLE consultation_message DROP FOREIGN KEY fk_consultation_message_sender;
ALTER TABLE consultation_session DROP FOREIGN KEY fk_consultation_session_assigned_to;
ALTER TABLE consultation_session DROP FOREIGN KEY fk_consultation_session_customer;
ALTER TABLE consultation_ticket_link DROP FOREIGN KEY fk_consultation_ticket_link_converted_by;
ALTER TABLE customer_business_domain_access DROP FOREIGN KEY fk_cda_customer;
ALTER TABLE feedback DROP FOREIGN KEY fk_feedback_customer;
ALTER TABLE iam_role_binding DROP FOREIGN KEY fk_iam_role_binding_granted_by;
ALTER TABLE iam_role_binding DROP FOREIGN KEY fk_iam_role_binding_user;
ALTER TABLE operation_log DROP FOREIGN KEY fk_operation_log_operator;
ALTER TABLE platform_organization DROP FOREIGN KEY fk_platform_organization_leader;
ALTER TABLE ticket DROP FOREIGN KEY fk_ticket_assigned_to;
ALTER TABLE ticket DROP FOREIGN KEY fk_ticket_customer;
ALTER TABLE ticket_event_log DROP FOREIGN KEY fk_ticket_event_operator;
ALTER TABLE ticket_reply DROP FOREIGN KEY fk_ticket_reply_sender;
-- 补：fresh-install 时以下 FK 仍指向 user_account，须一并卸除（联调库为手工卸除，迁移内缺失）
--   auth_login_log.fk_auth_login_log_user（rebaseline 创建）
--   business_domain.fk_business_domain_created_by / fk_business_domain_updated_by（V202605200003 创建）
ALTER TABLE auth_login_log DROP FOREIGN KEY fk_auth_login_log_user;
ALTER TABLE business_domain DROP FOREIGN KEY fk_business_domain_created_by;
ALTER TABLE business_domain DROP FOREIGN KEY fk_business_domain_updated_by;

-- 2) 卸掉遗留表自身外键，再 DROP
ALTER TABLE user_domain_role DROP FOREIGN KEY fk_udr_user;
ALTER TABLE user_domain_role DROP FOREIGN KEY fk_udr_role;
ALTER TABLE user_domain_role DROP FOREIGN KEY fk_udr_domain;
ALTER TABLE user_global_role DROP FOREIGN KEY fk_ugr_user;
ALTER TABLE user_global_role DROP FOREIGN KEY fk_ugr_role;
ALTER TABLE user_organization DROP FOREIGN KEY fk_user_org_user;
ALTER TABLE user_organization DROP FOREIGN KEY fk_user_org_org;

DROP TABLE IF EXISTS user_organization;
DROP TABLE IF EXISTS user_domain_role;
DROP TABLE IF EXISTS user_global_role;
DROP TABLE IF EXISTS user_account;

-- 3) Seed：平台管理员（identity_subject + staff_account + platform_admin）
INSERT INTO identity_subject (id, subject_type, phone, status, created_at, updated_at)
VALUES (2, 'person', '13900000000', 'active', CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3))
ON DUPLICATE KEY UPDATE
    phone = VALUES(phone),
    status = 'active',
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO staff_account (
    id, subject_id, username, real_name, nickname, phone, email, password_hash,
    must_change_password, status, employment_status, source, auth_version,
    password_changed_at, created_at, updated_at
)
VALUES (
    2, 2, 'admin', '平台管理员', 'admin', '13900000000', 'agent@uniondesk.local',
    '{noop}admin123', 0, 'active', 'active', 'local', 1,
    CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
)
ON DUPLICATE KEY UPDATE
    subject_id = VALUES(subject_id),
    username = VALUES(username),
    phone = VALUES(phone),
    status = 'active',
    employment_status = 'active',
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO staff_account_platform_role (staff_account_id, platform_role_id, created_at)
SELECT sa.id, pr.id, CURRENT_TIMESTAMP(3)
FROM staff_account sa
CROSS JOIN platform_role pr
WHERE sa.username = 'admin'
  AND pr.code = 'platform_admin'
ON DUPLICATE KEY UPDATE created_at = VALUES(created_at);
