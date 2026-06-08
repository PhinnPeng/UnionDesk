## Why

US-S2-05 要求平台管理员维护**双层屏蔽词库**：平台全局词库（跨域生效）与各业务域域内词库。当前仅有域级 API（`/admin/domains/{id}/blocked-words`）与 `domain.blocked_word.*` 权限；缺少平台全局管理入口、全局 API，且权限命名未对齐 backlog 的 **`platform.blocked_word.*`** / **`platform.domain.control.blocked_word.*`**。

## What Changes

- **DB / Flyway**：确认 `blocked_word.business_domain_id` 可 NULL 表全局词；域内词绑定 domain_id；新增/迁移权限码与菜单。
- **后端**：
  - 平台全局 CRUD：`GET/POST/DELETE /api/v1/admin/blocked-words`（`business_domain_id IS NULL`）
  - 域内 CRUD：保留现有域路径，改绑 `platform.domain.control.blocked_word.*`
  - 同 scope 内词条去重（trim 后）；单条拒绝、批量跳过重复
  - GET 支持 keyword 模糊查询与分页
- **前端**：
  - 新建 `/platform/blockwords`（§4，平台页独立实现）
  - 升级 `detail-blockwords.tsx`（§5，域 Tab 独立实现）
  - `detail-sider` Tab 门控 `platform.domain.control.blocked_word.read`
- **Shared**：封装分页 + batch API

## Capabilities

### New Capabilities

- `domain-blockword-management`

## Impact

- `BlockedWordService` / Repository / Mapper
- `PermissionCodes` + `AdminPermissionCatalog` + Flyway
- `UnionDeskAdminWeb` 平台路由 + 域详情 Tab
- `packages/shared/src/api.ts`

## Non-Goals

- 屏蔽词在工单/咨询等业务流中的**运行时匹配**逻辑（本 Story 仅管理 CRUD）
- 业务域端（BusinessWeb）独立屏蔽词管理页
- CSV/文件导入导出
