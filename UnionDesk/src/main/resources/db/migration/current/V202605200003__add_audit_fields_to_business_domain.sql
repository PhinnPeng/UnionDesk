-- Add created_by and updated_by to business_domain
ALTER TABLE business_domain
    ADD COLUMN created_by BIGINT UNSIGNED DEFAULT NULL AFTER status,
    ADD COLUMN updated_by BIGINT UNSIGNED DEFAULT NULL AFTER created_by;

-- Add indexes
ALTER TABLE business_domain
    ADD KEY idx_business_domain_created_by (created_by),
    ADD KEY idx_business_domain_updated_by (updated_by);

-- FKs (optional but recommended since they point to user_account)
ALTER TABLE business_domain
    ADD CONSTRAINT fk_business_domain_created_by FOREIGN KEY (created_by) REFERENCES user_account(id),
    ADD CONSTRAINT fk_business_domain_updated_by FOREIGN KEY (updated_by) REFERENCES user_account(id);
