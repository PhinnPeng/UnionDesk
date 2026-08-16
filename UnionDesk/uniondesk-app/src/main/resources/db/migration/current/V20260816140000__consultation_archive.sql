-- 咨询会话归档：已关闭会话可手动/自动归档，归档后从默认列表隐藏（客户侧不可见）。
ALTER TABLE consultation_session
    ADD COLUMN archived_at DATETIME(3) NULL COMMENT '归档时间（NULL=未归档）';
