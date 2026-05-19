-- UnionDesk backend init schema (MVP)

CREATE TABLE IF NOT EXISTS business_domain (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    visibility_policy VARCHAR(32) NOT NULL DEFAULT 'global',
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_business_domain_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    mobile VARCHAR(20) NOT NULL,
    email VARCHAR(128) DEFAULT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_mobile (mobile),
    UNIQUE KEY uk_user_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS role (
    id INT UNSIGNED NOT NULL AUTO_INCREMENT,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(64) NOT NULL,
    scope VARCHAR(16) NOT NULL,
    is_system TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_global_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    role_id INT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_global_role (user_id, role_id),
    CONSTRAINT fk_ugr_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_ugr_role FOREIGN KEY (role_id) REFERENCES role(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_domain_role (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    role_id INT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_domain_role (user_id, role_id, business_domain_id),
    KEY idx_udr_domain_role (business_domain_id, role_id),
    CONSTRAINT fk_udr_user FOREIGN KEY (user_id) REFERENCES user_account(id),
    CONSTRAINT fk_udr_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_udr_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS customer_business_domain_access (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    customer_id BIGINT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    access_status VARCHAR(16) NOT NULL DEFAULT 'pending',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_customer_domain_access (customer_id, business_domain_id),
    KEY idx_cda_domain_status (business_domain_id, access_status),
    CONSTRAINT fk_cda_customer FOREIGN KEY (customer_id) REFERENCES user_account(id),
    CONSTRAINT fk_cda_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consultation_session (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    session_no VARCHAR(32) NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    customer_id BIGINT UNSIGNED NOT NULL,
    session_status VARCHAR(32) NOT NULL DEFAULT 'open',
    assigned_to BIGINT UNSIGNED DEFAULT NULL,
    last_message_at DATETIME(3) DEFAULT NULL,
    closed_at DATETIME(3) DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_consultation_session_no (session_no),
    KEY idx_consultation_domain_status_updated (business_domain_id, session_status, updated_at),
    KEY idx_consultation_customer_updated (customer_id, updated_at),
    CONSTRAINT fk_consultation_session_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_consultation_session_customer FOREIGN KEY (customer_id) REFERENCES user_account(id),
    CONSTRAINT fk_consultation_session_assigned_to FOREIGN KEY (assigned_to) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consultation_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    consultation_session_id BIGINT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    seq_no INT UNSIGNED NOT NULL,
    sender_user_id BIGINT UNSIGNED DEFAULT NULL,
    sender_role VARCHAR(16) NOT NULL,
    message_type VARCHAR(16) NOT NULL DEFAULT 'text',
    content TEXT,
    payload JSON DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_consultation_message_seq (consultation_session_id, seq_no),
    KEY idx_consultation_message_session_created (consultation_session_id, created_at),
    CONSTRAINT fk_consultation_message_session FOREIGN KEY (consultation_session_id) REFERENCES consultation_session(id),
    CONSTRAINT fk_consultation_message_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_consultation_message_sender FOREIGN KEY (sender_user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ticket_type (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    sla_first_response_minutes INT UNSIGNED DEFAULT NULL,
    sla_resolve_minutes INT UNSIGNED DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_type_domain_code (business_domain_id, code),
    CONSTRAINT fk_ticket_type_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ticket (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_no VARCHAR(32) NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    customer_id BIGINT UNSIGNED NOT NULL,
    ticket_type_id BIGINT UNSIGNED NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL,
    priority VARCHAR(16) NOT NULL DEFAULT 'normal',
    source VARCHAR(16) NOT NULL DEFAULT 'web',
    assigned_to BIGINT UNSIGNED DEFAULT NULL,
    custom_fields JSON DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_ticket_domain_status_priority (business_domain_id, status, priority, created_at),
    KEY idx_ticket_customer_created (customer_id, created_at),
    KEY idx_ticket_assigned_status (assigned_to, status, updated_at),
    CONSTRAINT fk_ticket_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_ticket_customer FOREIGN KEY (customer_id) REFERENCES user_account(id),
    CONSTRAINT fk_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_type(id),
    CONSTRAINT fk_ticket_assigned_to FOREIGN KEY (assigned_to) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS consultation_ticket_link (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    consultation_session_id BIGINT UNSIGNED NOT NULL,
    ticket_id BIGINT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    converted_by BIGINT UNSIGNED DEFAULT NULL,
    converted_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_consultation_ticket_link_session (consultation_session_id),
    UNIQUE KEY uk_consultation_ticket_link_ticket (ticket_id),
    CONSTRAINT fk_consultation_ticket_link_session FOREIGN KEY (consultation_session_id) REFERENCES consultation_session(id),
    CONSTRAINT fk_consultation_ticket_link_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
    CONSTRAINT fk_consultation_ticket_link_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_consultation_ticket_link_converted_by FOREIGN KEY (converted_by) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ticket_reply (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    sender_user_id BIGINT UNSIGNED DEFAULT NULL,
    sender_role VARCHAR(16) NOT NULL,
    reply_type VARCHAR(16) NOT NULL,
    content TEXT,
    attachment_urls JSON DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ticket_reply_ticket_created (ticket_id, created_at),
    CONSTRAINT fk_ticket_reply_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
    CONSTRAINT fk_ticket_reply_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_ticket_reply_sender FOREIGN KEY (sender_user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS ticket_event_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    ticket_id BIGINT UNSIGNED NOT NULL,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    operator_user_id BIGINT UNSIGNED DEFAULT NULL,
    payload JSON DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_ticket_event_ticket_created (ticket_id, created_at),
    CONSTRAINT fk_ticket_event_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id),
    CONSTRAINT fk_ticket_event_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_ticket_event_operator FOREIGN KEY (operator_user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS feedback (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED NOT NULL,
    customer_id BIGINT UNSIGNED NOT NULL,
    feedback_type VARCHAR(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    internal_notes TEXT,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_feedback_domain_status_created (business_domain_id, status, created_at),
    CONSTRAINT fk_feedback_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_feedback_customer FOREIGN KEY (customer_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS operation_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id BIGINT UNSIGNED DEFAULT NULL,
    operator_user_id BIGINT UNSIGNED DEFAULT NULL,
    module VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    request_id VARCHAR(64) DEFAULT NULL,
    before_data JSON DEFAULT NULL,
    after_data JSON DEFAULT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_operation_domain_module_created (business_domain_id, module, created_at),
    KEY idx_operation_operator_created (operator_user_id, created_at),
    KEY idx_operation_request_id (request_id),
    CONSTRAINT fk_operation_log_domain FOREIGN KEY (business_domain_id) REFERENCES business_domain(id),
    CONSTRAINT fk_operation_log_operator FOREIGN KEY (operator_user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO role (code, name, scope, is_system)
VALUES
    ('customer', 'Customer', 'domain', 1),
    ('agent', 'Agent', 'domain', 1),
    ('domain_admin', 'Domain Admin', 'domain', 1),
    ('super_admin', 'Super Admin', 'global', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    scope = VALUES(scope),
    is_system = VALUES(is_system);
