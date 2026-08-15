# 研究：咨询排队与双模式接入（Redis 高性能方案）

> 2026-08-16 落盘｜E-EMP1 升级方案｜用户拍板：排队 + 自动/手动双模式 + 客服在线门控 + **Redis 高性能方案**（本地 30379 验证可用，密码走环境变量）

## 环境验证

- Redis：127.0.0.1:30379（反代转发，PID 28572 同 MySQL/MinIO），AUTH `9H4mqZcAKeNsUtuOGaWImpqv` → +OK，PING 正常
- 项目现状：无 spring-data-redis 依赖、application.yml 无 redis 配置、无运行实例 → 需新增依赖+配置（**凭据走环境变量，不写字面量**）
- MySQL 8（30306）——SKIP LOCKED 可用但本方案用 Redis 原子操作

## 方案要点（已确认）

1. **在线状态**：Redis String `agent:online:{domainId}:{staffId}` = JSON{status,mode,lastHeartbeat}，心跳 SETEX 90s（TTL 自动过期=离线，无需显式离线）；Hash `agent:online:{domainId}`（field=staffId）供在线列表查询
2. **接入模式**：并入在线 value 的 mode（auto/manual）；客服手工切换，**在线才能开启**（开 auto 时校验自己在线，并触发拉取）
3. **排队**：Redis List `queue:consult:{domainId}`——客户发起时无在线 auto 客服 → LPUSH 入队 + DB session_status='queued'；客服开自动/上线 → **RPOP 原子取队**（多客服并发无锁竞争）→ 分配自己 → open
4. **自动分配（推送快路径）**：客户发起时 HGETALL 在线客服 → 过滤 auto 模式 → least_loaded（DB COUNT 未完结）→ 条件更新分配
5. **DB 权威持久层**：session_status（queued/open/closed）+ assigned_to 落库；Redis 为热路径索引；双写一致性（入队/取队 Redis+DB），异常降级（DB 兜底，可选定时补队列）
6. **权限**：consultation.claim/close 新增（三处联动：PermissionCodes + iam_permission 种子迁移 + AdminPermissionCatalog）
7. **撤回**：consultation_message 加 retracted_at/retracted_by（迁移）；2 分钟内、本人会话自己的消息
8. **结束**：endSession open→closed；未转单可关；重复拒绝；与转单自动关闭同终态互不冲突

## 页面结构（已出图，见会话）

- 无新增页面：工作台咨询 Tab（/domain/workbench?tab=consultation）与独立入口（/domain/consultations）组件级增强
- 组件：接入模式开关/在线提示/仅看我的/排队徽标/接入按钮/结束按钮/消息撤回

## 关键文件（实现参考）

- `uniondesk-ticket/.../consultation/core/ConsultationService.java`（createSession:166/replyAdmin:157/listAdminSessions:128）
- `ConsultationRuntimeController.java`（45-127 端点）
- `consultation_session` 表（session_status varchar：可加 queued 无迁移；assigned_to 已有）
- `consultation_message` 表（需迁移加撤回列）
- `PermissionCodes.java`（217-220 咨询码）、`AdminPermissionCatalog.java`（450 行附近）
- 迁移先例：`V20260813200000__consultation_runtime_permissions.sql`
- 前端：`pages/domain/consultations/index.tsx`（列表+抽屉）、`pages/domain/workbench/index.tsx`
