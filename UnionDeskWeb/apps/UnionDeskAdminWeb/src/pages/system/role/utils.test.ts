import { describe, expect, it, vi } from "vitest";

vi.mock("#src/router/extra-info/app-scope", () => ({
	appScopes: {
		business: "business",
		platform: "platform",
	},
}));

import type { RoleItemType } from "#src/api/system/role";

import { filterAssignablePlatformRoles, filterRolesByAppScope } from "./utils";

const roles: RoleItemType[] = [
	{ id: 1, code: "platform_admin", name: "平台管理员", scope: "global", system: true },
	{ id: 2, code: "super_admin", name: "超级管理员", scope: "global", system: true },
	{ id: 3, code: "domain_admin", name: "业务域管理员", scope: "domain", system: true },
];

describe("filterAssignablePlatformRoles", () => {
	it("仅返回 global 角色且排除 super_admin", () => {
		expect(filterAssignablePlatformRoles(roles).map(role => role.code)).toEqual(["platform_admin"]);
	});
});

describe("filterRolesByAppScope", () => {
	it("platform scope 仅 global", () => {
		expect(filterRolesByAppScope(roles, "platform").map(role => role.code)).toEqual([
			"platform_admin",
			"super_admin",
		]);
	});

	it("business scope 仅 domain", () => {
		expect(filterRolesByAppScope(roles, "business").map(role => role.code)).toEqual(["domain_admin"]);
	});
});
