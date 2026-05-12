import type { PermissionSnapshotMenu } from "@uniondesk/shared";

import { describe, expect, it } from "vitest";

import { buildBackendRoutesFromSnapshot } from "#src/api/user/utils";
import { appScopes } from "#src/router/extra-info/app-scope";
import { generateUserMenus } from "./generate-user-menus";

describe("generateUserMenus", () => {
	it("does not render duplicate platform catalog keys when backend catalogs share a normalized path", () => {
		const snapshotMenus: PermissionSnapshotMenu[] = [
			{
				id: 1,
				code: "CAT_ORG",
				parentId: null,
				name: "组织管理",
				path: null,
				component: null,
				scope: "platform",
				children: [
					{
						id: 2,
						code: "MENU_USER",
						parentId: 1,
						name: "用户管理",
						path: "/platform/user",
						component: "platform/user",
						scope: "platform",
					},
					{
						id: 3,
						code: "MENU_DEPT",
						parentId: 1,
						name: "部门管理",
						path: "/platform/dept",
						component: "platform/dept",
						scope: "platform",
					},
				],
			},
			{
				id: 4,
				code: "CAT_PERMISSION",
				parentId: null,
				name: "权限管理",
				path: null,
				component: null,
				scope: "platform",
				children: [
					{
						id: 5,
						code: "MENU_ROLE",
						parentId: 4,
						name: "角色管理",
						path: "/platform/role",
						component: "./system/role",
						scope: "platform",
					},
					{
						id: 6,
						code: "MENU_MENU",
						parentId: 4,
						name: "菜单管理",
						path: "/platform/menu",
						component: "./system/menu",
						scope: "platform",
					},
				],
			},
		];

		const menus = generateUserMenus(buildBackendRoutesFromSnapshot(snapshotMenus), appScopes.platform);
		const topLevelKeys = menus.map(menu => menu.key);

		expect(new Set(topLevelKeys).size).toBe(topLevelKeys.length);
		expect(JSON.stringify(menus)).toContain("/platform/role");
		expect(JSON.stringify(menus)).toContain("/platform/menu");
	});
});
