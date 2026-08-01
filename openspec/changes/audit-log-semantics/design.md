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

写入不变；前端增加 `portalTypeLabel`、`resultLabel` 映射（与平台页、域详情页共用常量）。

### 7. 兼容策略

- 读取 `detail` 时：若以 `{` 开头尝试 JSON 格式化降级展示；否则按纯文本 `white-space: pre-wrap` 渲染。
- 筛选「动作」仍用机器码下拉（catalog 提供 code + label）。

## Risks

- 菜单改名后历史明细不会回溯更新（接受）
- 权限 diff 需加载变更前快照，角色更新接口要读旧 permissions（已有 `replaceRolePermissions` 可扩展）

## Open Questions

- 工单类 `ticket.*` 审计是否纳入本期（建议 Non-Goal，仅预留 catalog 扩展位）
