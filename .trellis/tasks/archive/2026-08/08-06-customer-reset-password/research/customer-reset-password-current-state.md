# Research: 客户列表页 / 客户账号 / 改密能力 / 登录流程现状（重置客户密码 + 首次登录强制改密 前置调研）

- **Query**: 调研"重置客户密码 + 首次登录强制改密"功能涉及的现有代码现状（只读，file:line 锚点）
- **Scope**: internal（仓库代码 + 迁移脚本 + 文档）
- **Date**: 2026-08-06

---

## 1. 客户列表页（管理端前端）

### 页面文件
| 文件 | 说明 |
|---|---|
| `UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/platform/domains/detail/components/detail-customers.tsx` | 平台「域详情 → 客户 Tab」主组件（`DetailCustomers`，843 行），任务 PRD 所指列表页 |
| `UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx` | 业务控制台客户列表（复用同一组 API client 函数） |

### 列表表格列定义（detail-customers.tsx:631-677）
- 展示名 `display_name`、登录名 `login_name`、手机 `phone`、邮箱 `email`、状态 Tag（active/disabled）、来源 `source`、创建时间 `created_at`
- **操作列（:652-676）**：`查看`（打开 `CustomerViewModal`，:659）、`禁用`（ConfirmPopover，:662-669）、`启用`（:670-672）——**无重置密码、无删除**

### 工具栏按钮（:714-745）
- 添加客户（:717）、批量添加员工（:718）— 权限 `PLATFORM_DOMAIN_CONTROL_CUSTOMER_CREATE`
- 批量禁用（:726）、批量启用（:736）— 权限 `PLATFORM_DOMAIN_CONTROL_CUSTOMER_UPDATE_STATUS`
- 状态筛选 Select + 关键字搜索（:685-711）

### 调用的 API client（`UnionDeskWeb/packages/shared/src/api.ts`）
| 函数 | 行号 | HTTP |
|---|---|---|
| `fetchP0DomainCustomersPage` | api.ts:1254 | `GET /admin/domains/{domainId}/customers` |
| `fetchDomainCustomer` | api.ts:1316 | `GET .../customers/{customerId}` |
| `createDomainCustomerManual` | api.ts:1327 | `POST .../customers/manual` |
| `createDomainCustomersFromStaff` | api.ts:1339 | `POST .../customers/from-staff` |
| `updateDomainCustomerStatus` | api.ts:1361 | `PATCH .../customers/{customerId}/status` |

- 类型 `P0DomainCustomer`（shared 包），`normalizeP0DomainCustomer` api.ts:1282-1313 —— **无 must_change_password / password 相关字段**
- 权限常量：`src/pages/platform/domains/platform-domain-permissions`（`PLATFORM_DOMAIN_CONTROL_CUSTOMER_READ/CREATE/UPDATE_STATUS`）

---

## 2. 客户账号后端（存储表 / 实体 / Mapper / Repository）

### 表结构 `customer_account`（`UnionDesk/uniondesk-app/src/main/resources/db/migration/current/V202605200002__rebaseline_current_schema.sql:269-289`）
```sql
id, subject_id, login_name, display_name, avatar_url, phone, email,
password_hash varchar(255) NOT NULL,
must_change_password tinyint NOT NULL DEFAULT '0',   -- 已存在！
status, source,
auth_version int unsigned NOT NULL DEFAULT '1',       -- 已存在！
password_changed_at datetime(3) DEFAULT NULL,          -- 已存在！
created_at, updated_at
```
- `staff_account` 同构（同文件 :596-614：`must_change_password`、`auth_version`、`password_changed_at` 齐全）

### Entity
- `UnionDesk/uniondesk-iam/src/main/java/com/uniondesk/iam/entity/CustomerAccountPo.java`
  - 有 `passwordHash`（:12）、`mustChangePassword`（:13）；**无** `authVersion`、`passwordChangedAt` 字段
- `StaffAccountPo.java`（uniondesk-iam）：有 `authVersion`（:21）、`passwordHash`（:22）；**无** `mustChangePassword` 字段

### Mapper / Repository / Service
- Mapper 接口：`uniondesk-iam/.../mapper/CustomerAccountMapper.java`
- Mapper XML：`uniondesk-iam/src/main/resources/mapper/iam/CustomerAccountMapper.xml`
  - `selectById`（:16-21）只查 `id, subject_id, username, nickname, phone, email, status` —— **不读 password_hash / must_change_password**
  - `insert`（:27-34）写入 `password_hash, must_change_password, auth_version=1`
