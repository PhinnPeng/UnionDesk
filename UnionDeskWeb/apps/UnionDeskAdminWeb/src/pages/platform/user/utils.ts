import type { CreateIamUserPayload, IamUser, PlatformOrganizationView, UpdateIamUserPayload } from "@uniondesk/shared";

import type { RoleItemType } from "#src/api/system/role";

export type PlatformUserStatus = "active" | "disabled" | "offboard";

export interface PlatformUserRow {
	id: number;
	username: string;
	mobile: string;
	email: string;
	departmentLabels: string[];
	roleLabels: string[];
	status: PlatformUserStatus;
	lastLoginAt: string;
}

export type PlatformUserFormValues = {
	username: string;
	mobile: string;
	email?: string | null;
	password?: string;
	organizationId?: number | null;
	roleCodes?: string[];
	remark?: string | null;
};

export type PlatformUserSearchValues = {
	keyword?: string;
};

export const PLATFORM_USER_TOOLBAR_ACTIONS = [
	{
		key: "create",
		label: "新增用户",
		auth: "platform.user.create",
	},
	{
		key: "batchOffboard",
		label: "批量离职",
		auth: "platform.user.disable",
	},
	{
		key: "importExport",
		label: "导入导出",
		auth: "platform.user.import",
	},
] as const;

export const PLATFORM_USER_ROW_ACTIONS = [
	{
		key: "edit",
		label: "编辑",
		auth: "platform.user.update",
	},
	{
		key: "offboard",
		label: "离职",
		auth: "platform.user.disable",
	},
	{
		key: "resetPassword",
		label: "重置密码",
		auth: "platform.user.reset_password",
	},
] as const;

export function resolvePlatformUserStatus(user: IamUser): PlatformUserStatus {
	if (user.employmentStatus === "offboarded") {
		return "offboard";
	}
	return user.status === 1 ? "active" : "disabled";
}

export function buildDepartmentNameMap(departments: PlatformOrganizationView[]): Map<number, string> {
	return new Map(departments.map(department => [department.id, `${department.name} / ${department.code}`]));
}

export function buildRoleNameMap(roles: RoleItemType[]): Map<string, string> {
	return new Map(roles.map(role => [role.code, `${role.name} / ${role.code}`]));
}

export function toPlatformUserRow(
	user: IamUser,
	departmentNameMap: Map<number, string>,
	roleNameMap: Map<string, string>,
): PlatformUserRow {
	const organizationIds = user.organizationIds ?? [];
	const roleCodes = user.roleCodes ?? [];

	return {
		id: user.id,
		username: user.username,
		mobile: user.mobile || "-",
		email: user.email || "-",
		departmentLabels: organizationIds.length > 0
			? organizationIds.map(organizationId => departmentNameMap.get(organizationId) ?? `部门 #${organizationId}`)
			: ["全局"],
		roleLabels: roleCodes.length > 0
			? roleCodes.map(roleCode => roleNameMap.get(roleCode) ?? roleCode)
			: ["-"],
		status: resolvePlatformUserStatus(user),
		lastLoginAt: "-",
	};
}

export function filterPlatformUsers(users: IamUser[], search: PlatformUserSearchValues): IamUser[] {
	const keyword = search.keyword?.trim().toLowerCase() ?? "";
	if (!keyword) {
		return users;
	}

	return users.filter((user) => {
		const fields = [
			user.username,
			user.mobile,
			user.email ?? "",
			user.remark ?? "",
		];
		return fields.some(field => field.toLowerCase().includes(keyword));
	});
}

function normalizeFormValues(values: PlatformUserFormValues) {
	return {
		username: values.username.trim(),
		mobile: values.mobile.trim(),
		email: values.email?.trim() || null,
		password: values.password?.trim() || "",
		roleCodes: values.roleCodes ?? [],
		organizationId: values.organizationId ?? null,
		remark: values.remark?.trim() ?? "",
	};
}

export function buildCreatePlatformUserPayload(values: PlatformUserFormValues): CreateIamUserPayload {
	const normalized = normalizeFormValues(values);
	if (!normalized.password) {
		throw new Error("请填写登录密码");
	}
	return {
		username: normalized.username,
		mobile: normalized.mobile,
		email: normalized.email,
		remark: normalized.remark || null,
		password: normalized.password,
		accountType: "admin",
		roleCodes: normalized.roleCodes,
		businessDomainIds: [],
		organizationIds: normalized.organizationId == null ? [] : [normalized.organizationId],
	};
}

export function buildUpdatePlatformUserPayload(
	values: PlatformUserFormValues,
	accountType: CreateIamUserPayload["accountType"] = "admin",
): UpdateIamUserPayload {
	const normalized = normalizeFormValues(values);
	const payload: UpdateIamUserPayload = {
		username: normalized.username,
		mobile: normalized.mobile,
		email: normalized.email,
		remark: normalized.remark,
		accountType,
		roleCodes: normalized.roleCodes,
		businessDomainIds: [],
		organizationIds: normalized.organizationId == null ? [] : [normalized.organizationId],
	};
	return payload;
}

export function generateResetPassword(): string {
	const groups = [
		"0123456789",
		"abcdefghijklmnopqrstuvwxyz",
		"ABCDEFGHIJKLMNOPQRSTUVWXYZ",
		"!@#$%^&*",
	];
	const chars = groups.join("");
	const password = groups.map(group => group[Math.floor(Math.random() * group.length)]);

	while (password.length < 16) {
		password.push(chars[Math.floor(Math.random() * chars.length)]);
	}

	for (let index = password.length - 1; index > 0; index -= 1) {
		const swapIndex = Math.floor(Math.random() * (index + 1));
		[password[index], password[swapIndex]] = [password[swapIndex], password[index]];
	}

	return password.join("");
}
