## ADDED Requirements

### Requirement: 客户账号凭证与全局展示同表

customer_account MUST 同时承载登录凭证与全局展示（username、nickname、avatar_url、phone、email）；340001 MUST 将 login_name 重命名为 username、display_name 重命名为 nickname。

#### Scenario: 客户展示与 API 对齐

- **WHEN** 查询客户账号
- **THEN** API 字段 nickname 对应 customer_account.nickname 列，与 business_domain_id 无关