- Repository：`uniondesk-iam/.../repository/CustomerAccountRepository.java`
- Service：`uniondesk-iam/.../core/CustomerAccountService.java`
  - `create()`（:38-64）：`passwordEncoder.encode`（:53）+ `po.setMustChangePassword(command.mustChangePassword() ? 1 : 0)`（:54）
  - `CreateCustomerCommand(..., password, boolean mustChangePassword)`（:102-109）
- 注意：`uniondesk-ticket` 模块有**另一套** `CustomerAccountPo/Mapper/Repository`（ticket 域视图，与 iam 独立）

### 关键结论
- **`getMustChangePassword` 全仓 Java 无任何调用点**（只有 getter/setter 与 create 写入）——字段只写不读。

---

## 3. 现有改密 / 重置密码能力

### 3.1 自服务（Auth 模块，`uniondesk-app/.../auth`）
| 端点 | Controller | Service |
|---|---|---|
| `POST /api/v1/auth/password/reset-request`（忘记密码，发 token） | `AuthController.java:70-76` | `AuthService.requestPasswordReset` :413-441（30 分钟 token；customer 走 inbox 通道，staff 走 email） |
| `POST /api/v1/auth/password/reset`（token 确认新密码） | `AuthController.java:78-82` | `AuthService.resetPassword` :444-451 |
| `PUT /api/v1/auth/password`（登录后改密，需旧密码） | `AuthController.java:84-88` | `AuthService.changePassword` :454-463 |

- `changePassword` 按 clientCode 区分账号类型（:455）：`ud-customer-web` → customer，否则 staff；校验旧密码（:458）→ `loginAccountService.updatePassword`（:461）→ `authVersionService.incrementVersion`（:462）
- `resetPassword`：`consumePasswordResetToken`（:445）→ `updatePassword`（:449）→ `incrementVersion`（:450）

### 3.2 管理员重置
- **平台用户（staff）**：无专用接口。前端「重置密码」按钮复用**更新员工接口**：
  - `UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/platform/user/components/reset-password-modal.tsx:57` → `fetchUpdatePlatformUser(user.id, { password })`
  - `apps/UnionDeskAdminWeb/src/api/platform/iam.ts:104-110` → `PUT v1/admin/staff/{id}`
  - 后端 `uniondesk-iam/.../web/StaffController.java:98-116 updateStaff`（`UpdateStaffRequest.password`）→ `StaffAccountService.update` → `authVersionService.incrementVersion`（:114）
- **客户（customer）**：**无任何管理员重置接口**。`uniondesk-domain/.../web/DomainCustomerController.java`（`/api/v1/admin/domains/{domainId}`）只有 5 个端点：`GET /customers`（:42）、`GET /customers/{customerId}`（:31）、`POST /customers`（:56）、`POST /customers/manual`（:68）、`POST /customers/from-staff`（:80）、`PUT/PATCH /customers/{customerId}/status`（:92）——无 password 端点

### 3.3 `platform.user.reset_password` 权限的落地情况
- 常量：`PermissionCodes.java:60`；目录：`AdminPermissionCatalog.java:103`
- 迁移：`uniondesk-app/.../migration/archive/V202605150002__platform_user_reset_password_permission.sql`（插入权限 + `/platform/user` 下「重置密码」按钮菜单，http_method/path_pattern 为 NULL）；`V202605190002__platform_user_reset_password_menu_backfill.sql`（super_admin/platform_admin 角色回填）
- **后端没有任何 `@RequirePermission(PLATFORM_USER_RESET_PASSWORD)` 的 Controller 方法**（grep 仅命中目录/常量/测试）。该权限目前只被前端按钮用作 UI 权限：`apps/UnionDeskAdminWeb/src/pages/platform/user/utils.ts:32-64`（`PLATFORM_USER_ROW_ACTIONS` → `auth: "platform.user.reset_password"`，:64）
- 即：**"重置平台用户密码"是一个挂名权限 + 前端按钮，实质行为 = 复用 PUT /admin/staff/{id}**

