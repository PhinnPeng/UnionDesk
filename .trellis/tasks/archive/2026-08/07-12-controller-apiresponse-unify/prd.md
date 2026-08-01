# Controller层统一返回ApiResponse优化

## Goal

排查所有 Controller 层，将未采用 `ApiResponse<T>` 统一返回结构的接口统一为 `ApiResponse` 信封格式，确保前端收到的所有响应均为 `{ success, code, message, data }` 结构。

## Background

项目规范（`.trellis/spec/backend/error-handling.md`）明确要求：所有 REST API 必须返回 `ApiResponse<T>` 统一信封。规范中描述了"Controller 直接返回业务对象，Handler 自动包装为 `ApiResponse.ok(data)`"的机制，但实际代码中：

1. **不存在自动包装机制**：`ApiExceptionHandler` 仅处理异常，无 `ResponseBodyAdvice` 做成功响应包装。
2. **绝大多数 Controller 返回裸 DTO / `List<T>` / `Map<K,V>` / `void`**，未包裹 `ApiResponse`。
3. 仅 `DashboardController`（demo）和 `ConsultationController`（demo）显式返回 `ApiResponse`。

## Requirements

### R1: 所有非 void Controller 方法返回 `ApiResponse<T>`
- 成功响应必须为 `{ success: true, code: "0", message: "ok", data: T }` 格式。
- 通过实现 `ResponseBodyAdvice` 自动包装，Controller 保持返回业务对象。

### R2: `void` 方法保持原样
- `@ResponseStatus(HttpStatus.NO_CONTENT)` 的 delete/reorder 等操作返回 HTTP 204，无需包装。
- `ResponseBodyAdvice` 对 `void` 返回和 204 状态码跳过包装。

### R3: 禁止返回裸 `List<T>`
- 列表接口必须返回 `ListView`（含 `total` + `items`）或 `PageResult<T>`（含 `total` + `list`）。
- 当前返回 `List<T>` 的方法需改为包装结构。

### R4: 禁止返回裸 `Map<K,V>`
- `InboxController` 和 `HealthController` 返回 `Map`，需改为 DTO record。

### R5: `Object` 返回类型需改为具体 DTO
- `IamController.listMenusTree()` 返回 `Object`（可能是 `List` 或 `Map`），需统一为 DTO。

### R6: 不破坏现有前端契约
- 前端已按 `ApiResponse` 结构解析响应（`DashboardController`/`ConsultationController` 可验证）。
- 对前端而言，此次变更使其从"有时包裹有时不包裹"变为"始终包裹"，属正向修复。

## Acceptance Criteria

- [ ] 新增 `ResponseBodyAdvice` 实现，自动包装非 `ApiResponse` / 非 `void` / 非 204 的返回值
- [ ] 所有 `List<T>` 返回改为 `ListView` 或 `PageResult<T>`
- [ ] 所有 `Map<K,V>` 返回改为 DTO record
- [ ] `IamController.listMenusTree()` 返回类型从 `Object` 改为具体 DTO
- [ ] `HealthController` 的 `Map` 返回改为 DTO record（或排除在包装范围外）
- [ ] `mvnw test` 全量通过
- [ ] `.trellis/spec/backend/error-handling.md` 规范与实现一致（修正自动包装描述）

## Constraints

- 不修改业务逻辑，仅调整返回类型和包装方式。
- 不删除 `PageResult<T>` record（已有 Controller 在用，与新 `ListView` 并存）。
- `HealthController` 为健康检查端点，可考虑排除在统一包装外（K8s 探针可能期望原始格式）。

## Out of Scope

- 前端代码适配（前端已按 `ApiResponse` 结构解析）。
- `ApiExceptionHandler` 异常处理逻辑调整。
- 新增错误码。
