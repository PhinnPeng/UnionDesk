# Error Handling

> API 统一响应结构与异常抛出约定。

---

## Overview

### 统一响应结构

所有 REST API 接口必须返回统一信封结构 `ApiResponse<T>`，禁止直接返回原始数据或自定义 DTO。

```java
// uniondesk-common/.../ApiResponse.java
public record ApiResponse<T>(boolean success, String code, String message, T data) {
    public static <T> ApiResponse<T> ok(T data) { ... }
    public static <T> ApiResponse<T> ok(T data, String message) { ... }
    public static ApiResponse<Void> error(String code, String message) { ... }
}
```

**响应字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `success` | boolean | `true` 表示成功，`false` 表示失败 |
| `code` | String | `"0"` 表示成功，其他为错误码 |
| `message` | String | 成功时为 `"ok"`，失败时为中文错误描述 |
| `data` | T | 成功时返回业务数据，失败时为 `null` |

**成功响应示例**：
```json
{
  "success": true,
  "code": "0",
  "message": "ok",
  "data": { "id": "1", "name": "标题" }
}
```

**失败响应示例**：
```json
{
  "success": false,
  "code": "40001",
  "message": "参数校验失败",
  "data": null
}
```

### 全局处理

统一响应由两个 `@RestControllerAdvice` 组件协同工作：

1. **`ApiResponseWrapper`**（`ResponseBodyAdvice`）：自动将 Controller 返回的业务对象包装为 `ApiResponse.ok(data)`
   - Controller 方法直接返回业务对象 → 自动包装为 `ApiResponse.ok(data)`
   - 已是 `ApiResponse<T>` 的返回 → 原样返回，不二次包装
   - `void` 返回 / HTTP 204 → 跳过
   - `String` 返回 → 跳过

2. **`ApiExceptionHandler`**：异常自动捕获并包装为 `ApiResponse.error(code, message)`

---

## 错误码

| 来源 | 文件 | 范围 |
|------|------|------|
| 全局 | `uniondesk-common/.../ErrorCodes.java` | 10001 认证、40001 校验、50001 内部错误等 |
| 域业务 | `uniondesk-domain/.../DomainErrorCodes.java` | 41101+，带 `toException()` |

---

## 推荐抛出方式（按优先级）

### 1. 域业务异常（首选）

用于可预期的业务规则违反，消息稳定、可测试。

```java
// InvitationCodeService.java
throw DomainErrorCodes.INVITATION_DISALLOWED.toException();
// → code="41102", message="该业务域不支持邀请码入域"
```

### 2. IllegalArgumentException / IllegalStateException

用于参数/状态校验；**消息使用中文**，会尽量原样返回给客户端。

```java
// TicketTypeRepository.java
throw new IllegalArgumentException("事项类型不存在");
// → HTTP 400, message 保留
```

### 3. ResponseStatusException

用于需要特定 HTTP 状态码的场景（如 409 冲突）。

```java
// TicketAttributeService.java
throw new ResponseStatusException(HttpStatus.CONFLICT, "全局已存在同名属性");
```

> 注意：`ApiExceptionHandler` 对 `ResponseStatusException` 可能使用通用 message 映射，自定义文案不一定透传。优先用 `DomainErrorCodes` 或 `IllegalArgumentException` 若需稳定中文提示。

### 4. 认证专用异常

- `AuthenticationFailedException` → 401
- `AuthCaptchaException` → 400

定义在 `uniondesk-iam/.../auth/core/`。

---

## Handler 映射摘要

| 异常 | HTTP | 典型 code |
|------|------|-----------|
| `AuthenticationFailedException` | 401 | 10001 |
| `AuthCaptchaException` | 400 | 10003 |
| `DomainBusinessException` | 按枚举 | 411xx |
| `IllegalArgumentException` | 400 | 40002 |
| `MethodArgumentNotValidException` | 400 | 40001 |
| `DataAccessException` | 500 | 50001（记录 error 日志） |
| 未捕获 `Exception` | 500 | 50001（记录 error 日志） |

---

## Controller 层原则

### 返回类型规范

**必须遵循**：所有 Controller 方法的返回类型必须是 `ApiResponse<T>` 或可被自动包装为 `ApiResponse` 的业务对象。

#### ✅ 正确做法

