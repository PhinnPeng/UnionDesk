# SLA 配置重构设计（域内自配置 · 全局单配置面板）

> 2026-08-16 立项（调研 + 三轮决策已拍板）。SLA 管理从「双层规则列表」降级为「业务域内单份 SLA 配置面板」：固定 首次响应/解决响应 时限 + 超时动作可配置块（升级优先级固定 + 更换处理人/添加关注人动态）+ 内嵌工作日历（工作日/节假日/周末）。
> 依据：`docs/architecture/adr-005-sla-config-redesign.md`、`docs/architecture/adr-sla-two-tier-config.md`（被部分取代）、现状代码（SlaService/SlaScanJob/sla-rule-form.tsx/platform-sla-management）。

## 一、现状关键事实（已勘察核实）

- **菜单**：域端 `BUSINESS-DOMAIN-SLA`「SLA 管理」icon=NULL（`V20260813140000:5-9`）；平台端 `ADM0000000053`「SLA管理」icon=`ClockCircleOutlined` 但**未注册进前端 menuIcons 白名单，实际不渲染**（`menu-icons.ts:43-84`）；补图标先例 `V20260728201000__business_menu_icons.sql`。
- **权限**：域 `domain.sla.read/create/update`（无 delete）；平台 `platform.sla.read/create/update/delete` 已入 PermissionCodes 与种子（`V20260816130000:46-71`）但不在 AdminPermissionCatalog；平台菜单按钮码与 API 码不一致（`platform.domain.control.overview` vs `platform.sla.*`）。
- **日历**：`sla_calendar`（business_domain_id NOT NULL FK, name, config json）config 为**自由 JSON 无结构校验**，全链路**零消费**（规则表单日历下拉 disabled、引擎不算日历）；测试示例 `{"timezone":"UTC","working_days":[1,2,3,4,5]}`。
- **规则引擎**：`SlaService`（uniondesk-support）loadPolicy 匹配链（域规则 类型+优先级>仅类型>仅优先级>域默认 → 全局兜底）→ 写 deadline；`SlaScanJob`（uniondesk-ticket）每分钟扫描 `sla_status='tracking'` 超时候选 → `processSlaBreach` 执行动作；`claimBreachAction` 原子 UPDATE 保证每工单一次。
- **动作**：`breach_action_json` 键 `raise_priority_to`（旧）/`escalate_priority`（bool 按序升）/`assign_to_staff_account_id`/`add_watcher_staff_account_ids`；升级按 `sort_order ASC` 升到下一紧急档（`SlaService.escalatePriority:266-279`）。
- **紧急配置**：`is_urgent_config` 死字段（全链路未参与）；`urgent` 优先级档每域已种（sort_order=0）。
- **数据**：库中无任何 sla_rule/sla_calendar 种子数据，重构无迁移包袱。

## 二、决策总览（三轮 11 项，已拍板）

| # | 决策点 | 结论 |
|---|---|---|
| D1 | 配置归属 | **仅业务域内自配置，平台不管理**；平台 SLA 入口移除（域端 /domain/sla 为唯一配置入口） |
| D2 | 紧急配置 | **后置**：本轮配置面板不显示「紧急配置」区，数据模型预留字段 |
| D3 | 超时动作形态 | **升级优先级固定块 + 更换处理人/添加关注人动态块**；顶部右侧下拉添加动作块，可加多个、可删除 |
| D4 | 日历能力 | **工作日（周几）+ 节假日（日期列表）+ 周末是否工作（开关）**；引擎按日历折算 SLA 分钟 |
| D5 | 配置粒度 | **域内一份通配配置**，作用于该域所有工单，不再按类型/优先级拆分 |
| D6 | 日历归属 | **内嵌单日历**：配置面板内嵌工作日历区，无独立日历 Tab、无多套日历 |
| D7 | 平台端处置 | **隐藏保留**：平台 SLA 菜单 hidden=1，页面/接口/权限码保留不删 |
| D8 | 旧数据 | **新表承载**（sla_config 每域一行），旧 sla_rule/sla_calendar 保留不读 |
| D9 | 升级动作 | **开关 + 目标档下拉**（可选升到哪一级），取代「仅按序升」 |
| D10 | 菜单图标 | 域菜单 icon 回填 **FieldTimeOutlined** + 前端 menuIcons 注册 |
| D11 | 菜单更名 | 「SLA 管理」→ **「SLA 配置」**（域菜单名 + 页面标题） |

## 三、页面设计（/domain/sla 重构）

### 3.1 页面结构（域端「SLA 配置」单页面板，无 Tabs）

```
BasicContent
└── Card「SLA 配置」bordered={false} extra=说明（作用于本域全部工单）
    └── flex flex-col gap-4
        ├── 时限配置区（Card「响应时限」）
        │   ├── 首次响应（InputNumber 分钟，空=不启用）
        │   └── 解决响应（InputNumber 分钟，空=不启用）
        ├── 超时动作区（Card「超时动作」）
        │   ├── 顶栏：说明文案 + 右上「添加动作」下拉（更换处理人 / 添加关注人）
        │   ├── 固定块：升级优先级 [Switch] 目标档 [Select 优先级下拉]（开关开启时显示目标档）
        │   └── 动态块列表（Form.List）
        │       ├── 更换处理人块：MemberPicker 单选（超时后强制指派处理人）
        │       └── 添加关注人块：MemberPicker 多选（超时后追加关注人，不覆盖已有）
        │       └── 每块右侧删除按钮
        │       └── 底部提示：执行顺序固定：升级优先级 → 更换处理人 → 添加关注人，每工单仅执行一次
        └── 工作日历区（Card「工作日历」）
            ├── 工作日：周一~周日 七个复选（默认周一~周五）
            ├── 周末是否工作：Switch（关闭=周六日不计 SLA 工时）
            └── 节假日：日期列表（DatePicker 添加 + 标签删除，节假日不计 SLA 工时）
        底部：保存（primary）/ 重置
```

