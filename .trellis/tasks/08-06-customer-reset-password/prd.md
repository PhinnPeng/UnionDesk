# 优化客户列表页：新增重置客户密码功能（首次登录强制改密）

## Goal

业务控制台「客户管理」列表页新增「重置客户密码」能力；管理员重置后，该客户下次登录被强制要求修改密码，改密成功前不能进入业务页面。同时优化该列表页排版：所有列（含操作列）内容居中。

## Background（已确认事实）

- 客户列表页现状：`UnionDeskWeb/apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx`（业务控制台），操作列仅查看/禁用/启用；平台域详情客户 Tab `detail-customers.tsx` 复用同一组 API client。
- 表结构已就绪：`customer_account` 已有 `password_hash / must_change_password / auth_version / password_changed_at`（`V202605200002__rebaseline_current_schema.sql:269-289`）。
- 字段只写不读：`getMustChangePassword` 全仓无调用；登录两分支（`AuthService.login` :106 / `loginCustomer` :232）不检查；`CustomerAccountMapper.selectById` 不查该列；`LoginAccountMapper.xml:110-116 updateCustomerPassword` 改密后不清标志位。
- 已有能力可复用：自服务改密 `PUT /api/v1/auth/password`（`AuthController.java:84-88`）、`AuthVersionService.incrementVersion`（重置/改密后吊销全部活跃会话）、手动添加客户/员工转客户创建时已写随机密码 + `must_change_password=1`。
- 客户管理员重置接口不存在；客户登录响应 `AuthDtos.LoginResponse` 无强制改密字段；客户 Web 无改密页；前端登录流程无强制改密分支。
- 权限体系：业务控制台权限码 `domain.customer.*`（`PermissionCodes.java:18-20`），DB 权限行 + 角色回填模式见 `archive/V202605031203__p0_domain_customer_permissions.sql`。

## Requirements

- R1 页面范围：仅业务控制台「客户管理」列表页（`apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx`）新增「重置密码」入口，加在表格操作列；平台域详情客户 Tab 本次不改。
- R2 列表页排版：该列表页所有列（含操作列）内容居中对齐；修复字段占位不合理问题：
  - 创建时间改为格式化时间（`YYYY-MM-DD HH:mm`，现状直接渲染原始 ISO 串 `2026-05-31T13:24:25.043`）
  - 展示名/邮箱两个无宽度弹性列实测各占 739px（总表宽 2270px）严重失衡，改为合理固定宽度（如展示名 180、邮箱 220，带 ellipsis）
  - 操作列加宽（现有 140px 容纳查看/禁用两按钮，新增重置密码后需 ~190px）
  - 调整 `scroll={{ x }}` 与新的列宽总和匹配
- R3 重置密码：新密码由后端生成随机密码（SecureRandom，12 位字母数字），重置成功后弹窗一次性展示供管理员复制转交客户。
- R4 权限：新增权限码 `domain.customer.reset_password`（后端 `PermissionCodes` + DB 权限行 + 角色回填 domain_admin/super_admin），前端按钮用 `AuthGuarded` 控制可见性。
- R5 强制改密（前端强制跳转方式）：客户登录响应（`LoginResponse`）新增 `must_change_password` 标志；客户 Web 登录后检测到标志即强制跳转改密页，改密成功前不能进入业务页面；后端不额外拦截业务 API。
- R6 改密链路：客户在改密页输入当前密码（一次性密码）+ 新密码，复用 `PUT /api/v1/auth/password`；后端客户改密成功后将 `must_change_password` 清零。
- R7 存量数据自动生效：手动添加客户/员工转客户创建时已写入 `must_change_password=1`（随机密码不可知），机制打通后这些客户首次登录同样被强制改密。
- R8 会话吊销复用：重置密码走 `AuthVersionService.incrementVersion`，该客户全部活跃会话自动吊销（旧凭证立即失效）。

## Acceptance Criteria

- [ ] AC1 业务控制台客户列表操作列出现「重置密码」入口，无权限账号不可见
- [ ] AC2 列表全部列（含操作列）内容居中显示；列宽按：展示名 180 / 登录名 140 / 手机 130 / 邮箱 220 / 状态 90 / 来源 110 / 创建时间 150 / 操作 190（scroll x 同步调整）；创建时间格式化为 `YYYY-MM-DD HH:mm`
- [ ] AC3 管理员重置后弹窗一次性展示随机生成的新密码（可复制），并提示客户首次登录将强制改密
- [ ] AC4 被重置客户登录响应携带 `must_change_password=true`，客户 Web 强制跳转改密页，改密前访问业务路由（/home 等）被弹回改密页
- [ ] AC5 客户改密成功后标志位清零，刷新页面不再拦截，正常进入业务页面；旧密码登录失败、新密码可登录
- [ ] AC6 重置/改密后该客户所有活跃会话被吊销（旧 token 调用接口返回 401）
- [ ] AC7 客户 Web 新增改密页（当前密码 + 新密码 + 确认新密码），改密成功跳转首页
- [ ] AC8 新增迁移 `V20260806120000__domain_customer_reset_password_permission.sql、V20260806130000__domain_customer_reset_password_menu.sql` 执行成功，`domain_admin`/`super_admin` 角色获得新权限

## Out of Scope

- 平台域详情客户 Tab（detail-customers.tsx）加重置入口
- 重置/改密后的站内信、邮件通知
- 后端拦截 must_change_password=1 客户的业务 API
- staff（平台用户/员工）密码强制改密机制（`staff_account.must_change_password` 已有字段，行为不变）
- 客户 Web「我的」页自主改密入口（仅强制改密页）

## Technical Notes

- 后端改动：uniondesk-domain 控制器/服务新增重置端点；uniondesk-iam 打通 `must_change_password` 读写（`LoginAccountMapper.xml` SQL、`LoginAccountService`、`CustomerAccountService`）；uniondesk-app `AuthService.loginCustomer` 登录响应携带标志、`AuthDtos.LoginResponse` 末尾追加可选字段（compact 构造器同步重载，向后兼容）。
- 前端改动：shared 包新增 `resetDomainCustomerPassword` / `changePassword` API 与类型；管理端列表页按钮 + 一次性密码弹窗 + 全列居中；客户 Web 新建 `/change-password` 页 + 路由守卫（标志持久化在 auth session storage，刷新可恢复）。
- 详细设计见 `design.md`，执行计划见 `implement.md`。
