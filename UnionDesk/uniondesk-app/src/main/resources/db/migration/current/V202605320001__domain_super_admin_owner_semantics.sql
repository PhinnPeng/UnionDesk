-- US-S1-02：super_admin 产品称「所有人」；补齐历史域 permission_item 与 iam_role_binding

UPDATE domain_role
SET name = '业务域所有人',
    updated_at = CURRENT_TIMESTAMP(3)
WHERE preset = 1
  AND code = 'super_admin';

INSERT INTO domain_role_permission (domain_role_id, permission_item_id, created_at)
SELECT dr.id, pi.id, CURRENT_TIMESTAMP(3)
FROM domain_role dr
CROSS JOIN permission_item pi
WHERE dr.code = 'super_admin'
  AND dr.preset = 1
  AND NOT EXISTS (
      SELECT 1
      FROM domain_role_permission drp
      WHERE drp.domain_role_id = dr.id
        AND drp.permission_item_id = pi.id
  );

INSERT INTO iam_role_binding (
    user_id,
    role_id,
    binding_scope,
    business_domain_id,
    status,
    created_at,
    updated_at
)
SELECT
    udr.user_id,
    udr.role_id,
    'domain',
    udr.business_domain_id,
    1,
    CURRENT_TIMESTAMP(3),
    CURRENT_TIMESTAMP(3)
FROM user_domain_role udr
JOIN role r ON r.id = udr.role_id
WHERE r.code = 'super_admin'
  AND NOT EXISTS (
      SELECT 1
      FROM iam_role_binding irb
      WHERE irb.user_id = udr.user_id
        AND irb.role_id = udr.role_id
        AND irb.binding_scope = 'domain'
        AND irb.business_domain_id = udr.business_domain_id
  );
