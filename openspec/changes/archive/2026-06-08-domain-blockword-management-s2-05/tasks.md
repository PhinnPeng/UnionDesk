## 1. Flyway 与权限

- [x] 1.1 `domain.blocked_word.*` → `platform.domain.control.blocked_word.*` rename + 角色/菜单
- [x] 1.2 新增 `platform.blocked_word.*` 三码 + 平台菜单 `/platform/blockwords`
- [x] 1.3 域详情屏蔽词 Tab 按钮 catalog；`database-increment-plan.md` 登记

## 2. 后端

- [x] 2.1 分页 GET（keyword 模糊 + page/page_size）+ batch POST（全局 + 域内）
- [x] 2.2 单条 POST 重复校验；批量批内/库内去重跳过
- [x] 2.3 域内 API 改绑 `platform.domain.control.blocked_word.*`
- [x] 2.4 ControllerTests + ServiceTests

## 3. Shared

- [x] 3.1 分页 + batch API 封装；`BlockedWordBatchResult` 类型

## 4. 前端

- [x] 4.1 **平台页** `blockwords/index.tsx`（§4，独立实现）
- [x] 4.2 **域 Tab** `detail-blockwords.tsx` + sider 门控（§5，独立实现）
- [x] 4.3 `platform-com-registry` + 权限常量 + labels

## 5. 验收

- [x] 5.1 单测 + typecheck + 手工：查询分页、批量去重、删除确认
- [x] 5.2 backlog/tracker 收口
