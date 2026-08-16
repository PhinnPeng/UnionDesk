# 客户工单页设计（CustomerWeb V3 服务台）

> 2026-08-16 立项（研究 + 三轮决策已拍板）。聚焦 CustomerWeb V3 服务台「工单页」：查看/筛选（待提交/进行中/已完成/待评价）、客户侧删除与批量删除、草稿提示续填、标题+描述必填。
> 依据：Figma `CustomerWeb`（97-2 画布 · v3 服务台）、`UnionDeskCustomerWeb/src/pages/tickets/*`、后端 `uniondesk-ticket`、`docs/design/adr-004-customer-ticket-page.md`。

## 一、现状关键事实（已勘察核实）

**Figma 设计稿（97:2）**：仅两画板 —— 登录页（132:2）与服务台首页（132:155，含侧边栏/顶栏/欢迎卡/统计条/我的工单卡片列表/通知卡）。**无独立工单页画板**，本设计需新增。

**前端 CustomerWeb**（`UnionDeskWeb/apps/UnionDeskCustomerWeb`）：
- 列表页 `pages/tickets/index.tsx`：左侧类型栏 + 搜索框 + 生命周期分段（全部/待处理/进行中/已完成）＋卡片列表（类型标签+状态标签+标题+工单号+更新时间）。无分页（`portal.currentDomainTickets` 一次性加载）、无删除、无批量。
- 新建页 `pages/tickets/new.tsx`：三步（选类型→填写详情→完成），仅标题（必填）+ 详细说明（必填）。无草稿，取消即丢弃。
- 详情页 `pages/tickets/detail.tsx`：回复/补充说明/满意度评价（1-5 星，resolved/closed 后）齐备。
- API 层 `packages/shared/src/api.ts:2834-2957`：`/domains/{id}/tickets/my`（仅 status+limit）、创建、回复、撤回、满意度。**无删除/批量/草稿接口**。
- 生命周期聚合桶 `utils/ticket-lifecycle.ts`：pending(open) / active(processing, waiting_customer) / done(resolved, closed, withdrawn)。

**后端**（`UnionDesk/uniondesk-ticket`）：
- `ticket` 表（V202605200002 基线:963-1003）：无 `is_deleted`、无 draft 字段；`version` 乐观锁；status 为旧 6 态字符串（open/processing/waiting_customer/resolved/closed/withdrawn）。
- 客户列表 `TicketService.listCustomerTickets:514`：仅 status + limit，强制按 customerId 过滤，无分页/关键字/类型。
- 满意度：`TicketSatisfactionService`，EVALUABLE_STATUSES = [closed, resolved]，一单一评（uk_satisfaction_ticket）。
- 撤回：`withdrawCustomerTicket:341`（open/new/processing 可撤回→withdrawn）。
- 状态字典 `ticket_status`（V202607070001）：平台预置 not_started/in_progress/completed/cancelled，与运行时旧 6 态**未打通**（已列入 E-EMP3 治理项，本次不触碰）。

**与目标需求差距**：无草稿、无删除/批量、无待评价桶、无「待提交」概念、列表无分页（本次不引入，保持前端过滤）。

## 二、决策总览（三轮 12 项，已拍板）

| # | 决策点 | 结论 |
|---|---|---|
| D1 | 状态模型 | **互斥四桶**：待提交=草稿；进行中=open+processing+waiting_customer；待评价=resolved/closed 且未评价；已完成=已评价的完成单+withdrawn。每单只落一桶 |
| D2 | 草稿存储 | **后端持久化**（新建 `ticket_draft` 表），每客户每业务域一个草稿 |
| D3 | 删除语义 | **软删除**：ticket 加 `deleted_at`/`deleted_by`，客户列表不可见；客服端保留可见（可加「客户已删除」标记） |
| D4 | 删除范围 | **全部状态可删**（草稿/进行中/待评价/已完成），进行中及以后弹强确认 |
| D5 | Tab 结构 | **全部+四桶**，默认「全部」 |
| D6 | 草稿保存 | **自动保存**（输入防抖 800ms），离开未提交也保存 |
| D7 | 批量交互 | **hover 复选框** + 顶部批量操作条 |
| D8 | 删除可恢复 | **不可恢复**（软删仅客户侧不可见，确认弹窗明示） |
| D9 | 类型筛选 | **保留左侧类型栏** |
| D10 | 统计条 | **加四桶计数统计条**（对齐 Figma v3 首页 stats-grid） |
| D11 | 草稿落点 | 「继续」→ **直接进填写详情**（类型沿用草稿，可改） |
| D12 | 全部含草稿 | **「全部」不含草稿**，草稿仅出现在「待提交」Tab |

