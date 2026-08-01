# 团队模板

## Goal

在平台「事项配置」提供**团队模板**（业务域事项方案包）：管理员组合若干平台事项类型；创建业务域时可选用模板，系统将类型及其装配**一次性深拷贝**到域内。模板有效内容随平台依赖（事项类型当前配置）变化；已建域快照不随模板/平台变更回写。

## Confirmed Decisions

| 项 | 决定 |
|:---|:---|
| 产品口径 | **团队模板**（不用「配置方案」） |
| 模板与依赖 | 模板项引用平台 `ticket_type`；建域时取依赖**当时**配置深拷贝 |
| 已建域 | 与模板解耦；不可换模板、不可模板升级推送 |
| 模板项效果 | 指定拷贝哪些平台类型、顺序、是否含表单 schema / 工作流 / 描述模板 |
| 入口 | 事项配置侧栏与属性/类型/状态并列 |
| 建域 | 可选模板；不选则空域 |
| 系统属性 | MVP 槽位可继续引用平台系统属性 id（与现网一致） |

## Requirements

- R1：`ticket_team_template` + `ticket_team_template_item`；`business_domain.applied_team_template_*`
- R2：平台 CRUD API + 权限 `platform.ticket_config.template.*`
- R3：事项配置侧栏「团队模板」列表/新建/编辑（勾选平台类型 + 排序 + include 开关）
- R4：建域可选 active 模板；`TeamTemplateApplyService` 深拷贝类型/槽位/published schema/flow/描述模板
- R5：种子系统模板（如默认客服方案，挂现网平台类型）
- R6：改模板 items 时 `version++`；仅影响之后新建域

## Acceptance Criteria

- [ ] AC1：事项配置左侧可见「团队模板」，可 CRUD（系统模板不可删）
- [ ] AC2：模板至少关联 1 个 `scope=platform` 的事项类型
- [ ] AC3：建域选择模板后，域内出现对应类型及已勾选的装配副本
- [ ] AC4：建域后修改平台类型或模板，已建域配置不变
- [ ] AC5：不选模板可建空域；disabled 模板不可被选用
- [ ] AC6：权限码与侧栏/按钮 AuthGuarded 生效

## Out of Scope

- 已建域换模板 / 模板升级合并
- 把平台变更热推到已建域
- 复制域 SLA/通知完整配置（可后续）
- 文案「配置方案」双名并存
- 旧 `ticket_template`（单类型 content 预设）改造

## References

- `openspec/changes/matter-config-global/design.md` §4 / §10.1（背景；本任务以现网 `ticket_type` 为准）
- 会话收敛：模板随依赖变、域跟快照走
