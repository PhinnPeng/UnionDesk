# Comet Design Handoff

- Change: domain-audit-logs-s2-06
- Phase: design
- Mode: compact
- Context hash: 886865863f4a4457c1deceea19bdc80fca57f9b30b1190c016c5c076d823f049

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/domain-audit-logs-s2-06/proposal.md

- Source: openspec/changes/domain-audit-logs-s2-06/proposal.md
- Lines: 1-31
- SHA256: 435adb88742799ac574a6faed4edea78d17f55aa047dddde7c4e2552ff91e881

```md
## Why

US-S2-06 要求平台管理员在业务域详情「业务日志」Tab 查看该域的**操作日志**与**登录日志**。当前 `detail-logs.tsx` 仅有单表、无筛选、无登录日志子 Tab，且走平台 API + `domain_id` 过滤；域级 API（`/domains/{id}/audit-logs`、`/domains/{id}/login-logs`）与权限码（`domain.audit_log.read` / `domain.login_log.read`）已就绪但未在前端域详情完整暴露。

## What Changes

- **前端**：升级 `detail-logs.tsx`——与 `pages/platform/audit-logs` 对齐的双 Tab（操作日志 / 登录日志）、`TableSearchForm` 筛选、分页列定义；固定当前 `domainId`，不展示业务域选择器。
- **API 封装**：在 `#src/api/platform/audit.ts`（或等价位置）补充域级 `fetchDomainAuditLogs` / `fetchDomainLoginLogs`。
- **权限与门控**：Tab 与 `AuthGuarded` 使用既有域权限码；`detail-sider` 按读权限隐藏「业务日志」Tab（若用户无任何日志读权限）。
- **Flyway（若缺）**：在 `PLATFORM-DOMAIN-DETAIL` 下登记「业务日志」catalog + 查询按钮，绑定 `domain.audit_log.read` / `domain.login_log.read`；`database-increment-plan.md` 登记。
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
```

## openspec/changes/domain-audit-logs-s2-06/design.md

- Source: openspec/changes/domain-audit-logs-s2-06/design.md
- Lines: 1-47
- SHA256: 902ea38c7f090f11b4bb787e080ef1b34aa9fa1a177c9634bd05c399c9e1d65f

```md
## Context

- 平台统一页：`pages/platform/audit-logs/index.tsx` 已实现审计 + 登录双 Tab、筛选与列定义。
- 域详情：`detail-logs.tsx` 仅审计列表，调用 `GET /api/v1/admin/audit-logs?domain_id=`，`AuthGuarded` 为 `domain.audit_log.read`。
- 后端域路径已存在且单测覆盖：`AuditLogController`、`LoginLogController`。
- 权限码（DB 既有）：`domain.audit_log.read`、`domain.login_log.read`；平台码 `platform.audit_log.read` / `platform.login_log.read` 用于 `/platform/audit-logs`。

## Goals

- 域详情「业务日志」= **操作日志** + **登录日志** 子 Tab，交互与列与平台审计页对齐（去掉业务域筛选）。
- 调用 **域级 API**，权限与 `@RequirePermission` 一致。
- 中文空态/错误提示；审计日志不可删。

## Decisions

### 1. API 选型：域路径优先

| Tab | 路径 | 权限 |
|:---|:---|:---|
| 操作日志 | `GET /api/v1/admin/domains/{domainId}/audit-logs` | `domain.audit_log.read` |
| 登录日志 | `GET /api/v1/admin/domains/{domainId}/login-logs` | `domain.login_log.read` |

不再使用平台 API + `domain_id` 查询参数（避免与域权限语义不一致）。

### 2. 权限与 backlog 对齐说明

Backlog / sprint-plan 写作 `platform.audit-logs.read`；**代码与 Flyway 实码为 `platform.audit_log.read`（下划线）**。本 Story 域详情 Tab 门控采用 **`domain.audit_log.read`**（操作）与 **`domain.login_log.read`**（登录），与域 API 绑定一致；不在本 Story 做平台码 rename。

### 3. 前端实现策略

- **独立升级** `detail-logs.tsx`（不抽跨页共用 Panel，对齐 S2-05 域 Tab 做法）。
- 复用平台页列定义与 `TableSearchForm` 字段子集：
  - 操作：操作者、动作、时间范围、（可选 keyword）
  - 登录：门户、结果、时间范围、（可选 keyword）
- `detail-sider`：有 `domain.audit_log.read` 或 `domain.login_log.read` 之一即显示 Tab；子 Tab 内再分别门控。

### 4. Flyway

- 检查 `PLATFORM-DOMAIN-DETAIL` 下是否已有「业务日志」catalog；若无则 INSERT catalog + 查询按钮，并给已有域详情入口角色补 menu/permission 绑定（模式同 S2-05 Step 6）。

### 5. 后端缺口（待 build 验证）

域级 `login-logs` 当前参数为 `operator/action/startTime/endTime`；若与平台登录 Tab 的 `result`/`portal_type` 不一致，扩展 `listDomainLoginLogs` 查询条件，保持向后兼容。

## Risks / Open Questions

- 域详情侧栏 Tab 文案 backlog 写「日志」，现网为「业务日志」——保持现网文案，除非产品明确要求改名。
```

## openspec/changes/domain-audit-logs-s2-06/tasks.md

- Source: openspec/changes/domain-audit-logs-s2-06/tasks.md
- Lines: 1-24
- SHA256: 8e9bb136aae4920646a8311197699ef04f56a125926f2ae7eeb376c68ab659f0

```md
## 1. Flyway 与权限（若缺则补）

- [ ] 1.1 确认/补登记 `PLATFORM-DOMAIN-DETAIL` 下「业务日志」catalog + `domain.audit_log.read` / `domain.login_log.read` 按钮
- [ ] 1.2 为已有域详情菜单权限的角色自动授权；`database-increment-plan.md` 登记

## 2. 后端（按需）

- [ ] 2.1 核对域级 audit/login API 筛选参数与平台页一致；缺则扩展 `AuditLogService` + Controller
- [ ] 2.2 补充/更新 ControllerTests（域路径 + 筛选）

## 3. 前端 API

- [ ] 3.1 `fetchDomainAuditLogs` / `fetchDomainLoginLogs` 封装（`#src/api/platform/audit.ts`）

## 4. 前端域详情 Tab

- [ ] 4.1 升级 `detail-logs.tsx`：双 Tab + TableSearchForm + 列与平台页对齐
- [ ] 4.2 `detail-sider` Tab 门控；分 Tab `AuthGuarded`（audit / login 权限）
- [ ] 4.3 权限 labels（若菜单登记涉及新展示）

## 5. 验收

- [ ] 5.1 typecheck + 相关单测；手工：双 Tab 分页筛选、无权限空态
- [ ] 5.2 backlog / S2-closure-tracker 收口
```