```java
// 1. 直接返回单个对象，由 Handler 自动包装
@GetMapping("/ticket-types/{id}")
public PlatformTicketTypeDetailView getTicketType(@PathVariable long id) {
    return ticketTypeService.getDetail(id);  // 自动包装为 ApiResponse.ok(data)
}

// 2. 返回列表视图对象（包含 total/items）
@GetMapping("/ticket-attributes")
public TicketAttributeListView listAttributes(...) {
    return ticketAttributeService.list(...);  // 自动包装
}

// 3. 手动构造 ApiResponse（如需自定义 message）
@PostMapping("/ticket-types")
public ApiResponse<PlatformTicketTypeView> create(...) {
    var result = ticketTypeService.create(...);
    return ApiResponse.ok(result, "创建成功");
}
```

#### ❌ 错误做法

```java
// 错误：直接返回原始 List，缺少 total 字段
@GetMapping("/ticket-types/{id}/attribute-slots")
public List<AttributeSlotView> listSlots(@PathVariable long id) {
    return slotService.list(id);  // ❌ 不要直接返回 List
}

// 正确做法：返回包装对象
@GetMapping("/ticket-types/{id}/attribute-slots")
public AttributeSlotListView listSlots(@PathVariable long id) {
    List<AttributeSlotView> slots = slotService.list(id);
    return new AttributeSlotListView(slots.size(), slots);  // ✅ 包含 total
}
```

### 列表响应规范

列表查询必须返回包含 `total` 和 `items` 的结构，统一使用 `ApiResponse<XXXListView>`：

```java
// 列表视图 DTO 定义
public record TicketAttributeListView(long total, List<TicketAttributeView> items) {}

// Controller 返回类型
@GetMapping("/ticket-attributes")
public TicketAttributeListView listAttributes(...) {
    return ticketAttributeService.list(...);  // 自动包装为 ApiResponse.ok(data)
}
```

**列表响应示例**：
```json
{
  "success": true,
  "code": "0",
  "message": "ok",
  "data": {
    "total": 2,
    "items": [
      { "id": "1", "name": "标题" },
      { "id": "13", "name": "描述" }
    ]
  }
}
```

### 其他原则

- 参数校验：Spring `@Valid` + DTO；框架校验失败自动走 40001。
- 业务校验：在 **Service（core）** 或 **Repository** 抛出，不在 Controller 堆叠 if。
- 权限：用 `@RequirePermission(PermissionCodes.X)`，勿在方法内手写权限 if。
- 用户可见文案：**中文**，简短明确。

---

## 反模式

| 反模式 | 说明 | 正确做法 |
|--------|------|----------|
| ❌ 直接返回 `List<T>` | 列表接口直接返回数组，缺少 `total` 字段 | ✅ 返回 `ApiResponse<XXXListView>` |
| ❌ Controller 返回手写 `Map` | 使用 `Map<String, Object>` 构造响应 | ✅ 使用 `ApiResponse<T>` 或业务 DTO |
| ❌ 吞掉异常返回 `success=true` | 捕获异常后返回成功状态 | ✅ 抛出异常让 Handler 处理 |
| ❌ 英文错误消息面向管理端用户 | 错误消息使用英文 | ✅ 用户可见文案使用中文 |
| ❌ 在多处重复同一业务错误文案 | 相同错误文案分散在各处 | ✅ 收敛到 `DomainErrorCodes` |

### 常见错误示例

#### 错误：直接返回 List
```java
// ❌ 不要这样做
@GetMapping("/items")
public List<ItemView> listItems() {
    return service.findAll();  // 返回 [{...}, {...}]
}
```

#### 正确：返回包装对象
```java
// ✅ 应该这样做
@GetMapping("/items")
public ItemListView listItems() {
    List<ItemView> items = service.findAll();
    return new ItemListView(items.size(), items);  // 返回 {total: 2, items: [...]}
}

// 分页场景同样使用 ListView
@GetMapping("/items")
public ItemListView listItems(int page, int size) {
    return service.findPage(page, size);  // 返回 {total: 100, items: [...]}
}
```

---

## 参考文件

| 文件 | 说明 |
|------|------|
| `uniondesk-common/.../web/ApiResponse.java` | 统一响应结构定义 |
| `uniondesk-app/.../common/web/ApiResponseWrapper.java` | ResponseBodyAdvice 自动包装（成功响应） |
| `uniondesk-app/.../common/web/ApiExceptionHandler.java` | 全局异常处理与错误响应包装 |
| `uniondesk-common/.../ErrorCodes.java` | 全局错误码定义 |
| `uniondesk-domain/.../DomainErrorCodes.java` | 域业务错误码定义 |
| `uniondesk-ticket/.../web/TicketAttributeDtos.java` | ListView DTO 示例 |
| `uniondesk-ticket/.../core/TicketAttributeService.java` | 业务异常抛出示例 |
