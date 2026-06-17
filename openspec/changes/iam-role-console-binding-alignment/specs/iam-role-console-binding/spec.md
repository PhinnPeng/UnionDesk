## ADDED Requirements

### Requirement: 平台角色仅绑定平台控制台

系统 MUST 使 `role.scope = global` 的平台角色（含 `platform_admin`、`super_admin`、`security_auditor`）仅拥有：

- `iam_admin_menu.scope = platform` 的菜单/按钮绑定；
- `iam_permission.permission_scope = platform` 且 permission code 以 `platform.` 开头的直授或按钮授权。

平台角色的权限快照 `actions` MUST NOT 含不以 `platform.` 开头的 permission code。

#### Scenario: platform_admin 快照仅含 platform 前缀

- **WHEN** 用户仅绑定 `platform_admin`（global）并加载 AdminWeb 权限快照
- **THEN** `actions` 中每条 code 均以 `platform.` 开头
- **AND** `menuTree` 仅含 `scope=platform` 节点

#### Scenario: admin bootstrap 绑定 platform_admin

- **WHEN** 种子账号 `admin` 登录 AdminWeb
- **THEN** 其 IAM 绑定为 `platform_admin`（`user_global_role`）
- **AND** 不绑定 global `super_admin`

#### Scenario: platform_admin 默认首页

- **WHEN** `platform_admin` 登录且快照 actions 仅含 `platform.*`
- **THEN** 访问 `/` 重定向至 `/platform/home`

### Requirement: 业务域角色仅绑定业务域控制台

系统 MUST 使 `role.scope = domain` 的业务域角色（含 `domain_admin`、`agent`）仅拥有：

- `iam_admin_menu.scope = business` 的菜单/按钮绑定；
- 非 `platform.*` 前缀的业务 permission code（如 `domain.*`、`ticket.*`）。

业务域角色的权限快照 MUST NOT 含 `platform.*` 前缀的 actions，且 `menuTree` MUST NOT 含 `/platform/` 路由模块。

#### Scenario: domain_admin 快照无 platform 前缀

- **WHEN** 用户仅绑定 `domain_admin` 并加载权限快照
- **THEN** `actions` 不含以 `platform.` 开头的 code
- **AND** 侧栏 `menuTree` 不含 platform scope 菜单

#### Scenario: domain_admin 默认首页

- **WHEN** `domain_admin` 登录
- **THEN** 访问 `/` 重定向至业务域首页（`/home` 或 `VITE_BASE_HOME_PATH`）

#### Scenario: agent 权限包审计

- **WHEN** 种子数据 Flyway 收敛完成
- **THEN** `agent` 角色不存在 platform scope 菜单绑定
- **AND** 不存在 `platform.*` 直授

### Requirement: 双控制台显式组合

用户同时具备平台角色与业务域角色时，系统 MUST 通过 **分离绑定**（`user_global_role` + `user_domain_role`）实现，不得通过单个 global 角色内嵌 business 直授模拟。

#### Scenario: 双角色组合默认首页

- **WHEN** 用户同时绑定 `platform_admin` 与 `domain_admin@domain-A`
- **THEN** 快照 actions 同时含 `platform.*` 与非 `platform.*`
- **AND** `platformAccess=true`
- **AND** 访问 `/` 重定向至业务域首页（维持 E2-00 三元规则）

### Requirement: 角色授权保存 scope 校验

保存角色菜单/按钮授权（`replaceRolePermissions`）时，系统 MUST 拒绝跨界绑定并返回明确错误：

- global 角色不得绑定 business scope 菜单或 non-`platform.*` 按钮权限；
- domain 角色不得绑定 platform scope 菜单或 `platform.*` 按钮权限。

#### Scenario: domain 角色勾选 platform 按钮被拒绝

- **WHEN** 管理员为 `domain_admin` 保存含 platform scope 菜单按钮的授权
- **THEN** 请求失败并提示 scope 不一致

#### Scenario: global 角色勾选 business 菜单被拒绝

- **WHEN** 管理员为 `platform_admin` 保存含 business scope 菜单的授权
- **THEN** 请求失败并提示 scope 不一致

### Requirement: global super_admin 为 break-glass

global `super_admin` MUST 视为 break-glass 系统角色：权限包与 `platform_admin` 同为纯 platform 控制台能力；普通用户管理 UI MUST NOT 将其作为可分配角色（仅 seed / 应急保留）。

#### Scenario: super_admin 不含 business 直授

- **WHEN** global `super_admin` 加载权限快照
- **THEN** `actions` 不含 `ticket.*`、`domain.user.*` 等业务域直授码
- **AND** 不含 business scope 菜单绑定
