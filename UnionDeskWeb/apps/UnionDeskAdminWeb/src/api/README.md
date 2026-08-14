## api 目录介绍

> api 目录存放所有请求接口文件，按照页面划分目录，一个页面对应一个目录，目录可以嵌套，但目录下的文件需要包含请求接口文件和类型定义文件。

下面是一个典型的目录结构 [`src/api/user`](https://github.com/condorheroblog/react-antd-admin/tree/main/src/api/user)：

```zsh
├── api
│   └── user                  # 用户页面, 按照页面划分 api
│       ├── index.ts          # 请求接口文件
│       └── types.ts          # 类型定义文件
```

如果页面下有页面，则可以继续嵌套目录，例如：[`src/api/system`](https://github.com/condorheroblog/react-antd-admin/tree/main/src/api/system)。

## 文件说明

### 类型定义文件

类型变量名一般以对应的页面名作为开始，以 `Type` 结尾，例如：

```ts
export interface RoleItemType {
	id: number
	createTime: number
	updateTime: number
	name: string
	code: string
	status: 1 | 0
	remark: string
}
```

### 请求接口文件

一个经典的请求接口文件如下所示：

> 请求充分利用了 HTTP 方法，request.get、request.post 等，忽略加载动画通过 `ignoreLoading` 参数实现。

特别注意：

1. GET 请求的参数放在 `searchParams` 对象中，POST、PUT 等请求的参数放在 `json` 对象中。
2. 请求的路径不能以 `/` 开头。

```ts
import type { RoleItemType } from "./types";
import { request } from "#src/utils/request";

export * from "./types";

/* 获取角色列表 */
export function fetchRoleList(data: any) {
	return request.get<ApiListResponse<RoleItemType>>("role-list", { searchParams: data, ignoreLoading: true }).json();
}

/* 新增角色 */
export function fetchAddRoleItem(data: RoleItemType) {
	return request.post<ApiResponse<string>>("role-item", { json: data, ignoreLoading: true }).json();
}

/* 修改角色 */
export function fetchUpdateRoleItem(data: RoleItemType) {
	return request.put<ApiResponse<string>>("role-item", { json: data, ignoreLoading: true }).json();
}

/* 删除角色 */
export function fetchDeleteRoleItem(id: number) {
	return request.delete<ApiResponse<string>>("role-item", { json: id, ignoreLoading: true }).json();
}
```

## `request.ts` 介绍

`request.ts` 是封装了 `[Ky](https://github.com/sindresorhus/ky)` 的请求库，代码实现请看 `[src/utils/request](https://github.com/condorheroblog/react-antd-admin/tree/main/src/utils/request)`。

## 数据获取约定（2026-08-14 统一请求处理层）

> 全仓 HTTP 请求统一经 `src/utils/request`（ky 单实例，相对 `/api` + vite proxy/反代）。历史 `src/api/backend.ts`（自实现 fetch）已并入并删除。

### 请求出口

- **统一函数**：`requestBackendJson(path, options)`（`#src/utils/request` 导出）——HTTP 错误抛 `HttpRequestError(status, message, code)`，成功响应自动信封解包（`data`/`result`）
- **选项**：`{ method, json, headers, silentError }`——`silentError: true` 时不弹全局错误提示，由调用方展示友好文案（如轮询/静默刷新）
- **自动行为**（ky 层）：token/client-code/lang 头注入、401 自动刷新重试、超时 10s、全局进度条
- **禁止**：业务文件直接 `fetch(`/自建 ky/axios 实例；新 API 一律走 `requestBackendJson` 或 `request` 实例

### 数据获取（React Query 约定层，不建封装）

- 页面服务端数据获取统一 `useQuery(queryKey, apiFn)`；变更统一 `useMutation`（范本：`src/pages/system/role/index.tsx`）
- API 函数保持「typed promise」形态（platform/*.ts 模式），页面层决定用 useQuery 或直接调用
- `queryKey` 建议 `[模块, 参数]` 数组形式（如 `["domains", { page }]`）
