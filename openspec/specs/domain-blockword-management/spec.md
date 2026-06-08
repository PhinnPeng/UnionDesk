## ADDED Requirements

### Requirement: 平台全局屏蔽词库

平台管理员 MUST 能在平台控制台维护全局屏蔽词；词条 `business_domain_id` 为 NULL，跨域生效。

#### Scenario: 全局分页列表

- **WHEN** GET `/api/v1/admin/blocked-words` 且持有 `platform.blocked_word.read`
- **THEN** 返回 `P0PageResult`，支持 `keyword` 模糊匹配与 `page`/`page_size` 分页

#### Scenario: 全局单条与批量增删

- **WHEN** POST 单条或 POST `/batch` 且持有 `platform.blocked_word.create`
- **THEN** 创建词条；重复项跳过（批量）或 400（单条）
- **WHEN** DELETE 且持有 `platform.blocked_word.delete`
- **THEN** 删除全局词条

#### Scenario: 平台管理页

- **WHEN** 访问 `/platform/blockwords` 且持有 read 权限
- **THEN** 展示 TableSearchForm 查询栏 + Table 列表 + 单条/批量添加

### Requirement: 域内屏蔽词库（平台控制台）

平台管理员 MUST 能在业务域详情「屏蔽词库」Tab 维护该域词条；交互与全局页一致。

#### Scenario: 域内 CRUD 与查询

- **WHEN** 调用 `/api/v1/admin/domains/{domainId}/blocked-words` 且持有 `platform.domain.control.blocked_word.*`
- **THEN** 增删查仅作用于该 `domainId`；GET 支持 keyword 模糊与分页

#### Scenario: Tab 门控

- **WHEN** 用户无 `platform.domain.control.blocked_word.read`
- **THEN** 侧栏不展示「屏蔽词库」Tab

### Requirement: 权限命名与迁移

系统 MUST 使用双层权限命名；自 `domain.blocked_word.*` 迁移域内码。

#### Scenario: 权限码结构

- **WHEN** 查看 Flyway 与 PermissionCodes
- **THEN** 存在 `platform.blocked_word.{read,create,delete}` 与 `platform.domain.control.blocked_word.{read,create,delete}`

#### Scenario: 迁移不丢权

- **WHEN** 自 `domain.blocked_word.*` 升级
- **THEN** 映射至 `platform.domain.control.blocked_word.*` 且角色绑定不丢失

### Requirement: 词条校验

#### Scenario: trim 与空词

- **WHEN** 提交词条仅含空白
- **THEN** 拒绝并返回中文错误

#### Scenario: 同 scope 重复（单条）

- **WHEN** 单条 POST 在同 scope 添加已存在词条（trim 后）
- **THEN** 400 并返回「该屏蔽词已存在」

#### Scenario: 批量去重

- **WHEN** POST batch 且词条在批内或库内已存在
- **THEN** 批内自动去重；库内重复进入 `skipped`，不中断整批

#### Scenario: 关键字模糊查询

- **WHEN** GET 带 `keyword`
- **THEN** 返回 word 子串匹配结果的分页列表

#### Scenario: 空态与错误中文

- **WHEN** 列表为空或 API 失败
- **THEN** UI 展示中文空态或错误提示

#### Scenario: 删除二次确认

- **WHEN** 用户点击表格删除按钮
- **THEN** ConfirmPopover 确认后调用 DELETE API
