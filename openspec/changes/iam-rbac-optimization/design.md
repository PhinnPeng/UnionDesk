# IAM RBAC — 高层设计

定稿 Design Doc：`docs/superpowers/specs/2026-06-09-iam-rbac-optimization-design.md`

**双表方案摘要：**

- `iam_permission` = 权限能力字典
- `iam_admin_menu.button` = 角色配置勾选入口（`permission_code`）
- `iam_role_permission` = 保存角色时从 button 物化，供 API 鉴权
- 前端 snapshot.actions → `hasPermission`；域详情侧栏硬编码保留

**前端调整摘要：** 删除 `detail-shared` 权限别名层，统一 `platform-domain-permissions.ts` 正式常量。
