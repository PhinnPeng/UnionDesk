# 属性类型工作流功能 — 技术设计文档

> 基于 PRD（`prd.md`）的详细技术设计，覆盖数据库、后端 API、前端组件三层。

---

## 1. 数据库设计

### 1.1 新表：`ticket_transition_rule`

```sql
-- V202607120001__ticket_transition_rule.sql
-- 步骤规则：每个 from→to 转换对应一条 rule 记录

CREATE TABLE ticket_transition_rule (
    id                  BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    domain_id           BIGINT UNSIGNED NOT NULL COMMENT '业务域 ID，平台级为 0',
    ticket_type_id      BIGINT UNSIGNED NOT NULL COMMENT '事项类型 ID',
    from_state_code     VARCHAR(64)     NOT NULL COMMENT '源状态 code（关联 status_flow.states[].code）',
    to_state_code       VARCHAR(64)     NOT NULL COMMENT '目标状态 code',
    step_name           VARCHAR(128)    NOT NULL DEFAULT '' COMMENT '步骤显示名称',

    -- 规则配置
    permission_mode     VARCHAR(16)     NOT NULL DEFAULT 'none' COMMENT 'none | members | roles',
    member_ids          JSON            NOT NULL COMMENT '允许的成员 ID 列表 []',
    role_ids            JSON            NOT NULL COMMENT '允许的角色 ID 列表 []',
    required_slot_ids   JSON            NOT NULL COMMENT '附加属性槽位 ID 列表 []',
    attribute_updates   JSON            NOT NULL COMMENT '属性值变更 [{slot_id, value, value_type}]',

    sort_order          INT             NOT NULL DEFAULT 0,
    created_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at          DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),

    PRIMARY KEY (id),
    UNIQUE KEY uk_transition_unique (domain_id, ticket_type_id, from_state_code, to_state_code),
    KEY idx_transition_type (domain_id, ticket_type_id),
    KEY idx_transition_from (domain_id, ticket_type_id, from_state_code)
) COMMENT='步骤转换规则';
```

**设计要点**：
- `domain_id = 0` 表示平台级规则
- **无外键约束**（遵循项目规范），引用完整性由应用层保证
- `UNIQUE KEY` 保证同一类型下同一 from→to 只有一条 rule
- JSON 字段默认 `'[]'` 或 `'[]'`
- `attribute_updates` 结构：`[{"slot_id": "xxx", "value": "...", "value_type": "string"}]`

### 1.2 现有表变更

无需修改现有表。`status_flow` JSON 结构保持不变（states + transitions），rule 数据独立存储。

### 1.3 权限种子数据

```sql
-- 复用已有权限或新增（如果需要独立的规则读写权限）
-- 规则随工作流一起保存，使用现有的 TICKET_TYPE_UPDATE 权限即可
-- 无需新增权限码
```

---

## 2. 后端设计

### 2.1 新增文件清单

| 层 | 文件路径 | 说明 |
|----|---------|------|
| entity | `uniondesk-ticket/.../entity/TicketTransitionRulePo.java` | PO |
| mapper | `uniondesk-ticket/.../mapper/TicketTransitionRuleMapper.java` | Mapper 接口 |
| mapper-xml | `uniondesk-ticket/.../resources/mapper/ticket/TicketTransitionRuleMapper.xml` | SQL |
| repository | `uniondesk-ticket/.../repository/TicketTransitionRuleRepository.java` | Repository |
| core | `uniondesk-ticket/.../core/TicketTransitionRuleService.java` | 业务逻辑 |
| dto | `uniondesk-ticket/.../web/TicketTransitionRuleDtos.java` | DTO |

### 2.2 Entity：TicketTransitionRulePo

```java
package com.uniondesk.ticket.entity;

public class TicketTransitionRulePo {
    public static final String PERMISSION_MODE_NONE = "none";
    public static final String PERMISSION_MODE_MEMBERS = "members";
    public static final String PERMISSION_MODE_ROLES = "roles";

    private long id;
    private long domainId;
    private long ticketTypeId;
    private String fromStateCode;
    private String toStateCode;
    private String stepName;
    private String permissionMode;     // none / members / roles
    private String memberIdsJson;      // JSON array of Long
    private String roleIdsJson;        // JSON array of Long
    private String requiredSlotIdsJson;// JSON array of Long/String
    private String attributeUpdatesJson;// JSON array of {slot_id, value, value_type}
    private int sortOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // getters & setters (省略，遵循现有 PO 风格)
}
```

