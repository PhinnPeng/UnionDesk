# 实施计划：业务域首次选择页

## 任务地图

```
P1 后端：UserConfigService 收藏域读写 + 收藏接口（GET/PUT /auth/me/favorite-domains）
P2 后端：loginCustomer 登录响应 accessibleDomains 按收藏排序
P3 前端 shared：FavoriteDomains 类型 + fetchFavoriteDomains/updateFavoriteDomains API
P4 前端：App.tsx 路由守卫分支（1 域直进 / ≥2 域选择页 / 0 域引导）
P5 前端：/domains 页面重构为选择页（墨条 + 卡片网格 + 星标 + 加入弹层 + 骨架/空态）
P6 验证：后端 compile + 前端 typecheck + 浏览器冒烟
```

## 实施清单

### Step 1：后端收藏域能力（uniondesk-app）

1. [ ] `UserConfigService`：+ `getFavoriteDomainIds(userId): List<Long>` / `saveFavoriteDomainIds(userId, List<Long>)`（user_config config_key='favorite_domain_ids'，value_type='json'；空列表时删除或存空数组）
2. [ ] `AuthController`：+ `GET /api/v1/auth/me/favorite-domains`、`PUT /api/v1/auth/me/favorite-domains`（body `{domain_ids}`，白名单过滤已加入域）
3. [ ] `AuthService` / 新 `FavoriteDomainService`：校验 + 读写

验证：`.\mvnw.cmd -pl uniondesk-app -am compile`；curl 冒烟（GET 空 → PUT 保存 → GET 回读；非法域 id 被过滤）

### Step 2：登录响应排序

4. [ ] `AuthService.loginCustomer`：构造 `accessibleDomains` 后按 `favorite_domain_ids` 重排（收藏在前，其余保序）
5. [ ] staff 分支不动

验证：登录响应中收藏域排在前面

### Step 3：前端 shared

6. [ ] `types.ts`：`FavoriteDomainsResponse` / `UpdateFavoriteDomainsRequest`
7. [ ] `api.ts`：`fetchFavoriteDomains` / `updateFavoriteDomains`（PUT 全量覆盖）

### Step 4：路由守卫

8. [ ] `App.tsx` `LandingRedirect`：通用登录成功一律 → `/domains`（选择页，不自动进入；单域同样进选择页）；零域 → `/domains`（引导态）
9. [ ] `RequireSession`：`activeDomain` 为空 → `/domains`（未选择域不可进业务页）

### Step 5：选择页重构（pages/domains/index.tsx）

10. [ ] 墨色欢迎条（欢迎文案 + 收藏提示）
11. [ ] 域卡片网格：joinedDomains 数据源 + 收藏排序 + ★ 星标切换（本地重排 + updateFavoriteDomains 串行保存）+ 点击进入（selectCustomerDomainLive → /home）
12. [ ] 加入弹层（开放域列表 + 邀请码）复用/重构现有加入逻辑
13. [ ] 零域引导态 + 加载骨架
14. [ ] 样式对齐 DESIGN.md（白底/墨条/云灰带/两档圆角，参考 Frameground 09-domain-select frame）

### Step 6：验证

15. [ ] `pnpm run check:utf8`、`pnpm run typecheck:admin`、`pnpm -C apps/UnionDeskCustomerWeb run typecheck`
16. [ ] 浏览器冒烟：多域登录 → 选择页（顶部收藏卡 + 底部列表）→ 星标切换重排 → 进入 /home；单域登录 → 同样进选择页；零域 → 引导态；不足 3 卡显示虚线占位

## 验证命令

```powershell
cd UnionDesk
.\mvnw.cmd -pl uniondesk-app -am compile
cd ../UnionDeskWeb
pnpm run check:utf8
pnpm -C apps/UnionDeskCustomerWeb run typecheck
```

## 风险与回滚

- `LandingRedirect` 分支改动影响所有登录跳转——回滚点：还原 App.tsx
- 登录响应排序改动影响 customer 登录——回滚点：还原 loginCustomer
- user_config 新 key 无表变更，可安全回滚
