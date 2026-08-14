import type { PlatformOrganizationView } from "@uniondesk/shared";

import { requestBackendJson } from "#src/utils/request";

export type PlatformOrganizationCreatePayload = {
	code: string;
	name: string;
	parentId?: string | null;
	leaderUserId?: string | null;
	orderNo?: number;
	status?: number;
	remark?: string | null;
};

export type PlatformOrganizationUpdatePayload = {
	code?: string;
	name?: string;
	parentId?: string | null;
	leaderUserId?: string | null;
	orderNo?: number;
	status?: number;
	remark?: string | null;
};

type OrganizationListResult = {
	total: number;
	items: PlatformOrganizationView[];
};

export async function fetchPlatformOrganizations(): Promise<PlatformOrganizationView[]> {
	const result = await requestBackendJson<OrganizationListResult>("v1/iam/organizations");
	return (result?.items ?? []).filter((item): item is PlatformOrganizationView => Boolean(item));
}

export function fetchCreatePlatformOrganization(data: PlatformOrganizationCreatePayload): Promise<PlatformOrganizationView> {
	return requestBackendJson<PlatformOrganizationView>("v1/iam/organizations", {
		method: "POST",
		json: data,
	});
}

export function fetchUpdatePlatformOrganization(id: string, data: PlatformOrganizationUpdatePayload): Promise<PlatformOrganizationView> {
	return requestBackendJson<PlatformOrganizationView>(`v1/iam/organizations/${id}`, {
		method: "PUT",
		json: data,
	});
}

export function fetchDeletePlatformOrganization(id: string): Promise<void> {
	return requestBackendJson<void>(`v1/iam/organizations/${id}`, {
		method: "DELETE",
	});
}
