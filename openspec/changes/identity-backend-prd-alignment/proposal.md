## Why

PRD 要求 `identity_subject` + 双账号 + 分端登录 + 域成员/入域，但实现仍依赖 legacy `user_account` 与分散的展示逻辑。需一步到位收敛目标态，同时 **避免过度拆表**（profile 独立表），降低迁移与 JOIN 成本。

## What Changes

- **L0**：`IdentitySubjectService` 唯一主体写入口。
- **L1 合一**：`staff_account` 保留凭证 + 平台展示同表；`customer_account` 保持现有凭证 + 全局展示同表（不拆、不降列）。
- **L2 域内员工展示**：在 **`domain_member` 增加 `domain_*` 展示列**（不建 `domain_member_profile`）。
- **服务层**：`StaffAccountService`、`CustomerAccountService`、`IdentityPresentationService`（域内 fallback）。
- **认证/IAM/域**：切 staff/customer_account；**停写 `user_account`**，移除 login fallback。
- **域权限单轨**：`domain_member_role`；Flyway 从 `user_domain_role` 补录。
- **Flyway 单脚本**：DROP 身份核心表 FK + 补列 + 回填 + 补录（**不**创建 profile 三表；**不**新增 DB FK）。
- **引用完整性**：身份域表间为逻辑引用 + 索引；写路径由 Service 业务规则校验（见 design D6）。

## Capabilities

### New Capabilities

- `identity-subject`
- `staff-account`（合一语义：凭证 + 平台展示）
- `customer-account`（合一语义：凭证 + 全局展示）
- `auth-login`
- `domain-membership`（含 domain_member 域内列）
- `identity-presentation`（域内 fallback 解析）

### Modified Capabilities

- （无 main spec）

## Impact

- **后端**：~18 Java 文件；1 Flyway 脚本
- **BREAKING（内部）**：停写 user_account、移除 login fallback
- **非目标**：profile 拆表、DROP user_account、客户域级昵称、免密切换 UI
