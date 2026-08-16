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

### 3.2.1 工作日历 V2：周 × 时间网格（设计升级，取代 3.1 的周几复选）

**背景**：3.1 的「工作日复选 + 周末开关」粒度太粗（一天要么全计要么全不计，无法表达 9:00-12:00 上午段 + 13:30-17:30 下午段）。升级为「周一到周日 × 00:00-23:59」的 30 分钟网格表格。

**网格结构**：

```
标题行：周一 周二 周三 周四 周五 周六 周日
标题列（行头）：
00:00-00:30  ▢ ▢ ▢ ▢ ▢ ▢ ▢
00:30-01:00  ▢ ▢ ▢ ▢ ▢ ▢ ▢
...
09:00-09:30  ▣ ▣ ▣ ▣ ▣ ▢ ▢   ← 勾选=该周几该时段计入 SLA 工时
...
23:30-24:00  ▢ ▢ ▢ ▢ ▢ ▢ ▢
```

- 粒度：**30 分钟/格**（一天 48 行），行标题为时段区间。
- 交互：**点击单格切换**；**按住拖动连续涂选**（起止格一次填满，与绘制轨迹一致）。
- 默认配置：周一至周五勾选 `09:00-12:00`（6 格）与 `13:30-17:30`（8 格），周六/周日全空。
- 数据模型（calendar_json）：

```json
{
  "time_slots": [
    { "weekday": 1, "start": "09:00", "end": "12:00" },
    { "weekday": 1, "start": "13:30", "end": "17:30" }
  ],
  "holidays": ["2026-10-01"]
}
```

- **兼容**：旧结构 `{working_days:[1..5], weekend_work:false}` 读取时映射为整天时段（`[{weekday:1,start:"00:00",end:"24:00"},...]`）；`weekend_work:true` 时补周六/周日整天时段；保存新结构后归一为 time_slots。
- **移除**：`weekend_work` 字段废弃（网格已含 7 列，周末勾选即生效）；旧字段仅做读取兼容。

**节假日区（网格下方）**：

```
法定节假日（自动同步）            [同步按钮]
  2026-10-01 国庆节 · 2026-10-02 国庆节 · …（节日只读列表，含调休上班日标注）
自定义节假日
  [DatePicker 添加] 2026-08-19 ✕  2026-09-30 ✕   （Tag 可删，域级手动）
```

- **全局节假日表**（新表 `holiday_date`，跨域共享）：
  - 数据源：**timor.tech**（免费、无需 key、10000 次/IP/24h）：`GET https://timor.tech/api/holiday/year/{year}/`，返回 `type`：0=工作日 / 1=周末 / 2=节日 / 3=调休上班。
  - 同步：后端定时 job（`@Scheduled`，每天 03:00 + 启动时）拉取**当年+次年**全年数据，幂等 upsert（按 date 唯一键）；前端「同步」按钮（POST `/admin/holidays/sync`）手动即时触发。
  - 存储：仅落 type=2（节日，name 存节日名）与 type=3（调休上班）；type 0/1 不落库（网格默认即可表达）。
  - HTTP 客户端：Spring `RestClient`（spring-web 传递依赖已具备，无需新依赖），base-url/超时走环境变量 `HOLIDAY_API_BASE_URL`（默认 `https://timor.tech/api/holiday`）、`HOLIDAY_API_TIMEOUT_MS`（默认 5000），仿 `MINIO_ENDPOINT` 注入风格。
- **判定规则**（SLA 折算时，优先级从高到低）：
  1. 域自定义 `holidays` 含该日 → **整天不计工时**（用户显式排除优先）；
  2. 全局 `holiday_date` type=2（节日）→ **整天不计工时**（即使网格勾选）；
  3. 全局 `holiday_date` type=3（调休上班）→ **计入工时**，按该周几的 time_slots 计算（若该周几无时段，按默认 09:00-12:00/13:30-17:30）；
  4. 其余 → 按该周几的 time_slots 折算（时段内累计工作分钟，时段外不计）。

**引擎算法（plusWorkingMinutes 升级）**：由「逐分钟循环 + 整天跳过」改为「按天窗口 + 时段分段折算」：
- 每天先算该日工作分钟数 `W(day)`（time_slots 区间长度求和，受节日/调休/自定义假日修正）；
- `from → from + minutes`：按天推进，`remaining -= W(day)`，当日不足一天时按 from 时刻在该日时段内的位置截断；
- 复杂度 O(天数)，消除 O(minutes) 逐分钟循环。

**接口新增**：

