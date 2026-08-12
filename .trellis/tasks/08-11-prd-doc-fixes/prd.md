# PRD文档修正与权限码升格

## Goal

修正 prd.md §4.4 自洽检验过度声明；feature-list.md §8 权限码升格（F4.19/F3.2/F4.2/F4.4/F4.6/F4.18/F3.4）；2026-08-12 评审后并入：PRD 高危残留修正、连字符权限码统一迁移、裸奔权限码门控补齐归入映射、CustomerWeb antd v6 升级登记。

## 决策记录（2026-08-12 评审 Q&A 落盘）

- **D1（Q1）**：PRD 高危残留修正**并入本任务**（见 R1）。
- **D2（Q2）**：连字符权限码**统一迁移立项**（见 R2，代码侧，可作独立实施批次）。
- **D3（Q3）**：裸奔权限码门控补齐**归入各功能页对应任务**（见 R3 映射表；无对应任务者待立项）。
- **D4（Q5）**：CustomerWeb antd v5 → v6 **并入升级计划**（见 R4；仓库无既有升级计划文档，先登记、随计划建立时并入）。
- **D5（Q4）**：未答 → 维持默认：`08-11-group-role-management` 等 P0-②（`08-12-p0-cross-domain-security`）完成后启动。

## Requirements

- **R0（原任务范围，保留）**：
  - 修正 prd.md §4.4 自洽检验过度声明：四项全勾仅覆盖「编号↔页面↔章节」，未覆盖 §4.1 架构约束与 L3/L4（foundation-rules/data-model）一致性
  - feature-list.md §8 权限码升格：F4.19 `platform.domain.control.config.*` 未命中码回填；F3.2/F4.2/F4.4/F4.6 推断码确认；F4.18/F3.4 相应处理
- **R1（D1，高危残留修正）**：
  - C-1：prd.md §4.1.2「注册策略三选一」旧模型 → 入域双开关（`registration_enabled`/`invitation_enabled`），与 foundation-rules §2.2.2/§2.2.4、附录 A 对齐；同步修正 §3.1、§5.4.1 F4.1 措辞
  - C-2：prd.md §4.1.3 已物理拆除表 `user_global_role`/`user_domain_role` → `domain_member_role` + `staff_account_platform_role`（data-model.md:70，Flyway V20260719100446）
  - 顺带（低成本同段处理）：`role.scope` 取值口径统一（prd.md:153 `global` vs feature-list.md:316 `platform`）
  - §4.4 自洽检验扩展覆盖 §4.1 与 L3/L4 一致性
- **R2（D2，连字符码统一迁移）**：`platform.domain.control.general.update-status`、`platform.domain.control.customer.update-status` → `update_status`（下划线），迁移面：后端权威码表 `PermissionCodes.java:85-93` + Flyway `iam_permission` 种子 + 前端 `platform-domain-permissions.ts:11,16`、`permission-code-labels.ts:66,70` + 全部使用点（`detail-baseinfo.tsx:102`、`detail-customers.tsx:664,728` 等）；需评估存量授权记录兼容/迁移策略
- **R3（D3，裸奔门控补齐归入映射）**：以下「后端有码、前端 0 门控」项按映射归入对应任务（无对应任务者待立项，立项时携带）：
  | 码 | 功能页 | 归入任务 |
  |---|---|---|
  | `platform.dashboard.read` | F4.6 平台首页仪表盘 | 待立项 |
  | `domain.sla.*` | F2.2 客户 SLA 感知 / F4.15 SLA 管理 | 待立项（E4 未排期） |
  | `inbox.*` | 管理端收件箱 | 待立项 |
  | `attachment.*` | 附件上传/下载页 | 并入 `08-11-ticket-dynamic-form`（附件链路关联）或待立项 |
  | `platform.system_config.*` | F4.4 系统设置/安全 | 待立项 |
  | `platform.role_permission.*`、`platform.role.bind` | F4.9 平台角色管理 | 与 `08-11-group-role-management` P3 双轨治理关联，门控补齐可先行立项 |
  | `platform.log.audit.read`、`platform.log.login.read` | F4.12 平台日志 | 待立项 |
- **R4（D4，antd v6 升级登记）**：CustomerWeb `antd 5.24.5` → v6（与 AdminWeb `^6.3.7`、AGENTS.md「全栈 AntD v6」声明对齐）；仓库无既有升级计划文档，登记待立项

## Acceptance Criteria

- [x] AC1 prd.md §4.1.2/§4.1.3 与附录 A、foundation-rules、data-model 一致，无已废弃模型/表名残留；`role.scope` 口径统一（2026-08-12 完成：注册策略→入域双开关、已拆除表→双轨真相源、scope 口径说明）
- [x] AC2 §4.4 自洽检验覆盖 §4.1 架构约束与 L3/L4 一致性（2026-08-12 完成：新增第 5 项检查）
- [x] AC3 连字符码全量迁移：后端权威码表/种子/前端常量/使用点零残留；存量授权数据兼容策略确定并执行（2026-08-12 完成：PermissionCodes.java ×2、前端常量 ×2 文件、新迁移 V20260812150000 UPDATE iam_permission/iam_admin_menu；前端 typecheck + 后端 uniondesk-iam 编译通过）
- [ ] AC4 R3 映射表各归入项在对应任务中可追踪（各任务 prd.md 注明）——映射表已在本任务 prd.md R3 落盘；各归入任务立项时携带
- [x] AC5 R4 升级登记完成（本任务 R4 登记；升级计划文档建立时并入）
- [x] AC6 原升格项（F4.19/F3.2/F4.2/F4.4/F4.6/F4.18/F3.4）同步完成（2026-08-12 完成，feature-list.md §8 回填）

## Notes

- 本任务保持 planning；R1/R0 为文档侧修改，R2 为代码侧迁移（可拆独立实施批次，建议与 R1 同批次评审）。
- 证据来源：2026-08-12 三路 trellis-research 只读评审（PRD 交叉核对 / 权限码双向审计 / 项目现状），发现全部未落盘（用户要求报告前零产出）。
- Lightweight tasks can remain PRD-only.
- For complex tasks, add `design.md` for technical design and `implement.md` for execution planning before `task.py start`.
