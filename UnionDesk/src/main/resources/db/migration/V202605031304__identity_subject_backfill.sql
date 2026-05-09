-- 回填现有 user_account 对应的 identity_subject，保证历史账号和集成测试可正常解析主体
INSERT INTO identity_subject (id, subject_type, phone, status, created_at, updated_at)
SELECT
    ua.id,
    'person',
    COALESCE(NULLIF(ua.mobile, ''), CONCAT('user-', ua.id)),
    'active',
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
FROM user_account ua
LEFT JOIN identity_subject s ON s.id = ua.id
WHERE s.id IS NULL;
