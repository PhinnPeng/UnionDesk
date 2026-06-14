## 1. 审计语义基础设施

- [x] 1.1 新增 `AuditActionCatalog`：`action` 机器码 → 中文 `actionLabel`
- [x] 1.2 新增 `AuditTargetFormatter`：业务域/角色/成员等资源 → `名称-短码`
- [x] 1.3 新增 `AuditDetailBuilder`：字段变更与权限 diff 多行文本（`\n`）
- [x] 1.4 新增 `AuditLogWriter` 统一写入 `uniondesk-support`，替换各模块散落 `recordAudit` 字符串拼接（首期迁移平台域 + IAM）

## 2. 后端写入点改造

- [x] 2.1 `DomainService`：创建/更新/删除/状态变更 — 动作语义化、目标 `名称-短码`、明细记录变更后摘要（含启用/禁用区分）
- [x] 2.2 `AdminMenuService.replaceRolePermissions`：新增审计；对比前后菜单/按钮，明细列出新增/移除权限（中文菜单路径）
- [x] 2.3 `AuditLogEventListener`（域成员状态）：目标与明细语义化
- [x] 2.4 `AuditLogView` DTO 增加 `actionLabel`；列表 API 填充

## 3. 前端展示

- [x] 3.1 `platform/audit-logs`：动作列显示 `actionLabel`（无则降级 `action`）；明细 `pre-wrap`；目标直接展示
- [x] 3.2 `detail-audit-logs`：同上
- [x] 3.3 `detail-login-logs` + 平台登录 Tab：门户类型、登录结果中文标签
- [x] 3.4 动作筛选下拉改为 catalog（code 提交、label 展示）

## 4. 验收

- [x] 4.1 单测：`AuditTargetFormatter`、`AuditDetailBuilder`（角色权限 diff）、`DomainService` 审计 payload
- [x] 4.2 手工：更新业务域 → 日志动作/目标/明细可读；更新角色权限 → 明细含增删菜单中文列表；旧 JSON 明细降级展示正常
