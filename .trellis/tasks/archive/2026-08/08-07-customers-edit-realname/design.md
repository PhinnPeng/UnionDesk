# 设计：客户编辑功能（实名信息编辑弹窗 + 后端更新接口）

## 1. 目标与边界

- 业务控制台客户列表操作列「编辑」图标：由只读详情弹窗改为可编辑表单弹窗。
- 可编辑字段：展示名 / 真实姓名 / 手机 / 邮箱 / 身份证号（选填）；登录名只读（接口不接收）。
- 身份证脱敏：后端存储明文，接口响应一律脱敏（`110***********1234`）；前端编辑回显脱敏值，提交时含 `*` 的身份证值视为未修改不发送。
- 只读「查看详情」保留（更多下拉菜单项，继续打开只读 `CustomerViewModal`）。

## 2. 后端设计

### 2.1 数据模型（迁移）

`V20260807120000__customer_realname_identity.sql`（current/）：
- `ALTER TABLE customer_account ADD COLUMN real_name varchar(64) NULL, ADD COLUMN id_card_no varchar(64) NULL`
- 新权限行 `domain.customer.update`（scope=domain, resource=domain.customer, action=update, method=PUT, path=`/api/v1/admin/domains/*/customers/*`）
- 角色回填 `domain_admin` / `super_admin`（沿用 `V20260806120000` 的 `INSERT ... ON DUPLICATE KEY UPDATE` 模式）

### 2.2 权限码

- `PermissionCodes.DOMAIN_CUSTOMER_UPDATE = "domain.customer.update"`（iam/core/PermissionCodes.java）

### 2.3 服务链路（iam 模块）

`CustomerAccountService` 新增：

```
updateProfile(long customerAccountId, UpdateProfileCommand cmd)
  1. findById 校验存在
  2. 手机变更时：
     - identitySubjectRepository.findIdByPhone(newPhone) 存在且 != 当前 subjectId → 抛「手机号已被占用」
     - IdentitySubjectMapper 新增 updatePhone（UPDATE identity_subject SET phone=?, updated_at=? WHERE id=?）
  3. CustomerAccountMapper 新增 updateProfile（UPDATE customer_account SET nickname=?, phone=?, email=?, real_name=?, id_card_no=?, updated_at=? WHERE id=?）
  4. DuplicateKeyException 兜底 → 「手机号已被占用」
```

配套改动：
- `CustomerAccountPo` + `realName` / `idCardNo` 字段
- `CustomerAccountMapper.xml`：selectById / 列表 select 补列；+ `updateProfile` 语句
- `CustomerAccountRepository` + `updateProfile`、+ `updateSubjectPhone`
- `IdentitySubjectMapper.xml` + `updatePhone`；`IdentitySubjectRepository` + `updatePhone`
- `CustomerAccountService.CustomerAccount` record + `realName` / `idCardNo`（构造器同步，`toCustomerAccount` 补字段；`DomainCustomerService.loadCustomerAccount` 调用点同步）

### 2.4 控制器与 DTO（domain 模块）

- `DomainCustomerDtos.UpdateDomainCustomerProfileRequest`：
  `display_name`(必填) / `real_name`(可选) / `phone`(必填) / `email`(可选, @Email) / `id_card_no`(可选)
- `DomainCustomerDtos.DomainCustomerView` 末尾追加 `real_name`、`id_card_no`（响应侧**脱敏值**；同步全部构造点：`toCustomerView`、`updateCustomerStatus` 手动构造）
- `DomainCustomerService.updateCustomerProfile(domainId, customerId, request)`：
  1. loadDomain + loadCustomerById（归属校验）
  2. 身份证校验：非空时 18 位 GB 11643 格式（正则 `^\d{17}[\dXx]$` + 校验位算法，私有方法 `isValidIdCardNo`）
  3. 组装 command 调 `customerAccountService.updateProfile`（`display_name` → nickname 语义）
  4. 返回 `toCustomerView`（id_card_no 经 `maskIdCardNo`：前 3 + `***********` + 后 4；为空返回 null）
