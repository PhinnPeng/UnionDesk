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
