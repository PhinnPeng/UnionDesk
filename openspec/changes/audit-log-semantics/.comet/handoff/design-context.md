# Comet Design Handoff

- Change: audit-log-semantics
- Phase: design
- Mode: compact
- Context hash: 9548b955094baa7d6fb26804c0d2583096ac2e6d1dad7458bee479557c7f6bdc

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/audit-log-semantics/proposal.md

- Source: openspec/changes/audit-log-semantics/proposal.md
- Lines: 1-30
- SHA256: 46c69a83a85b13269487ada18a88d2bd86b57867fe752c75befee34a2cc95157

```md
## Why

平台/业务域「操作日志」「登录日志」页面已可查看记录，但展示内容偏技术化：动作为 `domain.update` 等机器码、目标为 `domain:code`、明细为原始 JSON。运营与管理员难以快速理解「谁对什么做了什么、具体变了什么」。角色权限更新等关键操作甚至尚未写入审计。

## What Changes

- **操作日志语义化（写入 + 展示）**
  - **动作**：保留机器码 `action` 供筛选；新增/统一中文标签，如「业务域更新」「业务域启用」「业务域禁用」。
  - **目标**：统一为 `资源名称-短码`（如 `演示业务域-demo`），替代 `domain:demo` 等技术格式。
  - **明细**：记录操作完成后的可读摘要，支持换行；如角色权限更新时列出新增/移除的菜单权限（中文名称）。
- **补齐审计埋点**：平台角色权限更新、业务域状态变更等当前缺失或过于粗糙的写入点。
- **登录日志展示语义化**：门户类型、登录结果等字段在前端展示中文标签（写入结构可不变）。

## Capabilities

### New Capabilities

- `audit-log-semantics` — 操作/登录日志字段语义化与审计写入规范

## Impact

- 后端：`uniondesk-support` 审计写入辅助；`DomainService`、`AdminMenuService` / `IamController`、现有 `AuditLogEventListener` 等写入点
- 前端：`platform/audit-logs`、`detail-audit-logs`、`detail-login-logs` 列渲染与明细换行
- API：`AuditLogView` 响应可增加 `actionLabel`（可选，或前端映射表）

## Non-Goals

- 历史日志批量回填/迁移（旧记录保持原样，展示层做兼容降级）
- 日志删除、导出、告警
- 工单模块全量审计语义化（可作为后续增量，本 change 仅列扩展点）
```

## openspec/changes/audit-log-semantics/design.md

- Source: openspec/changes/audit-log-semantics/design.md
- Lines: 1-96
- SHA256: e46db5e41c0d15dd39d00926bd2cb70892ab22f83694c8d032574b265958b87c

[TRUNCATED]

```md
## Context

当前审计写入分散在 `DomainService`、`TicketService`、`NotificationCenterService`、`AuditLogEventListener` 等，格式不统一：

| 字段 | 现状 | 问题 |
| :--- | :--- | :--- |
| `action` | `domain.update`、`domain.member.update_status` | 用户看到的是机器码 |
| `target` | `domain:{code}`、`domain_member:{id}` | 缺少业务名称 |
| `detail` | JSON 字符串，多为请求快照 | 非「变更后」可读摘要 |

前端 `platform/audit-logs`、`detail-audit-logs` 直接渲染原始字段，无映射层。

角色 `replaceRolePermissions` **未写审计**，无法满足「更新角色时说明增加了哪些菜单权限」。

## Goals

1. 操作日志三字段对用户可读：动作（中文）、目标（`名称-短码`）、明细（多行文本摘要）。
2. 保留 `action` 机器码，筛选/API 查询行为不破坏。
3. 角色权限变更有完整审计明细（含菜单权限增删的中文列表）。
4. 登录日志列表门户/结果等展示中文。

## Decisions

### 1. 双轨字段策略

- **库表不变**：仍使用 `audit_log.action / target / detail`（TEXT）。
- **`action`**：继续存机器码（如 `platform.role.permissions.update`）。
- **`target`**：写入时改为 `名称-短码` 或 `名称-短码` 等价可读格式。
- **`detail`**：新记录写入**多行纯文本**摘要（`\n` 换行）；旧 JSON 记录由展示层兼容解析。

API `AuditLogView` 增加可选 `actionLabel`（由后端 catalog 映射），减少前端重复维护。

### 2. 集中审计写入器

在 `uniondesk-support` 新增 `AuditLogWriter`（或 `AuditSemantics` 包）：

```
AuditActionCatalog     action 码 → 中文标签
AuditTargetFormatter   解析 domain/role/ticket 等资源 → 名称-短码
AuditDetailBuilder     构建多行明细（字段变更、权限 diff）
```

各业务 Service 调用统一入口，避免散落字符串拼接。

### 3. 动作码规范（首期）

| 机器码 | 中文标签 |
| :--- | :--- |
| `platform.domain.create` | 业务域创建 |
| `platform.domain.update` | 业务域更新 |
| `platform.domain.update_status` | 业务域启用/禁用（明细区分） |
| `platform.domain.delete` | 业务域删除 |
| `platform.role.permissions.update` | 角色权限更新 |
| `domain.member.update_status` | 域成员状态变更 |

> 将现有 `domain.*` 码统一加 `platform.` 前缀或保留旧码并在 catalog 双向映射——实施时以不破坏筛选为准。

### 4. 目标格式

- **业务域**：`{domain.name}-{domain.code}`（示例：`演示域-demo`）
- **角色**：`{role.name}-{role.code}`（平台角色）
- **域成员**：`{staffDisplayName}-{loginName}` 或 `成员-{id}` 兜底

### 5. 明细示例（角色权限更新）

```
角色：超级管理员（super_admin）
业务域：演示域（demo）

