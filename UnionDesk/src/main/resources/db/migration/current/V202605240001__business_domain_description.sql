ALTER TABLE business_domain
    ADD COLUMN description VARCHAR(512) NULL COMMENT '业务域描述' AFTER name;