### 2.3 Repository 关键方法

```java
@Repository
public class TicketTransitionRuleRepository {

    /** 查询某类型的所有 rules */
    List<TicketTransitionRulePo> findByDomainIdAndTypeId(long domainId, long typeId);

    /** 查询某类型某 transition 的 rule */
    TicketTransitionRulePo findByDomainIdAndTypeIdAndTransition(
        long domainId, long typeId, String fromCode, String toCode);

    /** 批量插入 */
    void batchInsert(List<TicketTransitionRulePo> rules);

    /** 按 typeId 删除所有 rules */
    int deleteByDomainIdAndTypeId(long domainId, long typeId);

    /** 按 state code 删除关联 rules（状态移除时调用） */
    int deleteByDomainIdAndTypeIdAndStateCode(long domainId, long typeId, String stateCode);
}
```

### 2.4 Core Service：TicketTransitionRuleService

```java
@Service
public class TicketTransitionRuleService {

    /**
     * 保存工作流配置（事务入口）
     * 1. 更新 ticket_type.status_flow
     * 2. 删除旧 rules
     * 3. 插入新 rules
     */
    @Transactional
    public WorkflowConfigView saveWorkflowConfig(
        long domainId, long typeId,
        Object statusFlow,           // 新的 status_flow JSON
        List<SaveTransitionRuleRequest> rules // 前端传来的完整规则列表
    );

    /**
     * 加载工作流完整配置（status_flow + rules）
     */
    public WorkflowConfigView loadWorkflowConfig(long domainId, long typeId);

    /**
     * 同步 transitions 到 rules：
     * 当 status_flow 中有新的 from→to 但 rule 表中没有时，
     * 自动创建空规则记录
     */
    public List<TicketTransitionRulePo> syncRulesWithTransitions(
        long domainId, long typeId,
        List<String> existingTransitions // [{from, to}, ...]
    );
}
```

### 2.5 API 设计

#### 2.5.1 工作流配置整体保存（事务）

复用现有 `PUT /api/v1/admin/domains/{domain_id}/ticket-types/{type_id}` 接口，扩展 request body：

```java
// 在 TicketConfigDtos.UpdateTicketTypeRequest 中扩展（或新建专用 DTO）
public record UpdateWorkflowConfigRequest(
    Object status_flow,                          // states + transitions
    List<SaveTransitionRuleRequest> transition_rules  // 可选，为空则仅更新 flow
) {}

public record SaveTransitionRuleRequest(
    String from_state_code,
    String to_state_code,
    String step_name,
    String permission_mode,       // none | members | roles
    List<Long> member_ids,
    List<Long> role_ids,
    List<String> required_slot_ids,
    List<AttributeUpdateItem> attribute_updates
) {}

public record AttributeUpdateItem(
    String slot_id,
    Object value,
    String value_type  // string | number | boolean | date
) {}
```

**返回值**：扩展 `TicketTypeView` 增加 `transition_rules` 字段：

```java
public record TransitionRuleView(
    String id,
    String from_state_code,
    String to_state_code,
    String step_name,
    String permission_mode,
    List<Long> member_ids,
    List<Long> role_ids,
    List<String> required_slot_ids,
    List<AttributeUpdateItemView> attribute_updates
) {}

public record AttributeUpdateItemView(
    String slot_id,
    Object value,
    String value_type
) {}
```

#### 2.5.2 加载工作流配置

扩展现有 `GET /api/v1/admin/domains/{domain_id}/ticket-types` 返回的单条 type 数据，或在加载 type detail 时一并返回 rules。

**推荐方案**：在获取 ticket type detail 时，额外查询并附上 `transition_rules` 字段。前端一次请求拿到全部工作流数据。

```
GET /api/v1/admin/domains/{domain_id}/ticket-types/{type_id}
→ Response: TicketTypeView { ..., transition_rules: [...] }
```

### 2.6 与现有代码的集成点

| 集成位置 | 变更内容 |
|---------|---------|
| `TicketConfigController.updateTicketType()` | 解析新字段 `transition_rules`，传入 service |
| `TicketConfigService.updateTicketType()` | 调用 `ruleService.saveWorkflowConfig()` |
| `TicketConfigService.toTicketTypeView()` | 附上 rules 数据 |
| `TicketConfigService.createTicketType()` | 初始化时创建默认 rules（基于 default flow 的 transitions） |
| `TicketConfigService.deleteTicketType()` | 级联删除 rules |

