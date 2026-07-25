-- W1: staff_account 离职字段 + staff_organization + 权限 path 切到 /admin/staff

ALTER TABLE staff_account
    ADD COLUMN employment_status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '在职状态 active|offboarded' AFTER status,
    ADD COLUMN offboarded_at DATETIME(3) NULL COMMENT '离职时间' AFTER employment_status,
    ADD COLUMN offboarded_by BIGINT UNSIGNED NULL COMMENT '离职操作人 staff_account.id' AFTER offboarded_at,
    ADD COLUMN offboard_reason VARCHAR(255) NULL COMMENT '离职原因' AFTER offboarded_by;

UPDATE staff_account
SET employment_status = 'active'
WHERE employment_status IS NULL OR employment_status = '';

CREATE TABLE IF NOT EXISTS staff_organization (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    staff_account_id BIGINT UNSIGNED NOT NULL,
    organization_id BIGINT UNSIGNED NOT NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_organization (staff_account_id, organization_id),
    KEY idx_staff_organization_org (organization_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工组织归属';

-- 历史组织归属迁入（若旧表仍在）
INSERT INTO staff_organization (staff_account_id, organization_id, created_at)
SELECT uo.user_id, uo.organization_id, uo.created_at
FROM user_organization uo
INNER JOIN staff_account sa ON sa.id = uo.user_id
WHERE NOT EXISTS (
    SELECT 1 FROM staff_organization so
    WHERE so.staff_account_id = uo.user_id
      AND so.organization_id = uo.organization_id
);

UPDATE iam_permission
SET path_pattern = '/api/v1/admin/staff',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users'
  AND http_method IN ('GET', 'POST');

UPDATE iam_permission
SET path_pattern = '/api/v1/admin/staff/*',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users/*'
  AND http_method IN ('PUT', 'DELETE');

UPDATE iam_permission
SET path_pattern = '/api/v1/admin/staff/*/offboard',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users/*/offboard';

UPDATE iam_permission
SET path_pattern = '/api/v1/admin/staff/*/restore',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users/*/restore';

UPDATE iam_permission
SET path_pattern = '/api/v1/admin/staff',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users/offboard-pool'
  AND code = 'platform.user.offboard_pool.read';

UPDATE iam_resource
SET path_pattern = '/api/v1/admin/staff',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users';

UPDATE iam_resource
SET path_pattern = '/api/v1/admin/staff/*',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users/*';

UPDATE iam_resource
SET path_pattern = '/api/v1/admin/staff/*/offboard',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users/*/offboard';

UPDATE iam_resource
SET path_pattern = '/api/v1/admin/staff/*/restore',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users/*/restore';

UPDATE iam_resource
SET path_pattern = '/api/v1/admin/staff',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE path_pattern = '/api/v1/iam/users/offboard-pool';
