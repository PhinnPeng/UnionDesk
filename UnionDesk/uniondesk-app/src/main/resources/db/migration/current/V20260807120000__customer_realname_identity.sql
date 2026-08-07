-- 域客户实名信息：customer_account 新增真实姓名/身份证号列 + 客户编辑权限
-- 身份证按明文存储，接口响应一律脱敏（前 3 + *********** + 后 4），管理端不展示完整证号

ALTER TABLE customer_account
    ADD COLUMN real_name VARCHAR(64) NULL COMMENT '真实姓名' AFTER email,
    ADD COLUMN id_card_no VARCHAR(64) NULL COMMENT '身份证号（接口返回一律脱敏）' AFTER real_name;

INSERT INTO iam_permission (
    code,
    name,
    description,
    permission_scope,
    resource_code,
    action_code,
    http_method,
    path_pattern,
    status
)
VALUES
    ('domain.customer.update', '编辑域客户资料', '编辑域客户展示名/真实姓名/手机/邮箱/身份证号，登录名不可修改', 'domain', 'domain.customer', 'update', 'PUT', '/api/v1/admin/domains/*/customers/*', 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    description = VALUES(description),
    permission_scope = VALUES(permission_scope),
    resource_code = VALUES(resource_code),
    action_code = VALUES(action_code),
    http_method = VALUES(http_method),
    path_pattern = VALUES(path_pattern),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(3);

INSERT INTO iam_role_permission (role_id, permission_id)
SELECT r.id, p.id
FROM role r
JOIN iam_permission p ON p.code = 'domain.customer.update'
WHERE r.code IN ('domain_admin', 'super_admin')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id),
    permission_id = VALUES(permission_id);
