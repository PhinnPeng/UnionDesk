INSERT INTO staff_account (
    id,
    subject_id,
    login_name,
    phone,
    email,
    password_hash,
    must_change_password,
    status,
    source,
    auth_version,
    password_changed_at,
    created_at,
    updated_at
)
SELECT
    ua.id,
    ua.id,
    ua.username,
    ua.mobile,
    ua.email,
    ua.password_hash,
    0,
    CASE
        WHEN ua.status = 1 THEN 'active'
        ELSE 'disabled'
    END,
    'local',
    COALESCE(ua.auth_version, 1),
    NULL,
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
FROM user_account ua
WHERE ua.account_type = 'admin'
ON DUPLICATE KEY UPDATE
    subject_id = VALUES(subject_id),
    login_name = VALUES(login_name),
    phone = VALUES(phone),
    email = VALUES(email),
    password_hash = VALUES(password_hash),
    must_change_password = VALUES(must_change_password),
    status = VALUES(status),
    source = VALUES(source),
    auth_version = VALUES(auth_version),
    updated_at = CURRENT_TIMESTAMP(3);
