# 事项类型描述模板富文本

## Goal

将「描述模板」Tab 从 Formily 表单设计器改为语雀式 Markdown 富文本：默认预览、左下角进入编辑、保存即发布；创建事项时若类型已配置系统 `description` 槽位则自动预填模板。

## Confirmed Decisions

| 项 | 决定 |
|:---|:---|
| Formily | 从描述模板 Tab 剔除；本轮不删 `form_schema*` 表 |
| 编辑器 | BlockNote（斜杠 `/`、MD 存取） |
| 打开 | 预览模式 |
| 编辑 | 左下角「编辑」进入编辑器 |
| 保存 | 写 `description_template_md`，即已发布 |
| 填充 | 有系统属性 `description` 时，创建表单空描述预填模板 |

## Requirements

- R1：`ticket_type.description_template_md` + 详情/更新 API
- R2：TemplateTab 预览 / 编辑 / 保存 / 取消
- R3：平台与域事项类型配置入口对齐
- R4：创建事项时自动填充（有 description 槽位且字段为空）

## Acceptance Criteria

- [ ] AC1：打开描述模板为预览；左下角编辑进入 BlockNote
- [ ] AC2：保存后再次打开预览为最新内容
- [ ] AC3：无 Formily 于该 Tab
- [ ] AC4：类型含 description 系统槽位时，创建表单预填模板；已有输入不覆盖
- [ ] AC5：无模板或无 description 槽位时不强制填充

## Out of Scope

草稿发布流、图片上传 MinIO、协同、AI、删除 form_schema 表、BlockNote XL
