-- US-S3-01：工单类型 platform 域详情权限 + status/form_schema 列 + 预置类型

ALTER TABLE ticket_type
    ADD COLUMN status varchar(16) NOT NULL DEFAULT 'active' COMMENT 'active=启用 disabled=停用';

ALTER TABLE ticket_type
    ADD COLUMN form_schema json DEFAULT NULL COMMENT 'Formily 表单 schema';

-- platform.domain.control.ticket_type.*（新增，保留 domain.ticket_type.*）
INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.ticket_type.read', '查看工单类型', '查看业务域工单类型与模板', 'platform',
       'ticket_type', 'platform_domain_control_ticket_type_read', 'GET', '/api/v1/admin/domains/*/ticket-types', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.ticket_type.read');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.ticket_type.create', '新建工单类型', '创建业务域工单类型与模板', 'platform',
       'ticket_type', 'platform_domain_control_ticket_type_create', 'POST', '/api/v1/admin/domains/*/ticket-types', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.ticket_type.create');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.ticket_type.update', '编辑工单类型', '更新业务域工单类型与模板', 'platform',
       'ticket_type', 'platform_domain_control_ticket_type_update', 'PUT', '/api/v1/admin/domains/*/ticket-types/*', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.ticket_type.update');

INSERT INTO iam_permission (
    code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status
)
SELECT 'platform.domain.control.ticket_type.delete', '删除工单类型', '删除业务域工单类型与模板', 'platform',
       'ticket_type', 'platform_domain_control_ticket_type_delete', 'DELETE', '/api/v1/admin/domains/*/ticket-types/*', 1
WHERE NOT EXISTS (SELECT 1 FROM iam_permission WHERE code = 'platform.domain.control.ticket_type.delete');

INSERT INTO iam_admin_menu (code, node_type, scope, name, route_path, component_key, permission_code, parent_id, order_no, icon, hidden, status, required)
SELECT 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE', 'catalog', 'platform', '工单管理', NULL, NULL, NULL,
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-DETAIL' LIMIT 1),
       12, 'FormOutlined', 1, 1, 0
WHERE NOT EXISTS (SELECT 1 FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE');

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TT-READ', 'button', 'platform', '查看工单类型', 'platform.domain.control.ticket_type.read',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' LIMIT 1),
       0, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_type.read' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TT-CREATE', 'button', 'platform', '新建工单类型', 'platform.domain.control.ticket_type.create',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' LIMIT 1),
       1, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_type.create' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TT-UPDATE', 'button', 'platform', '编辑工单类型', 'platform.domain.control.ticket_type.update',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' LIMIT 1),
       2, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_type.update' AND node_type = 'button'
);

INSERT INTO iam_admin_menu (code, node_type, scope, name, permission_code, parent_id, order_no, status, required)
SELECT 'TMP-BTN-DOMAIN-TT-DELETE', 'button', 'platform', '删除工单类型', 'platform.domain.control.ticket_type.delete',
       (SELECT id FROM iam_admin_menu WHERE code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' LIMIT 1),
       3, 1, 0
WHERE NOT EXISTS (
    SELECT 1 FROM iam_admin_menu WHERE permission_code = 'platform.domain.control.ticket_type.delete' AND node_type = 'button'
);

UPDATE iam_admin_menu
SET code = CONCAT('ADM', LPAD(id, 10, '0'))
WHERE code IN ('TMP-BTN-DOMAIN-TT-READ', 'TMP-BTN-DOMAIN-TT-CREATE', 'TMP-BTN-DOMAIN-TT-UPDATE', 'TMP-BTN-DOMAIN-TT-DELETE');

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.ticket_type.read',
    'platform.domain.control.ticket_type.create',
    'platform.domain.control.ticket_type.update',
    'platform.domain.control.ticket_type.delete'
) AND p.status = 1
WHERE r.code IN ('super_admin', 'platform_admin');

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE' AND m.status = 1
WHERE r.code IN ('super_admin', 'platform_admin');

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, btn.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu btn ON btn.node_type = 'button' AND btn.status = 1
    AND btn.permission_code IN (
        'platform.domain.control.ticket_type.read',
        'platform.domain.control.ticket_type.create',
        'platform.domain.control.ticket_type.update',
        'platform.domain.control.ticket_type.delete'
    )
