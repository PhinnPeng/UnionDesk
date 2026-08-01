# 设计：团队模板

## 1. 架构摘要

```text
平台事项配置
└── 团队模板（引用 N 个 platform ticket_type）
        │
        │  建域可选 apply（深拷贝快照）
        ▼
业务域 ticket_type + slots + form_schema + flow + description_template_md
（applied_team_template_id/version 永久记录；之后解耦）
```

| 对象 | 行为 |
|------|------|
| 团队模板 | 项 = 对平台类型的引用；有效装配内容随平台类型当前配置变化 |
| 业务域 | 建域时深拷贝一次；之后不随模板/平台依赖变化 |

## 2. 数据模型

### 2.1 `ticket_team_template`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| code | VARCHAR(64) UNIQUE | 创建后不可改 |
| name | VARCHAR(128) | |
| description | VARCHAR(500) | 默认 '' |
| icon | VARCHAR(64) NULL | |
| status | VARCHAR(16) | active / disabled |
| is_system | TINYINT | 系统预置不可删 |
| sort_order | INT | |
| version | INT | 改 items 时 +1；建域写入域侧 |
| created_by / updated_by | BIGINT NULL | |
| created_at / updated_at | DATETIME(3) | |

### 2.2 `ticket_team_template_item`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | |
| team_template_id | BIGINT FK | |
| ticket_type_id | BIGINT FK | 必须 `ticket_type.scope=platform` |
| sort_order | INT | |
| include_form_schema | TINYINT | 默认 1 |
| include_workflow | TINYINT | 默认 1 |
| include_description_template | TINYINT | 默认 1 |
| UNIQUE(team_template_id, ticket_type_id) | | |

### 2.3 `business_domain` 扩展

| 字段 | 说明 |
|------|------|
| applied_team_template_id | NULL=空域 |
| applied_team_template_version | 创建时模板 version |
| applied_team_template_at | 应用时间 |

### 2.4 溯源（现网已有）

- 域 `ticket_type.source_global_type_id` → 平台类型
- 域属性 `source_attribute_id`（若未来复制域自定义属性时用）

## 3. 后端

### 3.1 API

前缀：`/api/v1/platform/ticket-team-templates`

| 方法 | 路径 | 权限 |
|------|------|------|
| GET | `/` | template.read |
| GET | `/{id}` | template.read |
| GET | `/options` | template.read 或 domain.create | active 列表供建域 |
| POST | `/` | template.create |
| PUT | `/{id}` | template.update（改 items → version++） |
| DELETE | `/{id}` | template.delete（拒删 is_system） |
| PUT | `/reorder` | template.update |

建域：`POST /api/v1/admin/domains` body 增加可选 `team_template_id`。

权限新增：

```text
platform.ticket_config.template.read|create|update|delete
```

### 3.2 服务

| 类 | 职责 |
|----|------|
| `TicketTeamTemplateService` | CRUD、items 全量替换、校验 platform 类型 |
| `TeamTemplateApplyService` | 建域事务内深拷贝 |
| `DomainService.createDomain` | 创建后可选调用 Apply，写 applied_* |

### 3.3 Apply 算法（贴现网）

对每个 item（按 sort_order）：

1. 平台类型 P → INSERT 域类型 D（`source_global_type_id=P.id`；code 冲突加后缀）
2. 复制 P 的 attribute slots → D（MVP：系统属性仍引用平台 attribute_id）
3. `include_form_schema`：复制 P 当前 published form_schema → D published v1
4. `include_workflow`：复制 flow_status / transition / rules → D
5. `include_description_template`：复制 `description_template_md`
6. 写 `business_domain.applied_team_template_*`

失败整单回滚。

### 3.4 校验

- 模板 items ≥ 1；类型必须 platform 且存在
- disabled 模板不可 apply
- 系统模板不可删；code 创建后不可改

## 4. 前端

### 4.1 入口

- `TicketConfigSider` + `section=templates`
- `TeamTemplatesPanel`（对齐 attributes/statuses 面板风格）
- 建域向导：替换「即将推出」卡为可选 active 模板

### 4.2 编辑体验

1. 基本信息：code / name / description / icon / status  
2. 包含类型：多选平台类型 + 拖拽排序 + 三项 include 开关  
3. 保存改 items 时提示：将递增 version，仅影响之后新建域

### 4.3 shared

- `packages/shared`：`TeamTemplate*` 类型 + API 封装

## 5. 波次

| 波次 | 内容 |
|------|------|
| M1 | Flyway + CRUD API + 侧栏列表/编辑 |
| M2 | ApplyService + 建域选模板 + 种子 `default_cs` |
| M3 | include 细开关 UX、删除保护文案、审计（可选） |

## 6. 可行性评估

### 6.1 结论

**可行。** 建议按 M1→M2 落地；风险可控，无阻塞性架构冲突。

### 6.2 有利条件（现网已具备）

| 能力 | 现状 |
|------|------|
| 平台/域事项类型 | `ticket_type.scope` + `source_global_type_id` |
| 属性槽位 | `TicketTypeAttributeSlotService` 可按类型复制 |
| 表单发布 | `ticket_form_schema` published 可复制 |
| 工作流表 | flow_status / transition 已扁平化，可按 typeId 拷贝 |
| 描述模板 | `description_template_md` 字段已存在 |
| 事项配置壳 | 侧栏 section 模式易扩展 `templates` |
| 建域入口 | `domains-modal` 已有模板占位卡 |
| OpenSpec 背景 | `matter-config-global` §4 语义已对齐 |

### 6.3 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| Apply 拷贝面大、事务长 | 中 | 限制模板内类型数（如 ≤20）；事务内分步校验；集成测试覆盖 |
| 工作流/规则 JSON 含硬编码 id | 中 | Apply 后做 id 重映射或仅拷贝 code 型规则；M2 单测覆盖典型规则 |
| 系统属性引用平台 vs 域隔离叙事 | 低 | MVP 明确文档：字段定义可共用，装配/schema/flow 为域副本 |
| 模板「随依赖变」被误解为域热更新 | 低 | UI 文案：版本递增只影响新建域；AC4 验收 |
| 与旧 `ticket_template` 命名混淆 | 低 | API/表名一律 `ticket_team_template`；产品文案「团队模板」 |
| 权限/菜单 Flyway 遗漏 | 低 | 与现 `platform.ticket_config.*` 同批种子 |

### 6.4 不推荐的替代（已否决）

- 域与模板保持活依赖（违背已确认「域跟快照」）
- 模板内嵌冻结大 JSON 全量配置（难维护；与「随依赖变」矛盾）
- 独立「配置方案」菜单名（已统一团队模板）

### 6.5 工作量粗估

| 波次 | 量级 |
|------|------|
| M1 | 中小（表+CRUD+列表页，模式可抄状态/属性面板） |
| M2 | 中（Apply 为核心复杂度） |
| M3 | 小 |

**总体：可实施；优先保证 Apply 正确性与「域不回写」验收。**