### 3.4 改密 SQL 细节（重要）
`uniondesk-iam/src/main/resources/mapper/auth/LoginAccountMapper.xml`
- `updateStaffPassword`（:102-108）：只更新 `password_hash, password_changed_at, updated_at`
- `updateCustomerPassword`（:110-116）：同样只更新 `password_hash, password_changed_at, updated_at`
- **两条语句都不清 `must_change_password`** —— 即使将来登录强制改密后走 `PUT /api/v1/auth/password`，标志位也不会自动归零

---

## 4. 登录与密码校验流程

### 登录入口
- `AuthController.java:54-60` `POST /api/v1/auth/login` → `AuthService.login`（:106-219）
  - staff 分支：`findByIdentifier(..., "staff")`（:145）→ status 校验（:150）→ offboarded 校验（:155）→ `passwordEncoder.matches`（:160-164）→ 建 session（:178-189）→ 发 JWT（:177, :190）
- 客户分支 `loginCustomer`（:232-358）：`findByIdentifier(..., "customer")`（:240）→ status（:244）→ `passwordEncoder.matches`（:249-253）→ 建 session（:272-283）→ JWT（:284）→ 新 IP 风险通知（:299-341）
- **登录流程不检查 `must_change_password`**（两分支均无）

### 密码加密
- Spring `PasswordEncoder`（BCrypt）：`AuthService.java:52`；校验 `matches`（:160/:249/:458/:568）；编码 `CustomerAccountService.java:53`、`LoginAccountService.updatePassword`（:86-93）

### JWT 与 auth_version
- `JwtTokenService.java:68-81`：claims = `iss, sub, uid, sid, role, cid, typ, bd, iat, exp` —— **JWT 中不含 auth_version**
- `JwtAuthenticationFilter.java:54-102`：每请求解析 token（:71）→ clientCode 匹配（:72）→ **`loginSessionService.validateAndTouch(sessionId)`**（:75）→ 通过才建立认证；有 token 但校验失败 → 401（:92-95）
- `AuthVersionService.java`：
  - `incrementVersion`（:42-61）：SQL 递增 `auth_version` + **吊销该用户全部活跃会话**（`auth_login_session.session_status='revoked'`，:102-112）
  - `validateVersion`（:66-69）：**全仓无调用**（JWT 无版本 claim，过滤器不比对）
- 因此「改密/重置后旧凭证失效」的实际机制 = **会话吊销**（旧 access token 因 session 被吊销 → 每请求 validateAndTouch 失败 → 401；refresh 也被拒，`AuthService.refreshToken` :472 `validateAndTouch`），而非 JWT claim 比对

---

## 5. 是否已有「首次登录强制改密」机制

**结论：无完整机制，仅有字段与写入点，无读取/强制/回写逻辑。**

| 层面 | 现状 |
|---|---|
| 字段 | `customer_account.must_change_password`、`staff_account.must_change_password` 均存在（default 0） |
| 写入点 | 手动添加客户：`DomainCustomerService.addCustomerManual`（uniondesk-domain `core/DomainCustomerService.java:68-86`）——**随机 UUID 密码**（:83）+ `mustChangePassword=true`（:84）；员工转客户：`resolveOrCreateCustomerAccountFromStaff`（:207-219，同 :216-217）；自助注册：`AuthService.register`（:377-383，false） |
| 读取点 | **无**（`getMustChangePassword` 无调用；登录两分支不检查；`CustomerAccountMapper.selectById` 不查该列） |
| 回写点 | **无**（`LoginAccountMapper.xml` 两条 updatePassword 不清 0） |
| 响应字段 | `AuthDtos.LoginResponse`（`AuthDtos.java:85-124`）无 must_change 字段（只有 riskLoginNotified） |
| 前端 | `UnionDeskWeb` 全仓 grep `must_change_password / mustChangePassword / force_change_password / require_change_password / 强制改密 / 首次登录` 均无代码命中 |
| 文档佐证 | `docs/qa/implementation-traceability.md:50`：「客户手动添加后初始密码 US-S1-06 随机密码 + must_change_password，无管理端通知/重置 UI → 产品 backlog 后续 Story」 |

---

## 6. 前端登录流程（是否检查强制改密）

- **管理端** `apps/UnionDeskAdminWeb/src/pages/login/`：`index.tsx`（登录布局 + 模式切换）、`components/password-login.tsx`、`forgot-password.tsx`、`register-password.tsx`、`code-login.tsx`
  - 登录提交 → `store/auth.ts` `login()`（:86-163）：`fetchLogin` → 存 token/user/domains → `fetchPermissionSnapshot` → 进入首页 —— **无强制改密判断**
