-- 团队模板（平台事项方案包）+ 建域溯源字段 + 权限/菜单

CREATE TABLE ticket_team_template (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    code            VARCHAR(64)     NOT NULL,
    name            VARCHAR(128)    NOT NULL,
    description     VARCHAR(500)    NOT NULL DEFAULT '',
    icon            VARCHAR(64)     NULL,
    status          VARCHAR(16)     NOT NULL DEFAULT 'active' COMMENT 'active | disabled',
    is_system       TINYINT         NOT NULL DEFAULT 0,
    sort_order      INT             NOT NULL DEFAULT 0,
    version         INT             NOT NULL DEFAULT 1 COMMENT '改 items 时 +1',
    created_by      BIGINT UNSIGNED NULL,
    updated_by      BIGINT UNSIGNED NULL,
    created_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_team_template_code (code),
    KEY idx_ticket_team_template_status_sort (status, sort_order)
) COMMENT='团队模板（引用平台事项类型）';

CREATE TABLE ticket_team_template_item (
    id                              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    team_template_id                BIGINT UNSIGNED NOT NULL,
    ticket_type_id                  BIGINT UNSIGNED NOT NULL COMMENT 'platform ticket_type.id',
    sort_order                      INT             NOT NULL DEFAULT 0,
    include_form_schema             TINYINT         NOT NULL DEFAULT 1,
    include_workflow                TINYINT         NOT NULL DEFAULT 1,
    include_description_template    TINYINT         NOT NULL DEFAULT 1,
    created_at                      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at                      DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_team_template_item_type (team_template_id, ticket_type_id),
    KEY idx_team_template_item_template_sort (team_template_id, sort_order)
) COMMENT='团队模板项（平台类型引用）';

ALTER TABLE business_domain
    ADD COLUMN applied_team_template_id BIGINT UNSIGNED NULL COMMENT '建域时套用的团队模板' AFTER updated_by,
    ADD COLUMN applied_team_template_version INT NULL COMMENT '套用时模板 version' AFTER applied_team_template_id,
    ADD COLUMN applied_team_template_at DATETIME(3) NULL COMMENT '套用时间' AFTER applied_team_template_version;

INSERT INTO iam_permission (code, name, description, permission_scope, resource_code, action_code, http_method, path_pattern, status)
SELECT v.code, v.name, v.name, 'platform', v.code, v.code, v.http_method, v.path_pattern, 1
FROM (
    SELECT 'platform.ticket_config.template.read' AS code, '团队模板-查看' AS name, 'GET' AS http_method, '/api/v1/platform/ticket-team-templates/**' AS path_pattern
    UNION ALL SELECT 'platform.ticket_config.template.create', '团队模板-创建', 'POST', '/api/v1/platform/ticket-team-templates'
    UNION ALL SELECT 'platform.ticket_config.template.update', '团队模板-更新', 'PUT', '/api/v1/platform/ticket-team-templates/**'
    UNION ALL SELECT 'platform.ticket_config.template.delete', '团队模板-删除', 'DELETE', '/api/v1/platform/ticket-team-templates/*'
) v
WHERE NOT EXISTS (
    SELECT 1 FROM iam_permission p WHERE p.code = v.code
);

INSERT INTO iam_role_permission (role_id, permission_id, created_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_permission p ON p.code IN (
    'platform.ticket_config.template.read',
    'platform.ticket_config.template.create',
    'platform.ticket_config.template.update',
    'platform.ticket_config.template.delete'
)
WHERE r.code IN ('super_admin', 'platform_admin')
  AND NOT EXISTS (
      SELECT 1 FROM iam_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

INSERT INTO iam_admin_menu (
    code, node_type, scope, name, route_path, component_key, permission_code,
    parent_id, order_no, icon, hidden, status, required
)
SELECT
    'PLATFORM-TICKET-CONFIG-TEMPLATES',
    'menu',
    'platform',
    '团队模板',
    '/platform/ticket-config/templates',
    'platform/ticket-config/templates',
    NULL,
    parent.id,
    40,
    'ClusterOutlined',
    1,
    1,
    0
FROM iam_admin_menu parent
WHERE parent.route_path = '/platform/ticket-config'
  AND parent.node_type = 'menu'
  AND parent.status = 1
  AND NOT EXISTS (
      SELECT 1 FROM iam_admin_menu m WHERE m.route_path = '/platform/ticket-config/templates'
  );

INSERT IGNORE INTO iam_admin_role_menu_relation (role_id, menu_id, created_at)
SELECT r.id, m.id, CURRENT_TIMESTAMP(3)
FROM role r
JOIN iam_admin_menu m ON m.route_path = '/platform/ticket-config/templates' AND m.status = 1
WHERE r.code IN ('super_admin', 'platform_admin');

-- 系统预置：客服标准模板（items 取当前全部 active 平台类型；若无类型则为空壳，可事后编辑）
INSERT INTO ticket_team_template (code, name, description, icon, status, is_system, sort_order, version)
SELECT 'default_cs', '客服标准模板', '预置客服场景事项类型包；建域时深拷贝一次后与模板解耦', NULL, 'active', 1, 0, 1
WHERE NOT EXISTS (
    SELECT 1 FROM ticket_team_template t WHERE t.code = 'default_cs'
);

INSERT INTO ticket_team_template_item (
    team_template_id, ticket_type_id, sort_order,
    include_form_schema, include_workflow, include_description_template
)
SELECT
    tmpl.id,
    tt.id,
    tt.sort_order,
    1,
    1,
    1
FROM ticket_team_template tmpl
JOIN ticket_type tt ON tt.scope = 'platform' AND tt.status = 'active'
WHERE tmpl.code = 'default_cs'
  AND NOT EXISTS (
      SELECT 1
      FROM ticket_team_template_item i
      WHERE i.team_template_id = tmpl.id AND i.ticket_type_id = tt.id
  );
