# 业务域端 P2：双日志 + 域角色

## Goal

填实 `/domain/audit-logs`、`/domain/login-logs`、`/domain/roles`，使业务端可只读查本域日志，并管理本域角色（能力不低于平台只读；写能力按 design 默认开放）。

父任务：`07-27-business-console-full-mirror`。

## Confirmed Decisions

| 决策 | 结论 |
|:---|:---|
| 操作/登录日志 | **只读**列表，对齐平台 detail Tab；`domain.audit_log.read` / `domain.login_log.read` |
| 域角色 | 列表 + 权限抽屉；**写能力开放**（create/update/delete/permission.update），有码才显按钮；无写 API 时至少做到平台级只读 + 在 PR 注明缺口 |
| domainId | 会话活跃域（同 P0/P1） |
| 菜单显隐 | 按读码细粒度 |

## Requirements

### R1 — `/domain/audit-logs`
- MUST 本域操作日志筛选/分页只读；权限 `domain.audit_log.read`。

### R2 — `/domain/login-logs`
- MUST 本域登录日志筛选/分页只读；权限 `domain.login_log.read`。

### R3 — `/domain/roles`
- MUST 列出本域角色；可查看角色权限（`domain.role.read` / `domain.role.permission.read`）。
- SHOULD 在 API 与权限码已具备时提供创建/编辑/删除角色与更新权限勾选（`domain.role.create|update|delete` / `domain.role.permission.update`）。
- MUST NOT 与 `/system/role`（IAM）混淆。

### R4 — 横切
- 会话域绑定；后端缺口并列 `domain.*`；平台 detail 无回归。

### R5 — Out of Scope
- 运营概览 KPI、通知成品、入域写增强

## Acceptance Criteria

- [ ] AC1：有读码可开操作日志页并查本域数据
- [ ] AC2：有读码可开登录日志页并查本域数据
- [ ] AC3：有 `domain.role.read` 可开域角色页；权限抽屉可用
- [ ] AC4：写码存在且 API 可用时，角色写入口可用；否则只读不报错
- [ ] AC5：三页会话域绑定；业务端 `domain.*`；平台无回归
- [ ] AC6：三页 Empty「功能开发中」消失
