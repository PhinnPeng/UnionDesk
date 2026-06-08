# Tasks: identity-backend-prd-alignment

> 设计权威：[`docs/superpowers/specs/2026-06-06-identity-scheme-a-design.md`](../../../docs/superpowers/specs/2026-06-06-identity-scheme-a-design.md)

## 1. Flyway

- [x] 1.1 `V202605340001__identity_target_alignment.sql`：DROP 身份核心表 11 条 FK（§5.1）
- [x] 1.2 同脚本：staff/customer `login_name` RENAME → `username`；customer `display_name` → `nickname`；重命名 UK 索引
- [x] 1.3 同脚本：`staff_account` ADD `real_name`、`nickname`、`avatar_url`（若缺）
- [x] 1.4 同脚本：`domain_member` ADD `domain_nickname` 及 domain_avatar/contact 列
- [x] 1.5 同脚本：自 `user_account.nickname` 回填 `staff_account.real_name`
- [x] 1.6 同脚本：`user_domain_role` → `domain_member_role` 补录
- [x] 1.7 登记 `database-increment-plan.md`

## 2. 核心服务

- [x] 2.1 `IdentitySubjectService`（resolveByPhone、resolveEffectiveSubjectId、requireActiveSubject）+ 单元测试
- [x] 2.2 `StaffAccountService`（整行 CRUD；列 username / real_name / nickname）
- [x] 2.3 `CustomerAccountService`（整行 CRUD；列 username / nickname）
- [x] 2.4 `IdentityPresentationService`（real_name + nickname 解析链）+ 单元测试
- [x] 2.5 各 Service 内联 §6 引用校验 + 单元测试（StaffAccountServiceTests / CustomerAccountServiceTests）

## 3. 认证层

- [x] 3.1 `LoginAccountService`：单表 staff/customer；删除 user_account fallback
- [x] 3.2 域角色仅 `domain_member_role`
- [x] 3.3 `AuthService` / `AuthVersionService` / `LoginSessionService` / `LoginAuditService`（customer 写 nickname 列）
- [x] 3.4 Auth 登录路径测试（LoginAccountServiceTests；SpringBoot 集成测试依赖 DB）

## 4. IAM 与 Staff API

- [x] 4.1 `IamService` 停写 `user_account`（createUser 拒绝 INSERT，改走 Staff/Customer API）
- [x] 4.2 `StaffController` / `StaffDtos` / IAM DTO：统一 `username` + `real_name` + `nickname`（移除 loginName/login_name）
- [x] 4.3 测试更新

## 5. 域成员与客户

- [x] 5.1 `DomainMemberService`：domain_nickname + PresentationService
- [x] 5.2 `DomainCustomerService`：Subject 归一；customer_account.nickname
- [x] 5.3 `DomainBootstrapService`：去 user_domain_role 双写
- [x] 5.4 相关测试

## 6. 扫描与验证

- [x] 6.1 修正仍依赖 user_account / login_name / display_name 的 JDBC 与 SQL（核心路径；AuditLog 已部分对齐）
- [x] 6.2 `mvn test` + 后端重启冒烟（需联调 MySQL + Flyway 340001）
- [x] 6.3 更新 `data-model.md`
- [x] 6.4 shared types：`real_name` + `nickname`；detail-members 姓名/昵称列对齐
