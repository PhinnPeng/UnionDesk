# Research: 权限码 ticket.claim/ticket.close 现状 + AuthGuarded + rowSelection 先例

- Query: 权限码在权限表/菜单绑定中的现状（DB 迁移 + 前端权限常量）、AuthGuarded 用法、带 rowSelection 的列表页先例
- Scope: internal
- Date: 2026-08-16

## Findings

### 1. 权限码定义（后端）

`uniondesk-iam/src/main/java/com/uniondesk/iam/core/PermissionCodes.java`：
- `TICKET_CLAIM = "ticket.claim"`（行 211）、`TICKET_CLOSE = "ticket.close"`（行 215）、`TICKET_VIEW_DOMAIN_ALL = "ticket.view.domain_all"`（行 213）、`TICKET_ASSIGN`（212）、`TICKET_REPLY`（214）、`TICKET_MERGE`（216）等

### 2. 权限种子与菜单绑定（DB 迁移）

- 基础种子：`uniondesk-app/src/main/resources/db/migration/archive/V202605031201__p0_ticket_notification_attachment_permissions.sql`（行 8、12）——`iam_permission` 种子：`('ticket.claim','领取工单',...,'POST','/api/v1/admin/domains/*/tickets/*/claim')`、`('ticket.close','关闭工单',...,'PATCH','/api/v1/admin/domains/*/tickets/*/status')`；并给角色绑定（行 53-75）
- **最新绑定**：`db/migration/current/V20260813160000__ticket_queue_menu_and_permissions.sql`（75 行，工单队列 S5 任务 7）：
  - A) agent 角色补齐 `ticket.merge` + 只读权限（行 5-19）
  - B) 业务端一级菜单 `BUSINESS-DOMAIN-TICKET-QUEUE`（/domain/ticket-queue）（行 21-26）
  - C) **按钮码**挂在菜单下（行 28-63）：`BUSINESS-DOMAIN-TICKET-QUEUE-READ`(ticket.view.domain_all)、`-CLAIM`(ticket.claim)、`-ASSIGN`(ticket.assign)、`-REPLY`(ticket.reply)、`-CLOSE`(ticket.close)、`-MERGE`(ticket.merge)——`node_type='button'`、`required` 仅 READ=1
  - D) `domain_admin`/`agent` 角色绑定菜单+按钮（行 65-75，`INSERT IGNORE` + `m.code LIKE 'BUSINESS-DOMAIN-TICKET-QUEUE-%'`）
- **结论：ticket.claim / ticket.close 已作为工单队列菜单按钮码绑定到 domain_admin/agent，批量按钮可直接复用同一权限码，无需新增权限**（AC5 用 `AuthGuarded auth="ticket.claim"/"ticket.close"` 即可）
- 注意：按钮码在 `iam_admin_menu`（`uk_iam_admin_menu_permission_code` 全局唯一，注释行 28），同一 permission_code 不能重复插入菜单按钮——若批量按钮想要独立按钮码需新增 menu 种子（非必须）

### 3. 后端鉴权注解

- `@RequirePermission(value = PermissionCodes.TICKET_CLAIM, domainIdParam = "domain_id")` 用于 controller 方法（`TicketController` 行 131、167 等）；批量端点照抄（`StaffDomainMemberBatchController` 行 58-59 是批量端点 @RequirePermission 先例）
- 注意 `PermissionCodes` 在 uniondesk-iam，ticket 模块已依赖（`TicketController` 行 6 import）

### 4. AuthGuarded 组件用法（前端）

- `src/components/auth-guarded/index.tsx`：`<AuthGuarded auth={code|code[]} fallback={...}>children</AuthGuarded>`，内部 `useAuth().hasPermission(code)`
- `useAuth`（`src/hooks/use-auth/index.ts`）：从 `useUserStore(state => state.actions)` 取权限码数组；`code` 为空返回 true；数组为 `some` 语义；scope 由路由推断（platform/system/business）
- 工单队列现状用法：列表整体 `auth="ticket.view.domain_all"`（`ticket-queue/index.tsx` 行 333-335）；抽屉内领取/指派按钮 `auth="ticket.claim"` / `"ticket.assign"`（`ticket-detail-drawer.tsx` 行 166-171）
- 前端权限常量：`platform/user/index.tsx` 有 `PLATFORM_USER_ROW_ACTIONS` 等集中常量表（含 auth 字段）模式；ticket 队列页目前直接写字符串字面量

### 5. rowSelection + 批量工具栏先例

**最佳先例：`src/pages/platform/user/index.tsx`（平台用户管理）**
- 行 116：`const [selectedRowKeys, setSelectedRowKeys] = useState<Key[]>([])`
- 行 458-467：Card `extra` 内 `<AuthGuarded auth={...}>` 包批量按钮（「批量离职」`danger` + `disabled={selectedRowKeys.length === 0}`）
- 行 478-484：Table `rowSelection={{ selectedRowKeys, onChange: keys => setSelectedRowKeys(keys), getCheckboxProps: record => ({ disabled: record.status === "offboard" }) }}`——**getCheckboxProps 可按行禁用勾选**（R1 批量领取可借此禁用非 open/new 行、批量关闭禁用终态行）
- 批量动作：`handleOffboardUsers(selectedRowKeys.map(String))`（行 461-462）
- 其他 rowSelection 页面：`domain/customers/index.tsx`（行 563-568，仅弹窗内选员工用）、`domain/members/index.tsx`、`platform/domains/detail/components/detail-members.tsx`、`detail-customers.tsx`——均为选择用途，无批量工具栏

**批量调用 API 先例（后端返回部分成功）**：`StaffDomainMemberBatchIntegrationTest.java`（uniondesk-app 测试）覆盖 `batch-status`；前端对应页面在 platform/user 的 BatchDisableModal（`./components/batch-disable-modal`）——可参考其失败展示形态。

## Caveats

- 现有前端权限常量散落页面内字符串（ticket-queue 页 `auth="ticket.claim"` 字面量），对齐现状即可，不必强行抽常量（AGENTS.md §2.5 禁止无必要抽常量文件）
- `hasPermission` 的数组语义是 `some`（任一命中即放行），AuthGuarded 批量按钮按单码使用即可
- 若批量领取/关闭希望按钮独立显示权限（例如「批量领取」与「领取」分别控权），需新增菜单按钮种子——当前建议直接复用 ticket.claim/ticket.close（PRD AC5 原文即如此）
