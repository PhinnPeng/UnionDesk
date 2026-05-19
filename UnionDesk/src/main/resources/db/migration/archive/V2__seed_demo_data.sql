INSERT INTO business_domain (code, name, visibility_policy, status)
VALUES ('default', 'Default Domain', 'global', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    visibility_policy = VALUES(visibility_policy),
    status = VALUES(status);

INSERT INTO user_account (id, mobile, email, password_hash, status)
VALUES (1, '13800000000', 'admin@uniondesk.local', '{noop}admin123', 1)
ON DUPLICATE KEY UPDATE
    mobile = VALUES(mobile),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    status = VALUES(status);

INSERT INTO ticket_type (business_domain_id, code, name, sla_first_response_minutes, sla_resolve_minutes)
SELECT id, 'general', 'General Ticket', 60, 1440
FROM business_domain
WHERE code = 'default'
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sla_first_response_minutes = VALUES(sla_first_response_minutes),
    sla_resolve_minutes = VALUES(sla_resolve_minutes);

INSERT INTO ticket (ticket_no, business_domain_id, customer_id, ticket_type_id, title, description, status, priority, source)
SELECT 'T202604190001', d.id, 1, tt.id, 'Login page cannot submit tickets', 'Demo ticket used to show the open queue.', 'open', 'normal', 'web'
FROM business_domain d
JOIN ticket_type tt ON tt.business_domain_id = d.id AND tt.code = 'general'
WHERE d.code = 'default'
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    status = VALUES(status),
    priority = VALUES(priority),
    source = VALUES(source);

INSERT INTO ticket (ticket_no, business_domain_id, customer_id, ticket_type_id, title, description, status, priority, source)
SELECT 'T202604190002', d.id, 1, tt.id, 'Billing data loads slowly', 'Demo ticket used to show the processing queue.', 'processing', 'high', 'web'
FROM business_domain d
JOIN ticket_type tt ON tt.business_domain_id = d.id AND tt.code = 'general'
WHERE d.code = 'default'
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    status = VALUES(status),
    priority = VALUES(priority),
    source = VALUES(source);

INSERT INTO ticket (ticket_no, business_domain_id, customer_id, ticket_type_id, title, description, status, priority, source)
SELECT 'T202604190003', d.id, 1, tt.id, 'Resolved ticket sample', 'Demo ticket used to show the resolved queue.', 'resolved', 'normal', 'web'
FROM business_domain d
JOIN ticket_type tt ON tt.business_domain_id = d.id AND tt.code = 'general'
WHERE d.code = 'default'
ON DUPLICATE KEY UPDATE
    title = VALUES(title),
    description = VALUES(description),
    status = VALUES(status),
    priority = VALUES(priority),
    source = VALUES(source);

INSERT INTO consultation_session (session_no, business_domain_id, customer_id, session_status, assigned_to, last_message_at)
SELECT 'CS202604190001', d.id, 1, 'open', 1, CURRENT_TIMESTAMP(3)
FROM business_domain d
WHERE d.code = 'default'
ON DUPLICATE KEY UPDATE
    session_status = VALUES(session_status),
    assigned_to = VALUES(assigned_to),
    last_message_at = VALUES(last_message_at);

INSERT INTO consultation_session (session_no, business_domain_id, customer_id, session_status, assigned_to, last_message_at, closed_at)
SELECT 'CS202604190002', d.id, 1, 'closed', 1, CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
FROM business_domain d
WHERE d.code = 'default'
ON DUPLICATE KEY UPDATE
    session_status = VALUES(session_status),
    assigned_to = VALUES(assigned_to),
    last_message_at = VALUES(last_message_at),
    closed_at = VALUES(closed_at);

INSERT INTO consultation_message (consultation_session_id, business_domain_id, seq_no, sender_user_id, sender_role, message_type, content, payload)
SELECT cs.id, d.id, 1, 1, 'customer', 'text', 'The ticket list seems inconsistent.', NULL
FROM consultation_session cs
JOIN business_domain d ON d.id = cs.business_domain_id
WHERE cs.session_no = 'CS202604190001'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    message_type = VALUES(message_type),
    sender_user_id = VALUES(sender_user_id),
    sender_role = VALUES(sender_role);

INSERT INTO consultation_message (consultation_session_id, business_domain_id, seq_no, sender_user_id, sender_role, message_type, content, payload)
SELECT cs.id, d.id, 2, 1, 'agent', 'text', 'I will check whether another app changed the workflow.', NULL
FROM consultation_session cs
JOIN business_domain d ON d.id = cs.business_domain_id
WHERE cs.session_no = 'CS202604190001'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    message_type = VALUES(message_type),
    sender_user_id = VALUES(sender_user_id),
    sender_role = VALUES(sender_role);

INSERT INTO consultation_message (consultation_session_id, business_domain_id, seq_no, sender_user_id, sender_role, message_type, content, payload)
SELECT cs.id, d.id, 1, 1, 'customer', 'text', 'Please convert this chat into a ticket.', NULL
FROM consultation_session cs
JOIN business_domain d ON d.id = cs.business_domain_id
WHERE cs.session_no = 'CS202604190002'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    message_type = VALUES(message_type),
    sender_user_id = VALUES(sender_user_id),
    sender_role = VALUES(sender_role);

INSERT INTO ticket_reply (ticket_id, business_domain_id, sender_user_id, sender_role, reply_type, content, attachment_urls)
SELECT t.id, t.business_domain_id, 1, 'agent', 'text', 'The ticket has been accepted by support.', NULL
FROM ticket t
WHERE t.ticket_no = 'T202604190002'
ON DUPLICATE KEY UPDATE
    content = VALUES(content),
    reply_type = VALUES(reply_type),
    sender_user_id = VALUES(sender_user_id),
    sender_role = VALUES(sender_role);

INSERT INTO consultation_ticket_link (consultation_session_id, ticket_id, business_domain_id, converted_by)
SELECT cs.id, t.id, cs.business_domain_id, 1
FROM consultation_session cs
JOIN ticket t ON t.ticket_no = 'T202604190003'
WHERE cs.session_no = 'CS202604190002'
ON DUPLICATE KEY UPDATE
    converted_by = VALUES(converted_by);
