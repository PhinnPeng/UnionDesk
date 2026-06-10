# 权限登记 Checklist（双表方案）

新增或重命名一个权限时，按顺序打勾。代码字符串必须与 `PermissionCodes.java` 一致。

## 双表分工速记

| 表 | 管什么 |
|:---|:---|
| `iam_permission` | 权限定义（code、scope、API 路径） |
| `iam_admin_menu.button` | 角色配置 UI 里的可勾选授权点（`permission_code`） |
| `iam_admin_role_menu_relation` | 角色勾选了哪些 menu/catalog/button |
| `iam_role_permission` | **保存角色时自动同步**，勿手工 Flyway 写入 |

---

## 登记步骤

### 代码层

- [ ] **1.** `PermissionCodes.java` 增加正式常量（禁止 `@Deprecated` 别名）
- [ ] **2.** `AdminPermissionCatalog.java` 增加同码 `PermissionDefinition`（含 scope、httpMethod、pathPattern）
- [ ] **3.** Controller 方法加 `@RequirePermission(同常量)`

### 数据库层（Flyway）

- [ ] **4.** `iam_permission`：INSERT 或 UPDATE（code、name、permission_scope、path）
- [ ] **5.** `iam_admin_menu`：
  - 若需角色配置入口：hidden `catalog`（域详情下）或现有 menu 下
  - **button** 节点：`permission_code` = 同码
- [ ] **6.** `iam_admin_role_menu_relation`：为目标角色（如 super_admin）绑定 catalog + button
- [ ] **7.** **不要** 直写 `iam_role_permission`（由管理端保存角色或迁移脚本中的等价逻辑同步）

### 前端（UnionDeskAdminWeb）

- [ ] **8.** `platform-domain-permissions.ts` 增加同字符串 export（域相关权限）
- [ ] **9.** 页面/按钮：`AuthGuarded` 或 `hasPermission(正式常量)`
- [ ] **10.** 若为域详情新 tab：`detail-sider.tsx` 增加 `NAV_ITEMS` + `hasPermission` filter
- [ ] **11.** `permission-code-labels.ts` 增加中文标签（菜单管理 UI）
- [ ] **12.** **禁止** 在 `detail-shared.ts` 增加权限别名

### 验证

- [ ] **13.** 后端：`AdminPermissionCatalogTests` 更新期望
- [ ] **14.** 前端：`pnpm exec tsc --noEmit`
- [ ] **15.** **重新登录** → `GET /iam/me/permission-snapshot` 的 `actions` 含新码
- [ ] **16.** 无权限角色：侧栏/tab/按钮隐藏 + API 403

---

## 域详情 tab 附加项（如有）

| 登记项 | 位置 |
|:---|:---|
| tab key | `detail-shared.ts` → `detailTabs` |
| 侧栏显示 | `detail-sider.tsx` → `NAV_ITEMS` + filter |
| 页面门控 | `detail-*.tsx` → `AuthGuarded` |
| hidden catalog | Flyway → `PLATFORM-DOMAIN-CONTROL-*` |

---

## 命名约定

```
platform.domain.control.<feature>.<action>   # 域控制台（平台岗）
platform.log.<type>.read                     # 平台级日志
domain.member.<action>                       # 域内成员
platform.domain.list.read                    # 域列表
```

旧前缀 `domain.admin.*`、`domain.audit_log.*`、`platform.domain.customer.*` **禁止使用**。