---

## 3. 前端设计

### 3.1 新增/修改文件清单

| 文件路径 | 操作 | 说明 |
|---------|------|------|
| `types.ts` | **修改** | 新增 `TransitionRule`、`WorkflowConfig` 等类型 |
| `api.ts` | **修改** | `updateDomainTicketType` 扩展参数；可能新增 helper |
| `ticket-type-config/index.tsx` | **修改** | 增加 rules 状态、草稿逻辑 |
| `workflow-tab.tsx` | **重写** | 完全替换旧实现 |
| `components/workflow-tab/workflow-tab.tsx` | **新增** | 主视图容器（列表/表格切换）|
| `components/workflow-tab/list-view.tsx` | **新增** | 列表视图组件 |
| `components/workflow-tab/table-view.tsx` | **新增** | 表格（矩阵）视图组件 |
| `components/workflow-tab/add-state-modal.tsx` | **新增** | 添加状态 Modal |
| `components/workflow-tab/add-step-modal.tsx` | **新增** | 创建步骤 Modal |
| `components/workflow-tab/rule-config-modal.tsx` | **新增** | 入口1：选步骤+配规则 Modal |
| `components/workflow-tab/step-drawer.tsx` | **新增** | 入口2：步骤设置 Drawer |
| `components/workflow-tab/rule-type-picker.tsx` | **新增** | 规则类型选择器（图标卡片下拉）|
| `components/workflow-tab/rule-forms/` | **新增目录** | 各规则类型的表单组件 |
| `components/workflow-tab/local-draft.ts` | **新增** | localStorage 草稿管理 hook |
| `ticket-type-flow-designer.tsx` | **保留但不再引用** | 弃用，不删除（避免破坏其他引用）|

### 3.2 类型定义（types.ts 新增）

```typescript
/** 单条步骤规则 */
export type TransitionRule = {
  id?: string;                    // 后端返回时有 ID，新建时无
  from_state_code: string;
  to_state_code: string;
  step_name: string;
  permission_mode: 'none' | 'members' | 'roles';
  member_ids: number[];
  role_ids: number[];
  required_slot_ids: string[];   // slot ID 列表
  attribute_updates: AttributeUpdateItem[];
  sort_order?: number;
};

export type AttributeUpdateItem = {
  slot_id: string;
  value: unknown;
  value_type: 'string' | 'number' | 'boolean' | 'date';
};

/** 工作流完整配置（前后端传输结构） */
export type WorkflowConfig = {
  status_flow: TicketStatusFlow;
  transition_rules: TransitionRule[];
};

/** 保存请求 */
export type SaveWorkflowConfigBody = {
  status_flow: TicketStatusFlow;
  transition_rules: Omit<TransitionRule, 'id'>[];
};
```

### 3.3 组件架构

```
WorkflowTab (主容器)
├── Toolbar（工具栏）
│   ├── [+ 添加状态] 按钮 → AddStateModal
│   ├── [创建步骤] 按钮 → AddStepModal
│   ├── [配置规则] 按钮 → RuleConfigModal（入口1）
│   └── 视图切换 Segmented: [列表视图] [表格视图]
├── ListView（列表视图，默认）
│   └── Collapse.Group（按 from_state 分组）
│       └── 每组内:
│           ├── StepRow（步骤行）
│           │   └── 点击 → StepDrawer（入口2）
│           └── [+ 创建步骤]
├── TableView（表格/矩阵视图）
│   └── Table（行=from, 列=to）
│       └── Cell → 有步骤显示摘要 / 无步骤显示[+]创建
└── Footer（底部栏）
    ├── [取消修改] 按钮
    └── [应用配置] 按钮（loading + 脏标记）
```

### 3.4 核心组件详细设计

#### 3.4.1 WorkflowTab 主容器

