## Why

权限登记存在多处副本（PermissionCodes 别名、前端 `detail-shared` 别名层、`permission-code-labels` 旧码），易导致漂移。架构上采用 **iam_permission + iam_admin_menu 双表分工** 已足够，无需合并表或新增 Registry；需在开发态收敛到正式权限码并文档化登记流程。

## What Changes

- **双表架构定稿**：iam_permission（能力定义）+ iam_admin_menu.button（授权 UI 挂载）+ iam_role_permission（保存角色时物化）
- **后端清理**：删除 `PermissionCodes` @Deprecated 别名；`AdminPermissionCatalog` 去除 `DOMAIN_ADMIN_READ` 等旧项
- **前端清理**（基于 UnionDeskAdminWeb 审计）：
  - 删除 `platform-domain-permissions.ts` deprecated re-export
  - 删除 `detail-shared.ts` 权限别名层；组件直引正式常量
  - 删除 `detail-logs.tsx` shim
  - 清理 `permission-code-labels.ts` 旧码
- **文档**：`permission-registration-checklist.md`、`iam-rbac-dual-table-architecture.md`

## Capabilities

### New Capabilities

- `iam-rbac-governance` — 双表 RBAC 治理 + 开发态权限码清理

### Modified Capabilities

- （无）

## Impact

- 后端：`PermissionCodes.java`、`AdminPermissionCatalog.java`、相关测试
- 前端：域模块 10+ 组件、`permission-code-labels.ts`
- 文档：`docs/architecture/`
- **无新表、无新 API、无新 Flyway（默认）**

## Non-Goals

- 合并 iam_permission 与 iam_admin_menu
- DomainControlFeatureRegistry
- menu 驱动域详情侧栏
- CatalogConsistencyTest（后期可选）