## 三、页面设计

### 3.1 页面结构（1440×1024，对齐 v3 首页骨架）

```
aside.sidebar (240px)          —— 复用首页侧边栏（首页/工单/咨询/通知/我的 + 底部三栏）
└── 工单 nav-item 置 active
header.topbar (1200px, 高64)   —— 「我的工单」标题 + 搜索框（工单号/标题/类型）+ 「提交工单」主按钮
main (1200px)
├── 统计条 stats-grid（4 卡）   —— 待提交 / 进行中 / 已完成 / 待评价（计数，点击跳对应 Tab）
├── 类型 rail + 主区两栏
│   ├── aside 类型栏（全部类型+各类型计数，保留现状）
│   └── section 主区
│       ├── ud-segment 五段 Tab：全部 / 待提交 / 进行中 / 已完成 / 待评价
│       └── 卡片列表（ticket-item，复用首页卡片语言）
│           ├── hover：左侧复选框 + 右上角删除按钮
│           └── 批量模式：顶部批量操作条（已选 N 项 · 删除）
```

### 3.2 统计条（对齐 Figma v3 首页 stats-grid 视觉）

- 四卡：待提交 / 进行中 / 已完成 / 待评价，各显计数。
- 点击卡片跳对应 Tab（等价点击分段按钮）。
- 计数口径与四桶一致：待提交=草稿数；进行中/已完成=对应桶；待评价=resolved/closed 且未评价数。

### 3.3 列表卡片（复用首页 `div.ticket-item` 语言）

| 元素 | 说明 |
|---|---|
| 复选框 | hover 显示，勾选进入批量模式（顶部批量条出现） |
| 类型标签 | `ticket-tag--tech/account/billing` 风格 |
| 状态标签 | 沿用 StatusTag（待处理/处理中/待补充/已解决/已关闭/已撤回） |
| 标题 | 主文本 |
| 工单号 + 更新时间 | meta 行 |
| 删除按钮 | hover 右上角出现；单删用 ConfirmPopover（项目规范），文案「删除后仅从您的列表移除，客服端仍可见且不可恢复」 |

**草稿卡**（仅「待提交」Tab）：无工单号，标「草稿」标签 + 类型 + 标题（空标题显示「未命名草稿」）+ 「最后编辑 xx 前」；操作：点击卡片 → 继续填写（进新建表单）；删除按钮 → 直接删草稿（轻确认）。

### 3.4 新建流程（三步改两态 + 草稿钩子）

```
点击「提交工单」
├─ 存在草稿 ──→ Modal「发现尚未提交的草稿，是否继续」
│                ├─ 继续 ──→ 直接进「填写详情」（类型沿用草稿所选，可更改类型）
│                └─ 放弃 ──→ 删除草稿 → 进「选择类型」
└─ 无草稿 ────→ 进「选择类型」（现状不变）
填写详情：标题/描述输入防抖 800ms 自动保存草稿（PUT draft upsert）
提交：校验标题+描述非空 → 创建正式工单 → 删除草稿 → 成功页（现状不变）
离开/取消：不清草稿（下次进来继续提示）
```

- 草稿允许标题/描述为空（保存即存）；**提交时标题+描述必填**（前端 required + 后端校验，双保险）。
- 「更改类型」在表单内保留（现状），改后草稿同步类型。

### 3.5 删除与批量删除