```
Props: domainId, typeId, ticketType
State:
  - statusFlow: TicketStatusFlow
  - transitionRules: TransitionRule[]
  - viewMode: 'list' | 'table'
  - isDirty: boolean
  - saving: boolean
  - platformStatuses: TicketStatusDefinition[] （字典数据）

Effects:
  1. 初始加载: fetchTicketTypeDetail → 解析 status_flow + transition_rules
  2. 草稿恢复检测: checkLocalDraft() → 如有过期草稿则提示恢复
  3. 定时草稿: setInterval(5min) → saveLocalDraft()
  4. 脏检测: useEffect deep compare → setDirty
  5. 离开确认: useBlocker(isDirty)

Callbacks:
  - handleAddState(statusCode) → 添加到 statusFlow.states
  - handleRemoveState(stateCode) → 移除 state + 清理关联 transitions + rules
  - handleAddStep(from, to) → 添加 transition + 创建空 rule
  - handleRemoveStep(ruleId) → 移除 transition + rule
  - handleUpdateRule(ruleId, patch) → 局部更新 rule
  - handleApply() → 调用 save API → 成功后清除草稿 + 重置 dirty
  - handleCancel() → 重新加载后端数据 + 清除草稿
```

#### 3.4.2 AddStateModal（添加状态 Modal）

参考用户截图图3：
```
UI 结构:
┌─ 添加史诗状态 ──────────────────────┐
│                                      │
│  史诗状态                             │
│  [请选择状态 ▾]                       │
│                                      │
│  没有找到你想要的？前往新建事项状态     │
│                                      │
│  ☐ 任何状态可转换到该状态              │
│                                      │
│        [添加]  [取消]                 │
└──────────────────────────────────────┘

行为:
- 下拉数据源: fetchPlatformTicketStatuses() 仅 active 状态
- 已添加的状态不在选项中（filter: !statusFlow.states.some(s => s.code === status.code)）
- 「前往新建」→ navigate('/platform/ticket-config/statuses')
- 开关打开 → 为 statusFlow.states 中每个已有 state 创建到新状态的 transition + 空 rule
```

#### 3.4.3 AddStepModal（创建步骤 Modal）

参考用户截图图1/图2：
```
UI 结构:
┌─ 创建步骤 ──────────────────────────┐
│                                      │
│  开始状态  →  结束状态                │
│  [待处理 ▾]   →  [已取消 ▾]           │
│                                      │
│  步骤名称: 已取消                     │
│  （自动填充=结束状态名称，可编辑）      │
│                                      │
│         [确定]  [取消]                │
└──────────────────────────────────────┘

行为:
- 下拉选项: 从 statusFlow.states 中选取
- 去重校验: 该 from→to 组合是否已存在
- step_name 默认值 = to 状态的 name
- 确认后: 添加到 statusFlow.transitions + 创建空 rule 记录
```

#### 3.4.4 RuleConfigModal（入口1：配置规则 Modal）

参考用户新截图（Modal 选步骤）：
```
UI 结构:
┌─ 添加规则 ──────────────────────────┐
│                                      │
│  📋 附加属性                         │
│  状态变更前，需要额外录入属性的值...    │
│                                      │
│  当前步骤                             │
│  [已取消 ▾]                           │
│  （下拉按 from 分组，格式：            │
│   未开始                              │
│   待处理  [未开始 → 待处理 ✓]         │
│   ...）                               │
│                                      │
│  附加属性                             │
│  [请选择属性 ▾]                       │
│                                      │
│        [确定]  [取消]                 │
└──────────────────────────────────────┘

触发方式: 点击工具栏「配置规则」按钮
流程: 选规则类型 → 选步骤 → 填写规则表单 → 确定
```

#### 3.4.5 StepDrawer（入口2：步骤设置 Drawer）

参考用户新截图（Drawer 侧边抽屉）：
```
UI 结构（右侧 Drawer, width=520px）:
┌─ 步骤设置 ─ ✕ ─────────────────────┐
│  通过步骤可将事项从...               │
│                                      │
│  步骤名称      状态转化              │
│  已取消 ✏️     [处理中] → [已取消]    │
│                                      │
│  规则                    [⊕]        │
│  执行状态转换前检测限制条件...        │
│                                      │
│  ┌─ 规则卡片（如有）───────────┐     │
│  │ 📋 附加属性: 优先级    [删除] │     │
│  └─────────────────────────────┘     │
│                                      │
│  🔒📋👤📝                            │
│  尚未添加规则，点击右上方"+"添加规则   │
│                                      │
│  （点击"+"弹出规则类型选择器浮层）     │
│  ┌─ 限制步骤权限 ──────────┐        │
│  │ 附加属性                │        │
│  │更改处理人               │        │
│  │更改属性值               │        │
│  └─────────────────────────┘        │
│                                      │
│  [删除该步骤]                         │
└──────────────────────────────────────┘

特点:
- 步骤名称可编辑（inline input）
- 状态转化只读展示（带颜色 Tag）
- 规则以卡片形式列出
- "+"按钮点击后弹出 RuleTypePicker（图标卡片下拉选择器）
- 选择后在 Drawer 内展开该规则的表单
```

