import type { CreateIamUserPayload, IamUser, UpdateIamUserPayload } from "@uniondesk/shared";

import { requestBackendJson } from "#src/utils/request";

type StaffAccountApi = {
	id: string
	username: string
	real_name?: string | null
	nickname?: string | null
	phone?: string | null
	mobile?: string | null
	email?: string | null
	status: number
	employmentStatus: string
	accountType?: string
	roleCodes?: string[]
	businessDomainIds?: string[]
	organizationIds?: string[]
	platformRoles?: string[]
	offboardedAt?: string | null
	offboardedBy?: string | null
	offboardReason?: string | null
};

type StaffPageResult = {
	total: number
	list: StaffAccountApi[]
};

function toIamUser(staff: StaffAccountApi): IamUser {
	const mobile = staff.mobile ?? staff.phone ?? "";
	return {
		id: staff.id,
		username: staff.username,
		mobile,
		email: staff.email ?? null,
		remark: staff.real_name ?? staff.nickname ?? null,
		accountType: staff.accountType ?? "admin",
		status: staff.status,
		employmentStatus: staff.employmentStatus,
		roleCodes: staff.roleCodes ?? staff.platformRoles ?? [],
		businessDomainIds: staff.businessDomainIds ?? [],
		organizationIds: staff.organizationIds ?? [],
		offboardedAt: staff.offboardedAt ?? null,
		offboardedBy: staff.offboardedBy ?? null,
		offboardReason: staff.offboardReason ?? null,
	};
}

function toCreateStaffBody(data: CreateIamUserPayload) {
	return {
		username: data.username,
		phone: data.mobile,
		email: data.email,
		password: data.password,
		accountType: data.accountType,
		roleCodes: data.roleCodes,
		businessDomainIds: data.businessDomainIds,
		organizationIds: data.organizationIds ?? [],
		real_name: data.remark ?? undefined,
	};
}

function toUpdateStaffBody(data: UpdateIamUserPayload) {
	return {
		username: data.username,
		phone: data.mobile,
		email: data.email,
		password: data.password,
		accountType: data.accountType,
		roleCodes: data.roleCodes,
		businessDomainIds: data.businessDomainIds,
		organizationIds: data.organizationIds,
		status: data.status,
		real_name: data.remark ?? undefined,
	};
}

export async function fetchPlatformUsers(organizationId?: string): Promise<IamUser[]> {
	const params = new URLSearchParams({
		page: "1",
		page_size: "1000",
	});
	if (organizationId != null) {
		params.set("organizationId", String(organizationId));
	}
	const page = await requestBackendJson<StaffPageResult>(`v1/admin/staff?${params.toString()}`);
	return (page.list ?? []).map(toIamUser);
}

export async function fetchPlatformOffboardPoolUsers(): Promise<IamUser[]> {
	const page = await requestBackendJson<StaffPageResult>("v1/admin/staff?status=offboarded&page=1&page_size=1000");
	return (page.list ?? []).map(toIamUser);
}

export async function fetchCreatePlatformUser(data: CreateIamUserPayload): Promise<IamUser> {
	const created = await requestBackendJson<StaffAccountApi>("v1/admin/staff", {
		method: "POST",
		json: toCreateStaffBody(data),
	});
	return toIamUser(created);
}

export async function fetchUpdatePlatformUser(id: string, data: UpdateIamUserPayload): Promise<IamUser> {
	const updated = await requestBackendJson<StaffAccountApi>(`v1/admin/staff/${id}`, {
		method: "PUT",
		json: toUpdateStaffBody(data),
	});
	return toIamUser(updated);
}

export async function fetchOffboardPlatformUser(id: string, reason?: string): Promise<IamUser> {
	const updated = await requestBackendJson<StaffAccountApi>(`v1/admin/staff/${id}/offboard`, {
		method: "POST",
		json: {
			reason,
		},
	});
	return toIamUser(updated);
}

export async function fetchRestorePlatformUser(id: string): Promise<IamUser> {
	const updated = await requestBackendJson<StaffAccountApi>(`v1/admin/staff/${id}/restore`, {
		method: "POST",
	});
	return toIamUser(updated);
}

/** 跨域批量停用单域结果（TR-04 部分成功） */
export interface DomainBatchFailure {
	domain_id: number
	reason: string
}

/** 跨域批量停用结果 */
export interface DomainBatchStatusResult {
	success: string[]
	failed: DomainBatchFailure[]
}

/** 跨域批量停用：`POST v1/admin/staff/{staffId}/domain-members/batch-status`（需 step-up 令牌） */
export async function batchDisableDomainMembers(
	staffId: string,
	domainIds: string[],
	stepUpToken: string,
): Promise<DomainBatchStatusResult> {
	return requestBackendJson<DomainBatchStatusResult>(`v1/admin/staff/${staffId}/domain-members/batch-status`, {
		method: "POST",
		json: {
			domain_ids: domainIds,
			status: "disabled",
		},
		headers: { "X-UD-Step-Up-Token": stepUpToken },
	});
}

export interface AdminPermissionCodeView {
	code: string
	name: string
	permissionScope?: "platform" | "domain" | "shared" | string
	httpMethod: string
	pathPattern: string
}

export async function fetchAdminPermissionCodes(scope?: string): Promise<AdminPermissionCodeView[]> {
	const query = scope ? `?scope=${encodeURIComponent(scope)}` : "";
	const result = await requestBackendJson<{ total: number; items: AdminPermissionCodeView[] }>(`v1/iam/admin-permission-codes${query}`);
	return (result?.items ?? []).filter((item): item is AdminPermissionCodeView => Boolean(item));
}
