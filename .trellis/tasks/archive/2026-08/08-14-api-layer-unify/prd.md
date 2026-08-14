# AdminWeb API 层收敛：统一走 utils/request 工具层

## Goal

消除 AdminWeb 双 HTTP 出口（`backend.ts` 自实现 fetch vs `utils/request` ky 封装）的架构歧义：**全仓业务 API 统一走 `utils/request`（ky）单出口**，行为一致（token 刷新重试/全局进度/超时/错误处理），为后续开发提供单一约定。

## 现状（勘察已确认）

- `src/api/backend.ts`：78 行自实现 fetch 封装（`requestBackendJson`），15 个业务模块使用；**缺 ky 版的刷新重试/全局进度/跳登录能力**；BASE_URL 硬编码 `http://localhost:8080/api`
- `src/utils/request/index.ts`：模板自带 ky 完整封装（beforeRequest hook 头注入/白名单/超时 10s；afterResponse 刷新重试/goLogin/globalProgress），仅 5 个文件直连（attachment/home/sla/import-export/notifications）
- `src/api/platform/*`（15 模块）全部 `import { requestBackendJson } from "#src/api/backend"`
- 双轨历史成因：react-antd-admin 模板 ky 层 + P0 期自写 fetch 封装并存，惯性扩散

## Requirements

- R1（单出口）`backend.ts` **彻底删除**；全仓业务请求统一经 `utils/request`（ky）统一请求处理层发出
- R2（能力并入统一层）`requestBackendJson(path, options)` 的能力（头注入、`silentError` 静默、HTTP 错误→`HttpRequestError(status, message, code)`、信封解包 `parseApiResponse`）**移入 `utils/request`** 并导出——15 个业务模块仅改 import 路径（`#src/api/backend` → `#src/utils/request`），调用方式不变
- R3（行为对齐）统一后获得：token 过期自动刷新重试、全局进度条、超时（ky 默认）——与 5 个直连文件行为一致
- R4（BASE_URL 环境化）`BACKEND_API_BASE_URL` 改为 `import.meta.env.VITE_API_BASE_URL`（缺省 `http://localhost:8080/api`）；URL 仅允许 http/https（Mimosa 约束）
- R5（测试）`backend.test.ts` 迁移/删除适配（能力并入后测试移到统一层）；`utils.test.ts` 更新；typecheck 全绿
- R6（React Query 约定，2026-08-14 Q2 决策）**固化约定不建封装**：页面服务端数据获取统一 `useQuery(queryKey, apiFn)` / 变更统一 `useMutation`（`pages/system/role` 为范本），不新建 `useApiQuery` 封装层；约定写入 `src/api/README.md`（新增「数据获取约定」小节）

## Acceptance Criteria

- [ ] AC1 `src/api/backend.ts` 已删除；全仓 grep 无 `#src/api/backend` import、无业务文件直接 `fetch(`/`ky(` 直连
- [ ] AC2 `utils/request` 导出 `requestBackendJson`（签名/语义与旧版一致：silentError/HttpRequestError/信封解包）；17 个业务模块仅 import 路径变更、无行为变化
- [ ] AC3 token 刷新重试生效（401 → refresh → 重放，ky 层机制验证或单测覆盖）
- [ ] AC4 BASE_URL 走 `VITE_API_BASE_URL="/api"` + vite proxy（dev）/反代（生产）；`backendRequest` 硬编码实例删除
- [ ] AC5 typecheck + 相关单测通过；关键页面冒烟（登录/域列表/工单队列）
- [ ] AC6 `src/api/README.md` 含「数据获取约定」（useQuery/useMutation + API 函数），无新增封装层

## Out of Scope

- shared 包（`packages/shared/src/api.ts`，axios 第三套）——跨端共享层，CustomerWeb 依赖，2026-08-14 Q3 决策**本轮不动**，双端统一独立立项
- `silentError` 语义调整；错误文案体系重构
- 业务模块按域重组（platform/ 目录改名等）
- `useApiQuery` 封装层（Q2 决策：约定层而非封装层）

## 参考证据

- `UnionDeskWeb/apps/UnionDeskAdminWeb/src/api/backend.ts`（:10 BASE_URL、:46 fetch、:29-78 requestBackendJson）
- `UnionDeskWeb/apps/UnionDeskAdminWeb/src/utils/request/index.ts`（ky 实例 + hooks）
- `src/api/utils.ts`（parseApiResponse 信封识别）
- 15 个业务模块 import 清单（grep `#src/api/backend`）
