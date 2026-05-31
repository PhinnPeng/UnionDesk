# UnionDesk

> 面向企业客户服务场景的多业务域工单平台，统一处理 **咨询**、**工单**、**反馈/建议** 三类请求。

当前仓库为 UnionDesk 的项目工作区。产品需求、领域规则与迭代 Backlog 以 **`docs/`** 为权威（见 [`docs/README.md`](docs/README.md)）；根目录 `doc/` 为历史文档，只读参考。前端 `UnionDeskWeb/`、后端 `UnionDesk/` 已存在。本 README 描述运行方式；`AGENTS.md` 约束 Agent 协作。

---

## 1. 仓库结构

```text
UnionDesk/
├─ AGENTS.md                # 开发协作与代码代理（Agent）指导规范
├─ README.md                # 本文件
├─ docs/                    # 产品/架构/Backlog（权威）
│  ├─ product/              # PRD、Backlog、Sprint 计划
│  └─ architecture/         # 数据模型、增量计划、ADR
├─ doc/                     # 历史设计文档（只读参考）
├─ UnionDesk/               # 后端服务工程（已存在，启动链路待稳定）
└─ UnionDeskWeb/            # 前端工作区（已初始化）
   ├─ apps/UnionDeskAdminWeb     # 管理端
   ├─ apps/UnionDeskCustomerWeb  # 客户端
   └─ packages/                  # 共享包
```

## 2. 项目定位

- **UnionDesk**（后端）：身份认证、IAM、业务域、工单、咨询、反馈、通知、审计，统一 REST API。
- **UnionDeskWeb**（前端）：Monorepo，包含：
  - `apps/UnionDeskCustomerWeb`：客户端（提单、查单、反馈）
  - `apps/UnionDeskAdminWeb`：管理端（客服工作台、配置、权限）
  - `packages/`：共享 API Client、类型、组件

详细职责见 [`docs/product/prd.md`](docs/product/prd.md) 与历史 `doc/系统架构设计.md`。

## 3. MVP 目标（摘自 PRD V1.0）

MVP 范围详见 [`docs/product/prd.md`](docs/product/prd.md) §1.4；迭代排期见 [`docs/product/backlog-stories.md`](docs/product/backlog-stories.md)。

**P0 最小闭环**：身份登录、业务域管理、RBAC、客户提单、客服处理、基础 SLA、审计日志、站内信必达 + SMTP 可用时邮件、基础附件上传、健康检查、最小部署。

**P1 增强**：在线咨询、咨询转工单、满意度评价、反馈/建议、动态表单引擎、批量操作、客户注销。

**P2 平台化**：知识库全流程、VitePress 帮助中心、全文搜索、群发通知、归档导出、安全告警、灰度发布。

## 4. 技术栈基线

| 层次 | 选型 |
|---|---|
| 后端运行时 | Java 21+（目标 Java 25 LTS，按团队可用版本就近落地） |
| 后端框架 | Spring Boot 3.x、Spring Security（JWT Access + Refresh） |
| 持久层 | MyBatis（或 MyBatis-Plus） |
| 数据库迁移 | Flyway |
| 缓存 | Redis |
| 异步 | RabbitMQ（评估后再引入） |
| 数据库 | MySQL 8.x（开发先用 8.0，目标 8.4 LTS） |
| 对象存储 | S3 协议（开发用 MinIO） |
| 前端 | React + TypeScript + Vite + Ant Design |
| 包管理 | pnpm workspace |
| 日志 | ELK 优先，本地回退 Logback 滚动文件 |
| 容器化 | Docker / docker compose |

完整细节见 `doc/技术栈方案.md`。

## 5. 当前完成度

- [x] 文档权威链 `docs/`（vision、prd、foundation-rules、backlog）
- [x] Product Backlog（`docs/product/backlog-stories.md`）
- [x] `AGENTS.md` 开发协作规范
- [x] 前端 Monorepo 脚手架（pnpm + Vite + AntD）
- [x] 管理端平台入口、菜单/角色基础页面
- [x] 后端工程脚手架（Spring Boot + Flyway + MyBatis）
- [ ] 后端启动链路与 Flyway 迁移链路稳定化
- [x] 联调环境说明（见 [`docs/product/sprint-0-plan.md`](docs/product/sprint-0-plan.md) §3，已部署则勿重复 compose）
- [ ] 鉴权与 IAM 最小闭环
- [ ] 工单核心闭环（提交 → 处理 → 关闭）

## 6. 下一步路线

### 阶段 0：S0 收口（已完成）
1. S0 文档入库与权威链对齐（见 [`docs/product/sprint-0-plan.md`](docs/product/sprint-0-plan.md)）。
2. 联调使用**已部署** MySQL/Redis/MinIO（见 sprint-0-plan §3）；`docker-compose.yml` 仅结构演示。

### 阶段 1：S1 管理端 Walking Skeleton（已完成，2026-05-26）
1. 签 off 见 [`docs/product/sprint-1-plan.md`](docs/product/sprint-1-plan.md) §11；Story 见 backlog Sprint 1。

### 阶段 2：S2 E2 + 平台域详情 + 体验（当前）
1. Committed 见 [`docs/product/sprint-2-plan.md`](docs/product/sprint-2-plan.md) §2 与 [`docs/product/backlog-stories.md`](docs/product/backlog-stories.md) Sprint 2。

### 阶段 3 及以后（下轮规划）
1. SLA 计时与超时预警。
2. 咨询会话与"咨询转工单"。
3. 通知模板（站内 → 邮件/短信）。
4. OpenAPI 驱动的前端类型化 API Client。

## 7. 快速开始（参考）

> **开发环境已部署时，不需要重复部署中间件。**  
> 仓库内 `docker-compose.yml` **仅演示** compose 结构，日常联调**不要**以此为准重复 `docker compose up`。连接信息见 [`docs/product/sprint-0-plan.md`](docs/product/sprint-0-plan.md) §联调环境。

```powershell
# 后端（库 uniondesk，见 UnionDesk/src/main/resources/application.yml）
cd UnionDesk
.\mvnw.cmd spring-boot:run

# 管理端（S0/S1 重点）
cd UnionDeskWeb
pnpm install
pnpm -C apps/UnionDeskAdminWeb dev

# 客户端（可选）
pnpm -C apps/UnionDeskCustomerWeb dev
```

验证：`GET http://127.0.0.1:8080/actuator/health` → `{"status":"UP"}`。

## 8. 参考

- **文档权威链**：[`docs/README.md`](docs/README.md)
- 产品 / Backlog / Sprint：[`docs/product/`](docs/product/)
- 领域规则：[`docs/product/foundation-rules.md`](docs/product/foundation-rules.md)
- 历史文档（只读）：`doc/` 目录
- 开发协作规范：`AGENTS.md`

## 9. 编码规范

- 仓库文本文件统一使用 **UTF-8（无 BOM）**。
- 提交前请在前端工作区执行编码检查：

```powershell
pnpm --dir UnionDeskWeb run check:utf8
```
