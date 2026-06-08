---
comet_change: domain-blockword-management-s2-05
role: technical-design
canonical_spec: openspec
archived-with: 2026-06-08-domain-blockword-management-s2-05
status: final
---

# US-S2-05 双层屏蔽词库 — 技术设计

## 1. 业务目标

**目标**：平台管理员维护**双层屏蔽词库**——平台全局词（跨域）与各业务域域内词；两层**权限分离**、**页面独立实现**；词条 trim、禁止空词、同 scope 去重；UI 中文空态/错误提示。

| 入口 | 路由 / 位置 | 权限前缀 | 数据 scope |
|:---|:---|:---|:---|
| **平台管理页** | `/platform/blockwords` | `platform.blocked_word.*` | `business_domain_id IS NULL` |
| **业务域详情 Tab** | 域详情 →「屏蔽词库」 | `platform.domain.control.blocked_word.*` | `business_domain_id = {domainId}` |

**非目标**：运行时内容匹配引擎、CSV/文件导入导出、BusinessWeb 域端独立页。

---

## 2. 数据库表 `blocked_word`

本 Story **无 DDL 变更**。

```sql
CREATE TABLE IF NOT EXISTS `blocked_word` (
  `id`                   bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键',
  `business_domain_id`   bigint unsigned DEFAULT NULL COMMENT 'NULL=平台全局；非 NULL=域内',
  `word`                 varchar(128)    NOT NULL COMMENT '屏蔽词（trim 后入库）',
  `created_at`           datetime(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`),
  KEY `idx_blocked_word_domain` (`business_domain_id`),
  CONSTRAINT `fk_blocked_word_domain`
    FOREIGN KEY (`business_domain_id`) REFERENCES `business_domain` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='屏蔽词';
```

- 同 scope 内 `word`（trim 后）不可重复 → 应用层 `exists`（MySQL UNIQUE 对 NULL domain 不可靠）。
- Flyway 本 Story 仅权限/菜单，不改表结构。

---

## 3. 公共 Service 行为（两页共用后端逻辑）

以下规则对**全局 scope** 与**域内 scope** 对称；Controller 按路径区分 scope，Service 按 `domainId` 是否传入区分。

### 3.1 词条创建

| 项 | 决策 |
|:---|:---|
| **单条** | `POST { "word": "..." }`；重复 → 400「该屏蔽词已存在」 |
| **批量** | `POST .../batch { "words": ["...", "..."] }`；批内 trim 去重；库内重复 **skipped**，不整批失败 |
| **批量响应** | `{ created_count, skipped: [{ word, reason }] }` |
| **词条形态** | 完整字符串，最长 128 字符；单次 batch ≤200 条 |

### 3.2 列表查询

| 项 | 决策 |
|:---|:---|
| **关键字** | `keyword` → SQL `word LIKE CONCAT('%', ?, '%')`（子串模糊） |
| **分页** | `page`（从 1）、`page_size`（默认 20）→ `P0PageResult<BlockedWord>` |
| **排序** | `created_at DESC, id DESC` |

### 3.3 运行时匹配（后续 Story，本 Story 不实现）

词库去重：trim 后**精确相等**。运行时命中（后续）：待检文本 `contains(word)` 子串包含。

---

## 4. 平台管理页 `/platform/blockwords`

### 4.1 页面结构

布局遵循 AGENTS.md §2.7（参考 `domains/index.tsx`）。

```text
BasicContent
└── flex flex-col gap-4
    ├── Card「筛选条件」
    │   └── TableSearchForm → Form.Item「屏蔽词」keyword
    ├── Card「屏蔽词列表」 extra=[刷新][添加屏蔽词][批量添加]
    │   └── Table（屏蔽词 | 创建时间 | 操作）
    │         pagination
    └── Modal ×2（单条添加 / 批量添加）
