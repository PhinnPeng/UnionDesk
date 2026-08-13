import type { IamUser, PlatformOrganizationView } from "@uniondesk/shared";

import { describe, expect, it } from "vitest";

import {
	PLATFORM_USER_ROW_ACTIONS,
	PLATFORM_USER_TOOLBAR_ACTIONS,
	buildCreatePlatformUserPayload,
	buildDepartmentNameMap,
	buildRoleNameMap,
	buildUpdatePlatformUserPayload,
	filterPlatformUsers,
	generateResetPassword,
	resolvePlatformUserStatus,
	toPlatformUserRow,
} from "./utils";

const users: IamUser[] = [
	{
		id: 1,
		username: "alice",
		mobile: "13800000000",
		email: "alice@example.com",
		accountType: "admin",
		status: 1,
		employmentStatus: "active",
		roleCodes: ["super_admin"],
		businessDomainIds: [1],
		organizationIds: [10],
	},
	{
		id: 2,
		username: "bob",
		mobile: "13900000000",
		email: "bob@example.com",
		accountType: "admin",
		status: 1,
		employmentStatus: "active",
		roleCodes: [],
		businessDomainIds: [],
		organizationIds: [],
	},
];

describe("platform user utils", () => {
	it("keeps the action order and permission codes", () => {
		expect(PLATFORM_USER_TOOLBAR_ACTIONS.map(item => item.label)).toEqual(["新增用户", "批量离职", "导入导出"]);
		expect(PLATFORM_USER_ROW_ACTIONS.map(item => item.label)).toEqual(["编辑", "离职", "重置密码"]);
		expect(PLATFORM_USER_TOOLBAR_ACTIONS.map(item => item.auth)).toEqual(["platform.user.create", "platform.user.disable", "platform.user.import"]);
		expect(PLATFORM_USER_ROW_ACTIONS.map(item => item.auth)).toEqual(["platform.user.update", "platform.user.disable", "platform.user.reset_password"]);
	});

	it("maps user rows with department and role labels", () => {
		const departments: PlatformOrganizationView[] = [
			{ id: 10, code: "sales", name: "销售", parentId: null, parentName: null, leaderUserId: null, leaderName: null, orderNo: 1, status: 1, remark: null, createdAt: "2026-05-01T00:00:00" },
			{ id: 11, code: "ops", name: "运营", parentId: 10, parentName: "销售", leaderUserId: null, leaderName: null, orderNo: 2, status: 1, remark: null, createdAt: "2026-05-02T00:00:00" },
		];
		const roles = [
			{ id: 7, code: "super_admin", name: "超级管理员", scope: "global", system: true },
		];

		const row = toPlatformUserRow(
			users[0],
			buildDepartmentNameMap(departments),
			buildRoleNameMap(roles),
		);

		expect(row.departmentLabels).toEqual(["销售"]);
		expect(row.roleLabels).toEqual(["超级管理员"]);
		expect(row.status).toBe("active");
		expect(resolvePlatformUserStatus(users[0])).toBe("active");

		const globalRow = toPlatformUserRow(
			users[1],
			buildDepartmentNameMap(departments),
			buildRoleNameMap(roles),
		);

		expect(globalRow.departmentLabels).toEqual(["所有部门"]);
		expect(globalRow.roleLabels).toEqual(["-"]);
	});

	it("filters users by username, account, mobile and email", () => {
		expect(filterPlatformUsers(users, { keyword: "ali" }).map(user => user.id)).toEqual([1]);
		expect(filterPlatformUsers(users, { keyword: "139" }).map(user => user.id)).toEqual([2]);
		expect(filterPlatformUsers(users, { keyword: "example.com" }).map(user => user.id)).toEqual([1, 2]);
		expect(filterPlatformUsers(users, { keyword: "" })).toEqual(users);
	});

	it("normalizes form values for create and update payloads", () => {
		expect(buildCreatePlatformUserPayload({
			username: " alice ",
			mobile: " 13800000000 ",
			email: " alice@example.com ",
			password: " 12345678 ",
			roleCodes: ["super_admin"],
			organizationId: 1,
			remark: " 新用户备注 ",
		})).toEqual({
			username: "alice",
			mobile: "13800000000",
			email: "alice@example.com",
			remark: "新用户备注",
			password: "12345678",
			accountType: "admin",
			roleCodes: ["super_admin"],
			businessDomainIds: [],
			organizationIds: [1],
		});

		expect(buildUpdatePlatformUserPayload({
			username: " alice ",
			mobile: " 13800000000 ",
			email: " alice@example.com ",
			password: "   ",
			roleCodes: ["super_admin"],
			organizationId: 1,
			remark: " 备注 ",
		}, "customer")).toEqual({
			username: "alice",
			mobile: "13800000000",
			email: "alice@example.com",
			remark: "备注",
			accountType: "customer",
			roleCodes: ["super_admin"],
			businessDomainIds: [],
			organizationIds: [1],
		});
	});

	it("generates 16-character password with required character groups", () => {
		const password = generateResetPassword();
		expect(password).toHaveLength(16);
		expect(password).toMatch(/[0-9]/);
		expect(password).toMatch(/[a-z]/);
		expect(password).toMatch(/[A-Z]/);
		expect(password).toMatch(/[!@#$%^&*]/);
	});
});
