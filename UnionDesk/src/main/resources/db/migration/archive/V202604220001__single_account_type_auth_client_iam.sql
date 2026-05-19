ALTER TABLE user_account
    ADD COLUMN account_type VARCHAR(16) NULL AFTER status;

CREATE TEMPORARY TABLE tmp_user_role_stats AS
SELECT
    u.id AS user_id,
    MAX(CASE WHEN ur.role_code = 'customer' THEN 1 ELSE 0 END) AS has_customer_role,
    MAX(CASE WHEN ur.role_code <> 'customer' THEN 1 ELSE 0 END) AS has_non_customer_role
FROM user_account u
LEFT JOIN (
    SELECT ugr.user_id, r.code AS role_code
    FROM user_global_role ugr
    JOIN role r ON r.id = ugr.role_id
    UNION ALL
    SELECT udr.user_id, r.code AS role_code
    FROM user_domain_role udr
    JOIN role r ON r.id = udr.role_id
) ur ON ur.user_id = u.id
GROUP BY u.id;

SET @account_type_conflict_ids = (
    SELECT GROUP_CONCAT(user_id ORDER BY user_id SEPARATOR ',')
    FROM tmp_user_role_stats
    WHERE has_customer_role = 1 AND has_non_customer_role = 1
);

SET @account_type_conflict_message = CONCAT(
    'account_type backfill conflict user ids: ',
    COALESCE(@account_type_conflict_ids, '')
);

