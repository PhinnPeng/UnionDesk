# 业务域首次选择页：收藏置顶 + 登录引导进入

## Goal

客户 Web 通用登录后，多域用户进入「业务域首次选择页」选择进入域：仅展示已加入业务域，收藏（★）置顶排序、点击星标即时切换；单域用户直进首页；零域用户进选择页引导态。配套收藏域读写接口与登录响应排序。

## Background（已确认事实）

- 页面定位：**登录后首次选择页**（每次通用登录都出现，不记忆默认域）；日常切换走顶部下拉栏（Frameground 08-domain-switcher 方案，本任务不实施下拉 UI）
- 内容：**仅已加入域卡片 + 「加入新业务域」入口**（开放域/邀请码弹层）
- 视觉：白底 + 顶部墨色欢迎条 + 云灰带衬托白色域卡片（DESIGN.md 语言，Frameground 09-domain-select frame 已落地）
- 现状代码：`/domains` 独立页存在（AppShell 顶栏点击跳转）；`enterDedicatedDomain` 专属登录直达；`preferredDefaultDomainId` 存 `user_config`（user_id + config_key + config_value + value_type）
- 收藏存储：`user_config` 新增 `config_key='favorite_domain_ids'`（value_type='json'，有序数组）

## Requirements

- R1 后端收藏接口：`GET /api/v1/auth/me/favorite-domains`（读有序收藏列表）、`PUT /api/v1/auth/me/favorite-domains`（全量覆盖保存，body `{domain_ids}`）；domain_ids 白名单过滤（必须为已加入域）
- R2 登录响应排序：`loginCustomer` 的 `accessibleDomains` 按收藏列表排序（收藏在前、保持收藏内部顺序，其余保序）；staff 分支不变
- R3 前端 API：shared 新增 `fetchFavoriteDomains` / `updateFavoriteDomains` 与类型
- R4 路由守卫（不允许自动进入）：通用登录后一律进入 `/domains` 选择页（不记忆、不自动进入，养成选择习惯）；`RequireSession` 中未选择域（activeDomain 空）→ `/domains`；专属登录直达该域不变
- R5 选择页（改造 `/domains`）：墨色欢迎条 + **顶部卡片区**（1 大卡 + 2 小卡 + 加入卡同网格，收藏优先；不足 3 张显示虚线占位卡：默认灰虚线、hover 主色虚线）+ **底部列表区**（第 4+ 个域紧凑列表行）＋ 卡片 hover 交互（边框主色 + 阴影 + 上移 2px）＋ 加入弹层（开放域/邀请码）＋ 零域引导态 + 加载骨架；样式按 DESIGN.md（参考 Frameground 09/09a/09b/09c frame）
- R6 收藏接口为选择页与顶部下拉栏共用（下拉 UI 实施不在本任务）

## Acceptance Criteria

- [ ] AC1 GET/PUT 收藏接口可用：PUT 保存后 GET 回读一致；非法域 id 被过滤；未登录 401
- [ ] AC2 多域客户登录响应 `accessibleDomains` 收藏域在前
- [ ] AC3 多域通用登录 → `/domains` 选择页，收藏域卡片置顶
- [ ] AC4 选择页点 ★ 切换收藏并本地重排，接口保存成功
- [ ] AC5 选择页点击卡片进入对应域 `/home`（switch-domain 生效，顶部下拉显示当前域）
- [ ] AC6 通用登录一律进选择页（单域/零域/多域均不自动进入）；零域 → 选择页引导态
- [ ] AC7 专属登录直达该域不变
- [ ] AC8 后端 `mvnw compile` 通过；前端 typecheck（admin + customer-web）与 check:utf8 通过；浏览器冒烟全链路

## Out of Scope

- 顶部下拉栏 UI 实施（方案已定，另行任务）
- 「记忆上次进入域」/ 登录自动进默认域（用户已确认：通用登录每次都进选择页）
- 员工端收藏排序
- 收藏域作为默认域（preferredDefaultDomainId 语义不变）

## Technical Notes

- 后端改动：uniondesk-app（UserConfigService 扩展 + AuthController 收藏接口 + AuthService.loginCustomer 排序）
- 前端改动：shared（类型 + API）、CustomerWeb（App.tsx 守卫分支 + pages/domains 重构）
- 详细设计见 `design.md`，执行计划见 `implement.md`
