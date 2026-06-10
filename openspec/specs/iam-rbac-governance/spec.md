## ADDED Requirements

### Requirement: 双表 RBAC 分工

系统 MUST 保持 `iam_permission` 与 `iam_admin_menu` 分离：`iam_permission` 存储权限定义与 scope；`iam_admin_menu.button` 通过 `permission_code` 挂载授权勾选点；MUST NOT 将二者合并为单表。

#### Scenario: 新权限登记走双表

- **WHEN** 开发者登记新权限
- **THEN** MUST 同时维护 `iam_permission` 行与 `iam_admin_menu` button 行（同 code）
- **AND** `iam_role_permission` MUST 由角色保存流程物化，非手工 Flyway 直写

### Requirement: 权限码单一来源（开发态）

`PermissionCodes.java` 与前端 `platform-domain-permissions.ts` MUST NOT 保留已迁移权限的别名；引用 MUST 使用正式常量名。

#### Scenario: 清理别名后构建通过

- **WHEN** 移除全部 `@Deprecated` 与前端别名层
- **THEN** Java 测试与 `tsc --noEmit` MUST 通过
- **AND** 域详情组件 MUST 从 `platform-domain-permissions.ts` 直接导入正式常量

### Requirement: 前端权限引用规范

域模块权限常量 MUST 集中在 `platform-domain-permissions.ts`；`detail-shared.ts` MUST NOT 再 export 权限别名；`hasPermission` / `AuthGuarded` MUST 使用 snapshot.actions 中的正式码。

#### Scenario: detail-shared 无权限别名

- **WHEN** 完成前端清理
- **THEN** `detail-shared.ts` 仅保留 tab 类型与工具函数
- **AND** grep `DOMAIN_.*_PERMISSION` 在域模块中 MUST 为零匹配

### Requirement: 登记流程文档

项目 MUST 提供双表登记 checklist，覆盖 PermissionCodes → Catalog → Flyway(permission+button) → Controller → 前端常量 → 验证。

#### Scenario: checklist 指导 S2 新 Story

- **WHEN** 开发者打开 `permission-registration-checklist.md`
- **THEN** 文档 MUST 包含双表四步与域详情 tab 附加项
- **AND** MUST 标明禁止直写 `iam_role_permission`
