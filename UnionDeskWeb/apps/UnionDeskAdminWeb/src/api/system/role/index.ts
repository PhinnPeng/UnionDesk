import type { RoleItemType, RolePayload, RolePermissions } from "./types";

import { requestBackendJson } from "#src/utils/request";

export * from "./types";

/* 获取角色列表 */
export async function fetchRoleList(): Promise<RoleItemType[]> {
	const result = await requestBackendJson<{ total: number; items: RoleItemType[] }>("v1/iam/roles");
	return (result?.items ?? []).filter((item): item is RoleItemType => Boolean(item));
}

/* 新增角色 */
export function fetchAddRole(data: RolePayload): Promise<RoleItemType> {
	return requestBackendJson<RoleItemType>("v1/iam/roles", {
		method: "POST",
		json: data,
	});
}

/* 修改角色 */
export function fetchUpdateRole(id: number, data: RolePayload): Promise<RoleItemType> {
	return requestBackendJson<RoleItemType>(`v1/iam/roles/${id}`, {
		method: "PUT",
		json: data,
	});
}

/* 删除角色 */
export function fetchDeleteRole(id: number): Promise<void> {
	return requestBackendJson<void>(`v1/iam/roles/${id}`, {
		method: "DELETE",
	});
}

/* 获取角色权限（菜单+按钮 ID） */
export function fetchRolePermissions(roleId: number): Promise<RolePermissions> {
	return requestBackendJson<RolePermissions>(`v1/iam/roles/${roleId}/permissions`);
}

/* 更新角色权限 */
export function fetchUpdateRolePermissions(
	roleId: number,
	data: { menuIds: number[]; buttonIds: number[] },
): Promise<RolePermissions> {
	return requestBackendJson<RolePermissions>(`v1/iam/roles/${roleId}/permissions`, {
		method: "PUT",
		json: data,
	});
}
