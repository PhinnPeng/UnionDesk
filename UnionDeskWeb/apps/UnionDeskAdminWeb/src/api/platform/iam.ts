import type { CreateIamUserPayload, IamUser, UpdateIamUserPayload } from "@uniondesk/shared";

import { requestBackendJson } from "#src/api/backend";

export function fetchPlatformUsers(organizationId?: number): Promise<IamUser[]> {
	const query = organizationId != null ? `?organizationId=${organizationId}` : "";
	return requestBackendJson<IamUser[]>(`v1/iam/users${query}`);
}

export function fetchPlatformOffboardPoolUsers(): Promise<IamUser[]> {
	return requestBackendJson<IamUser[]>("v1/iam/users/offboard-pool");
}

export function fetchCreatePlatformUser(data: CreateIamUserPayload): Promise<IamUser> {
	return requestBackendJson<IamUser>("v1/iam/users", {
		method: "POST",
		json: data,
	});
}

export function fetchUpdatePlatformUser(id: number, data: UpdateIamUserPayload): Promise<IamUser> {
	return requestBackendJson<IamUser>(`v1/iam/users/${id}`, {
		method: "PUT",
		json: data,
	});
}

export function fetchOffboardPlatformUser(id: number, reason?: string): Promise<IamUser> {
	return requestBackendJson<IamUser>(`v1/iam/users/${id}/offboard`, {
		method: "POST",
		json: {
			reason,
		},
	});
}

export function fetchRestorePlatformUser(id: number): Promise<IamUser> {
	return requestBackendJson<IamUser>(`v1/iam/users/${id}/restore`, {
		method: "POST",
	});
}

export interface AdminPermissionCodeView {
	code: string
	name: string
	permissionScope?: "platform" | "domain" | "shared" | string
	httpMethod: string
	pathPattern: string
}

export function fetchAdminPermissionCodes(scope?: string): Promise<AdminPermissionCodeView[]> {
	const query = scope ? `?scope=${encodeURIComponent(scope)}` : "";
	return requestBackendJson<AdminPermissionCodeView[]>(`v1/iam/admin-permission-codes${query}`);
}
