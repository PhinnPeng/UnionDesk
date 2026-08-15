# 员工端「工作台」聚合 + 事项配置挂入系统设置

> 2026-08-15 立项，来源：@trellis-research 模块整合研究（用户拍板「A：工作台 / Q2：可行挂入，执行调整」）。

## Goal

1. **「工作台」聚合**：新增一级菜单「工作台」（/domain/workbench），Tabs 壳页聚合「工单队列 + 在线咨询」；原两个一级菜单改隐藏（hideInMenu，路由保留可直接 URL 访问）；咨询转工单后可跳转工单上下文。原子页面与权限码（ticket.* / consultation.*）保持独立。
2. **事项配置挂入系统设置**：菜单归属调整——`/domain/ticket-config` 挂入「系统设置 → 功能配置」目录（与 SLA 管理并列），**路径保留不变**；12 个权限码与页面、角色绑定不动；父目录角色绑定按 SLA 迁移先例传播。

## 现状（勘察核实）

- 菜单：工单队列（BUSINESS-DOMAIN-TICKET-QUEUE，一级 order25）+6 按钮；在线咨询（BUSINESS-DOMAIN-CONSULTATION，一级 order26）+3 按钮；事项配置（BUSINESS-DOMAIN-TICKET-CONFIG，一级 order30）+12 按钮；系统设置（BUSINESS-DOMAIN-SETTINGS catalog order50）→ 功能配置（FEATURES catalog）下已有 参数配置/屏蔽词库/通知配置/SLA 管理
- 工单详情页：独立全页路由（hidden 顶层）；咨询：页内抽屉
- SLA 挂入先例：V20260813140000（挂靠功能配置+保留路径 /domain/sla + 父目录角色传播）
- 页面组件：`pages/domain/ticket-queue/index.tsx`、`pages/domain/consultations/index.tsx`（均 BasicContent+Card 骨架，自带域解析）

## Requirements

- R1 菜单迁移（Flyway V20260815xxxx）：新增「工作台」menu（/domain/workbench，order25，一级）+ 角色绑定（参照工单队列绑定集）；工单队列/在线咨询菜单 hidden=1（按钮不动，路由仍生成）；事项配置 parent_id → 功能配置 catalog，order_no 置 SLA 之后；角色绑定传播核查补绑（父目录）
- R2 前端工作台壳页 `pages/domain/workbench/index.tsx`：Tabs（工单队列/在线咨询）内嵌两个原子页面组件（复用现有页面组件，保留各自滚动/守卫）；路由注册 /domain/workbench；咨询转工单成功后提供「前往工单」跳转（在工作台上下文内切换 Tab 并打开工单）
- R3 权限：工作台入口权限 = ticket.view.domain_all 或 consultation.view 任一（页面内各自 AuthGuarded 保持）；权限码零改动
- R4 事项配置：菜单挂靠后页面/路由/权限码/路径全部不变；面包屑由后端菜单树自动正确
- R5 不动平台端（/platform/ticket-config 独立 scope）

## Acceptance Criteria

- [ ] AC1 登录后左侧菜单：出现「工作台」一级菜单；工单队列/在线咨询不再显示为一级菜单；事项配置显示在 系统设置→功能配置 下（SLA 管理之后）
- [ ] AC2 工作台 Tabs 内：工单队列列表可用（筛选/领取/指派/关闭）；在线咨询列表/抽屉/回复/转工单可用
- [ ] AC3 直接访问 /domain/ticket-queue、/domain/consultations 仍可打开（路由保留）
- [ ] AC4 咨询转工单成功后可在工作台跳转该工单（工单 Tab 打开详情）
- [ ] AC5 /domain/ticket-config 直接访问与菜单进入均正常（类型/属性/状态三段）；面包屑为 系统设置→功能配置→事项配置
- [ ] AC6 typecheck 通过；浏览器冒烟通过

## Constraints

- 原子页面组件不改业务逻辑（仅必要时导出复用）；不改权限码；临时脚本仅放 agent-work/
- 菜单迁移以 code 为幂等键；角色传播照 V20260813140000 先例
