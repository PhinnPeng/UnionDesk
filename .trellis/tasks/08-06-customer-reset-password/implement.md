# 实施计划：重置客户密码 + 首次登录强制改密

## 任务地图（含依赖顺序）

```
P1 后端权限+重置接口（独立可验）
P2 后端登录响应携带标志 + 改密 SQL 打通（依赖 P1 的 SQL 改动？否，独立；但 resetPassword 与 updatePassword 共用 SQL 改动，需先改 SQL）
P3 前端管理端：重置按钮 + 一次性密码弹窗 + 全列居中（依赖 P1 接口）
P4 前端客户 Web：改密页 + 强制跳转守卫（依赖 P2 响应）
```

实际执行顺序：P1 → P2（SQL 先行，两步骤在同一批改动内完成）→ P3 → P4。

## 实施清单

### Step 1：后端 SQL 与实体（uniondesk-iam）

1. [x] `LoginAccountMapper.xml`：`updateCustomerPassword` 增加 `must_change_password = #{mustChangePassword}`；客户查询语句（findCustomerByIdentifier / findCustomerById 的 customer 分支）select 增加 `must_change_password` 列
2. [x] `LoginAccountMapper.java` 接口：updateCustomerPassword 增加 `@Param("mustChangePassword") int mustChangePassword`
3. [x] `LoginAccountRepository.java`：updateCustomerPassword 同步参数
4. [x] `LoginAccountPo.java`：+ `mustChangePassword` 字段
5. [x] `LoginAccountService.java`：`updatePassword` 客户分支传 false；+ `resetPassword(accountId, rawPassword)` 传 true
6. [x] `CustomerAccountService.java`（iam/core）：+ `resetPassword(long accountId, String rawPassword)`（encode + 落库，走 LoginAccountService.resetPassword 或 Repository 直连，保持与现有 updatePassword 同一路径）
7. [x] `PermissionCodes.java`：+ `DOMAIN_CUSTOMER_RESET_PASSWORD = "domain.customer.reset_password"`
8. [x] 新迁移 `uniondesk-app/src/main/resources/db/migration/current/V20260806120000__domain_customer_reset_password_permission.sql、V20260806130000__domain_customer_reset_password_menu.sql`：iam_permission 插入 + domain_admin/super_admin 角色回填（沿用 V202605031203 模式）

验证：`.\mvnw.cmd -pl uniondesk-app -am compile`（或 uniondesk-iam 单模块 compile）通过；启动后 Flyway 迁移成功、权限行入库

### Step 2：后端重置接口与登录响应（uniondesk-domain / uniondesk-app）

9. [x] `DomainCustomerDtos.java`：+ `ResetCustomerPasswordResponse(password, mustChangePassword)`
10. [x] `DomainCustomerService.java`：+ `resetCustomerPassword(domainId, customerId)`（归属校验 → 随机密码 → CustomerAccountService.resetPassword → AuthVersionService.incrementVersion）
11. [x] `DomainCustomerController.java`：+ `PUT /customers/{customerId}/password`（@RequirePermission(DOMAIN_CUSTOMER_RESET_PASSWORD)）
12. [x] `AuthService.java` loginCustomer：构造 LoginResponse 时传 `account.mustChangePassword()`（LoginAccount record 需带出字段）
13. [x] `AuthDtos.java`：LoginResponse 末尾 + `Boolean mustChangePassword`（JsonAlias `must_change_password`）+ compact 构造器重载
14. [x] 随机密码生成工具（DomainCustomerService 内私有方法，SecureRandom 12 位字母数字）

验证：启动后端；curl 重置接口返回 password；用新密码登录响应含 `must_change_password: true`；改密接口 `PUT /api/v1/auth/password` 成功后再次登录标志为 false；重置后旧 token 调接口返回 401

### Step 3：管理端前端（UnionDeskAdminWeb + shared）

15. [x] `packages/shared/src/types.ts`：+ `ResetDomainCustomerPasswordResponse`；`LoginResponse`/`AuthSessionState` + `mustChangePassword?: boolean`（LoginResponse 字段可在此步一并加）
16. [x] `packages/shared/src/api.ts`：+ `resetDomainCustomerPassword(domainId, customerId)`；+ `changePassword(oldPassword, newPassword)`（PUT /auth/password）；`login()` saveAuthSession 持久化 mustChangePassword
17. [x] `apps/UnionDeskAdminWeb/src/pages/domain/domain-permissions.ts`：+ `DOMAIN_CUSTOMER_RESET_PASSWORD`
18. [x] `apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx`：
    - 操作列 + 「重置密码」（AuthGuarded + ConfirmPopover → resetDomainCustomerPassword → 一次性密码 Modal，copyable + 提示首次登录强制改密）
    - 列宽调整：展示名 180 / 登录名 140 / 手机 130 / 邮箱 220 / 状态 90 / 来源 110 / 创建时间 150 / 操作 190，`scroll={{ x: 1280 }}`
    - 全部列 `align: "center"`（含操作列）
    - 创建时间格式化 `YYYY-MM-DD HH:mm`（dayjs）

验证：管理端启动；业务控制台客户列表全列居中；重置流程弹窗展示密码；无权限账号看不到按钮

### Step 4：客户 Web 强制改密（UnionDeskCustomerWeb）

19. [x] `packages/shared/src/customer-portal-live.ts`：loginCustomerLive 返回体 + mustChangePassword；restoreCustomerPortalLive 从 session 恢复
20. [x] `packages/shared/src/customer-portal.ts` 或 `storage.ts`：+ 标志读写 helper（`getStoredMustChangePassword` / `setStoredMustChangePassword`）
21. [x] `apps/UnionDeskCustomerWeb/src/pages/change-password/index.tsx`：新改密页（当前密码/新密码/确认新密码 → changePassword → 清除标志 → /home）
22. [x] `apps/UnionDeskCustomerWeb/src/App.tsx`：+ /change-password 路由；RequireSession/LandingRedirect 增加强制改密守卫（session.mustChangePassword && path !== /change-password → Navigate）

验证：客户 Web 启动；用重置后的密码登录被强制跳改密页；改密前访问 /home 被弹回；改密成功后进入首页且刷新不再拦截

### Step 5：全链路验证与收尾

23. [x] 端到端冒烟：管理员重置 → 客户用新密码登录 → 强制改密 → 改密后正常使用 → 旧密码登录失败
24. [x] `pnpm run check:utf8`、`pnpm run lint:admin`、`pnpm run typecheck:admin`、客户 Web typecheck
25. [x] `.\mvnw.cmd -pl uniondesk-app -am compile` 后端编译；相关单测（如有）

## 验证命令

```powershell
# 后端
cd UnionDesk
.\mvnw.cmd -pl uniondesk-app -am compile
.\mvnw.cmd -pl uniondesk-app test   # 如有相关测试

# 前端
cd UnionDeskWeb
pnpm run check:utf8
pnpm run lint:admin
pnpm run typecheck:admin
pnpm -C apps/UnionDeskCustomerWeb run typecheck
```

## 风险文件与回滚点

- `LoginAccountMapper.xml` updateCustomerPassword 改动影响所有客户改密路径（自服务改密/自助重置/管理员重置）——回滚点：还原该语句 + LoginAccountService 签名
- `AuthDtos.LoginResponse` 构造器改动影响所有登录调用点——回滚点：还原 DTO + AuthService
- 新迁移可安全回滚（仅权限行）
- 前端标志为可选字段，缺失时行为退化为现状
