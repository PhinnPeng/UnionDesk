# Sprint 0 计划 — 管理端奠基


| 文档版本 | 日期         | 周期  | 说明                     |
| ---- | ---------- | --- | ---------------------- |
| 1.0  | 2026-05-24 | 1 周 | E0：文档 + 盘点 + ADR；零业务交付 |


> Committed Story 见 `[backlog-stories.md](./backlog-stories.md)` US-S0-01～07。

---

## 1. Sprint 目标

1. 建立 L6 Product Backlog 与管理端 **implementation-inventory**
2. 明确 L3/L4/L5/L7 文档分工并完成口径对齐
3. 记录**已部署**联调环境（不执行 docker-compose）
4. 外部依赖 ADR（Formily / MinIO / SMTP）
5. **不交付**新业务功能、不执行 Flyway **squash** rebaseline（US-S0-07 基线快照 + 迁移备份除外）

---

## 2. Committed Stories（~9 SP）


| ID       | 标题                    | SP  | 状态   |
| -------- | --------------------- | --- | ---- |
| US-S0-01 | 管理端实现盘点               | 2   | Done |
| US-S0-02 | Backlog 骨架            | 2   | Done |
| US-S0-03 | 文档权威链                 | 2   | Done |
| US-S0-04 | 联调环境说明                | 1   | Done |
| US-S0-05 | increment-plan + 口径对齐 | 1   | Done |
| US-S0-06 | 外部依赖 ADR              | 2   | Done |
| US-S0-07 | 数据库基线快照与迁移备份          | 2   | Done |


---

## 3. 联调环境

> **开发环境已部署，不需要重复部署。**  
> 仓库内 `[docker-compose.yml](../../docker-compose.yml)` **仅演示** compose 结构，**不要**作为日常联调必做步骤。

### 3.1 中间件（已部署）


| 组件        | 地址                | 应用配置                                                                                                        |
| --------- | ----------------- | ----------------------------------------------------------------------------------------------------------- |
| MySQL     | `127.0.0.1:30306` | 库 `uniondesk`，用户 `uniondesk_app`（见 `[application.yml](../../UnionDesk/src/main/resources/application.yml)`） |
| Redis     | `127.0.0.1:30379` | 凭据见团队本地密钥，勿提交 Git                                                                                           |
| MinIO API | `127.0.0.1:30090` | Console `127.0.0.1:30091`；凭据见团队本地密钥                                                                         |


### 3.2 应用进程（本地启动）

```powershell
# 后端
cd UnionDesk
.\mvnw.cmd spring-boot:run

# 管理端（S0 重点）
cd UnionDeskWeb
pnpm install
pnpm -C apps/UnionDeskAdminWeb dev
```


| 检查项       | 预期                                                              |
| --------- | --------------------------------------------------------------- |
| 后端 health | `GET http://127.0.0.1:8080/actuator/health` → `{"status":"UP"}` |
| AdminWeb  | 浏览器打开 Vite 提示的本地端口，登录页可访问                                       |
| Flyway    | 启动日志 schema 版本 ≥ `202605250001`                                 |


### 3.3 凭据说明

账号密码由**已部署环境**维护；与 `[application.yml](../../UnionDesk/src/main/resources/application.yml)` 一致。如需本地覆盖，使用环境变量或 **不提交** 的本地配置文件。

---

## 4. Flyway 策略（S0 文档化）

见 `[database-increment-plan.md](../architecture/database-increment-plan.md)` §1。

- **S0**：不 squash rebaseline；执行 **US-S0-07**（联调库参考快照 + 迁移前 mysqldump，见 [`backup-restore.md`](../operations/backup-restore.md) §1.1.1）
- **S1 末**：评估管理端菜单稳定后 squash rebaseline（**须先全库备份**）

当前菜单相关迁移：`V202605200004`～`006`、`V202605210001`、`V202605220001`～`002`。

---

## 5. Definition of Done

- [x] `implementation-inventory.md` 列出管理端模块 Done/Partial/Todo
- [x] `backlog-stories.md` 可被 AGENTS.md 引用
- [x] L3/L4/L5 口径对齐（foundation §2.2.4、data-model §1、increment-plan §0）
- [x] ADR 三依赖有 MVP 决策
- [x] 无 docker-compose 部署、无新业务代码
- [x] US-S0-07：全库备份 + 参考快照 + Flyway 核查（2026-05-25 实跑）

---

## 6. 风险


| 风险                         | 缓解              |
| -------------------------- | --------------- |
| 根 README 仍指向旧 `doc/`       | US-S0-03 已改权威链（S0 收口 2026-05-24） |
| 菜单迁移过多                     | S1 末 rebaseline |
| CustomerWeb demo 与 API 不一致 | US-S1-05 排 S1   |


---

## 7. 下一步（S1）

S1 权威执行计划见 **[`sprint-1-plan.md`](./sprint-1-plan.md)**。

Committed 顺序：**S1-00a/00b（联调工程 P0）** → **US-S1-03（双字段）** → US-S1-07 / US-S1-08 / US-S1-05（合计约 13 SP 内）。Story 明细与 AC 仍以 [`backlog-stories.md`](./backlog-stories.md) 为准。