#### 3.4.6 RuleTypePicker（规则类型选择器）

参考用户新截图（图标卡片式）：
```
弹出层（Dropdown 或 Popover 内）:

┌─ 配置规则 ──────────────────────────┐
│                                      │
│  🔒 限制步骤权限                     │
│  状态变更前，检查当前成员权限...       │
│                                      │
│  📋 附加属性                         │
│  状态变更前，需要额外录入属性的值...    │
│                                      │
│  👤 更改处理人                        │
│  状态变更后，自动修改事项的处理人      │
│                                      │
│  📝 更改属性值                        │
│  状态变更后，自动修改属性的值          │
│                                      │
│  了解如何配置工作流规则 ↗             │
└──────────────────────────────────────┘

每种类型: 图标 + 标题 + 描述文字
点击后 → 在父容器中展开对应的规则表单
```

#### 3.4.7 各规则表单组件

**PermissionRuleForm（限制步骤权限）** — 仅域级显示：
```
权限模式: ( ) 不限制  (●) 指定成员  ( ) 指定角色

[指定成员时]:
  成员列表: [Transfer 穿梭框]
  左侧候选: fetchDomainStaffCandidates({domainId, keyword})
  右侧已选: member_ids

[指定角色时]:
  角色列表: [Transfer 穿梭框 or Select]
  数据源: fetchDomainRoles(domainId)
```

**RequiredAttributesRuleForm（附加属性）**：
```
附加属性: [Select 多选 or Transfer]
数据源: 当前类型的 slots（availableAttributes / slotRows）
选项格式: 属性名称（属性标签）
```

**AttributeUpdatesRuleForm（更改属性值）**：
```
属性值变更列表（可增减行）:

┌─ 行 1 ─────────────────────────────┐
│ 属性: [优先级 ▾]   值: [低 ▾]   [×] │
└────────────────────────────────────┘
┌─ 行 2 ─────────────────────────────┐
│ 属性: [完成时间 ▾] 值: [☑ 当前时间] [×] │
└────────────────────────────────────┘
                      [+ 添加]

属性下拉: 从当前类型的 slots 中选取
值控件: 根据 slot 关联的 attribute.field_type 动态渲染
  - input  → Input
  - select → Select（选项来自 attribute.type_config.options）
  - switch → Switch
  - date   → DatePicker + "当前时间" Checkbox
```

**AssigneeRuleForm（更改处理人）— 预留**：
```
┌─────────────────────────────────────┐
│  👤 更改处理人（功能开发中）          │
│  该功能即将上线，敬请期待             │
└─────────────────────────────────────┘
```

### 3.5 本地草稿管理（local-draft.ts）

```typescript
// hooks/useWorkflowDraft.ts

const DRAFT_KEY_PREFIX = 'workflow_draft_';
const SAVE_INTERVAL_MS = 5 * 60 * 1000;  // 5 分钟
const EXPIRY_MS = 3 * 24 * 60 * 60 * 1000; // 3 天

interface DraftData {
  savedAt: number;             // timestamp
  statusFlow: TicketStatusFlow;
  transitionRules: TransitionRule[];
}

function getDraftKey(domainId: string, typeId: string): string {
  return `${DRAFT_KEY_PREFIX}${domainId}::${typeId}`;
}

export function useWorkflowDraft(domainId: string, typeId: string) {
  const [hasDraft, setHasDraft] = useState(false);
  const [draftAge, setDraftAge] = useState<number | null>(null);

  // 保存草稿
  const saveDraft = useCallback((flow: TicketStatusFlow, rules: TransitionRule[]) => {
    const key = getDraftKey(domainId, typeId);
    const data: DraftData = { savedAt: Date.now(), statusFlow: flow, transitionRules: rules };
    localStorage.setItem(key, JSON.stringify(data));
    setHasDraft(true);
  }, [domainId, typeId]);

  // 加载草稿
  const loadDraft = useCallback((): DraftData | null => {
    const key = getDraftKey(domainId, typeId);
    const raw = localStorage.getItem(key);
    if (!raw) return null;
    try {
      const data: DraftData = JSON.parse(raw);
      if (Date.now() - data.savedAt > EXPIRY_MS) {
        localStorage.removeItem(key); // 过期清除
        setHasDraft(false);
        return null;
      }
      setHasDraft(true);
      setDraftAge(Date.now() - data.savedAt);
      return data;
    } catch {
        localStorage.removeItem(key);
        return null;
    }
  }, [domainId, typeId]);

  // 清除草稿
  const clearDraft = useCallback(() => {
    localStorage.removeItem(getDraftKey(domainId, typeId));
    setHasDraft(false);
    setDraftAge(null);
  }, [domainId, typeId]);

  // 定时自动保存
  useEffect(() => {
    const interval = setInterval(() => {
      // 由父组件通过 ref 回调触发实际保存
    }, SAVE_INTERVAL_MS);
    return () => clearInterval(interval);
  }, []);

  return { hasDraft, draftAge, saveDraft, loadDraft, clearDraft };
}
```

