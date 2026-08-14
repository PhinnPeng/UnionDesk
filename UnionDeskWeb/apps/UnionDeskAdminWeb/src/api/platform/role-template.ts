import { requestBackendJson } from "#src/utils/request";

/** 角色模板视图 */
export interface RoleTemplateItem {
	id: number
	code: string
	name: string
	description: string | null
	sync_strategy: "immediate" | "manual" | "none"
	locked_fields: string[]
	preset: boolean
	version: number
	created_by: number | null
	created_at: string
	updated_at: string
	applied_domain_count: number
}

/** 模板已下发域 */
export interface RoleTemplateAppliedDomain {
	domain_id: number
	instance_domain_role_id: number
	sync_mode: string
	instance_version: number | null
	applied_at: string
}

/** 模板详情 */
export interface RoleTemplateDetail {
	template: RoleTemplateItem
	applied_domains: RoleTemplateAppliedDomain[]
	permission_items: RoleTemplatePermissionItem[]
}

/** 权限目录项（permission_item） */
export interface RoleTemplatePermissionItem {
	id: number
	code: string
	name: string
	module: string
	type: string
}

/** 逐域结果 */
export interface RoleTemplateDomainResult {
	domain_id: number
	reason: string
}

/** 批量操作结果（部分成功） */
export interface RoleTemplateBatchResult {
	success: number[]
	skipped: RoleTemplateDomainResult[]
	failed: RoleTemplateDomainResult[]
}

/** 创建模板请求体 */
export interface CreateRoleTemplatePayload {
	code: string
	name: string
	description?: string | null
	locked_fields?: string[]
	sync_strategy?: "immediate" | "manual" | "none"
	permission_item_ids: number[]
}

/** 更新模板请求体 */
export interface UpdateRoleTemplatePayload {
	name?: string
	description?: string | null
	locked_fields?: string[]
	sync_strategy?: "immediate" | "manual" | "none"
	permission_item_ids?: number[]
}

/* 获取模板列表 */
export async function fetchRoleTemplateList(): Promise<RoleTemplateItem[]> {
	const result = await requestBackendJson<{ total: number; items: RoleTemplateItem[] }>("v1/iam/role-templates");
	return (result?.items ?? []).filter((item): item is RoleTemplateItem => Boolean(item));
}

/* 获取模板详情（含已下发域） */
export function fetchRoleTemplateDetail(templateId: number): Promise<RoleTemplateDetail> {
	return requestBackendJson<RoleTemplateDetail>(`v1/iam/role-templates/${templateId}`);
}

/* 获取权限目录（permission_item） */
export async function fetchRoleTemplatePermissionItems(): Promise<RoleTemplatePermissionItem[]> {
	const result = await requestBackendJson<{ total: number; items: RoleTemplatePermissionItem[] }>(
		"v1/iam/role-templates/permission-items",
	);
	return (result?.items ?? []).filter((item): item is RoleTemplatePermissionItem => Boolean(item));
}

/* 新增模板 */
export function createRoleTemplate(data: CreateRoleTemplatePayload): Promise<RoleTemplateItem> {
	return requestBackendJson<RoleTemplateItem>("v1/iam/role-templates", {
		method: "POST",
		json: data,
	});
}

/* 更新模板 */
export function updateRoleTemplate(templateId: number, data: UpdateRoleTemplatePayload): Promise<RoleTemplateItem> {
	return requestBackendJson<RoleTemplateItem>(`v1/iam/role-templates/${templateId}`, {
		method: "PUT",
		json: data,
	});
}

/* 删除模板 */
export function deleteRoleTemplate(templateId: number): Promise<void> {
	return requestBackendJson<void>(`v1/iam/role-templates/${templateId}`, {
		method: "DELETE",
	});
}

/* 下发模板到指定业务域 */
export function applyRoleTemplate(
	templateId: number,
	data: { domain_ids: number[]; sync_mode?: string },
): Promise<RoleTemplateBatchResult> {
	return requestBackendJson<RoleTemplateBatchResult>(`v1/iam/role-templates/${templateId}/apply`, {
		method: "POST",
		json: data,
	});
}

/* 手动同步模板到已下发实例 */
export function syncRoleTemplate(templateId: number, data?: { domain_ids?: number[] }): Promise<RoleTemplateBatchResult> {
	return requestBackendJson<RoleTemplateBatchResult>(`v1/iam/role-templates/${templateId}/sync`, {
		method: "POST",
		json: data ?? {},
	});
}

/* 解绑模板（实例转独立角色） */
export function unapplyRoleTemplate(
	templateId: number,
	data: { domain_ids: number[] },
): Promise<RoleTemplateBatchResult> {
	return requestBackendJson<RoleTemplateBatchResult>(`v1/iam/role-templates/${templateId}/unapply`, {
		method: "POST",
		json: data,
	});
}

/* 绑定成员到模板实例角色 */
export function bindMembersToRoleTemplate(
	templateId: number,
	data: { staff_ids: number[]; domain_ids: number[] },
): Promise<RoleTemplateBatchResult> {
	return requestBackendJson<RoleTemplateBatchResult>(`v1/iam/role-templates/${templateId}/bind-members`, {
		method: "POST",
		json: data,
	});
}
