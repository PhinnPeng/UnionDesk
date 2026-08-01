# 本 Sprint — 任务清单

> **事项属性设计**：[`design-attribute.md`](design-attribute.md)  
> **Sprint 总览**：[`proposal.md`](proposal.md)

---

## S1 事项属性（设计 v2 — [`design-attribute.md`](design-attribute.md)）★

- [x] **S1-1** Flyway：`ticket_attribute`（无 code；name+description；sort_order；audit）、`ticket_type_attribute`（slot_config；无 group_key）
- [x] **S1-2** Po/Mapper/Repository + `TicketAttributeService`（CRUD + reorder + type_config 校验）
- [x] **S1-3** `TicketTypeAttributeSlotService`（插入唯一、slot_config、reorder）
- [x] **S1-4** `FormSnapshotBuilder` + 扩展 `TicketFormSchemaService`（draft/publish；沿用 `ticket_form_schema` 表）
- [x] **S1-5** API：字典 reorder；`/attribute-slots`；`/form-release/*`（form-schema 旧路径部分保留）
- [x] **S1-6** promote：按 **name** 冲突检测（无 code）
- [x] **S1-7** 前端：字典页拖拽排序（≤100 无分页）
- [x] **S1-8** 前端：**属性编排页**（替代 form-design 主路径；插入/拔出/插槽配置）
- [x] **S1-9** 前端：全局/域内字典 Modal（四分 field_type + 动态 type_config）
- [x] **S1-10** shared `api.ts` / `types.ts`
- [ ] **S1-11** 验收：design-attribute §验收标准（需 Flyway + 手工冒烟）
- [ ] **S1-12** 跟进：全局事项配置侧栏菜单 Flyway；form-schema/form-release 统一；Attribute 专项单测

---

## S2 团队模板 + 建域复制

- [ ] **S2-1** Flyway：`ticket_team_template`、`_item`、`business_domain.applied_*`
- [ ] **S2-2** `TicketTeamTemplateService` + 平台 CRUD API
- [ ] **S2-3** `TeamTemplateApplyService`（含属性/插件/layout 拷贝）
- [ ] **S2-4** 建域表单可选模板 + DomainBootstrap 挂钩
- [ ] **S2-5** 种子：默认团队模板

---

## S3 全局事项配置壳 + 全局类型入口

- [ ] **S3-1** `ticket-config/layout.tsx` + 路由 + 菜单 Flyway
- [ ] **S3-2** 团队模板列表页
- [ ] **S3-3** 全局事项类型简列表（元数据；操作·属性接 S1）

---

## S4 域管控 · 事项类型列表

- [ ] **S4-1** `ticket-type-list-table.tsx` 共用
- [ ] **S4-2** 重构 `detail-tickets.tsx`（列、Switch、隐藏事项模板 Tab）
- [ ] **S4-3** `ticket_type` 扩展：category、customer_visible 等（若 S1 未做）

---

## S5 工作流（现网维持）

- [ ] **S5-1** 确认 Flow 页仍写 `status_flow_config`（无状态字典表）
- [ ] **S5-2** 列表「工作流」入口联通即可；无新设计文档

---

## S6 权限

- [ ] **S6-1** Flyway：`platform.ticket_config.*`、`platform.domain.control.ticket_attribute.*`
- [ ] **S6-2** `PermissionCodes` + 控制器校验 + 前端 AuthGuarded

---

## 文档

- [ ] 更新 `data-model.md` **仅属性相关**段落
- [ ] `design.md` 顶部指向 `design-attribute.md` 为主设计

---

## 明确不做（本 Sprint）

- [ ] `ticket_status` / 状态插件 / `ticket_flow_config` 拆表
- [ ] `domain.ticket_*`、CustomerWeb 联调
- [ ] 工作流 SLA/流转条件插件化