### 3.6 ListView 详细设计

```
┌─ 工作流  [列表视图|表格视图]  [🔍] ──────────────────┐
│ [+ 添加状态]  [创建步骤]  [配置规则]                      │
├──────────────────────────────────────────────────────────┤
│ ▼ 未开始                                                   │
│   ┌────────────────────────────────────────────────────┐ │
│   │ 步骤名称   目标状态    用户权限        操作          │ │
│   ├────────────────────────────────────────────────────┤ │
│   │ 处理中     [处理中]     全部成员     [配置] [删除]  │ │
│   │ 已完成     [已完成]     2 用户组     [配置] [删除]  │ │
│   └────────────────────────────────────────────────────┘ │
│   [+ 创建步骤]                                            │
│ ▶ 已完成                                                  │
│ ▶ 已取消                                                  │
└──────────────────────────────────────────────────────────┘

- 每组标题: 状态名称 + 颜色 Tag（根据 state_type 着色）
  - not_started → 灰色
  - in_progress → 蓝色
  - terminal → 绿色
- 步骤行:
  - 名称: step_name（可点击打开 Drawer）
  - 目标状态: to_state name（Tag，同上着色）
  - 用户权限摘要:
    - none → "全部成员" / "—"
    - members → "N 位成员"
    - roles → "N 个角色"
  - 操作: [配置] 打开 RuleConfigModal | [删除] 确认后移除
- 折叠/展开: 默认全部展开
```

### 3.7 TableView 详细设计（矩阵）

```
┌─ 工作流  [列表视图|表格视图]  ──────────────────────────────┐
│ [+ 添加状态]  [创建步骤]  [配置规则]                          │
├──────┬─────────┬─────────┬─────────┬─────────┬───────────┤
│      │ 未开始  │ 处理中  │ 已完成  │ 已取消   │ 任意状态  │
├──────┼─────────┼─────────┼─────────┼─────────┼───────────┤
│未开始│   ⊘    │ [+创建] │ [+创建] │ [+创建] │           │
├──────┼─────────┼─────────┼─────────┼─────────┼───────────┤
│处理中│ [+创建] │   ⊘    │ [已完成] │ [已取消] │           │
│      │         │         │ 2 用户组 │ 全部成员 │           │
├──────┼─────────┼─────────┼─────────┼─────────┼───────────┤
│已完成│ [+创建] │ [+创建] │   ⊘    │ [+创建] │           │
├──────┼─────────┼─────────┼─────────┼─────────┼───────────┤
│已取消│ [+创建] │ [+创建] │ [+创建] │   ⊘    │           │
└──────┴─────────┴─────────┴─────────┴─────────┴───────────┘

- 行头/列头: 状态名称 + 颜色圆点
- 对角线(同一状态): 置灰，不可点击
- 空单元格: 显示 "+" 点击创建步骤
- 有步骤的单元格: 显示步骤摘要（名称 + 权限），点击打开 Drawer
- 最后列"任意状态": 特殊列，显示"任何状态可转换到此状态"的步骤
```

### 3.8 平台级 vs 域级差异

```typescript
// WorkflowTab 内部判断
const isPlatformLevel = !domainId || domainId === '0';

// 规则配置可用性矩阵
const availableRuleTypes = useMemo(() => {
  const types = ['required_attributes', 'attribute_updates', 'assignee'];
  if (!isPlatformLevel) {
    types.unshift('permission'); // 域级额外支持权限控制
  }
  return types;
}, [isPlatformLevel]);

// RuleTypePicker 中根据此数组渲染可选类型
// 平台级的「限制步骤权限」选项不显示
```

