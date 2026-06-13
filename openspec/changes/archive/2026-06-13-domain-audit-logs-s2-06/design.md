## Context

- 平台统一页：`pages/platform/audit-logs/index.tsx` 已实现审计 + 登录双 Tab、筛选与列定义。
- 域详情：`detail-logs.tsx` 仅审计列表，调用 `GET /api/v1/admin/audit-logs?domain_id=`，`AuthGuarded` 为 `domain.audit_log.read`。
- 后端域路径已存在且单测覆盖：`AuditLogController`、`LoginLogController`。
- 权限码（DB 既有）：`domain.audit_log.read`、`domain.login_log.read`；平台码 `platform.audit_log.read` / `platform.login_log.read` 用于 `/platform/audit-logs`。

## Goals

- 域详情侧栏 **两个独立功能页**：「操作日志」「登录日志」（**不使用页内 Tabs**）。
- 域详情权限 **`platform.domain.control.audit_log.read` / `platform.domain.control.login_log.read`**（自 `domain.*` 迁移）。
- 平台页 `/platform/audit-logs` 权限 **`platform.log.audit.read` / `platform.log.login.read`**（自 `platform.audit_log.read` / `platform.login_log.read` 迁移）。
- 调用域级 API；中文空态/错误提示；审计日志不可删。

## Decisions

### 1. API 与权限

| 页面 | 路径 | 权限 |
|:---|:---|:---|
| 域详情 · 操作日志 | `GET .../domains/{domainId}/audit-logs` | `platform.domain.control.audit_log.read` |
| 域详情 · 登录日志 | `GET .../domains/{domainId}/login-logs` | `platform.domain.control.login_log.read` |
| 平台 · 操作日志 | `GET /api/v1/admin/audit-logs` | `platform.log.audit.read` |
| 平台 · 登录日志 | `GET /api/v1/admin/login-logs` | `platform.log.login.read` |

### 2. 前端实现策略

- 删除 `logs` / `detail-logs.tsx`；新增 `audit_logs` → `detail-audit-logs.tsx`、`login_logs` → `detail-login-logs.tsx`。
- 侧栏两项独立门控；平台页维持 Tabs，仅权限码变更。

### 3. Flyway

- 平台码 + 域码 rename；`PLATFORM-DOMAIN-DETAIL` 下两个 hidden catalog；角色绑定不丢失（S2-05 模式）。

### 4. 后端缺口

域级 `login-logs` 扩展 `result`/`portal_type`/`keyword` 查询参数。