- **单删**：卡片 hover 删除按钮 → ConfirmPopover 确认 → `DELETE /tickets/my/{id}` → 刷新列表。
- **批量**：勾选 N 项 → 顶部批量条「删除」→ Modal 确认（提示含「N 项，删除后不可恢复」）→ `POST /tickets/my/batch-delete` → 刷新列表。
- 校验：仅本人工单可删（后端强制 customerId）；草稿删走 `DELETE /tickets/my/draft`。
- 软删后：客户列表/统计条/Tab 计数即时减除；客服端仍可见（本期仅客户侧实现，客服端「客户已删除」标记列为可选后续项）。

## 四、接口设计（新增）

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| PUT | `/domains/{domain_id}/tickets/my/draft` | upsert 草稿（body: `{ticketTypeId, title, description}`），每客户每域一稿 | 客户本人 |
| GET | `/domains/{domain_id}/tickets/my/draft` | 取当前草稿（无则 404） | 客户本人 |
| DELETE | `/domains/{domain_id}/tickets/my/draft` | 放弃/删除草稿 | 客户本人 |
| DELETE | `/domains/{domain_id}/tickets/my/{ticket_id}` | 软删工单（body 带 version 乐观锁） | 客户本人 |
| POST | `/domains/{domain_id}/tickets/my/batch-delete` | 批量软删（body: `{ticketIds: []}`），幂等 | 客户本人 |
| GET | `/domains/{domain_id}/tickets/my` | 扩展返回 `evaluated` 布尔（left join ticket_satisfaction，供待评价桶判断） | 客户本人 |

- 草稿不占工单号：`ticket_draft` 独立表，正式提交才生成 ticket 记录与编号。
- 批量删除逐条软删（deleted_at=now, deleted_by=userId），返回成功/失败明细。

## 五、数据模型

```sql
-- 新增：客户工单草稿表（每客户每业务域一稿）
CREATE TABLE ticket_draft (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id  BIGINT UNSIGNED NOT NULL,
    customer_id         BIGINT UNSIGNED NOT NULL,
    ticket_type_id      BIGINT UNSIGNED NOT NULL,
    title               VARCHAR(255)    NOT NULL DEFAULT '',
    description         TEXT            NULL,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_draft_domain_customer (business_domain_id, customer_id)
) COMMENT='客户工单草稿';

-- 修改：ticket 表新增软删字段（迁移）
ALTER TABLE ticket
    ADD COLUMN deleted_at DATETIME(3) NULL COMMENT '客户侧软删时间（客服端仍可见）',
    ADD COLUMN deleted_by BIGINT UNSIGNED NULL COMMENT '删除人 customer_id';
-- 索引：客户列表查询需过滤 deleted
ALTER TABLE ticket ADD KEY idx_ticket_customer_deleted (customer_id, deleted_at);
```

## 六、状态桶映射（互斥）

| 客户桶 | 判定 | 备注 |
|---|---|---|
| 待提交 | 存在于 `ticket_draft` | 非工单状态，独立表 |
| 进行中 | status ∈ {open, processing, waiting_customer} | 沿用现有 active 桶 |
| 待评价 | status ∈ {resolved, closed} 且 satisfaction 无记录 | 评价后自动落已完成 |
| 已完成 | status ∈ {resolved, closed} 且有评价记录，或 status = withdrawn | 含已撤回 |

## 七、验收要点

1. 四桶 Tab + 统计条计数与列表过滤一致；「全部」不含草稿。
2. 新建时存在草稿 → 弹「发现尚未提交的草稿，是否继续」；继续进表单（类型沿用可改）、放弃清草稿。
3. 填写详情输入后自动保存；刷新/离开后再进仍有草稿；提交后草稿清除。
4. 单删/批量删：仅本人工单可删；软删后客户列表消失、客服端列表仍可见；批量含部分失败时提示失败明细。
5. 标题+描述必填：草稿阶段允许为空，提交时前后端双校验。
6. 删除后计数/统计条即时更新。