### 3.2 交互说明

- **保存**：PUT `/api/v1/admin/domains/{id}/sla-config`，单行 upsert；保存后立即生效（下次扫描/建单按新配置）。
- **动态块**：从右上「添加动作」下拉选择动作类型 → 追加到动作区；同类型可加多个（如两个更换处理人块按序执行）；块内 MemberPicker 数据源为当前域成员（`fetchDomainMembersPage`）。
- **目标档下拉**：数据源 `fetchDomainPriorityLevels`（urgent/high/normal/low）；保存时校验目标档存在且未低于当前默认档（前端提示即可，后端兜底校验）。
- **工作日历**：工作日复选至少一项（否则提示）；节假日日期允许跨年；周末开关与工作日复选独立（周末算工作日时周六/周日计入工时）。
- **高级模式（JSON）**：保留 Collapse「高级模式（JSON）」兜底（与可视化双向同步，JSON 为唯一数据源），字段重排后同步。

### 3.3 平台端处置

- 平台菜单 `ADM0000000053` hidden=1（迁移）；`/platform/sla-management` 页面与接口保留不删；`platform.sla.*` 权限码保留注册（隐藏菜单即不可达）。
- 平台端「全局默认规则 / 业务域规则（代管）」Segmented 页面不再有入口，代码保留。

## 四、数据模型

```sql
-- 新增：SLA 配置单行表（每业务域一行，域内通配）
CREATE TABLE sla_config (
    id                      BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    business_domain_id      BIGINT UNSIGNED NOT NULL COMMENT '业务域 id（每域一行）',
    first_response_minutes  INT UNSIGNED NULL COMMENT '首次响应时限（分钟，空=不启用）',
    resolution_minutes      INT UNSIGNED NULL COMMENT '解决响应时限（分钟，空=不启用）',
    breach_action_json      JSON NULL COMMENT '超时动作：{escalate_priority:{enabled,to_priority_level_id}, assign_to_staff_account_id, add_watcher_staff_account_ids[]}',
    calendar_json           JSON NULL COMMENT '工作日历：{working_days:[1,2,3,4,5], weekend_work:false, holidays:["2026-10-01"]}',
    urgent_first_response_minutes INT UNSIGNED NULL COMMENT '预留：紧急配置首响（后置）',
    urgent_resolution_minutes     INT UNSIGNED NULL COMMENT '预留：紧急配置解决（后置）',
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sla_config_domain (business_domain_id)
) COMMENT='域 SLA 配置（单行，取代 sla_rule 规则列表）';

-- 旧表 sla_rule / sla_calendar 保留不读（兼容回滚）；无种子数据，无需迁移。
```

**动作 JSON 约定**（breach_action_json）：

```json
{
  "escalate_priority": { "enabled": true, "to_priority_level_id": 2 },
  "assign_to_staff_account_id": 11,
  "add_watcher_staff_account_ids": [5, 9]
}
```

- `escalate_priority` 从 bool 升级为对象（兼容旧 bool 值：true=按序升到下一档，引擎兼容读）；目标档为空时按 sort_order 升下一档。
- 旧键 `raise_priority_to`/`sla_status` 引擎兼容保留（旧规则行为不变）。

## 五、引擎接入（实现要点）

1. **策略解析**：`SlaService.loadPolicy` 改为优先读 `sla_config` 单行（域内通配），未配置则回退旧匹配链（兼容旧数据）→ 未来旧链废弃。
2. **日历折算**：`applyOnCreate` 写 deadline 时按 `calendar_json` 折算 —— 把 SLA 分钟数从「自然分钟」改为「工作分钟」：跳过周末（weekend_work=false 时的周六日）与节假日日期；`SlaScanJob` 超时判定基于折算后的 deadline（仍用 now 比较）。
3. **动作执行**：`processSlaBreach` 扩展 `escalate_priority.to_priority_level_id`（指定目标档，validate 属于该域）；assign/watchers 沿用 forceAssign/appendWatchers（source=sla_breach）。
4. **每工单一次**：沿用 `sla_breach_actioned` 原子标志，不变。
5. **紧急字段**：`urgent_*` 列仅建表预留，引擎不读（后置）。

## 六、菜单迁移（SQL）

```sql
-- 1) 域菜单更名 + 补图标
UPDATE iam_admin_menu SET name = 'SLA 配置', icon = 'FieldTimeOutlined'
WHERE code = 'BUSINESS-DOMAIN-SLA';
-- 2) 平台 SLA 菜单隐藏
UPDATE iam_admin_menu SET hidden = 1 WHERE id = 53;  -- ADM0000000053
```

前端：`src/icons/menu-icons.ts` 白名单注册 `FieldTimeOutlined`；`routes/modules/domain.ts` 静态路由 handle.icon 已是 FieldTimeOutlined（兜底一致）。

## 七、验收要点

1. 域端菜单显示「SLA 配置」+ FieldTimeOutlined 图标；平台菜单隐藏不可达。
2. /domain/sla 为单页面板：首次响应/解决响应 + 超时动作区 + 工作日历区，无规则列表、无日历 Tab。
3. 超时动作：升级优先级开关+目标档下拉；右上「添加动作」可添加更换处理人/添加关注人动态块，可多可删；保存后 JSON 正确。
4. 工作日历：周几复选/周末开关/节假日列表保存后生效；建单 deadline 按工作分钟折算（周六日/节假日不计时）。
5. 保存 upsert 单行；空首次响应/解决响应=不启用对应时限。
6. 旧接口与旧表保留可回滚；平台端隐藏后接口仍可调用（无破坏）。
