# E-EMP3 状态机与SLA治理

> 2026-08-14 立项，`08-14-employee-handling-chain` 子任务（P1）。对应 PRD F2.1(状态机)/F2.2(SLA)。

## Goal

治理工单**状态机两套割裂**问题，并接入 **SLA 感知高亮**。当前运行时 `changeTicketStatus` 用硬编码 `isAllowedStatusTransition`，而可配置工作流（`TicketTypeFlowService`/`StatusFlowValidator`）未接入动作路径；DB 内置状态码与运行时码不一致。SLA 引擎（`SlaTimingEngine`）已实现但未接工单 UI。

## 现状（勘察核实）
- `TicketService.changeTicketStatus:200-202` 调 `isAllowedStatusTransition:688`（硬编码 open/new/processing/resolved/closed/withdrawn/merged）
- 运行时动作路径**不调用** `StatusFlowValidator`/`TicketTypeFlowService`（仅配置/复制期调用）
- DB 预置状态码 `not_started/in_progress/completed/cancelled`（`V202607070002`），与运行时码不一致 → 下拉可选状态与运行时合法状态不符
- SLA 引擎已实现（feature-list F4.15 / SlaTimingEngine），工单列表/详情 UI 未接

## Requirements
- R1 **运行时接可配置工作流**：`changeTicketStatus` 的合法流转校验改走 `TicketTypeFlowService`/`StatusFlowValidator`（按工单类型取配置流转），回退到默认流转；非法流转返回业务文案（如"状态流转不合法"）
- R2 **DB 码统一**：确认并消除 DB 内置状态码与运行时码不一致（对齐口径，必要时 Flyway 迁移），保证"下拉可选状态 ⊆ 运行时合法状态"
- R3 **SLA 高亮（F2.2）**：工单列表/详情接 `SlaTimingEngine` 计算 SLA 状态（正常/即将超时/已超时），前端 Tag/Tooltip 高亮
- R4 保障既有规则：TR-02（终态不可变更）、FR-01/FR-03 不破坏

> 决策默认：状态机**直接接可配置工作流**（治本），SLA 本轮纳入。

## Acceptance Criteria
- [ ] AC1 配置的状态流转在 `PATCH /status` 生效；非法流转返回业务文案
- [ ] AC2 下拉可选状态与运行时合法状态口径一致（无"可选但被拒"）
- [ ] AC3 工单列表/详情即将超时或已超时工单有高亮标识，与 SLA 数据一致
- [ ] AC4 `FrRulesAcceptanceTest`（FR-01/03、TR-02）全绿；既有工单/状态测试不回归

## Out of Scope
- 客户端动态表单/工单渲染（F1.1）
- 新状态流转编排 UI（仅接入既有可配置工作流）