| 方法 | 路径 | 说明 | 权限 |
|---|---|---|---|
| GET | `/api/v1/admin/holidays?year=2026` | 全局节假日列表（节日+调休，只读） | domain.sla.read |
| POST | `/api/v1/admin/holidays/sync` | 手动触发同步（幂等） | domain.sla.update |

**待确认默认（第二轮未答复，按推荐采用，可改）**：
- 调休上班日计入 SLA 工时（按网格时段；无时段时按默认 09:00-12:00/13:30-17:30）——**已采用推荐**；
- 同步年份范围：当年+次年——**已采用推荐**；
- 节假日 UI：全局只读列表 + 域自定义可编辑——**已采用推荐**。

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
    calendar_json           JSON NULL COMMENT '工作日历：{time_slots:[{weekday,start,end}], holidays:["2026-10-01"]}（兼容旧 working_days/weekend_work）',
    urgent_first_response_minutes INT UNSIGNED NULL COMMENT '预留：紧急配置首响（后置）',
    urgent_resolution_minutes     INT UNSIGNED NULL COMMENT '预留：紧急配置解决（后置）',
    created_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at              DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_sla_config_domain (business_domain_id)
) COMMENT='域 SLA 配置（单行，取代 sla_rule 规则列表）';

-- 旧表 sla_rule / sla_calendar 保留不读（兼容回滚）；无种子数据，无需迁移。
```

**节假日表（V2 新增，全局跨域共享）**：

```sql
-- 全局法定节假日（timor.tech 同步，仅落节日/调休两类；0/1 不落库）
CREATE TABLE holiday_date (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    date        DATE            NOT NULL COMMENT '日期',
    name        VARCHAR(64)     NULL COMMENT '节日名（如 国庆节）',
    type        TINYINT         NOT NULL COMMENT '2=法定节日（不计工时） 3=调休上班日（计工时）',
    year        INT             NOT NULL COMMENT '所属年份（当年+次年同步）',
    created_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at  DATETIME(3)     NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_holiday_date (date)
) COMMENT='全局法定节假日（timor.tech 同步，节日/调休上班）';
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
2. **日历折算（V2）**：`plusWorkingMinutes` 由「逐分钟循环 + 整天跳过」升级为「按天窗口 + 时段分段折算」——每天计算该日工作分钟数 `W(day)`（time_slots 区间求和，受节日/调休/自定义假日修正），按天推进 `remaining -= W(day)`，当日不足一天按 from 在时段内位置截断；复杂度 O(天数)。判定优先级：域 holidays（整天不计）→ 全局节日 type=2（整天不计）→ 全局调休 type=3（计入，按该周几时段；无时段按默认 09:00-12:00/13:30-17:30）→ 网格 time_slots（时段内计，时段外不计）。`SlaScanJob` 超时判定基于折算后 deadline（仍用 now 比较，无感）。
3. **动作执行**：`processSlaBreach` 扩展 `escalate_priority.to_priority_level_id`（指定目标档，validate 属于该域）；assign/watchers 沿用 forceAssign/appendWatchers（source=sla_breach）。
4. **每工单一次**：沿用 `sla_breach_actioned` 原子标志，不变。
5. **紧急字段**：`urgent_*` 列仅建表预留，引擎不读（后置）。
6. **节假日同步（V2 新增）**：`HolidaySyncJob`（uniondesk-ticket 复用 SchedulingConfiguration，`@Scheduled` 每天 03:00 + 启动时执行）→ `RestClient` 拉 timor.tech 当年+次年 → 幂等 upsert `holiday_date`；手动触发 `POST /admin/holidays/sync` 走同一服务方法。同步失败不影响主流程（try/catch + 日志，下轮重试）。
7. **兼容（V2）**：`calendar_json` 读取时兼容旧结构 `working_days/weekend_work`（映射整天时段）；保存新结构归一为 `time_slots`。`weekend_work` 字段废弃（网格含 7 列）。

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
4. 工作日历 V2：网格表格（周一~周日 × 30 分钟格）默认勾选周一至周五 09:00-12:00 + 13:30-17:30；点击单格切换、拖动连续涂选；保存后 time_slots 正确序列化。
5. 节假日：定时 job 同步当年+次年（timor.tech，幂等 upsert）；前端只读展示全局节日/调休 + 自定义节假日增删；「同步」按钮即时触发。
6. 折算正确性：网格时段外不计时；域 holidays/法定节日整天不计；调休上班日计入（按该周几时段）；deadline 落在正确时刻。
7. 兼容：旧 calendar_json（working_days/weekend_work）读取映射整天时段；旧接口与旧表保留可回滚。
8. 保存 upsert 单行；空首次响应/解决响应=不启用对应时限。
