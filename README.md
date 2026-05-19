# UnionDesk

> 面向企业客户服务场景的多业务域工单平台，统一处理 **咨询**、**工单**、**反馈/建议** 三类请求。

当前仓库为 UnionDesk 的项目工作区。产品需求、系统架构、数据库设计、技术栈方案已沉淀在 `doc/`；前端工作区 `UnionDeskWeb/` 已初始化（含 `UnionDeskAdminWeb` 管理端与 `UnionDeskCustomerWeb` 客户端），后端 `UnionDesk/` 已存在 Spring Boot + Flyway + MyBatis 工程，当前重点是修复并稳定启动链路与迁移链路。本 README 用于描述当前真实状态与下一步动作；`AGENTS.md` 用于约束后续开发协作。

---

## 1. 仓库结构

```text
UnionDesk/
├─ AGENTS.md                # 开发协作与代码代理（Agent）指导规范
├─ README.md                # 本文件
├─ doc/                     # 中文设计文档
│  ├─ 产品需求文档 v1.0.md   # 产品需求文档（唯一需求真相源）
│  ├─ 系统架构设计.md        # 模块化单体架构 + 演进路线
│  ├─ 数据库设计.md          # 实体、索引、生命周期
│  ├─ 技术栈方案.md          # 前后端、数据、日志、部署选型
│  └─ README.md             # 文档总览
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

详细职责与模块划分见 `doc/系统架构设计.md`。

## 3. MVP 目标（摘自 PRD V1.0）

MVP 按 P0 / P1 / P2 三个批次交付，详见 `doc/产品需求文档 v1.0.md § 1.4`。

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

- [x] 产品需求文档 PRD
- [x] 系统架构设计文档
- [x] 数据库设计文档
- [x] 技术栈方案
- [x] `AGENTS.md` 开发协作规范
- [x] 前端 Monorepo 脚手架（pnpm + Vite + AntD）
- [x] 管理端平台入口、菜单/角色基础页面
- [x] 后端工程脚手架（Spring Boot + Flyway + MyBatis）
- [ ] 后端启动链路与 Flyway 迁移链路稳定化
- [ ] 本地基础依赖启动说明（按需参考 docker compose + MySQL + MinIO）
- [ ] 鉴权与 IAM 最小闭环
- [ ] 工单核心闭环（提交 → 处理 → 关闭）

## 6. 下一步路线

### 阶段 0：基础设施
1. 稳定 `UnionDesk/` Spring Boot 启动链路，按 Flyway 执行并验证迁移脚本（见 `doc/数据模型迁移策略.md`）。
2. `UnionDeskWeb/` 已初始化，含 `apps/UnionDeskAdminWeb`、`apps/UnionDeskCustomerWeb`、`packages/`。
3. 提供 `docker-compose.yml`（MySQL 8、Redis、MinIO）作为本地依赖参考；已有可用环境时可直接复用或跳过。

### 阶段 1：最小闭环
1. 登录 / Token 刷新 / 基于 `business_domain_id` 的数据权限。
2. 业务域 CRUD + 工单类型 + 动态字段配置。
3. 客户端完成"选域 → 提单 → 查单"。
4. 管理端完成"工单池 → 分配 → 回复 → 关闭"。
5. 审计日志（`ticket_event_log` / `operation_log`）。

### 阶段 2：增强
1. SLA 计时与超时预警。
2. 咨询会话与"咨询转工单"。
3. 通知模板（站内 → 邮件/短信）。
4. OpenAPI 驱动的前端类型化 API Client。

## 7. 快速开始（参考）

> 后端工程已存在；基础依赖仅在本机没有可用环境时按需启动。前端已可本地启动。

```powershell
# 如本机没有可用的 MySQL / Redis / MinIO，再按需启动
docker compose up -d

# 后端
cd UnionDesk
.\mvnw.cmd spring-boot:run

# 前端
cd UnionDeskWeb
pnpm install
pnpm -C apps/UnionDeskCustomerWeb dev
pnpm -C apps/UnionDeskAdminWeb dev
```

## 8. 参考

- 产品需求：`doc/产品需求文档 v1.0.md`（唯一需求真相源）
- 系统架构：`doc/系统架构设计.md`
- 数据库设计：`doc/数据库设计.md`
- 技术栈方案：`doc/技术栈方案.md`
- 数据迁移：`doc/数据模型迁移策略.md`
- 开发协作规范：`AGENTS.md`
- GitNexus（`analyze --embeddings`）Windows / 网络修复与重装后打补丁：`scripts/gitnexus/README.md`

## 9. 编码规范

- 仓库文本文件统一使用 **UTF-8（无 BOM）**。
- 提交前请在前端工作区执行编码检查：

```powershell
pnpm --dir UnionDeskWeb run check:utf8
```
