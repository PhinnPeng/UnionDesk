## 1. 数据库与后端

- [x] 1.1 Flyway：`form_schema_draft`、`description`、`icon`；回填 draft；域内 code/name 唯一索引
- [x] 1.2 `TicketTypePo` / Mapper / Repository 扩展字段
- [x] 1.3 `TicketConfigService`：创建/更新唯一性校验；`saveFormSchemaDraft` / `publishFormSchema`
- [x] 1.4 `TicketConfigController`：draft PUT、publish POST；DTO 扩展；`updateTicketType` 不再写 `form_schema`
- [x] 1.5 `TicketConfigServiceTests` + `FormSchemaValidatorTests` 补充用例

## 2. Shared API

- [x] 2.1 `DomainTicketType` 增加 `description`、`icon`、`form_schema_draft`
- [x] 2.2 `saveDomainTicketTypeFormSchemaDraft` / `publishDomainTicketTypeFormSchema`
- [x] 2.3 `CreateDomainTicketTypeBody` / `UpdateDomainTicketTypeBody` 扩展

## 3. 前端 — 公共组件

- [x] 3.1 抽取 `src/components/icon-picker/`（菜单页改引用）
- [x] 3.2 `FormilyFormDesigner`：`onSaveDraft` / `onPublish`；`saveDraftSchema` / `publishSchema`
- [x] 3.3 `ActionsWidget` 业务化（保存/发布文案、loading、移除 Playground）

## 4. 前端 — 工单列表

- [x] 4.1 SubTab/Card 改名「工单列表」
- [x] 4.2 Table → Card 网格（居中）；未发布 Tag
- [x] 4.3 创建 Modal：code/name/description/icon；创建后 confirm 是否进入表单设计
- [x] 4.4 编辑 Modal：name/description/icon/status（code 只读）
- [x] 4.5 卡片操作：表单设计 / 编辑 / 启停 / 删除；状态流入口（可选 `/flow`）

## 5. 前端 — 表单设计页

- [x] 5.1 页面 `src/pages/common/form-design/index.tsx`；路由 `/platform/domains/ticket/form-design/:domainId/:typeId`（`platform-pages.ts`）
- [x] 5.2 独立页面：返回列表 + 仅设计器 + draft/publish 接线
- [x] 5.3 原 `ticket-type-config` 及 `/form` 路径重定向至 `/platform/domains/ticket/form-design/...`
- [x] 5.4 `platform-pages.ts` / tabbar 标题「表单设计 - {name}」

## 6. 验收

- [x] 6.1 `mvn test` + `pnpm run typecheck`
- [x] 6.2 agent-browser：创建 → 表单设计 → 保存草稿 → 发布 → 列表未发布 Tag 消失（2026-06-26 复验：PUT 差异化 draft → 列表 `未发布` → publish → Tag 消失；端口 3334）
