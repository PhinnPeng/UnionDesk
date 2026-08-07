# 客户编辑功能：实名信息编辑弹窗与后端更新接口

## Goal

业务控制台「客户管理」列表页（`apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx`）操作列「编辑」图标由只读详情弹窗改为**可编辑表单弹窗**；纳入大陆实名身份管理体系，支持编辑真实姓名与身份证号（脱敏）；后端新增客户信息更新接口。

## Background（已确认事实）

- 现状「编辑」图标打开 `CustomerViewModal`（只读 Descriptions，标题「客户详情」），无编辑能力。
- 后端无客户信息更新接口（仅有创建 manual/from-staff、状态更新、密码重置）。
- `customer_account` 表字段：login_name（唯一）/ display_name / phone（NOT NULL）/ email / status / source 等；**无 real_name、id_card_no 字段**（`identity_subject` 也无身份证）。
- 已确认决策：页面形态用 **Modal 编辑**（非路由详情页）；登录名不可修改；展示名/真实姓名/手机/邮箱可编辑，身份证选填（格式校验 + 脱敏展示）；纳入 real_name + id_card_no 字段。
- 权限先例：`domain.customer.reset_password` 权限码 + DB 权限行 + 角色回填模式（`V20260806120000__domain_customer_reset_password_permission.sql`）。

## Requirements

- R1 后端迁移：`customer_account` 新增 `real_name varchar(64)`、`id_card_no varchar(64)`（身份证存储需可逆？——**不可逆不适用**（管理端需展示明文给管理员核对？还是脱敏？），采用**脱敏存储决策**：存储明文但接口返回一律脱敏（前 3 后 4），管理端不展示完整证号；迁移含权限行 `domain.customer.update` + 角色回填 domain_admin/super_admin）
- R2 后端接口：`PUT /api/v1/admin/domains/{domainId}/customers/{customerId}`（`@RequirePermission(DOMAIN_CUSTOMER_UPDATE)`），请求：display_name / real_name / phone / email / id_card_no(选填)；校验：手机唯一（排除自身）、身份证 18 位 GB 11643 格式校验（选填时）；响应：更新后的客户视图（id_card_no 脱敏）
- R3 登录名不可修改：接口不接收 login_name；前端表单登录名只读展示
- R4 前端 shared：`updateDomainCustomer(domainId, customerId, payload)` API + `UpdateDomainCustomerRequest/Response` 类型
- R5 前端编辑弹窗：`CustomerViewModal` 改造为可编辑表单（或新组件 `CustomerEditModal`）：
  - 字段：展示名* / 真实姓名 / 手机* / 邮箱 / 身份证号（选填，placeholder 提示脱敏）
  - 登录名只读展示（Input disabled）
  - 提交调更新接口 → message 提示 → 刷新列表
  - 保留「查看详情」能力（更多下拉的「查看详情」仍打开只读详情，或编辑弹窗内切换只读/编辑两种模式——以最小改动为准）
- R6 身份证脱敏展示：详情/编辑弹窗中身份证以 `110***********1234` 形式展示（编辑时输入新值则按明文输入，提交后回显脱敏）
- R7 权限：`DOMAIN_CUSTOMER_UPDATE` 常量（domain-permissions.ts）+ 前端 AuthGuarded 包裹编辑按钮（沿用 reset_password 模式）；无权限时不显示编辑图标（或保留图标仅无权限隐藏）

## Acceptance Criteria

- [ ] AC1 迁移执行成功：customer_account 有 real_name / id_card_no 列；iam_permission 有 `domain.customer.update` 行；domain_admin/super_admin 角色回填成功
- [ ] AC2 PUT 接口：改展示名/手机/邮箱/真实姓名/身份证成功并返回脱敏视图；手机重复（其他客户）返回 409/业务错误；身份证格式非法返回 400；login_name 不可改
- [ ] AC3 编辑弹窗可编辑上述字段，登录名置灰只读；提交后列表刷新、成功提示
- [ ] AC4 身份证回显脱敏（`110***********1234`）；新建客户/员工导入等既有路径不受影响（新列为空）
- [ ] AC5 无 `domain.customer.update` 权限账号不显示编辑图标（或点击被拒）
- [ ] AC6 浏览器冒烟：编辑流程全链路（打开→改字段→提交→列表更新）
- [ ] AC7 后端 `mvnw compile` 通过；前端 typecheck:admin / check:utf8 通过

## Out of Scope

- 路由级客户详情页（本次用 Modal）
- 身份证真伪核验（公安接口对接）
- 登录名修改能力
- 客户 Web 自助编辑个人资料
- 列表增加身份证列（仅编辑/详情内展示）
