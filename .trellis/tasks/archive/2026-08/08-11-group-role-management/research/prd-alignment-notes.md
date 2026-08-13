# Research: prd.md V2.2 对齐事实

- **Query**: 现 PRD §4.3 页面结构 vs 实际页面（过时/缺失）；现 PRD §4.2 功能清单（F1.1–F4.4）vs 实际实现（已实现/未覆盖）
- **Scope**: mixed（prd.md + 双端代码勘察）
- **Date**: 2026-08-11

## Findings

### A. PRD §4.3 页面结构 vs 实际（当前 11 行 → 实际页面）

| PRD §4.3 行 | 实际页面 | 结论 |
|:---|:---|:---|
| 客户端 服务首页/提单页（F1.1, F1.3） | CustomerWeb `/home` + `/tickets/new` | 基本一致；提单页为静态字段（非动态表单） |
| 客户端 在线咨询窗口（F1.2） | CustomerWeb `/chat` | **占位页**（「即将开放」） |
| 客户端 我的工单/咨询历史（F1.4） | CustomerWeb `/tickets`、`/tickets/:id` | 我的工单一致；**咨询历史无** |
| 客户端 通知中心/评价入口（F1.5） | CustomerWeb `/inbox` | 通知中心已实现；**评价入口无（全站无满意度功能）** |
| 员工端 工单队列/工单详情（F2.1, F2.2） | AdminWeb `/platform/ticket-pool`、`/platform/ticket-detail`（P0 演示） | **已过时**：仅平台端演示页；员工工作台归属业务域端（`/home` 域端工作台，工单队列 US-S3-04 Todo）；SLA 高亮无 |
| 员工端 在线咨询工作台（F2.3） | 无 | **缺失**（E5 未排期） |
| 域管理后台 工单类型/SLA/通知模板（F3.1, F3.2） | AdminWeb 域端 `/domain/ticket-config` + `/domain/settings/notifications` + 平台 `/platform/sla-management` | 部分一致：工单类型 Done；SLA 在平台端实现（PRD 归域后台，实际在平台菜单）；通知模板域端占位 |
| 域管理后台 成员/客户/角色管理（F3.3） | AdminWeb 域端 `/domain/settings/{members,roles}`、`/domain/customers/list` | 一致（均 Done） |
| 平台管理后台 业务域管理/模板中心（F4.1, F4.2） | AdminWeb `/platform/domains`、`/platform/ticket-config/templates` | 业务域 Done；模板中心=团队模板已实现但**菜单已隐藏**（V20260726092200） |
| 平台管理后台 员工管理/离职池（F4.3） | AdminWeb `/platform/user`、`/platform/offboard-pool` | 一致（Done；导入导出 Todo） |
| 平台管理后台 安全策略/告警中心（F4.4） | AdminWeb `/platform/system-settings` | 系统设置 KV 已实现；**安全告警中心无（inventory §5.3 Todo）** |

**PRD 未列但实际存在的页面/模块**（V2.2 §4.3 建议补充）：
- 客户端：登录页（含滑块验证）、注册页、业务域选择页、个人中心（我的）、修改密码页
- 管理端-平台端：登录页、IAM 用户/角色/菜单/部门、组织配置（占位）、离职池、审计/登录日志、全局屏蔽词、事项配置全家桶（类型/属性/状态/模板）、SLA 管理、站内信、系统设置、附件、导入导出（占位）、域配置 KV
- 管理端-业务域端：域端首页/工作台、系统（用户/角色/菜单/部门，其中 user/dept 占位）、域设置 10 项（概览/事项配置/客户/入域/基础/成员/角色/参数/屏蔽词/通知占位/审计/登录日志）
- 异常页/隐私政策/服务条款/关于/access 演示等模板页（可归「模板遗留」备注）

### B. PRD §4.2 功能清单（F1.1–F4.4）vs 实际实现