```

```
+----------------------------------------------------------+
| [Card 筛选条件]  屏蔽词 [________]  [查询] [重置]         |
+----------------------------------------------------------+
| [Card 屏蔽词列表]              [刷新][添加][批量添加]     |
| | 屏蔽词 | 创建时间     | 操作(删除) |                    |
| | spam   | 2026-06-08  | 删除       |                    |
|                              < 1 2 3 >  共 42 条         |
+----------------------------------------------------------+
```

### 4.2 权限与菜单

| 权限码 | UI |
|:---|:---|
| `platform.blocked_word.read` | 页面包裹 `AuthGuarded`；无权限 → Empty |
| `platform.blocked_word.create` | 「添加」「批量添加」按钮 |
| `platform.blocked_word.delete` | 行内删除 `ConfirmPopover` |

Flyway：INSERT 全局三码 + menu `route_path=/platform/blockwords` + 三 button；`platform-com-registry` 注册 `platform/blockwords`。

### 4.3 后端 API

| 方法 | 路径 | 权限 |
|:---|:---|:---|
| GET | `/api/v1/admin/blocked-words?keyword&page&page_size` | `platform.blocked_word.read` |
| POST | `/api/v1/admin/blocked-words` | `platform.blocked_word.create` |
| POST | `/api/v1/admin/blocked-words/batch` | `platform.blocked_word.create` |
| DELETE | `/api/v1/admin/blocked-words/{wordId}` | `platform.blocked_word.delete` |

Controller：**新增** `PlatformBlockedWordController`；Service 调 `listGlobalPage` / `createGlobal` / `createGlobalBatch` / `deleteGlobal`。

### 4.4 前端实现

| 文件 | 说明 |
|:---|:---|
| `pages/platform/blockwords/index.tsx` | **新增**；页面逻辑自包含（状态、查询、Table、Modal） |
| `pages/platform/system/menu/components/platform-com-registry.ts` | 注册组件 |
| `pages/platform/system/menu/components/permission-code-labels.ts` | 全局三码中文 |

**交互要点**

- 查询/重置 → `page=1`，调 `fetchPlatformBlockedWordsPage`
- 单条 Modal → `createPlatformBlockedWord` → 刷新当前页
- 批量 Modal（TextArea，每行/逗号/顿号分隔）→ `createPlatformBlockedWordsBatch` → message 汇总 created/skipped
- 删除 → `ConfirmPopover`「确认删除该屏蔽词？」→ `deletePlatformBlockedWord`

### 4.5 Shared API（全局）

```ts
fetchPlatformBlockedWordsPage(params: { keyword?: string; page: number; page_size: number })
createPlatformBlockedWord(word: string)
createPlatformBlockedWordsBatch(words: string[])
deletePlatformBlockedWord(wordId: string)
```

路径前缀：`/admin/blocked-words`（无 domainId）。

---

## 5. 业务域详情 Tab「屏蔽词库」

### 5.1 页面结构

布局遵循 AGENTS.md §2.7（参考 `detail-members.tsx`）；**不使用** `BasicContent`。

```text
div（Tab 内容区）
├── Title level={5}「屏蔽词库」
├── Card「筛选条件」
│   └── TableSearchForm → Form.Item「屏蔽词」keyword
├── Card「屏蔽词列表」 extra=[刷新][添加屏蔽词][批量添加]
│   └── Table + pagination
└── Modal ×2
```

```
+----------------------------------------------------------+
| 屏蔽词库（域 {domainName}）                               |
+----------------------------------------------------------+
| [Card 筛选条件]  屏蔽词 [________]  [查询] [重置]         |
+----------------------------------------------------------+
| [Card 屏蔽词列表]              [刷新][添加][批量添加]     |
| | 屏蔽词 | 创建时间     | 操作(删除) |                    |
|                              < 1 2 3 >  共 N 条          |
+----------------------------------------------------------+
```

### 5.2 权限与 Tab 门控

| 权限码 | UI |
|:---|:---|
| `platform.domain.control.blocked_word.read` | Tab 可见 + 页内 `AuthGuarded` |
| `platform.domain.control.blocked_word.create` | 添加 / 批量添加 |
| `platform.domain.control.blocked_word.delete` | 行内删除 |

迁移：`domain.blocked_word.*` → **`platform.domain.control.blocked_word.*`**（与 `platform.domain.control.customer.*` 同级）。

| 旧码 | 新码 |
|:---|:---|
| `domain.blocked_word.read` | `platform.domain.control.blocked_word.read` |
| `domain.blocked_word.create` | `platform.domain.control.blocked_word.create` |
| `domain.blocked_word.delete` | `platform.domain.control.blocked_word.delete` |

Flyway：rename 域内三码 + hidden catalog **`PLATFORM-DOMAIN-CONTROL-BLOCKED-WORD`**（父 `PLATFORM-DOMAIN-DETAIL`）+ 三 button。

`detail-sider.tsx`：Tab 门控 `DOMAIN_BLOCKED_WORD_READ_PERMISSION`（export 自 `detail-shared.ts`）。

### 5.3 后端 API

| 方法 | 路径 | 权限 |
|:---|:---|:---|
| GET | `/api/v1/admin/domains/{domainId}/blocked-words?keyword&page&page_size` | `platform.domain.control.blocked_word.read` |
| POST | `/api/v1/admin/domains/{domainId}/blocked-words` | `platform.domain.control.blocked_word.create` |
| POST | `/api/v1/admin/domains/{domainId}/blocked-words/batch` | `platform.domain.control.blocked_word.create` |
| DELETE | `/api/v1/admin/domains/{domainId}/blocked-words/{wordId}` | `platform.domain.control.blocked_word.delete` |

Controller：**修改** `BlockedWordController`（路径不变，改绑新权限码）；Service 调 `listDomainPage` / `createDomain` / `createDomainBatch` / `deleteDomain`。

### 5.4 前端实现

| 文件 | 说明 |
|:---|:---|
| `pages/platform/domains/detail/components/detail-blockwords.tsx` | **修改**；页面逻辑自包含，接收 `domainId` prop |
| `pages/platform/domains/detail/components/detail-sider.tsx` | Tab 门控 |
| `pages/platform/domains/detail/components/detail-shared.ts` | export `DOMAIN_BLOCKED_WORD_*_PERMISSION` |
| `pages/platform/domains/platform-domain-permissions.ts` | `PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_*` 常量 |

**交互要点**（与 §4.4 能力一致，API 换域内路径）

- 查询 → `fetchBlockedWordsPage(domainId, ...)`
- 单条 → `createBlockedWord(domainId, word)`
- 批量 → `createBlockedWordsBatch(domainId, words)`
- 删除 → `deleteBlockedWord(domainId, wordId)`

常量使用 `PLATFORM_DOMAIN_CONTROL_BLOCKED_WORD_*`，**禁止**引用全局页权限码。

### 5.5 Shared API（域内）

```ts
fetchBlockedWordsPage(domainId, params: { keyword?: string; page: number; page_size: number })
createBlockedWord(domainId, word)
createBlockedWordsBatch(domainId, words)
deleteBlockedWord(domainId, wordId)
```

路径前缀：`/admin/domains/{domainId}/blocked-words`。

---

## 6. 后端公共层

### 6.1 模块

| 模块 | 操作 |
|:---|:---|
| `BlockedWordService` | 修改：分页、批量、全局/域内 scope 方法 |
| `BlockedWordRepository` / `BlockedWordMapper` | 修改：分页 SQL、查重 |
| `PlatformBlockedWordController` | **新增**（§4.3） |
| `BlockedWordController` | **修改**（§5.3） |
| `PermissionCodes` / `AdminPermissionCatalog` | 六码 + deprecated alias |
| `PlatformBlockedWordControllerTests` | **新增** |
| `BlockedWordControllerTests` | **修改** |

### 6.2 Service 伪码

```text
listGlobalPage(keyword, page, pageSize)   → business_domain_id IS NULL
listDomainPage(domainId, ...)             → business_domain_id = domainId
createGlobal / createGlobalBatch
createDomain / createDomainBatch
deleteGlobal / deleteDomain
parseBatchWords → trim、split、批内 Set 去重
```

### 6.3 Mapper

- `selectPageByGlobal` / `countByGlobal`
- `selectPageByDomain` / `countByDomain`
- `countByGlobalAndWord` / `countByDomainAndWord`
- `deleteByIdGlobal` / `deleteByIdAndDomainId`

### 6.4 异常

| 场景 | HTTP | 消息 |
|:---|:---|:---|
| 空词 | 400 | 屏蔽词不能为空 |
| 批量无有效词 | 400 | 请至少输入一个屏蔽词 |
| 单条重复 | 400 | 该屏蔽词已存在 |
| 批量库内重复 | 200 + skipped | reason: 已存在 |
| batch >200 | 400 | 单次最多添加 200 个屏蔽词 |
| 删除不存在 | 400 | 屏蔽词不存在 |

---

## 7. Flyway（`V202606080001__platform_blocked_word_permissions.sql`）

1. **iam_permission**：域内三码 rename（§5.2）；INSERT 全局三码（§4.2）。
2. **iam_admin_menu**：全局 menu + button（§4.2）；域内 hidden catalog + button（§5.2）。
3. **iam_role_permission** / **iam_admin_role_menu_relation**：super_admin + 详情角色补绑。
4. **database-increment-plan.md** §3 登记 US-S2-05。

---

## 8. 预期实现步骤

| 步骤 | 内容 | 检查点 |
|:---|:---|:---|
| 1 | Flyway + PermissionCodes + Catalog | 六码；菜单与 Tab 按钮 |
| 2 | Service + Mapper + 双 Controller | 单测通过 |
| 3 | shared 全局 + 域内 API | typecheck |
| 4a | **平台页** `blockwords/index.tsx` | 查询/分页/增删/批量 |
| 4b | **域 Tab** `detail-blockwords.tsx` + sider 门控 | 同上，域内 API |
| 5 | backlog / tracker 收口 | US-S2-05 → Done |

---

## 9. 文件清单

### 后端

| 操作 | 路径 |
|:---|:---|
| 新增 | `V202606080001__platform_blocked_word_permissions.sql` |
| 新增 | `PlatformBlockedWordController.java` / Tests |
| 修改 | `BlockedWordController.java` / Tests |
| 修改 | `BlockedWordService.java` / Repository / Mapper |
| 修改 | `PermissionCodes.java` / `AdminPermissionCatalog.java` |

### 前端 — 平台页（§4）

| 操作 | 路径 |
|:---|:---|
| 新增 | `pages/platform/blockwords/index.tsx` |
| 修改 | `platform-com-registry.ts` / `permission-code-labels.ts` |

### 前端 — 域详情 Tab（§5）

| 操作 | 路径 |
|:---|:---|
| 修改 | `detail-blockwords.tsx` / `detail-sider.tsx` |
| 修改 | `detail-shared.ts` / `platform-domain-permissions.ts` |

### Shared

| 操作 | 路径 |
|:---|:---|
| 修改 | `packages/shared/src/api.ts`（§4.5 + §5.5） |

---

## 10. 测试策略

- **单元**：分页 SQL；全局/域内 batch 去重；单条重复；权限码绑定。
- **手工 — 平台页**：keyword 查询、分页、单条/批量、删除确认。
- **手工 — 域 Tab**：同上；无 read 权限时 Tab 隐藏。
- **回归**：Flyway 后 `domain.blocked_word.*` alias 仍可访问域内 API。

---

## 11. 方案取舍

| 选项 | 结论 |
|:---|:---|
| 双 Controller + 共享 Service | **采用** |
| 平台页 / 域 Tab **各自独立 TSX**，不抽共用 Panel | **采用**（设计分轨、实现不混写） |
| Tag 墙 UI | 拒绝 |
| TableSearchForm + Table | **采用** |

---

## 12. 风险

- `component_key=platform/blockwords` 未注册 → 404；缓解：`platform-com-registry` 冒烟。
- 域详情角色缺 Tab 按钮 → 缓解：Flyway 补 `iam_admin_role_menu_relation`。
- 批量过大 → Service 限制 ≤200 条/次。
