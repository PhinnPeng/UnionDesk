## Context

- 表 `blocked_word` 已存在；当前 Service 仅全量 list、单条 create。
- 平台页不存在；域 Tab 为 Tag 墙。两层需独立页面 + 独立权限码。

## Goals

- **平台页** `/platform/blockwords`：`platform.blocked_word.*`
- **域详情 Tab**：`platform.domain.control.blocked_word.*`（自 `domain.blocked_word.*` 迁移）
- 两页能力对称：单条/批量添加、keyword 模糊 + 分页、TableSearchForm + Table
- **两页前端各自独立 TSX**，不抽共用 Panel 组件

## Decisions

### 平台页（§ design doc 4）

- API：`/api/v1/admin/blocked-words[...]`
- 前端：`pages/platform/blockwords/index.tsx`
- Flyway：全局 menu + 三码

### 域 Tab（§ design doc 5）

- API：`/api/v1/admin/domains/{domainId}/blocked-words[...]`
- 前端：`detail-blockwords.tsx` + `detail-sider` Tab 门控
- Flyway：rename 域内码 + hidden catalog

### 公共 Service（§ design doc 3、6）

- 分页、批量、去重逻辑共享；Controller 按路径分轨

## Risks

- 角色缺菜单/Tab 绑定 → Flyway 补绑
- batch >200 → Service 400