WHERE r.code IN ('super_admin', 'platform_admin');

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT DISTINCT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_admin_menu m ON (
    m.code = 'PLATFORM-DOMAIN-CONTROL-TICKET-TYPE'
    OR m.permission_code IN (
        'platform.domain.control.ticket_type.read',
        'platform.domain.control.ticket_type.create',
        'platform.domain.control.ticket_type.update',
        'platform.domain.control.ticket_type.delete'
    )
)
WHERE m.status = 1;

INSERT IGNORE INTO iam_role_permission (role_id, permission_id, created_at)
SELECT DISTINCT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_role_menu_relation rel ON rel.role_id = r.id
JOIN iam_admin_menu detail ON detail.id = rel.menu_id
    AND detail.route_path = '/platform/domains/detail'
    AND detail.node_type = 'menu'
    AND detail.status = 1
JOIN iam_permission p ON p.code IN (
    'platform.domain.control.ticket_type.read',
    'platform.domain.control.ticket_type.create',
    'platform.domain.control.ticket_type.update',
    'platform.domain.control.ticket_type.delete'
) AND p.status = 1;

-- 预置类型（domain_id=1）
INSERT INTO ticket_type (business_domain_id, code, name, status, status_flow_config, form_schema, created_at, updated_at)
SELECT 1, 'feedback', '反馈', 'active',
       JSON_OBJECT(
           'states', JSON_ARRAY(
               JSON_OBJECT('code', 'pending', 'name', '待处理', 'state_type', 'in_progress', 'allow_customer_withdraw', true, 'is_resolved', false),
               JSON_OBJECT('code', 'processing', 'name', '处理中', 'state_type', 'in_progress', 'allow_customer_withdraw', false, 'is_resolved', false),
               JSON_OBJECT('code', 'closed', 'name', '已关闭', 'state_type', 'terminal', 'allow_customer_withdraw', false, 'is_resolved', false)
           ),
           'transitions', JSON_ARRAY(
               JSON_OBJECT('from', 'pending', 'to', 'processing'),
               JSON_OBJECT('from', 'processing', 'to', 'closed')
           )
       ),
       JSON_OBJECT(
           'type', 'object',
           'properties', JSON_OBJECT(
               'title', JSON_OBJECT('type', 'string', 'title', '标题', 'x-component', 'Input', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true),
               'description', JSON_OBJECT('type', 'string', 'title', '详细描述', 'x-component', 'Input.TextArea', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true)
           )
       ),
       CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (SELECT 1 FROM ticket_type WHERE business_domain_id = 1 AND code = 'feedback');

INSERT INTO ticket_type (business_domain_id, code, name, status, status_flow_config, form_schema, created_at, updated_at)
SELECT 1, 'suggestion', '建议', 'active',
       JSON_OBJECT(
           'states', JSON_ARRAY(
               JSON_OBJECT('code', 'pending', 'name', '待处理', 'state_type', 'in_progress', 'allow_customer_withdraw', true, 'is_resolved', false),
               JSON_OBJECT('code', 'processing', 'name', '处理中', 'state_type', 'in_progress', 'allow_customer_withdraw', false, 'is_resolved', false),
               JSON_OBJECT('code', 'closed', 'name', '已关闭', 'state_type', 'terminal', 'allow_customer_withdraw', false, 'is_resolved', false)
           ),
           'transitions', JSON_ARRAY(
               JSON_OBJECT('from', 'pending', 'to', 'processing'),
               JSON_OBJECT('from', 'processing', 'to', 'closed')
           )
       ),
       JSON_OBJECT(
           'type', 'object',
           'properties', JSON_OBJECT(
               'title', JSON_OBJECT('type', 'string', 'title', '标题', 'x-component', 'Input', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true),
               'description', JSON_OBJECT('type', 'string', 'title', '详细描述', 'x-component', 'Input.TextArea', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true)
           )
       ),
       CURRENT_TIMESTAMP(3), CURRENT_TIMESTAMP(3)
WHERE NOT EXISTS (SELECT 1 FROM ticket_type WHERE business_domain_id = 1 AND code = 'suggestion');

UPDATE ticket_type
SET status = 'active',
    form_schema = JSON_OBJECT(
        'type', 'object',
        'properties', JSON_OBJECT(
            'title', JSON_OBJECT('type', 'string', 'title', '标题', 'x-component', 'Input', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true),
            'description', JSON_OBJECT('type', 'string', 'title', '详细描述', 'x-component', 'Input.TextArea', 'x-decorator', 'FormItem', 'required', true, 'x-system-field', true)
        )
    )
WHERE business_domain_id = 1 AND code = 'general' AND form_schema IS NULL;
