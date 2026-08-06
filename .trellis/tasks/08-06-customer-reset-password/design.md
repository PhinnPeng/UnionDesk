# 设计：重置客户密码 + 首次登录强制改密

## 1. 目标与边界

- 业务控制台「客户管理」列表页（`apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx`）操作列新增「重置密码」。
- 重置密码由后端生成随机密码，弹窗一次性展示。
- 被重置客户下次登录：后端登录响应携带 `must_change_password=true`，客户 Web 强制跳转改密页，改密成功前不可进入业务页面（前端强制，后端不拦截业务 API）。
- 列表所有列（含操作列）居中。
- 平台域详情客户 Tab（detail-customers.tsx）本次不动。

## 2. 后端设计（Spring Boot 多模块）

### 2.1 新权限码 `domain.customer.reset_password`

| 项 | 值 |
|---|---|
| 常量 | `PermissionCodes.DOMAIN_CUSTOMER_RESET_PASSWORD`（uniondesk-iam `iam/core/PermissionCodes.java`） |
| DB 迁移 | `uniondesk-app/src/main/resources/db/migration/current/V20260806XXXX__domain_customer_reset_password_permission.sql` |
| iam_permission 行 | code=`domain.customer.reset_password`，scope=`domain`，resource=`domain.customer`，action=`reset_password`，method=`PUT`，path=`/api/v1/admin/domains/*/customers/*/password` |
| 角色回填 | `domain_admin`、`super_admin`（沿用 `archive/V202605031203__p0_domain_customer_permissions.sql` 的 `INSERT ... ON DUPLICATE KEY UPDATE` 模式；agent 不加） |

### 2.2 重置接口

`uniondesk-domain/.../web/DomainCustomerController.java` 新增：

```
PUT /api/v1/admin/domains/{domainId}/customers/{customerId}/password
@RequirePermission(PermissionCodes.DOMAIN_CUSTOMER_RESET_PASSWORD)
响应: { "password": "<随机密码>", "must_change_password": true }
```

- `DomainCustomerService.resetCustomerPassword(domainId, customerId)`（uniondesk-domain `core/DomainCustomerService.java`）：
  1. 校验客户属于该域（复用 `loadCustomerAccount` / 现有归属校验）
  2. 生成随机密码（`SecureRandom`，12 位字母+数字，避免易混符号）
  3. 调 `CustomerAccountService.resetPassword(accountId, rawPassword)`（uniondesk-iam `iam/core/CustomerAccountService.java` 新增方法：`passwordEncoder.encode` + 落库 `password_hash`、`must_change_password=1`、`password_changed_at`）
  4. 调 `AuthVersionService.incrementVersion(accountId, "customer")`（uniondesk-app `auth/core/AuthVersionService.java`，已有能力：递增版本 + 吊销全部活跃会话）
  5. 返回一次性密码

### 2.3 改密 SQL（must_change_password 读写打通）

`uniondesk-iam/src/main/resources/mapper/auth/LoginAccountMapper.xml`：

- `updateCustomerPassword` 增加 `must_change_password = #{mustChangePassword}`（:110-116 现状只更新 password_hash / password_changed_at）
- `LoginAccountMapper` 接口与 `LoginAccountRepository.updateCustomerPassword` 同步加参数
- `LoginAccountService`（uniondesk-iam `auth/core/LoginAccountService.java`）：
  - 现有 `updatePassword(accountType, id, rawPassword)` → 客户分支传 `mustChangePassword=false`（自服务改密/自助重置成功即清除标志，同时满足"改密后标志归零"）
  - 新增 `resetPassword(accountId, rawPassword)` → 客户分支传 `mustChangePassword=true`（管理员重置置位）
- staff 路径（`updateStaffPassword`）保持不变

### 2.4 登录响应携带标志