| 编号 | 功能 | 状态 | 证据 |
|:---|:---|:---|:---|
| F1.1 提交工单（动态表单） | **Partial** | CustomerWeb 提单走真实 API，但仅 title/description 静态字段；`form_schema` 动态字段未渲染（attachmentIds 空数组）；联调链路存在 |
| F1.2 在线咨询 | **Todo** | `/chat` 占位页；E5 未排期 |
| F1.3 反馈/建议（工单类型路径） | **Done** | 「反馈」「建议」预置类型 + 启用/停用（US-S3-01 AC4）；无独立入口符合 MVP 决策 |
| F1.4 我的工单/咨询历史 | **Partial** | 我的工单/详情/回复/撤回 Done；咨询历史无 |
| F1.5 满意度评价 | **Todo** | 全站无（grep 0 命中）；评价入口、评价 API 均无 |
| F2.1 工单队列与详情处理 | **Partial** | 平台端 P0 演示页（ticket-pool/ticket-detail）；域端员工工单队列 US-S3-04 Todo |
| F2.2 SLA 感知与高亮 | **Todo** | 工单列表/详情无 SLA 高亮；SLA 规则引擎已有但未接入工单 UI（E4） |
| F2.3 在线咨询工作台 | **Todo** | E5 未排期 |
| F3.1 工单类型设计 | **Done** | Formily 表单设计器 + React Flow 状态流 + 属性插槽（US-S3-01/01a） |
| F3.2 SLA 规则与通知模板 | **Partial** | SLA 规则/日历平台端已实现（sla-management + 后端 sla 包）；**通知模板 = 域端 Empty 占位**（「功能开发中，菜单与权限已就绪」） |
| F3.3 成员/客户/角色管理 | **Done** | 域端 members/customers/roles 真实 API + 平台侧域详情 Tab（S2） |
| F4.1 业务域创建与管理 | **Done** | 列表/创建向导/详情 10 Tab/软删 step-up |
| F4.2 模板中心 | **Partial** | 团队模板（ticket-config/templates）已实现但菜单隐藏；「域模板提炼」语义与实现不完全一致 |
| F4.3 员工账号与离职池 | **Partial** | 用户 CRUD/离职/离职池/重置密码 Done；**导入导出 Todo**（import-export 页面存在 API 待查）；永久删除前端无入口 |
| F4.4 系统设置与安全告警 | **Partial** | 系统设置 KV Done；**安全告警中心 Todo**；step-up 部分（域删除） |

### C. PRD 未覆盖但已实现（V2.2/feature-list 新增条目建议，供用户确认）

| 建议编号域 | 功能 | 端侧 | 状态 |
|:---|:---|:---|:---|
| M1 新增 | 客户端登录（含滑块验证、专属域入口、新环境提醒） | 客户端 | Done |
| M1 新增 | 客户端注册与入域（注册/邀请码） | 客户端 | **Partial（本地 mock，US-S3-02）** |
| M1 新增 | 业务域选择/切换 | 客户端 | Done |
| M1 新增 | 个人中心/账号信息/退出登录 | 客户端 | Done（通知偏好占位） |
| M1 新增 | 修改密码（mustChangePassword 强制） | 客户端 | Done |
| M1 新增 | 站内信/通知中心（工单进展+系统消息） | 客户端 | Done |
| M2/M4 新增 | 审计日志/登录日志（平台+域双入口） | 管理端 | Done（两套页面重叠） |
| M4 新增 | 全局屏蔽词 + 域屏蔽词 | 管理端 | Done |
| M4 新增 | 组织/部门管理 | 管理端 | Done（org-config 占位） |
| M4 新增 | 菜单管理/角色权限（动态路由+按钮权限） | 管理端 | Done |
| M4 新增 | 事项属性/状态/团队模板 | 管理端 | Done（templates 隐藏） |
| M4 新增 | SLA 规则/工作日历 | 管理端 | Done（文档 E4 占位 vs 代码不符） |
| M4 新增 | 附件上传（MinIO） | 双端 | Done（P0 契约） |
| M4 新增 | 客户管理增强（编辑/重置密码/从员工导入） | 管理端-域 | Done |
| M4 新增 | 系统参数设置 KV | 管理端 | Done |

### D. 其他对齐事实

- **四端 → 双端**：PRD M1–M4 模块映射 —— M1=客户端；M2 员工端 + M3 域后台 → 管理端-业务域端；M4 平台后台 → 管理端-平台端（design.md §2 已定口径，与代码一致：员工工作台 `/home` 域端 scope=business）
- **账号体系**：客户账号登录已走真实 JWT（`login` API + `loadAuthSession`），与 PRD §4.1.1「统一身份、两端分离」方向一致；注册仍 mock
- **SLA 文档漂移**：backlog-epics E4 标「占位/S3 Stretch」，但代码（前端 sla-management 页 + 后端 `com.uniondesk.sla` 包含 SlaRuleController/SlaService/SlaTimingEngine/SlaCalendarPo/TicketSlaPolicyPo）已具雏形 —— feature-list 状态建议标「已实现（未挂验收 Story）」，备注与 backlog 差异
- **模板/演示页**：AdminWeb `/access/*`、`/route-nest/*`、`/about`、`/outside/*`、`/system/dept`（计数器）、`/system/user`（单 Input）、personal-center、dev POC（路由未注册）为模板遗留，建议在 feature-list 备注列标注「模板遗留，非业务功能」
- **历史版本行**：V2.1 版本说明表仅 V2.0/V2.1 两行，V2.2 需加行并保留历史（R1）

## Caveats / Not Found

- PRD §2.4 指标 ×4、§3.4 漏斗 ×5、§6.1 埋点、§6.4 筹备为「待确认」项（任务 R1 标「验收期回填」，不属本研究报告范围）
- 未核实点：`platform/ticket-pool`、`sla-management` 等页面在联调库 `iam_admin_menu` 的实际菜单挂载（代码存在，种子核对未逐一完成）
