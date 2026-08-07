# 实施计划：客户编辑功能（实名信息编辑弹窗 + 后端更新接口）

## 任务地图

```
P1 后端：迁移 + 权限码 + 实体/Mapper/Repository（iam）
P2 后端：服务方法 + DTO + Controller（domain/app）
P3 前端：shared API/类型 + 管理端编辑弹窗
P4 验证：后端编译 + 前端 typecheck + 浏览器冒烟
```

## 实施清单

### Step 1：后端数据层（uniondesk-iam）

1. [ ] 新迁移 `UnionDesk/uniondesk-app/src/main/resources/db/migration/current/V20260807120000__customer_realname_identity.sql`：
   - `ALTER TABLE customer_account ADD COLUMN real_name varchar(64) NULL AFTER email, ADD COLUMN id_card_no varchar(64) NULL AFTER real_name`
   - iam_permission 插入 `domain.customer.update`（参照 `V20260806120000__domain_customer_reset_password_permission.sql` 结构）
   - `domain_admin`/`super_admin` 角色回填（ON DUPLICATE KEY UPDATE 模式）
2. [ ] `PermissionCodes.java`：+ `DOMAIN_CUSTOMER_UPDATE = "domain.customer.update"`
3. [ ] `CustomerAccountPo.java`：+ `realName` / `idCardNo` 字段 + getter/setter
4. [ ] `CustomerAccountMapper.xml`：select 语句（selectById 及列表）补 `real_name, id_card_no`；+ `updateProfile` 语句（nickname/phone/email/real_name/id_card_no/updated_at WHERE id）
5. [ ] `CustomerAccountMapper.java`：+ `updateProfile` 方法签名
6. [ ] `CustomerAccountRepository.java`：+ `updateProfile(CustomerAccountPo)`、+ `updateSubjectPhone(subjectId, phone)`
7. [ ] `IdentitySubjectMapper.xml`：+ `updatePhone` 语句（phone, updated_at WHERE id）
8. [ ] `IdentitySubjectMapper.java` + `IdentitySubjectRepository.java`：+ `updatePhone`
9. [ ] `CustomerAccountService.java`：+ `updateProfile(long customerAccountId, UpdateProfileCommand)`（手机占用预检 → updateSubjectPhone → updateProfile → DuplicateKeyException 兜底）；`CustomerAccount` record + `realName`/`idCardNo`（toCustomerAccount 同步）；+ `UpdateProfileCommand` record

验证：`.\mvnw.cmd -pl uniondesk-app -am compile` 通过（含调用点同步）

### Step 2：后端服务/接口（uniondesk-domain）

10. [ ] `DomainCustomerDtos.java`：+ `UpdateDomainCustomerProfileRequest(display_name, real_name, phone, email, id_card_no)`（jakarta validation：display_name/phone @NotBlank、email @Email）；`DomainCustomerView` 末尾 + `real_name` / `id_card_no` 字段（同步 toCustomerView、updateCustomerStatus 构造点）
11. [ ] `DomainCustomerService.java`：+ `updateCustomerProfile(domainId, customerId, request)`（归属校验 → 身份证 GB 11643 格式校验 → customerAccountService.updateProfile → 返回 toCustomerView）；+ 私有 `isValidIdCardNo(String)`（17 位数字 + 校验位，GB 11643-1999 权重表）、`maskIdCardNo(String)`
12. [ ] `DomainCustomerController.java`：+ `PUT /customers/{customerId}`（@RequirePermission(PermissionCodes.DOMAIN_CUSTOMER_UPDATE)）

验证：后端编译；curl 冒烟（改字段成功/手机占用 409/身份证非法 400/返回脱敏）

### Step 3：前端（shared + AdminWeb）

13. [ ] `packages/shared/src/types.ts`：`P0DomainCustomer` + `real_name` / `id_card_no`（可选）；+ `UpdateDomainCustomerRequest`
14. [ ] `packages/shared/src/api.ts`：+ `updateDomainCustomer(domainId, customerId, payload)`
15. [ ] `apps/UnionDeskAdminWeb/src/pages/domain/domain-permissions.ts`：+ `DOMAIN_CUSTOMER_UPDATE`
16. [ ] `apps/UnionDeskAdminWeb/src/pages/domain/customers/index.tsx`：
    - 新增私有子组件 `CustomerEditModal`（Form：展示名*/真实姓名/登录名[disabled]/手机*/邮箱/身份证号；打开时 fetchDomainCustomer 预填；提交时 id_card_no 含 `*` 剔除；成功 → message + onClose + loadCustomers）
    - 操作列「编辑」图标：`AuthGuarded(DOMAIN_CUSTOMER_UPDATE)` 包裹 → 打开 CustomerEditModal
    - 更多下拉「查看详情」保留打开只读 `CustomerViewModal`
    - 无权限时不渲染编辑图标

验证：`pnpm run typecheck:admin`、`pnpm run check:utf8`

### Step 4：全链路验证

17. [ ] 浏览器冒烟：编辑图标 → 弹窗表单（登录名置灰）→ 修改展示名/真实姓名/手机/身份证 → 提交 → 列表刷新；身份证回显脱敏；「查看详情」仍只读
18. [ ] 无权限账号（如有）不显示编辑图标
19. [ ] `.\mvnw.cmd -pl uniondesk-app -am compile` 最终编译

## 验证命令

```powershell
cd UnionDesk
.\mvnw.cmd -pl uniondesk-app -am compile
cd ../UnionDeskWeb
pnpm run check:utf8
pnpm run typecheck:admin
```

## 风险文件与回滚点

- `DomainCustomerView` record 构造点：toCustomerView / updateCustomerStatus / resetCustomerPassword 返回（仅 View 不涉）——同步遗漏会编译失败，编译即验证
- `CustomerAccountService.CustomerAccount` record：DomainCustomerService.loadCustomerAccount 同步
- 手机唯一预检逻辑：identity_subject.phone 唯一键兜底，误伤风险低
- 迁移可回滚（加列 + 权限行）