- `DomainCustomerController` + `PUT /customers/{customerId}`，`@RequirePermission(PermissionCodes.DOMAIN_CUSTOMER_UPDATE)`

### 2.5 脱敏工具

`DomainCustomerService` 私有方法：
```java
private String maskIdCardNo(String raw) {
    if (raw == null || raw.length() < 8) return raw;
    return raw.substring(0, 3) + "***********" + raw.substring(raw.length() - 4);
}
```

## 3. 前端设计

### 3.1 shared

- `packages/shared/src/types.ts`：`P0DomainCustomer` + `real_name?: string | null`、`id_card_no?: string | null`（脱敏值）；+ `UpdateDomainCustomerRequest { display_name: string; real_name?: string; phone: string; email?: string; id_card_no?: string }`
- `packages/shared/src/api.ts`：+ `updateDomainCustomer(domainId, customerId, payload)` → `PUT /admin/domains/{domainId}/customers/{customerId}`（参照 updateDomainCustomerStatus 的 axios 封装）

### 3.2 管理端（customers/index.tsx）

- `domain-permissions.ts`：+ `DOMAIN_CUSTOMER_UPDATE`
- 新增私有子组件 `CustomerEditModal`（参照现有 CustomersAddModal 表单风格，antd `Form` + `App.useApp`）：
  - 打开时 `fetchDomainCustomer` 拉取详情填充表单
  - 字段：展示名*(Input)、真实姓名(Input)、登录名(Input disabled 只读)、手机*(Input)、邮箱(Input, 格式校验)、身份证号(Input, 选填，placeholder 提示「18 位身份证号」；回显后端脱敏值)
  - 提交：`id_card_no` 含 `*` 时从 payload 剔除；调 `updateDomainCustomer` → message 成功 → onClose + 刷新列表
- 操作列「编辑」图标：`AuthGuarded auth={DOMAIN_CUSTOMER_UPDATE}` 包裹，onClick 打开 CustomerEditModal（替代原 setViewCustomerId 打开只读 Modal）
- 保留：更多下拉「查看详情」继续打开只读 `CustomerViewModal`；`CustomerViewModal` 内 Descriptions 追加 真实姓名/身份证（脱敏回显）两项（若方便可加，属详情增强，最小改动为不加——以 implement 判断）

### 3.3 权限

编辑按钮 AuthGuarded(DOMAIN_CUSTOMER_UPDATE)；无权限时不渲染图标（跟随 reset_password 模式：fallback=null）。

## 4. 数据流

```
管理员点编辑 → fetchDomainCustomer → 表单预填（身份证脱敏）
→ 提交 payload（含 * 的身份证剔除）
→ PUT /admin/domains/{d}/customers/{c}
→ 校验（手机唯一/身份证格式）→ 更新 customer_account + identity_subject.phone
→ 响应脱敏视图 → message 成功 → 刷新列表
```

## 5. 兼容性与风险

- `DomainCustomerView` 追加字段：record 构造点仅 service 内部（toCustomerView / updateCustomerStatus），同步即可；前端类型为可选字段，缺失兼容。
- `CustomerAccountService.CustomerAccount` record 追加字段：调用点 `DomainCustomerService.loadCustomerAccount` 同步。
- 手机唯一：identity_subject.phone 唯一键兜底 + 服务预检；不引入 subject 合并逻辑。
- 迁移可回滚（仅加列 + 权限行）；已执行迁移不删 current/ 历史。
- 身份证仅脱敏展示，存储明文（管理端核对场景需要原始值；后续可评估 AES 加密列）。
- 平台域详情 Tab（detail-customers）本次不加编辑入口（与 R1 一致）。

## 6. 回滚

- 后端：还原代码 + 新迁移不动（列可保留）；前端还原按钮与 Modal。