- `LoginAccountPo`（uniondesk-iam `auth/entity/LoginAccountPo.java`）新增 `mustChangePassword` 字段
- `LoginAccountMapper.xml` 中客户查询语句（findCustomerByIdentifier / findCustomerById）select 增加 `must_change_password`
- `LoginAccountService.LoginAccount` record 增加字段
- `AuthService.loginCustomer`（uniondesk-app `auth/core/AuthService.java:232`）登录成功构造响应时传入
- `AuthDtos.LoginResponse`（uniondesk-app `auth/web/AuthDtos.java:85-124`）末尾追加 `Boolean mustChangePassword`（JsonAlias `must_change_password`），同步补 compact 构造器重载（沿用 `riskLoginNotified` 的 null 默认模式）
- staff 分支（`login` :106）传 null/false，行为不变

### 2.5 改动文件清单（后端）

| 文件 | 改动 |
|---|---|
| `uniondesk-iam/.../iam/core/PermissionCodes.java` | + 常量 |
| `uniondesk-app/src/main/resources/db/migration/current/V20260806XXXX__domain_customer_reset_password_permission.sql` | 新增迁移 |
| `uniondesk-domain/.../web/DomainCustomerController.java` | + 重置端点 |
| `uniondesk-domain/.../core/DomainCustomerService.java` | + resetCustomerPassword |
| `uniondesk-domain/.../web/DomainCustomerDtos.java` | + 响应 DTO |
| `uniondesk-iam/.../iam/core/CustomerAccountService.java` | + resetPassword |
| `uniondesk-iam/.../auth/core/LoginAccountService.java` | + resetPassword、updatePassword 传标志 |
| `uniondesk-iam/.../auth/repository/LoginAccountRepository.java` | + updateCustomerPassword 参数 |
| `uniondesk-iam/.../auth/mapper/LoginAccountMapper.java` | + updateCustomerPassword 参数 |
| `uniondesk-iam/.../resources/mapper/auth/LoginAccountMapper.xml` | updateCustomerPassword + 客户查询 select 加列 |
| `uniondesk-iam/.../auth/entity/LoginAccountPo.java` | + 字段 |
| `uniondesk-app/.../auth/core/AuthService.java` | loginCustomer 传标志 |
| `uniondesk-app/.../auth/web/AuthDtos.java` | LoginResponse + 字段 |

## 3. 前端设计

### 3.1 管理端（UnionDeskAdminWeb）

- `packages/shared/src/api.ts`：+ `resetDomainCustomerPassword(domainId, customerId)` → `PUT /admin/domains/{domainId}/customers/{customerId}/password`
- `packages/shared/src/types.ts`：+ `ResetDomainCustomerPasswordResponse { password: string }`
- `src/pages/domain/domain-permissions.ts`：+ `DOMAIN_CUSTOMER_RESET_PASSWORD`
- `src/pages/domain/customers/index.tsx`：
  - 操作列新增「重置密码」按钮（`AuthGuarded auth={DOMAIN_CUSTOMER_RESET_PASSWORD}` 包裹，ConfirmPopover 二次确认 → 调接口 → 弹出一次性密码 Modal（`Typography.Text copyable` 展示新密码 + 提示"客户首次登录将强制修改密码"））
  - **列宽与排版调整**（DOM 实测现状：展示名/邮箱各 739px 弹性失衡、创建时间渲染原始 ISO）：展示名 180 / 登录名 140 / 手机 130 / 邮箱 220 / 状态 90 / 来源 110 / 创建时间 150 / 操作 190，全部 `align: "center"`（含操作列与选择列保持默认），`scroll={{ x }}` 与新总宽（约 1240）匹配，`scroll={{ x: 1280 }}`
  - 创建时间渲染统一格式化 `YYYY-MM-DD HH:mm`（引入工具函数，dayjs 已在依赖中）
- 不改 detail-customers.tsx（平台域详情）

### 3.2 客户 Web（UnionDeskCustomerWeb + shared）

- `packages/shared/src/types.ts`：`LoginResponse` + `mustChangePassword?: boolean`；`AuthSessionState` + `mustChangePassword?: boolean`
- `packages/shared/src/api.ts`：
  - `login()`（:444）saveAuthSession 时持久化 `mustChangePassword`
  - + `changePassword(oldPassword, newPassword)` → `PUT /auth/password`（现状无此函数，`ChangePasswordRequest` DTO 后端已有）
  - + `updateStoredMustChangePassword(false)`（storage 侧 helper，改密成功后清除标志；实现在 customer-portal-live 或 storage）
