# CustomerWeb 首页设计落地（Figma 132:155 服务台首页）

## Goal

按 Figma 设计稿 132:155「Pixso 迁移 · 服务台首页」（1440×1024）重构 CustomerWeb 首页视觉与共享壳层：**240px 侧边栏**（品牌头 + 5 项导航 + 底部三栏）+ **顶栏**（页标题 / 搜索框 / 提交工单按钮）+ **渐变欢迎卡** + **统计卡 ×3** + **双栏**（我的工单 / 未读通知）+ **底部版权**。数据全部来自现有 portal 快照（不新增接口），路由与跳转行为保持，移动端 dock 保留。

## Background（已确认事实）

- 设计稿 132:155 结构（get_design_context 已取回）：
  - `aside.sidebar` 240px：`div.sidebar__header`（渐变 U 标 #4F46E5→#06B6D4 + UnionDesk 文字）+ `nav.nav` 5 项（首页/工单/咨询/通知/我的，每项 215×41 圆角 8；**首页激活态** = 浅紫底 rgba(79,69,229,0.08) + 左侧 3px 紫条 #4F45E5 + 紫字紫图标；闲置 = 灰 #6B7080；咨询带「二期」红角标 #F04545 38×21 圆角 6；通知带未读数角标 22×21）+ 侧边栏底部三栏：栏1 设置|消息（竖线分割，消息带未读红角标 3）、栏2 业务域「默认业务域 ▼」、栏3 个人身份（渐变头像 135deg #4F45E5→#05B5D4 首字 + 姓名「张三」）
  - `header.topbar` 64px：页标题 24px #1F2938 + 搜索框（#F7FAFC 400×40 圆角 16，图标 + 「搜索工单、通知...」）+ 右侧「提交工单」按钮（#4F45E5 120×39 圆角 8 白字）
  - `div.welcome-card`：渐变 #4F45E5→#7D3BED 高 138 圆角 20；「下午好，张三」28px 白 + 「有 3 个待处理的工单需要您的关注」16px 白 + 右侧「查看待处理」按钮（rgba(255,255,255,0.2) 120×45 圆角 8）
  - `div.stats-grid` ×3 白卡 365×187 圆角 16（gap 20）：待处理 3 / 进行中 5 / 已完成 8；48px 图标块圆角 12 浅色底（紫/青/绿 8% 透明）+ 深色图标 #1F2937 + 数字 32px 主题色 + 标签 14px #6B7380
  - `div.two-col`：右「未读通知」380px 卡（标题 18px + 「全部通知 →」#4F45E5 + 3 行：40px 图标块圆角 12 浅紫底 + 14px 标题 #1F2938 + 12px 时间 #9CA3B0）；左「我的工单」卡（标题 + 「查看全部 →」+ body 为栅格化图片，需用真实数据实现）
  - 底部版权「© 2026 UnionDesk · 客户服务中心」居中（node 152:3，与登录页同款）
- 设计色板：主紫 #4F45E5（渐变至 #7D3BED）、青 #05B5D4、绿 #0FBA82、红角标 #F04545、文字 #1F2938/#6B7080/#9CA3B0、浅底 rgba(79,69,229,0.08)/rgba(5,181,212,0.08)/rgba(15,186,130,0.08)、分隔 rgba(0,0,0,0.1)、搜索框 #F7FAFC
- 图标：设计为 **MD 风格实心图标**（home/ticket/chat/bell/user/gear/message/domain-grid/search/doc/clock/check-circle/shield），Figma 导出的 SVG **内置固定颜色**（#4F46E5/#6B7280/#64748B/#1F2937），未用 currentColor；资源已下载至 `agent-work/home-figma/`（imgIconNav*.svg / imgFrame*.svg / imgIconSolid*.svg / imgFrame3/4.svg），path 数据可直接取用
- 现状：`AppShell.tsx` 76px 图标栏 + 顶栏（标题/搜索/域 chip/头像）+ 移动端 dock；`pages/home/index.tsx` 玻璃卡 hero + 生命周期卡 + 最近工单列表（新版旧版并存需替换）；`tokens.css` 蓝调 --ud-* 体系（登录页 132:2 已依赖，**不可改动**）；`Icons.tsx` 为描边风格图标（与设计实心图标字形不同，需新增）
- 数据源（shared customer-portal 已确认）：`portal.account.displayName`（姓名）、`portal.activeDomain.name`（业务域）、`portal.unreadCount`（角标）、`portal.currentDomainTickets`（统计 + 我的工单）、`portal.inboxMessages`（未读通知，含 kind: ticket|domain|system / title / createdAt / isRead / jumpUrl）；`countByLifecycle`（utils/ticket-lifecycle.ts）与 `formatDateTime`（utils/date.ts）已有

## Requirements