新增菜单权限：
- 业务域管理 / 业务域列表
- 业务域管理 / 业务域控制台 / 操作日志

移除菜单权限：
- 系统设置 / 菜单管理
```

菜单名称从 `iam_admin_menu` 解析；权限码通过现有 `permission-code-labels` 同源 catalog（后端 Java 侧维护镜像或查库）。

### 6. 登录日志展示
```

Full source: openspec/changes/audit-log-semantics/design.md

## openspec/changes/audit-log-semantics/tasks.md

- Source: openspec/changes/audit-log-semantics/tasks.md
- Lines: 1-25
- SHA256: 9c65bf04baf08ec78b9a136b32da18acb34f70cca4bd532bb7f1c32867f2676f

```md
## 1. 审计语义基础设施

- [ ] 1.1 新增 `AuditActionCatalog`：`action` 机器码 → 中文 `actionLabel`
- [ ] 1.2 新增 `AuditTargetFormatter`：业务域/角色/成员等资源 → `名称-短码`
- [ ] 1.3 新增 `AuditDetailBuilder`：字段变更与权限 diff 多行文本（`\n`）
- [ ] 1.4 新增 `AuditLogWriter` 统一写入 `uniondesk-support`，替换各模块散落 `recordAudit` 字符串拼接（首期迁移平台域 + IAM）

## 2. 后端写入点改造

- [ ] 2.1 `DomainService`：创建/更新/删除/状态变更 — 动作语义化、目标 `名称-短码`、明细记录变更后摘要（含启用/禁用区分）
- [ ] 2.2 `AdminMenuService.replaceRolePermissions`：新增审计；对比前后菜单/按钮，明细列出新增/移除权限（中文菜单路径）
- [ ] 2.3 `AuditLogEventListener`（域成员状态）：目标与明细语义化
- [ ] 2.4 `AuditLogView` DTO 增加 `actionLabel`；列表 API 填充

## 3. 前端展示

- [ ] 3.1 `platform/audit-logs`：动作列显示 `actionLabel`（无则降级 `action`）；明细 `pre-wrap`；目标直接展示
- [ ] 3.2 `detail-audit-logs`：同上
- [ ] 3.3 `detail-login-logs` + 平台登录 Tab：门户类型、登录结果中文标签
- [ ] 3.4 动作筛选下拉改为 catalog（code 提交、label 展示）

## 4. 验收

- [ ] 4.1 单测：`AuditTargetFormatter`、`AuditDetailBuilder`（角色权限 diff）、`DomainService` 审计 payload
- [ ] 4.2 手工：更新业务域 → 日志动作/目标/明细可读；更新角色权限 → 明细含增删菜单中文列表；旧 JSON 明细降级展示正常
```

## openspec/changes/audit-log-semantics/specs/audit-log-semantics/spec.md

- Source: openspec/changes/audit-log-semantics/specs/audit-log-semantics/spec.md
- Lines: 1-57
- SHA256: 44e782083b43f19f0cb42def5b187b210ac8f024e42d43d864441e157085e101

```md
## ADDED Requirements

### Requirement: 操作日志动作中文展示

系统 MUST 为审计记录保留机器码 `action` 供筛选，并向 UI 提供对应中文 `actionLabel`。

#### Scenario: 业务域更新动作展示

- **WHEN** 审计记录 `action=platform.domain.update`（或兼容旧码 `domain.update`）
- **THEN** 列表展示动作「业务域更新」；筛选仍可按机器码查询

#### Scenario: 业务域状态变更动作展示

- **WHEN** 审计记录为启用或禁用业务域
- **THEN** 动作展示为「业务域启用」或「业务域禁用」（明细中说明变更前后状态）

### Requirement: 操作日志目标可读格式

写入审计时，目标字段 MUST 使用 `资源名称-短码` 格式，而非 `type:id` 技术格式。

#### Scenario: 业务域目标格式

- **WHEN** 对业务域 `name=演示域`、`code=demo` 执行更新
- **THEN** `target` 为 `演示域-demo`

### Requirement: 操作日志明细多行语义摘要

审计 `detail` MUST 以操作完成后的可读多行文本记录关键变更，支持换行展示。

#### Scenario: 角色权限更新明细

- **WHEN** 管理员更新平台角色菜单权限，新增与移除若干菜单/按钮
- **THEN** 写入审计明细，包含角色标识、新增权限列表（中文菜单路径）、移除权限列表（中文菜单路径），每条占一行或一段

#### Scenario: 业务域创建与删除

- **WHEN** 创建或删除业务域成功
- **THEN** 动作分别为「业务域创建」「业务域删除」；目标为 `名称-短码`；明细含操作完成后域名称与短码

#### Scenario: 旧动作码筛选兼容

- **WHEN** 历史记录 `action=domain.update` 或新记录 `action=platform.domain.update`
- **THEN** 均展示「业务域更新」；按机器码筛选时可命中对应码或 catalog alias 聚合

#### Scenario: 历史 JSON 明细兼容

- **WHEN** 列表展示 `detail` 以 `{` 开头的旧 JSON 记录
- **THEN** UI 降级为格式化 JSON 或简要摘要，不报错

### Requirement: 登录日志字段中文展示

登录日志列表 MUST 将门户类型、登录结果等技术值映射为中文标签展示。

#### Scenario: 登录结果展示

- **WHEN** `result=success` 或 `result=failure`
- **THEN** 列表分别展示「成功」「失败」（或项目统一文案）
```

