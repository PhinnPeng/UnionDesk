import type { PermissionSnapshotMenu } from "@uniondesk/shared";

import { describe, expect, it } from "vitest";

import { buildBackendRoutesFromSnapshot } from "#src/api/user/utils";
import { appScopes } from "#src/router/extra-info/app-scope";
import { generateUserMenus } from "./generate-user-menus";

describe("generateUserMenus", () => {
	it("renders platform catalogs with nested children matching admin menu tree", () => {
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
						name: "组织架构",
						path: "/platform/dept",
						component: "platform/dept",
						scope: "platform",
					},
					{
						id: 4,
						code: "MENU_OFFBOARD_POOL",
						parentId: 1,
						name: "离职池",
						path: "/platform/offboard-pool",
						component: "platform/offboard-pool",
						scope: "platform",
					},
					{
						id: 5,
						code: "MENU_ORG_CONFIG",
						parentId: 1,
						name: "组织配置",
						path: "/platform/org-config",
						component: "platform/org-config",
						scope: "platform",
					},
				],
			},
			{
				id: 6,
				code: "CAT_PERMISSION",
				parentId: null,
				name: "权限管理",
				path: null,
				component: null,
				scope: "platform",
				children: [
					{
						id: 7,
						code: "MENU_ROLE",
						parentId: 6,
						name: "角色管理",
						path: "/platform/role",
						component: "./system/role",
						scope: "platform",
					},
					{
						id: 8,
						code: "MENU_MENU",
						parentId: 6,
						name: "菜单管理",
						path: "/platform/menu",
						component: "./platform/system/menu",
						scope: "platform",
					},
				],
			},
		];

		const menus = generateUserMenus(buildBackendRoutesFromSnapshot(snapshotMenus), appScopes.platform);

		expect(menus).toHaveLength(2);
		expect(menus.map(menu => menu.label)).toEqual(["组织管理", "权限管理"]);

		const organizationChildren = menus[0]?.children?.map(child => child.key) ?? [];
		expect(organizationChildren).toEqual(expect.arrayContaining([
			"/platform/user",
			"/platform/dept",
			"/platform/offboard-pool",
			"/platform/org-config",
		]));
		expect(organizationChildren).toHaveLength(4);

		const permissionChildren = menus[1]?.children?.map(child => child.key) ?? [];
		expect(permissionChildren).toEqual(expect.arrayContaining([
			"/platform/role",
			"/platform/menu",
		]));
		expect(permissionChildren).toHaveLength(2);
	});
});
