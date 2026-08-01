# Design — 业务域端 P0 三页

## 0. Boundaries

| 层 | 范围 |
|:---|:---|
| 前端 | `pages/domain/{members,basic,onboarding}` 填实；`domain-permissions.ts` 补 general/member/invitation 常量 |
| API | 复用 `@uniondesk/shared` / 既有 `admin/domains/{id}/…`；鉴权码走 `domain.*` |
| 后端 | 优先不改；若某接口只认 `platform.domain.control.*`，再补并列 `domain.*`（单独提交说明） |
| 非范围 | 切域 UI、入域写、删域/启停域、共享 Panel 抽取 |

## 1. domainId 与数据流

```text
auth.defaultBusinessDomainId  ──►  resolveDomainId(accessibleDomains)
        │
        ├─► GET/PUT domain 基础信息
        ├─► members list/CRUD APIs
        └─► domain view（registration_enabled / invitation_enabled）只读展示
```

- 无有效域 ID：页内 Empty「暂无可用业务域」。
- 切域：依赖侧栏 `switchDomain` → `resetTabs` → 重导航；本页用 store 订阅刷新。

## 2. 页面对照

| 业务页 | 平台参考 | 差异 |
|:---|:---|:---|
| `domain/members/index.tsx` | `detail-members.tsx` | 权限改 `domain.member.*`；domainId 来自会话 |
| `domain/basic/index.tsx` | `detail-baseinfo.tsx` | **删除整个 DangerZone 启停/删除区**；仅保留基础信息表单 + 保存 |
| `domain/onboarding/index.tsx` | `detail-onboarding.tsx` | Switch `disabled` 或改为只读描述；不调 `updateAdminDomain` |

布局：继续 `BasicContent` + `Card`（与现占位一致）；列表页遵循 AGENTS 列表规范（筛选 Card + 列表 Card），人员页对齐平台成员列表交互。

## 3. 权限常量

在 `pages/domain/domain-permissions.ts` 增补（与 `PermissionCodes` / 父 design 一致）：

| 常量 | 码 |
|:---|:---|
| `DOMAIN_GENERAL_READ/UPDATE` | `domain.general.read/update` |
| `DOMAIN_MEMBER_*` | `domain.member.read/create/update_roles/update_status/delete` |
| `DOMAIN_INVITATION_CODE_READ` | `domain.invitation_code.read`（入域菜单/页读门槛） |

**不**在业务端引用 `DOMAIN_GENERAL_UPDATE_STATUS` / delete。

## 4. 后端鉴权核对

实现前扫描成员/域更新相关 Controller 的 `@RequirePermission`：

- 若已含 `domain.member.*` / `domain.general.*` → 前端直连。
- 若仅平台码 → 本子任务内最小补丁：并列允许业务码（不改业务逻辑）。

## 5. 风险与回滚

| 风险 | 缓解 |
|:---|:---|
| 复制平台文件时夹带启停/删除 | basic 页 PR 检查无 `update_status` / delete UI |
| 入域误开放写 | onboarding 无 `updateAdminDomain` 调用 |
| 双端回归 | 手测平台 detail 三 Tab 不坏 |

回滚：还原三页为 Empty 占位即可（菜单保留）。
