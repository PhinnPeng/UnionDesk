## Why

US-S2-06 要求平台管理员在业务域详情「业务日志」Tab 查看该域的**操作日志**与**登录日志**。当前 `detail-logs.tsx` 仅有单表、无筛选、无登录日志子 Tab，且走平台 API + `domain_id` 过滤；域级 API（`/domains/{id}/audit-logs`、`/domains/{id}/login-logs`）与权限码（`domain.audit_log.read` / `domain.login_log.read`）已就绪但未在前端域详情完整暴露。

## What Changes

- **前端**：域详情侧栏拆为两个独立页「操作日志」「登录日志」（**不用 Tabs**）；各页 `TableSearchForm` + 表格列对齐 `platform/audit-logs`；固定当前 `domainId`。
- **API 封装**：补充域级 `fetchDomainAuditLogs` / `fetchDomainLoginLogs`。
- **权限**：域详情 `platform.domain.control.audit_log.read` / `platform.domain.control.login_log.read`；平台页 `platform.log.audit.read` / `platform.log.login.read`（Flyway 自旧码迁移）。
- **Flyway**：域码 + 平台码 rename；`PLATFORM-DOMAIN-DETAIL` 下两个 catalog + 按钮；`database-increment-plan.md` 登记。
- **后端（按需）**：若域级登录日志 API 缺 `result` / `keyword` 等筛选，在 `AuditLogService` 与 Controller 补齐，与平台端参数对齐。

## Capabilities

### New Capabilities

- `domain-audit-logs` — 业务域详情内操作/登录日志只读查看

## Impact

- `UnionDeskWeb/.../detail-logs.tsx`、`detail-sider.tsx`
- `#src/api/platform/audit.ts`
- `AuditLogController` / `LoginLogController` / `AuditLogService`（仅当筛选缺口）
- Flyway + `AdminPermissionCatalog`（仅当域详情菜单未登记）

## Non-Goals

- 收敛 `platform/log/*` 与 `platform/audit-logs/` 双入口（inventory §5，另 Story）
- 审计/登录日志删除、导出
- BusinessWeb 域端独立日志页
- 将 backlog 文档中的 `platform.audit-logs.read` 重命名为新码（实库为 `platform.audit_log.read`，本 Story 域 Tab 使用 **域级** `domain.*` 码）
