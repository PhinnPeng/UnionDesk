## 0. POC（Gate）

- [x] 0.1 引入 Formily 2.x + `@xyflow/react`；独立 POC 页验证设计器与 DAG 可运行
- [x] 0.2 确认 antd 6 样式无阻塞问题后拆除 POC 或保留 dev-only 路由

## 1. Flyway 与权限

- [x] 1.1 `ticket_type.status`、`ticket_type.form_schema` 列
- [x] 1.2 `platform.domain.control.ticket_type.{read,create,update,delete}` + catalog
- [x] 1.3 预置 feedback/suggestion + §6.4 默认状态流；登记 `database-increment-plan.md`

## 2. 后端

- [x] 2.1 DTO：`status_flow` / `form_schema` 替换 `dynamic_fields`；更新测试
- [x] 2.2 `StatusFlowValidator`（TR-01 + 图完整性）
- [x] 2.3 `FormSchemaValidator` + `DefaultFormSchemaProvider`（title/description 系统字段）
- [x] 2.4 `TicketConfigService`：双字段读写、status、删除保护
- [x] 2.5 `TicketConfigController` 改绑 platform 权限

## 3. Shared

- [x] 3.1 `api.ts` / `types.ts` 对齐新 DTO

## 4. 前端 — 设计器内核

- [x] 4.1 `ticket-type-flow-utils.ts`（JSON ↔ React Flow）
- [x] 4.2 `ticket-type-flow-designer.tsx`（DAG + 属性面板 + 导入默认模板）
- [x] 4.3 `ticket-type-form-defaults.ts` + `ticket-type-form-designer.tsx`（系统字段锁定区）

## 5. 前端 — 页面集成

- [x] 5.1 `platform-domain-permissions.ts` 四码
- [x] 5.2 `detail-sider` Tab 门控
- [x] 5.3 `ticket-type-designer-drawer.tsx` 三 Tab 容器
- [x] 5.4 `detail-tickets.tsx` 列表 + [设计] 入口
- [x] 5.5 `ticket-template-modal.tsx` 模板 CRUD

## 6. 验收

- [x] 6.1 单测 + `pnpm run typecheck`
- [ ] 6.2 手工：Formily 保存预览、DAG 编辑、TR-01 失败提示、启停、删除保护
- [x] 6.3 同步 backlog/sprint（移除 MVP AC5）；US-S3-01 → Done