- `packages/shared/src/customer-portal-live.ts`：`loginCustomerLive` 返回体加 `mustChangePassword`；`restoreCustomerPortalLive` 从 session 恢复
- `apps/UnionDeskCustomerWeb/src/pages/change-password/index.tsx`：新改密页（当前密码 + 新密码 + 确认新密码；成功后清除标志 → `navigate("/home")`；样式对齐现有登录页）
- `apps/UnionDeskCustomerWeb/src/App.tsx`：
  - 新增路由 `/change-password`（RequireSession 内、AppShell 外，独立居中页）
  - 路由守卫：`loadAuthSession().mustChangePassword === true` 且目标路径非 `/change-password` → `<Navigate to="/change-password" replace />`（覆盖 RequireSession 子路由与 LandingRedirect；刷新后从 storage 恢复，标志持久）

### 3.3 改动文件清单（前端）

| 文件 | 改动 |
|---|---|
| `packages/shared/src/types.ts` | LoginResponse / AuthSessionState + 字段；+ 响应类型 |
| `packages/shared/src/api.ts` | + resetDomainCustomerPassword、changePassword；login 持久化标志 |
| `packages/shared/src/customer-portal.ts` | （视实现）标志读写 helper |
| `packages/shared/src/customer-portal-live.ts` | loginCustomerLive/restore 返回标志 |
| `packages/shared/src/storage.ts` | 标志持久化 helper（如放此处） |
| `apps/UnionDeskAdminWeb/src/pages/domain/domain-permissions.ts` | + 常量 |
| `apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx` | 操作列 + 重置按钮/弹窗；全列居中 |
| `apps/UnionDeskCustomerWeb/src/pages/change-password/index.tsx` | 新建改密页 |
| `apps/UnionDeskCustomerWeb/src/App.tsx` | 路由 + 强制跳转守卫 |

## 4. 数据流

```
管理员点击重置 ─→ PUT /admin/domains/{d}/customers/{c}/password
              ─→ 生成随机密码 → password_hash=新, must_change_password=1, password_changed_at=now
              ─→ incrementVersion → 该客户全部会话 revoked（旧 token 立即 401）
              ─→ 响应 { password } → 弹窗一次性展示

客户登录 ─→ POST /auth/login ─→ 响应 must_change_password=true
        ─→ 客户 Web 强制跳 /change-password
        ─→ PUT /auth/password（当前密码=一次性密码, 新密码）
        ─→ password_hash=新, must_change_password=0（清除）, 会话吊销→重新登录
        ─→ 清除前端标志 → 正常进入业务页面
```

## 5. 兼容性与风险

- **旧会话**：管理员重置后该客户现有 token 因会话吊销立即失效 → 需重新登录，符合"重置即生效"预期。
- **LoginResponse 追加字段**：末尾追加 + JsonAlias，向后兼容；compact 构造器需同步补重载（`AuthDtos.java:101-124` 现有模式）。
- **JWT 不含标志**：强制改密判断仅靠登录响应 + 前端 storage 持久化；页面刷新后从 storage 恢复（`restoreCustomerPortalLive` 路径）。token 过期重新登录时后端会重新下发标志，自愈。
- **后端不拦截**：客户若绕过前端直接调业务 API 不受限（用户已确认接受）。
- **迁移**：新增迁移放 `current/`（`archive/` 只读历史）；仅插入新权限行 + 角色回填，无破坏性变更；可随时回滚（删除权限行不影响业务）。
- **Staff 侧**：不动 `staff_account.must_change_password` 相关行为（`updateStaffPassword` 不触碰该列）。
- **禁用客户**：重置不校验状态（禁用客户无法登录，下次启用后首次登录强制改密）。

## 6. 回滚

- 后端：删除迁移 + 还原代码即可；已执行的迁移不删 `current/` 历史（Flyway 版本号不可复用，新迁移另行编号）。
- 前端：还原按钮与路由守卫；标志字段为可选字段，缺失时行为与现状一致。
