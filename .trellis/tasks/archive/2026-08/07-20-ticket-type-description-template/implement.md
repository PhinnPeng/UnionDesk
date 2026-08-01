# 实现计划

## W1 — 数据与 API
- [x] Flyway 增加 `description_template_md`
- [x] PO/DTO/Mapper/Service 读写
- [x] shared types + API client 字段

## W2 — TemplateTab UI
- [x] 预览模式（react-markdown 或轻量渲染）
- [x] 左下角编辑 → BlockNote
- [x] 保存/取消；权限 AuthGuarded

## W3 — 配置页清理
- [x] 平台/域 config 去掉 Formily 于 template tab 的 state/handlers
- [x] 安装 `@blocknote/*` 依赖（AdminWeb）

## W4 — 创建预填
- [x] 定位创建表单；有 description 槽位则预填
- [x] 冒烟：保存模板 → 打开预览 → 创建事项见填充

## 验证
- 前端 typecheck / 相关单测
- 后端编译或相关测试
