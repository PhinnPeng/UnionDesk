import type { IamUser } from "@uniondesk/shared";

import { requestBackendJson } from "#src/api/backend";

export function fetchPlatformUsers(): Promise<IamUser[]> {
	return requestBackendJson<IamUser[]>("v1/iam/users");
}

export function fetchPlatformOffboardPoolUsers(): Promise<IamUser[]> {
	return requestBackendJson<IamUser[]>("v1/iam/users/offboard-pool");
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
