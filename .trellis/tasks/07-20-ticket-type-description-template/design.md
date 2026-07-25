# 设计：事项类型描述模板富文本

## 架构

- 存储：`ticket_type.description_template_md` TEXT NULL
- 编辑器：BlockNote；权威格式 Markdown
- UI：预览默认 → 左下角编辑 → 保存写库回预览

## API

- GET 类型详情含 `description_template_md`
- PUT 类型更新支持该字段（或专用 PATCH）；保存即生效

## 前端

- 重写 `template-tab.tsx`：Preview + Editor modes
- 懒加载 BlockNote
- 平台 `platform-ticket-type-config-content` 与域 `ticket-type-config` 去掉 Formily 草稿/发布于该 Tab

## 运行时填充

- 创建事项初始化：若类型有 system key `description` 槽位且描述为空 → 填入 `description_template_md`
- 不覆盖用户已输入

## 波次

W1 Flyway+API → W2 TemplateTab → W3 去 Formily 接线 → W4 创建预填