- R1 侧边栏：76px 图标栏 → 240px 全宽导航（品牌头 + 5 项 + 底部三栏）；激活态/闲置态/角标按设计；「通知」角标接 `unreadCount`（>9 显示 9+）；「咨询」保持「二期」角标且不可达（soon）
- R2 顶栏：页标题 + 搜索框（placeholder「搜索工单、通知...」，回车跳 `/tickets?q=...`，行为不变）；右侧「提交工单」按钮 → `/tickets/new`；原域 chip/头像从顶栏迁出（桌面由侧边栏底部三栏承载；**移动端 <900px 侧边栏隐藏时域 chip 保留在顶栏**）
- R3 欢迎卡：渐变紫、问候（上午好/下午好/晚上好按小时 + 姓名）、待处理数文案「有 {n} 个待处理的工单需要您的关注」（n=0 时「暂时没有待处理的工单」）、右侧「查看待处理」→ `/tickets?life=pending`
- R4 统计卡 ×3：待处理 `life.pending` / 进行中 `life.active` / 已完成 `life.done`，主题色数字 + 浅色底图标块，点击跳 `/tickets?life=...` 对应值
- R5 双栏：左「我的工单」卡（标题 + 「查看全部 →」→ `/tickets` + 最近工单行：类型标签/标题/StatusTag/时间，复用 `StatusTag`）；右「未读通知」卡（未读优先取 3 条：图标块按 kind 区分 [ticket→工单图标、system→盾牌图标、domain→工单图标兜底] + 标题 + 相对时间 + 行点击跳 `jumpUrl`；「全部通知 →」→ `/inbox`）
- R6 版权「© 2026 UnionDesk · 客户服务中心」底部居中（本文件内可编辑常量，与登录页文案一致）
- R7 图标：按设计导出 path 数据新增 **currentColor 实心图标组件**（fill="currentColor"，导航激活/闲置态由 CSS 着色），保留现有 Icons.tsx 全部导出；新增图标仅服务本任务页面（nav 5 + footer 3 + 搜索 1 + 统计 3 + 通知 2 = 14 个）
- R8 令牌：tokens.css **新增** `--ud-cx-*` 设计色板 token（cx = Customer 设计稿），不改动现有 --ud-*（登录页已依赖）
- R9 行为保持：路由、导航、搜索、空域状态（「先选择一个服务空间 / 去选择业务域」保留）、移动端 dock；其他页面（工单/通知/我的/业务域）壳层适配后不回归

## Decisions（已确认）

- D1 图标策略：设计实心图标转 currentColor 组件（path 数据取自 agent-work/home-figma/ 下载的 SVG，viewBox 保持 20/14/18/24 不变），尺寸由 CSS 上下文控制（沿用现有 .ud-icon + 局部覆写模式）
- D2 色板策略：新增 --ud-cx-* token 承载设计色（主紫/青/绿/红/文字三档/浅底），不污染现有蓝调 token
- D3 内容宽度：--ud-content-max 1080px → 1136px（设计内容区 1136 宽，配合 100%-40px 在 1440 下左右各 32px 边距）；其他页面受益于更宽布局，无回归风险
- D4 顶栏头像移除（桌面由侧边栏栏3承载，移动端 dock 我的已覆盖）；域 chip 桌面隐藏、移动端显示（设计桌面顶栏无 chip）
- D5 「我的工单」卡 body：设计稿为栅格化图片 → 用真实数据实现紧凑行（类型标签 + 标题 + StatusTag + 相对时间，行间分隔线）
- D6 相对时间：home 文件内私有函数（「刚刚/x 分钟前/x 小时前/x 天前」→ 超 7 天回落 formatDateTime），仅本文件使用不抽公共
- D7 栏1「设置」无独立页面 → 跳 `/me`（账户设置语义）；「消息」→ `/inbox`（带未读角标）
- D8 窄屏适配：<900px 侧边栏隐藏 + 顶栏搜索隐藏（现有）；「提交工单」按钮 ≤480px 隐藏（dock 工单页可达）；首页卡片流式堆叠不溢出

## Acceptance Criteria

- [ ] AC1 桌面 1440 下首页与 132:155 视觉对齐：侧边栏/顶栏/欢迎卡/统计卡/双栏/版权逐块核对
- [ ] AC2 导航激活态样式正确（首页默认激活：浅紫底 + 3px 紫条 + 紫字紫图标）；未激活灰字灰图标；hover 有反馈
- [ ] AC3 数据驱动：统计数字、待处理文案、未读角标（通知 + 栏1消息）、通知列表、姓名、业务域名均来自 portal 快照；空域状态保留
- [ ] AC4 交互正确：搜索回车、提交工单、查看待处理、查看全部、通知行 jumpUrl、底部三栏（业务域/身份/消息）跳转
- [ ] AC5 移动端 <900px：dock 正常、域 chip 可访问、首页卡片不溢出；≤480px 提交工单按钮隐藏
- [ ] AC6 其他页面（/tickets /inbox /me /domains /chat）壳层渲染正常，无样式回归
- [ ] AC7 前端 typecheck（customer-web）+ check:utf8 通过；浏览器冒烟首页渲染正常

## Out of Scope

- 通知行已读交互（首页卡片仅跳转，标记已读在 /inbox 现有功能）
- 「设置」独立页面（D7 仅跳 /me）
- 登录页 / AdminWeb / 忘记密码
- 设计稿中栅格化图片区域的像素级还原（以真实数据实现为准，D5）

## Technical Notes

- 改动文件：`UnionDeskWeb/apps/UnionDeskCustomerWeb/src/components/AppShell.tsx`（壳层重构）、`src/components/Icons.tsx`（+14 图标）、`src/pages/home/index.tsx`（重写）、`src/styles/tokens.css`（+--ud-cx-*）、`src/styles/app.css`（侧边栏/顶栏/首页样式块）
- 图标 path 数据源：`agent-work/home-figma/`（临时目录，任务结束后清理；path 需内嵌进 Icons.tsx）
- 无新增依赖
- 详细设计见 design.md，执行计划见 implement.md
