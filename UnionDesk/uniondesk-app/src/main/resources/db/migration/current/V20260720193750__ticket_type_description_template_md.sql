ALTER TABLE ticket_type
  ADD COLUMN description_template_md TEXT NULL COMMENT '描述模板 Markdown（保存即发布）' AFTER description;
