## Context

- 控制台权限：`platform.domain.control.entry/overview/general.*`（Flyway 330004/330005）。
- 客户当前：`platform.domain.customer.*`（330002）— 应迁入 `control.customer.*`。
- 员工 Tab 仍用 `domain.member.*`（域 scope，不变）。

## Goals / Non-Goals

**Goals**

- 权限统一为 `platform.domain.control.customer.{read,create,update-status}`。
- 只读查看；菜单式 UI；禁用二次确认。

**Non-Goals**

- 资料编辑；`platform.domain.customer.*` 作为最终命名。

## Decisions

1. 三码迁移 + catalog `PLATFORM-DOMAIN-CONTROL-CUSTOMER`。
2. 前端 `PLATFORM_DOMAIN_CONTROL_CUSTOMER_*` + detail-shared export。
3. 旧常量 deprecated alias 便于渐进替换。

## Risks

- 角色绑定需 Flyway 同步，避免迁移后 403。
