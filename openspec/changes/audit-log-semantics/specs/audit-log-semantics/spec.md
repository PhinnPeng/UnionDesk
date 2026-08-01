## ADDED Requirements

### Requirement: 操作日志动作中文展示

系统 MUST 为审计记录保留机器码 `action` 供筛选，并向 UI 提供对应中文 `actionLabel`。

#### Scenario: 业务域更新动作展示

- **WHEN** 审计记录 `action=platform.domain.update`（或兼容旧码 `domain.update`）
- **THEN** 列表展示动作「业务域更新」；筛选仍可按机器码查询

#### Scenario: 业务域状态变更动作展示

- **WHEN** 审计记录为启用或禁用业务域
- **THEN** 动作展示为「业务域启用」或「业务域禁用」（明细中说明变更前后状态）

### Requirement: 操作日志目标可读格式

写入审计时，目标字段 MUST 使用 `资源名称-短码` 格式，而非 `type:id` 技术格式。

#### Scenario: 业务域目标格式

- **WHEN** 对业务域 `name=演示域`、`code=demo` 执行更新
- **THEN** `target` 为 `演示域-demo`

### Requirement: 操作日志明细多行语义摘要

审计 `detail` MUST 以操作完成后的可读多行文本记录关键变更，支持换行展示。

#### Scenario: 角色权限更新明细

- **WHEN** 管理员更新平台角色菜单权限，新增与移除若干菜单/按钮
- **THEN** 写入审计明细，包含角色标识、新增权限列表（中文菜单路径）、移除权限列表（中文菜单路径），每条占一行或一段

#### Scenario: 业务域创建与删除

- **WHEN** 创建或删除业务域成功
- **THEN** 动作分别为「业务域创建」「业务域删除」；目标为 `名称-短码`；明细含操作完成后域名称与短码

#### Scenario: 旧动作码筛选兼容

- **WHEN** 历史记录 `action=domain.update` 或新记录 `action=platform.domain.update`
- **THEN** 均展示「业务域更新」；按机器码筛选时可命中对应码或 catalog alias 聚合

#### Scenario: 历史 JSON 明细兼容

- **WHEN** 列表展示 `detail` 以 `{` 开头的旧 JSON 记录
- **THEN** UI 降级为格式化 JSON 或简要摘要，不报错

### Requirement: 登录日志字段中文展示

登录日志列表 MUST 将门户类型、登录结果等技术值映射为中文标签展示。

#### Scenario: 登录结果展示

- **WHEN** `result=success` 或 `result=failure`
- **THEN** 列表分别展示「成功」「失败」（或项目统一文案）