- **客户端** `apps/UnionDeskCustomerWeb/src/pages/login/index.tsx` + `LoginCaptcha.tsx`；登录态 store 在 `packages/shared/src/customer-portal-live.ts` / `customer-portal.ts`（`loginCustomerLive` :115）；`pages/me/index.tsx` 仅有业务域/通知/退出登录，**无改密入口**
- **401 处理**：`packages/shared/src/api.ts:172`（宿主注册跳登录回调）、:261（`status === 401` → 触发）
- **改密页**：两个 app 均**无**独立改密页面；改密能力仅有 AdminWeb 的 `reset-password-modal.tsx`（平台用户）与登录页 `forgot-password.tsx`（自助找回）

---

## 结论：实现「重置客户密码 + 首次登录强制改密」需触及的层（仅列现状）

### 前端层（UnionDeskWeb）
1. `apps/UnionDeskAdminWeb/src/pages/platform/domains/detail/components/detail-customers.tsx` —— 客户列表页，当前操作列只有查看/禁用/启用，需加「重置密码」行内按钮（现状无任何密码操作）
2. `packages/shared/src/api.ts` —— 客户 API client 集中地（现状 5 个函数，无重置密码函数）
3. `packages/shared/src/types.ts` —— `P0DomainCustomer` 类型（无密码相关字段）
4. 登录后跳转逻辑：`apps/UnionDeskAdminWeb/src/store/auth.ts` 与客户侧 `packages/shared/src/customer-portal-live.ts`（现状均无强制改密分支；后端响应也无该字段可读）

### 后端层（UnionDesk）
5. **接口**：`uniondesk-domain/.../web/DomainCustomerController.java` —— 客户管理端点宿主，现状无重置密码端点（staff 侧先例是复用 `PUT /admin/staff/{id}`，但客户域无等价物）
6. **服务**：`uniondesk-iam/.../core/CustomerAccountService.java` —— 无改密/重置方法（`LoginAccountService.updatePassword` :86-93 已存在改密底层能力，但不清 `must_change_password`）
7. **SQL**：`uniondesk-iam/.../resources/mapper/auth/LoginAccountMapper.xml:110-116` —— `updateCustomerPassword` 需在重置场景置 `must_change_password=1`、改密场景置 0（现状都不处理）
8. **登录强制**：`uniondesk-app/.../auth/core/AuthService.java` `login`（:106）/`loginCustomer`（:232）不检查标志；`AuthDtos.LoginResponse`（AuthDtos.java:85）无该字段；`CustomerAccountMapper.xml:16-21` selectById 不查该列
9. **实体**：`CustomerAccountPo.java`（无 authVersion/passwordChangedAt 字段，若需展示/回写 password_changed_at 需补）
10. **权限**：客户重置权限码不存在（现有 `platform.user.reset_password` 仅限 platform 用户且后端无绑定方法）；客户相关权限为 `PLATFORM_DOMAIN_CONTROL_CUSTOMER_*` 系列（前端 `platform-domain-permissions`）
11. **迁移**：`customer_account.must_change_password` 等字段已存在，无需加列；但 `V20260719100446__drop_legacy_identity_tables.sql`（current 目录）涉及历史表清理，改表时需注意 current/archive 双份迁移文件结构

### 已有资产（可直接复用）
- `customer_account.must_change_password / auth_version / password_changed_at` 三字段齐全
- 手动添加客户/员工转客户已在创建时写 `must_change_password=1` + 随机密码
- `AuthVersionService.incrementVersion` 已提供「改密即吊销会话」能力（改密/重置两处已调用）
- 前端 ConfirmPopover/AuthGuarded 组件模式、平台用户重置密码弹窗（`reset-password-modal.tsx`）可作 UI 参照

## Caveats / 未验证
- `StaffAccountService.update`（staff 密码更新路径）内部对 `must_change_password` 列的行为未逐行确认（`StaffAccountPo` 无该字段，推测 update 不触碰该列；StaffAccountMapper.xml 需复核）
- 客户登录响应（`LoginResponse`）中 `riskLoginNotified` 已占用末位参数，加字段需同步改 compact 构造器（AuthDtos.java:101-124）
- `uniondesk-ticket` 模块存在第二套 CustomerAccountPo/Mapper，若客户账号展示走 ticket 侧需一并确认
