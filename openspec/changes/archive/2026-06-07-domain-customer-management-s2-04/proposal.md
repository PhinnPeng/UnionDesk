## Why

US-S2-04 完善业务域详情「客户管理」Tab。现有 `platform.domain.customer.*` 与控制台权限树不一致；应对齐 **`platform.domain.control.customer.*`**（与 `control.general.*` 同级）。另需只读查看、normalize 修复、禁用二次确认、菜单式 UI。

## What Changes

- Flyway：`platform.domain.customer.*` → **`platform.domain.control.customer.*`**；catalog `PLATFORM-DOMAIN-CONTROL-CUSTOMER`。
- 后端：GET 单条；接口改绑新码；无资料编辑 PUT。
- 前端：`PLATFORM_DOMAIN_CONTROL_CUSTOMER_*` 常量；detail-customers 重构。
- Shared：fetchDomainCustomer + normalize。

## Capabilities

### New Capabilities

- `domain-customer-management`

## Impact

- Flyway + PermissionCodes + AdminPermissionCatalog + 前端 permissions + detail-customers
- **非目标**：资料编辑、`platform.domain.customer.*` 保留为长期命名
