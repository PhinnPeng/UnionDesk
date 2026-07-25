-- 表单 schema 版本表：草稿 + 发布历史（最多保留 10 条 published）

CREATE TABLE ticket_form_schema (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id  BIGINT UNSIGNED NOT NULL,
    ticket_type_id      BIGINT UNSIGNED NOT NULL,
    record_type         VARCHAR(16)     NOT NULL COMMENT 'draft | published',
    version_no          INT             NOT NULL COMMENT 'draft=0; published=1..n',
    form_schema         JSON            NOT NULL,
    published_by        BIGINT UNSIGNED NULL,
    published_at        DATETIME(3)     NULL,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_form_schema_type_record_version (ticket_type_id, record_type, version_no),
    KEY idx_ticket_form_schema_domain_type (business_domain_id, ticket_type_id),
    KEY idx_ticket_form_schema_type_published_at (ticket_type_id, published_at)
) COMMENT='事项类型表单 schema 草稿与发布历史';

INSERT INTO ticket_form_schema (
    business_domain_id,
    ticket_type_id,
    record_type,
    version_no,
    form_schema,
    published_at
)
SELECT
    tt.business_domain_id,
    tt.id,
    'published',
    1,
    COALESCE(tt.form_schema, tt.form_schema_draft),
    tt.updated_at
FROM ticket_type tt
WHERE COALESCE(tt.form_schema, tt.form_schema_draft) IS NOT NULL;

INSERT INTO ticket_form_schema (
    business_domain_id,
    ticket_type_id,
    record_type,
    version_no,
    form_schema
)
SELECT
    tt.business_domain_id,
    tt.id,
    'draft',
    0,
    COALESCE(tt.form_schema_draft, tt.form_schema)
FROM ticket_type tt
WHERE COALESCE(tt.form_schema_draft, tt.form_schema) IS NOT NULL;

ALTER TABLE ticket_type
    DROP COLUMN form_schema;

ALTER TABLE ticket_type
    DROP COLUMN form_schema_draft;