---

## 4. 数据流

### 4.1 加载流程

```
[页面挂载]
  → GET /api/v1/admin/domains/{domainId}/ticket-types/{typeId}
  → Response: { status_flow, transition_rules, ... }
  → 前端解析:
    - setStatusFlow(response.status_flow)
    - setTransitionRules(response.transition_rules)
  → 并行: fetchPlatformTicketStatuses() 加载字典用于添加状态
  → 检查 localStorage 是否有未过期草稿
    → 有 → 显示 Toast "检测到未保存的草稿，是否恢复？" [恢复] [丢弃]
    → 无 → 正常展示
```

### 4.2 保存流程

```
[用户点击"应用配置"]
  → 前端构建请求体:
    {
      status_flow: { states, transitions },  // 当前编辑的状态
      transition_rules: [...rules]           // 当前编辑的所有规则
    }
  → PUT /api/v1/admin/domains/{domainId}/ticket-types/{typeId}
  → 后端 Transaction:
    1. UPDATE ticket_type SET status_flow_config = ?
    2. DELETE FROM ticket_transition_rule WHERE domain_id=? AND ticket_type_id=?
    3. INSERT INTO ticket_transition_rule (...) VALUES (...), (...), ...
  → COMMIT
  → 返回更新后的 TicketTypeView (含新的 rules)
  → 前端:
    - 更新本地状态
    - clearDraft()
    - setDirty(false)
    - message.success("工作流已保存")
```

### 4.3 草稿自动保存流程

```
[组件挂载]
  → 启动 setInterval(5min)
  → 每次 tick:
    - if (isDirty) → saveDraft(currentFlow, currentRules)
    - else → skip
[用户编辑任意内容]
  → setDirty(true)
[页面卸载 / Tab 切换]
  → if (isDirty) → show confirm("有未保存的更改？") [离开] [留下]
[手动点击"取消修改"]
  → clearDraft()
  → 重新加载后端数据
  → setDirty(false)
```

---

## 5. 边界情况与错误处理

| 场景 | 处理方式 |
|------|---------|
| 并发编辑冲突 | 后端乐观锁（最后写入胜出）；前端提示"数据已被他人更新" |
| 删除已被工单引用的状态 | 后端校验：检查是否有处于该状态的工单，有则拒绝删除 |
| 删除有规则配置的步骤 | 级联删除 rule 记录，前端二次确认 |
| 网络中断时编辑 | localStorage 草稿保留，下次进入时可恢复 |
| 草稿过期（3天） | 自动清除，用户看到的是后端已保存版本 |
| 平台级尝试配置权限规则 | UI 隐藏该选项；若强行调用 API 则后端忽略 permission 相关字段 |
| 同一 from→to 重复创建 | 前端拦截 + 后端 UNIQUE KEY 兜底 |
| 规则引用了已删除的属性槽位 | 后端保存时不报错（无 FK），执行时忽略无效引用 |

---

## 6. 实现顺序建议

### Phase 1: 后端基础（约 2 天）
1. Flyway 迁移: `V202607120001__ticket_transition_rule.sql`
2. Entity + Mapper + XML + Repository
3. `TicketTransitionRuleService` 核心 CRUD
4. 集成到 `TicketConfigService`（save/load 时同步 rules）
5. 扩展 DTO 和 Controller response
6. 单元测试

### Phase 2: 前端骨架（约 2 天）
1. 类型定义 (`types.ts`)
2. API 函数扩展 (`api.ts`)
3. `useWorkflowDraft` hook
4. `WorkflowTab` 主容器（含工具栏、视图切换）
5. `ListView` 基础渲染
6. `TableView` 基础渲染

### Phase 3: 前端交互（约 3 天）
1. `AddStateModal`
2. `AddStepModal`
3. `RuleTypePicker`
4. `RuleConfigModal`（入口1）
5. `StepDrawer`（入口2）
6. 各规则表单组件
7. 应用配置 / 取消修改 / 脏检测
8. 平台 vs 域级差异处理

### Phase 4: 联调与收尾（约 1 天）
1. 前后端联调
2. 边界情况测试
3. 样式微调
