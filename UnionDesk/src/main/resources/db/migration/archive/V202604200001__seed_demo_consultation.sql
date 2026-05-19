-- 演示员工账号：必须在 user_global_role 等外键写入前存在（空库仅有 V2 的 id=1 客户账号）
INSERT INTO user_account (id, mobile, email, password_hash, status)
VALUES (2, '13900000000', 'agent@uniondesk.local', '{noop}agent123', 1)
ON DUPLICATE KEY UPDATE
    mobile = VALUES(mobile),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    status = VALUES(status);

INSERT INTO user_global_role (user_id, role_id)
SELECT 2, id FROM role WHERE code = 'super_admin'
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), role_id = VALUES(role_id);

INSERT INTO user_domain_role (user_id, role_id, business_domain_id)
SELECT 2, r.id, d.id
FROM role r
JOIN business_domain d ON d.code = 'default'
WHERE r.code IN ('agent', 'domain_admin')
ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), role_id = VALUES(role_id), business_domain_id = VALUES(business_domain_id);

INSERT INTO consultation_session (session_no, business_domain_id, customer_id, session_status, assigned_to, last_message_at)
SELECT 'C202604200001', d.id, 1, 'open', 2, CURRENT_TIMESTAMP(3)
FROM business_domain d
WHERE d.code = 'default'
ON DUPLICATE KEY UPDATE session_status = VALUES(session_status), assigned_to = VALUES(assigned_to), last_message_at = VALUES(last_message_at);

INSERT INTO consultation_message (consultation_session_id, business_domain_id, seq_no, sender_user_id, sender_role, content)
SELECT s.id, s.business_domain_id, 1, 1, 'customer', '我需要一个可演示的初步工单系统。'
FROM consultation_session s
WHERE s.session_no = 'C202604200001'
ON DUPLICATE KEY UPDATE content = VALUES(content);

INSERT INTO consultation_message (consultation_session_id, business_domain_id, seq_no, sender_user_id, sender_role, content)
SELECT s.id, s.business_domain_id, 2, 2, 'agent', '已收到，我们先演示提单、分派和关闭流程。'
FROM consultation_session s
WHERE s.session_no = 'C202604200001'
ON DUPLICATE KEY UPDATE content = VALUES(content);