SET @account_type_conflict_sql = IF(
    @account_type_conflict_ids IS NULL,
    'SELECT 1',
    CONCAT(
        'SIGNAL SQLSTATE ''45000'' SET MESSAGE_TEXT = ''',
        REPLACE(@account_type_conflict_message, '''', ''''''),
        ''''
    )
);

PREPARE stmt_account_type_conflict FROM @account_type_conflict_sql;
EXECUTE stmt_account_type_conflict;
DEALLOCATE PREPARE stmt_account_type_conflict;

UPDATE user_account ua
JOIN tmp_user_role_stats stats ON stats.user_id = ua.id
SET ua.account_type = CASE
    WHEN stats.has_non_customer_role = 1 THEN 'admin'
    ELSE 'customer'
END
WHERE ua.account_type IS NULL;

DROP TEMPORARY TABLE tmp_user_role_stats;

ALTER TABLE user_account
    MODIFY account_type VARCHAR(16) NOT NULL,
    ADD CONSTRAINT chk_user_account_type CHECK (account_type IN ('admin', 'customer'));

CREATE TABLE IF NOT EXISTS auth_client (
    client_code VARCHAR(64) NOT NULL,
    client_name VARCHAR(128) NOT NULL,
    allowed_account_type VARCHAR(16) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (client_code),
    KEY idx_auth_client_status (status),
    CONSTRAINT chk_auth_client_account_type CHECK (allowed_account_type IN ('admin', 'customer'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO auth_client (client_code, client_name, allowed_account_type, status)
VALUES
    ('ud-admin-web', 'UnionDesk Admin Web', 'admin', 1),
    ('ud-customer-web', 'UnionDesk Customer Web', 'customer', 1)
ON DUPLICATE KEY UPDATE
    client_name = VALUES(client_name),
    allowed_account_type = VALUES(allowed_account_type),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

ALTER TABLE auth_login_session
    ADD COLUMN client_code VARCHAR(64) NULL AFTER user_id;

UPDATE auth_login_session s
LEFT JOIN user_account u ON u.id = s.user_id
SET s.client_code = CASE
    WHEN u.account_type = 'admin' THEN 'ud-admin-web'
    ELSE 'ud-customer-web'
END
WHERE s.client_code IS NULL;

ALTER TABLE auth_login_session
    MODIFY client_code VARCHAR(64) NOT NULL,
    ADD KEY idx_auth_login_session_client_status (client_code, session_status, expires_at),
    ADD CONSTRAINT fk_auth_login_session_client FOREIGN KEY (client_code) REFERENCES auth_client(client_code);

DROP TRIGGER IF EXISTS trg_user_global_role_account_type_insert;
DROP TRIGGER IF EXISTS trg_user_global_role_account_type_update;
DROP TRIGGER IF EXISTS trg_user_domain_role_account_type_insert;
DROP TRIGGER IF EXISTS trg_user_domain_role_account_type_update;
DROP TRIGGER IF EXISTS trg_user_account_account_type_update;

DELIMITER $$
CREATE TRIGGER trg_user_global_role_account_type_insert
BEFORE INSERT ON user_global_role
FOR EACH ROW
BEGIN
    DECLARE v_account_type VARCHAR(16);
    DECLARE v_role_code VARCHAR(32);

    SELECT account_type INTO v_account_type
    FROM user_account
    WHERE id = NEW.user_id;

    SELECT code INTO v_role_code
    FROM role
    WHERE id = NEW.role_id;

    IF v_account_type = 'customer' AND v_role_code <> 'customer' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'customer account can only be granted customer role';
    END IF;

    IF v_account_type = 'admin' AND v_role_code = 'customer' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admin account cannot be granted customer role';
    END IF;
END$$

CREATE TRIGGER trg_user_global_role_account_type_update
BEFORE UPDATE ON user_global_role
FOR EACH ROW
BEGIN
    DECLARE v_account_type VARCHAR(16);
    DECLARE v_role_code VARCHAR(32);

    SELECT account_type INTO v_account_type
    FROM user_account
    WHERE id = NEW.user_id;

    SELECT code INTO v_role_code
    FROM role
    WHERE id = NEW.role_id;

    IF v_account_type = 'customer' AND v_role_code <> 'customer' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'customer account can only be granted customer role';
    END IF;

    IF v_account_type = 'admin' AND v_role_code = 'customer' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admin account cannot be granted customer role';
    END IF;
END$$

CREATE TRIGGER trg_user_domain_role_account_type_insert
BEFORE INSERT ON user_domain_role
FOR EACH ROW
BEGIN
    DECLARE v_account_type VARCHAR(16);
    DECLARE v_role_code VARCHAR(32);

    SELECT account_type INTO v_account_type
    FROM user_account
    WHERE id = NEW.user_id;

    SELECT code INTO v_role_code
    FROM role
    WHERE id = NEW.role_id;

    IF v_account_type = 'customer' AND v_role_code <> 'customer' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'customer account can only be granted customer role';
    END IF;

    IF v_account_type = 'admin' AND v_role_code = 'customer' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admin account cannot be granted customer role';
    END IF;
END$$

CREATE TRIGGER trg_user_domain_role_account_type_update
BEFORE UPDATE ON user_domain_role
FOR EACH ROW
BEGIN
    DECLARE v_account_type VARCHAR(16);
    DECLARE v_role_code VARCHAR(32);

    SELECT account_type INTO v_account_type
    FROM user_account
    WHERE id = NEW.user_id;

    SELECT code INTO v_role_code
    FROM role
    WHERE id = NEW.role_id;

    IF v_account_type = 'customer' AND v_role_code <> 'customer' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'customer account can only be granted customer role';
    END IF;

    IF v_account_type = 'admin' AND v_role_code = 'customer' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admin account cannot be granted customer role';
    END IF;
END$$

CREATE TRIGGER trg_user_account_account_type_update
BEFORE UPDATE ON user_account
FOR EACH ROW
BEGIN
    IF NEW.account_type <> OLD.account_type THEN
        IF NEW.account_type = 'customer' AND (
            EXISTS (
                SELECT 1
                FROM user_global_role ugr
                JOIN role r ON r.id = ugr.role_id
                WHERE ugr.user_id = NEW.id AND r.code <> 'customer'
            )
            OR EXISTS (
                SELECT 1
                FROM user_domain_role udr
                JOIN role r ON r.id = udr.role_id
                WHERE udr.user_id = NEW.id AND r.code <> 'customer'
            )
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'customer account can only own customer role';
        END IF;

        IF NEW.account_type = 'admin' AND (
            EXISTS (
                SELECT 1
                FROM user_global_role ugr
                JOIN role r ON r.id = ugr.role_id
                WHERE ugr.user_id = NEW.id AND r.code = 'customer'
            )
            OR EXISTS (
                SELECT 1
                FROM user_domain_role udr
                JOIN role r ON r.id = udr.role_id
                WHERE udr.user_id = NEW.id AND r.code = 'customer'
            )
        ) THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'admin account cannot own customer role';
        END IF;
    END IF;
END$$
DELIMITER ;

CREATE TABLE IF NOT EXISTS iam_resource (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    resource_type VARCHAR(16) NOT NULL,
    resource_code VARCHAR(128) NOT NULL,
    resource_name VARCHAR(128) NOT NULL,
    client_scope VARCHAR(32) NOT NULL,
    http_method VARCHAR(16) DEFAULT NULL,
    path_pattern VARCHAR(255) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_resource_code (resource_code),
    KEY idx_iam_resource_scope_type (client_scope, resource_type, status),
    KEY idx_iam_resource_api_route (http_method, path_pattern),
    CONSTRAINT chk_iam_resource_type CHECK (resource_type IN ('menu', 'api')),
    CONSTRAINT chk_iam_resource_scope CHECK (client_scope IN ('ud-admin-web', 'ud-customer-web', 'all')),
    CONSTRAINT chk_iam_resource_api_fields CHECK (
        (resource_type = 'api' AND http_method IS NOT NULL AND path_pattern IS NOT NULL)
        OR (resource_type = 'menu' AND http_method IS NULL)
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS iam_role_resource (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    role_id INT UNSIGNED NOT NULL,
    resource_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_iam_role_resource (role_id, resource_id),
    KEY idx_iam_role_resource_resource (resource_id),
    CONSTRAINT fk_iam_role_resource_role FOREIGN KEY (role_id) REFERENCES role(id),
    CONSTRAINT fk_iam_role_resource_resource FOREIGN KEY (resource_id) REFERENCES iam_resource(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO iam_resource (resource_type, resource_code, resource_name, client_scope, http_method, path_pattern, status)
VALUES
    ('menu', 'menu.admin.dashboard', 'Admin Dashboard', 'ud-admin-web', NULL, '/dashboard/analysis', 1),
    ('menu', 'menu.admin.home', 'Admin Workspace', 'ud-admin-web', NULL, '/home', 1),
    ('menu', 'menu.admin.system.login_governance', 'Login Governance', 'ud-admin-web', NULL, '/system/login-governance', 1),
    ('menu', 'menu.customer.workspace', 'Customer Workspace', 'ud-customer-web', NULL, '/', 1),

    ('api', 'api.auth.session.read', 'Read Current Session', 'all', 'GET', '/api/v1/auth/session', 1),
    ('api', 'api.auth.login_config.read', 'Read Login Config', 'all', 'GET', '/api/v1/auth/login-config', 1),
    ('api', 'api.auth.login_config.update', 'Update Login Config', 'ud-admin-web', 'PUT', '/api/v1/auth/login-config', 1),
    ('api', 'api.auth.online_sessions.read', 'Read Online Sessions', 'ud-admin-web', 'GET', '/api/v1/auth/online-sessions', 1),
    ('api', 'api.auth.online_sessions.revoke', 'Revoke Session', 'ud-admin-web', 'POST', '/api/v1/auth/online-sessions/*/revoke', 1),
    ('api', 'api.auth.users.revoke_sessions', 'Revoke User Sessions', 'ud-admin-web', 'POST', '/api/v1/auth/users/*/revoke-sessions', 1),
    ('api', 'api.auth.login_logs.read', 'Read Login Logs', 'ud-admin-web', 'GET', '/api/v1/auth/login-logs', 1),
    ('api', 'api.auth.logout', 'Logout', 'all', 'POST', '/api/v1/auth/logout', 1),

    ('api', 'api.dashboard.read', 'Read Dashboard', 'ud-admin-web', 'GET', '/api/v1/dashboard', 1),
    ('api', 'api.domains.read', 'Read Domains', 'all', 'GET', '/api/v1/domains', 1),

    ('api', 'api.tickets.list', 'List Tickets', 'all', 'GET', '/api/v1/tickets', 1),
    ('api', 'api.tickets.create', 'Create Ticket', 'all', 'POST', '/api/v1/tickets', 1),
    ('api', 'api.tickets.detail', 'Get Ticket Detail', 'all', 'GET', '/api/v1/tickets/*', 1),
    ('api', 'api.tickets.detail_by_id', 'Get Ticket Detail By Id', 'all', 'GET', '/api/v1/tickets/id/*', 1),
    ('api', 'api.tickets.status.update', 'Update Ticket Status', 'ud-admin-web', 'PATCH', '/api/v1/tickets/*/status', 1),
    ('api', 'api.tickets.processing', 'Mark Ticket Processing', 'ud-admin-web', 'POST', '/api/v1/tickets/*/processing', 1),
    ('api', 'api.tickets.resolved', 'Mark Ticket Resolved', 'ud-admin-web', 'POST', '/api/v1/tickets/*/resolved', 1),

    ('api', 'api.consultations.list', 'List Consultations', 'all', 'GET', '/api/v1/consultations', 1),
    ('api', 'api.consultations.messages.list', 'List Consultation Messages', 'all', 'GET', '/api/v1/consultations/*/messages', 1),
    ('api', 'api.consultations.messages.create', 'Create Consultation Message', 'all', 'POST', '/api/v1/consultations/messages', 1),
    ('api', 'api.consultations.messages.create_by_session', 'Create Consultation Message By Session', 'all', 'POST', '/api/v1/consultations/*/messages', 1),
    ('api', 'api.consultations.convert_ticket', 'Convert Consultation To Ticket', 'ud-admin-web', 'POST', '/api/v1/consultations/*/ticket', 1),

    ('api', 'api.iam.resources.read', 'Read IAM Resources', 'ud-admin-web', 'GET', '/api/v1/iam/resources', 1),
    ('api', 'api.iam.resources.create', 'Create IAM Resource', 'ud-admin-web', 'POST', '/api/v1/iam/resources', 1),
    ('api', 'api.iam.resources.update', 'Update IAM Resource', 'ud-admin-web', 'PUT', '/api/v1/iam/resources/*', 1),
    ('api', 'api.iam.role_resources.read', 'Read Role Resources', 'ud-admin-web', 'GET', '/api/v1/iam/roles/*/resources', 1),
    ('api', 'api.iam.role_resources.update', 'Update Role Resources', 'ud-admin-web', 'PUT', '/api/v1/iam/roles/*/resources', 1),
    ('api', 'api.iam.me.menu_resources.read', 'Read Current Menu Resources', 'all', 'GET', '/api/v1/iam/me/menu-resources', 1)
ON DUPLICATE KEY UPDATE
    resource_name = VALUES(resource_name),
    client_scope = VALUES(client_scope),
    http_method = VALUES(http_method),
    path_pattern = VALUES(path_pattern),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO iam_role_resource (role_id, resource_id)
SELECT r.id, ir.id
FROM role r
JOIN iam_resource ir
WHERE r.code = 'super_admin'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    resource_id = VALUES(resource_id);

INSERT INTO iam_role_resource (role_id, resource_id)
SELECT r.id, ir.id
FROM role r
JOIN iam_resource ir ON ir.resource_code IN (
    'menu.admin.dashboard',
    'menu.admin.home',
    'api.auth.session.read',
    'api.auth.login_config.read',
    'api.auth.logout',
    'api.dashboard.read',
    'api.domains.read',
    'api.tickets.list',
    'api.tickets.detail',
    'api.tickets.detail_by_id',
    'api.tickets.status.update',
    'api.tickets.processing',
    'api.tickets.resolved',
    'api.consultations.list',
    'api.consultations.messages.list',
    'api.consultations.messages.create',
    'api.consultations.messages.create_by_session',
    'api.consultations.convert_ticket',
    'api.iam.me.menu_resources.read'
)
WHERE r.code IN ('domain_admin', 'agent')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    resource_id = VALUES(resource_id);

INSERT INTO iam_role_resource (role_id, resource_id)
SELECT r.id, ir.id
FROM role r
JOIN iam_resource ir ON ir.resource_code IN (
    'menu.customer.workspace',
    'api.auth.session.read',
    'api.auth.login_config.read',
    'api.auth.logout',
    'api.domains.read',
    'api.tickets.list',
    'api.tickets.create',
    'api.tickets.detail',
    'api.consultations.list',
    'api.consultations.messages.list',
    'api.consultations.messages.create',
    'api.consultations.messages.create_by_session',
    'api.iam.me.menu_resources.read'
)
WHERE r.code = 'customer'
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    resource_id = VALUES(resource_id);
