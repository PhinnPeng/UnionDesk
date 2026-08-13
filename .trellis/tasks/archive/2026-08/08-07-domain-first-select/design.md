# 设计：业务域首次选择页（收藏置顶 + 登录引导进入）

## 1. 目标与边界

- 通用登录页登录成功后，多域用户**每次**进入「业务域选择页」（不记忆默认域），选择后进入；单域用户直进首页；零域用户进入选择页引导态
- 选择页仅展示**已加入**的业务域卡片，收藏（★）置顶，点击星标即时切换并重排
- 「加入新业务域」入口（开放域 + 邀请码弹层）
- 日常切换继续走顶部下拉栏（08-domain-switcher 方案，本任务仅复用收藏接口，下拉 UI 实施另列）
- 员工端（管理端）不做收藏排序（out of scope）

## 2. 后端设计

### 2.1 收藏域读写接口（uniondesk-app / auth）

```
GET /api/v1/auth/me/favorite-domains
  → { "domain_ids": [4, 1] }            # 有序收藏列表

PUT /api/v1/auth/me/favorite-domains
  body { "domain_ids": [4, 1] }
  → 200 { "domain_ids": [4, 1] }        # 全量覆盖保存
```

- 校验：domain_ids 必须为当前账号**已加入**的域（白名单过滤，非法 id 静默剔除或 400——设计：过滤 + 返回实际保存值）
- 存储：`user_config`（user_id + config_key='favorite_domain_ids' + value_type='json'），复用现有 UserConfigService（新增 getFavoriteDomainIds / saveFavoriteDomainIds）
- 鉴权：需登录（JWT 过滤器已覆盖），无需额外 @RequirePermission

### 2.2 登录响应排序（customer 分支）

- `AuthService.loginCustomer`（uniondesk-app auth/core/AuthService.java:232）：构造 `accessibleDomains` 时按 `favorite_domain_ids` 排序——收藏的域在前（保持收藏内部顺序），其余按原顺序
- staff 分支不变

## 3. 前端设计

### 3.1 shared（packages/shared）

- `types.ts`：`FavoriteDomainsResponse { domain_ids: number[] }`、`UpdateFavoriteDomainsRequest { domain_ids: number[] }`
- `api.ts`：`fetchFavoriteDomains()`（GET）、`updateFavoriteDomains(domainIds: number[])`（PUT，全量覆盖）

### 3.2 路由守卫改造（UnionDeskCustomerWeb App.tsx）

| 场景 | 行为 |
|---|---|
| 通用登录成功（多域） | → `/domains`（选择页） |
| 通用登录成功（单域） | → `/domains` 选择页（不自动进入） |
| 通用登录成功（零域） | → `/domains`（引导态） |
| 专属登录 `/d/:code/login` | → 直达该域（现有 enterDedicatedDomain 不变） |

- `App.tsx` `LandingRedirect`：通用登录成功一律 → `/domains`（选择页，不允许自动进入；单域同样进选择页）
- `RequireSession` 内：`activeDomain` 为空（未选择）→ 重定向 `/domains`
- `RequireDomain` 保留（保护需域页面）

### 3.3 选择页组件（改造现有 /domains 页面）

`apps/UnionDeskCustomerWeb/src/pages/domains/index.tsx` 重构为「业务域选择页」：

- **墨色欢迎条**（ink 底）："欢迎回来，你好 {name}" + 副标题
- **顶部卡片区**（收藏优先，固定三卡位 + 加入卡同网格）：
  - 结构：1 大卡（首个收藏，跨 2 行，云灰底 + 指标）+ 2 小卡（白底 + Soft Lift）+ 加入卡（虚线，横跨小卡列底部）
  - 排序：收藏在前；不足 3 张时显示**虚线占位卡**（默认灰虚线 `--steel`，hover 转主色虚线 `--primary` + 柔蓝底，文案"加入更多业务域"）
  - 卡片 hover：边框转主色 + 阴影加深 + `translateY(-2px)`；`:active` 复位
  - 星标点击：本地重排 + `updateFavoriteDomains` 全量保存（防抖/串行）
- **底部列表区**（第 4+ 个域）：紧凑列表行（图标/名称/描述截断/星标/进入箭头，发丝分隔，hover 柔蓝底）
- **加入入口**：顶部卡片区的虚线加入卡 → 弹层（开放域卡片 + 邀请码输入 + 加入按钮，复用现有注册/加入逻辑）
- **零域引导态**：卡片区全部占位 + 突出加入弹层
- **点击卡片**：`selectCustomerDomainLive(domainId)` → navigate `/home`
- **加载态**：卡片区 3 张骨架卡

### 3.4 与下拉栏的关系

- 收藏接口（2.1）为下拉栏（08-domain-switcher）与选择页共用
- 下拉栏 UI 实施不在本任务（方案已定）

## 4. 数据流

```
通用登录 → loginCustomer（accessibleDomains 按收藏排序）
        → LandingRedirect 分支：
            1 域 → /home
            ≥2 域 → /domains 选择页（收藏置顶）
            0 域 → /domains 引导态
选择页：点 ★ → updateFavoriteDomains → 本地重排
      点卡片 → selectCustomerDomainLive → switch-domain → /home
      加入 → 弹层（开放域/邀请码）→ 加入成功 → 卡片列表刷新
```

## 5. 兼容与风险

- `user_config` key 新增无表结构变更；`favorite_domain_ids` 为空 = 未收藏（行为与现状一致）
- 登录响应排序只影响 `accessibleDomains` 顺序，字段结构不变（前端/其他消费方兼容）
- `/domains` 路由复用，URL 不变（SEO/书签无影响）
- 选择页每次登录都出现（多域）——若后续要"记忆进入"，可在该 key 上扩展（当前不做）

## 6. 回滚

- 后端：还原 loginCustomer 排序 + 删除收藏接口（无表变更，user_config 数据无害）
- 前端：还原 App.tsx 守卫与 /domains 页面
