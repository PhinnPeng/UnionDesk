## ADDED Requirements

### Requirement: 纯 business 员工默认进入业务域首页

系统 MUST 使仅具备 business scope 菜单、且无平台角色（`super_admin` / `platform_admin`）的域内员工，登录 AdminWeb 后默认进入业务域首页，而非 `/platform/home`。

#### Scenario: domain_admin 登录首页

- **WHEN** 用户以 `domain_admin` 登录且权限快照 menuTree 仅含 `scope=business` 菜单
- **THEN** 登录成功跳转至 `VITE_BASE_HOME_PATH` 或菜单首项（如 `/system/menu`）
- **AND** 不跳转 `/platform/home`

### Requirement: 动态菜单与 business scope 一致

AdminWeb 侧栏动态菜单 MUST 与 `iam_admin_menu.scope=business` 授权一致，且不包含 `/platform/` 路径模块。

#### Scenario: 侧栏无平台模块

- **WHEN** 纯 business 员工加载菜单快照
- **THEN** 侧栏树中不出现 `/platform/domains` 等平台路由
- **AND** 至少包含一条可导航的 business 系统管理菜单

### Requirement: 至少一个 system 页面可达

业务域端 MUST 提供至少一个 `pages/system/*` 非 Empty 占位页面可通过菜单打开。

#### Scenario: 打开菜单管理或角色管理

- **WHEN** 用户点击「菜单管理」或「角色管理」
- **THEN** 页面渲染真实列表/树表内容（非占位 Empty）

### Requirement: 平台控制台能力判定

权限快照 `actions` 中若存在任意以 `platform.` 开头的权限码，或快照含 platform scope 菜单，或用户具备平台角色，系统 MUST 视为 `platformAccess=true`（可进入平台控制台、显示平台入口），**但不得单独据此决定默认首页**。

#### Scenario: 双控制台用户仍显示平台入口

- **WHEN** 用户快照 actions 同时含 `platform.*` 与 `domain.*`
- **THEN** `platformAccess=true`
- **AND** 顶栏平台入口可用

### Requirement: 默认首页按 actions 三元规则

登录 AdminWeb 后默认首页 MUST 仅依据快照 `actions` 中 `platform.*` 与非 `platform.*` 权限码是否存在判定：

| actions 组合 | 默认首页 |
|:---|:---|
| 仅有 `platform.*` | `/platform/home`（平台控制台） |
| 仅有非 `platform.*` | 业务域首页（如 `/system/menu`） |
| 同时有 `platform.*` 与非 `platform.*` | 业务域首页（统一业务域后台） |

#### Scenario: 仅平台权限进平台首页

- **WHEN** 快照 actions 仅含 `platform.*` 权限码
- **THEN** 登录后跳转 `/platform/home`

#### Scenario: 双具备进业务域首页

- **WHEN** 快照 actions 同时含 `platform.*` 与 `domain.*` 等非 platform 码
- **THEN** 登录后跳转业务域首页（如 `/system/menu`）
- **AND** 不跳转 `/platform/home`

### Requirement: 业务域按钮使用 domain 权限码

business scope 的 `iam_admin_menu` 按钮 MUST 使用 `domain.*` 权限码（如 `domain.menu.read`），不得使用 `platform.*` 前缀，以确保纯 business 快照不含 platform 权限判定噪声。

#### Scenario: domain_admin 快照 actions

- **WHEN** `domain_admin` 加载权限快照且 active scope 为 business
- **THEN** `actions` 含 `domain.menu.read` 等业务码
- **AND** `actions` 不含 `platform.menu.read` 等 platform 前缀码
