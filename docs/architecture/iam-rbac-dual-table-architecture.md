# IAM RBAC 双表架构说明

## 核心结论

- **`iam_permission`**：权限能力定义（What）
- **`iam_admin_menu`**：导航结构 + button 授权挂载点（Where to grant）
- **二者不合并**；通过 `permission_code` 字符串关联

## 数据流

```
开发者登记
  PermissionCodes + AdminPermissionCatalog
       ↓ Flyway
  iam_permission + iam_admin_menu(button)
       ↓ 管理员勾选角色
  iam_admin_role_menu_relation
       ↓ 保存角色（replaceRolePermissionRows）
  iam_role_permission（物化）
       ↓
  API: RequirePermissionInterceptor → hasAnyPermission
  FE:  permission-snapshot.actions → hasPermission()
```

## 前端特例

域详情页（`/platform/domains/detail/:id`）侧栏 tab 列表**硬编码**在 `detail-sider.tsx`，读权限门控认 `snapshot.actions`，不读 `menus`。

hidden catalog（`PLATFORM-DOMAIN-CONTROL-*`）仅服务于**角色权限配置 UI**，不驱动侧栏渲染。

## 详细设计

见 [2026-06-09-iam-rbac-optimization-design.md](../superpowers/specs/2026-06-09-iam-rbac-optimization-design.md)。
