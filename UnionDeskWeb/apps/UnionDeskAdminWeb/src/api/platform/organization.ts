import type { PlatformOrganizationView } from "@uniondesk/shared";

import { requestBackendJson } from "#src/api/backend";

export type PlatformOrganizationCreatePayload = {
	code: string;
	name: string;
	parentId?: number | null;
	leaderUserId?: number | null;
	orderNo?: number;
	status?: number;
	remark?: string | null;
};

export type PlatformOrganizationUpdatePayload = {
	code?: string;
	name?: string;
	parentId?: number | null;
	leaderUserId?: number | null;
	orderNo?: number;
	status?: number;
	remark?: string | null;
};

export function fetchPlatformOrganizations(): Promise<PlatformOrganizationView[]> {
	return requestBackendJson<PlatformOrganizationView[]>("v1/iam/organizations");
}

export function fetchCreatePlatformOrganization(data: PlatformOrganizationCreatePayload): Promise<PlatformOrganizationView> {
	return requestBackendJson<PlatformOrganizationView>("v1/iam/organizations", {
		method: "POST",
		json: data,
	});
}

export function fetchUpdatePlatformOrganization(id: number, data: PlatformOrganizationUpdatePayload): Promise<PlatformOrganizationView> {
	return requestBackendJson<PlatformOrganizationView>(`v1/iam/organizations/${id}`, {
		method: "PUT",
		json: data,
	});
}

export function fetchDeletePlatformOrganization(id: number): Promise<void> {
	return requestBackendJson<void>(`v1/iam/organizations/${id}`, {
		method: "DELETE",
	});
}
