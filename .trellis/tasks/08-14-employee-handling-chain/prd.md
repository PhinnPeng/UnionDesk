# 员工端处理链路打通（本次迭代）

> 2026-08-14 立项。父任务，承载本次迭代整体，含 4 个子任务：S-00 冒烟 / E-EMP1 咨询闭环 / E-EMP2 工单增强 / E-EMP3 状态机与 SLA。
> 依据：勘察 `docs/product/employee-handling-chain-plan.md` + `docs/product/prd.md` §5.2 + `docs/product/feature-list.md` F2.1~F2.3。

## Goal

将员工端处理链路从「代码就绪+后端测试通过+页面已挂路由」推进到「可用且闭环」：前端页面存在但**未经人工端到端冒烟**，且有明确缺口（咨询接入/结束/撤回、批量动作、内部备注时间线、状态机割裂、SLA 未接 UI）。本迭代先冒烟暴露真实问题，再定点补齐与增强。

## 已就绪（勘察核实，非本迭代新增）
- 工单队列：列表/分页/筛选（status/assignee/priority/keyword/assigned_to_me）→ `TicketController:83` + `pages/domain/ticket-queue`
- 工单详情：回复/附件/领取/指派/关注人/状态/关闭/合并 → `TicketController:109-178` + `detail.tsx`
- 咨询：列表/查看/回复/转工单 → `ConsultationRuntimeController` + `pages/domain/consultations`
- 工单权限码齐 + 目录 + 菜单种子；咨询权限码(view/convert/customer)在 PermissionCodes 与 DB 种子

## 明确缺口（本迭代目标）
| 子任务 | 缺口 | 对应 PRD |
|---|---|---|
| S-00 | 双端端到端人工冒烟未做；前端页面级测试=0 | 全量 |
| E-EMP1 | 咨询无 claim(接入) / end(结束) / 撤回收回；`consultation.*` 未入 AdminPermissionCatalog | F2.3 接待/聊天/转单 |
| E-EMP2 | 无批量领取/批量关闭；内部备注与时间线分组待完善 | F2.1 |
| E-EMP3 | 运行时状态机用硬编码 isAllowedStatusTransition，可配置工作流未接入；DB 内置码与运行时码不一致；F2.2 SLA 高亮未接 UI | F2.1/F2.2 |

## 子任务顺序与依赖
1. **S-00**（前置门禁，优先）：冒烟暴露真实问题 → 转成定点修复项
2. **E-EMP1**（P0，次优先，直接承接客户咨询权限）
3. **E-EMP2** / **E-EMP3**（P1，可与 EMP1 部分并行）

## 决策（默认已定，待用户最终敲定时改）
- 咨询接入 = **手动抢单 claim**（PRD F2.3「客服手动从队列中接入」）
- 状态机 = **直接接可配置工作流**（治本）+ 消除 DB 码不一致
- F2.2 SLA 高亮本轮纳入

## Acceptance（父级）
- [ ] 4 个子任务均实现并归档；父任务 children 全部 completed
- [ ] S-00 冒烟阻塞项清零后 EMP1-3 才可签 off
- [ ] 员工端"队列→领取→处理→关闭"与"咨询→接入→结束/转单"人为可闭环

## Out of Scope
- 客户端动态表单深层渲染（F1.1，另行立项）
- 客户端在线咨询成品交互（撤回等属 EMP1 员工端，客户端消息撤回若涉及另议）
- 平台端旧 `/platform/ticket-pool` 演示页迁移（不在本迭代）
