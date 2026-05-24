import type { PermissionSnapshotMenu } from "@uniondesk/shared";

import { describe, expect, it } from "vitest";

import { buildRoutesFromAdminMenuSnapshot } from "./build-routes-from-snapshot";

const identityPath = (path: string | null | undefined) => (path?.trim().replace(/\/+$/, "") ?? "");
const identityComponent = (component: string | null | undefined, path: string) =>
	component ?? `/src/pages${path}/index.tsx`;

describe("admin-menu-tree/build-routes-from-snapshot", () => {
	it("preserves separate platform catalog roots instead of merging under /platform", () => {
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
				],
			},
			{
				id: 3,
				code: "CAT_PERMISSION",
				parentId: null,
				name: "权限管理",
				path: null,
				component: null,
				scope: "platform",
				children: [
					{
						id: 4,
						code: "MENU_ROLE",
						parentId: 3,
						name: "角色管理",
						path: "/platform/role",
						component: "./system/role",
						scope: "platform",
					},
				],
			},
		];

		const routes = buildRoutesFromAdminMenuSnapshot(snapshotMenus, {
			normalizeMenuPath: identityPath,
			normalizeComponentPath: identityComponent,
		});

		expect(routes).toHaveLength(2);
		expect(routes.map(route => route.handle?.menuKey)).toEqual([
			"/platform/catalog/1",
			"/platform/catalog/3",
		]);
		expect(routes[0]?.children?.map(child => child.path)).toEqual(["/platform/user"]);
		expect(routes[1]?.children?.map(child => child.path)).toEqual(["/platform/role"]);
	});

	it("uses pathless layout for permission menu with sibling absolute child routes", () => {
		const snapshotMenus: PermissionSnapshotMenu[] = [
			{
				id: 48,
				code: "ADM0000000048",
				parentId: null,
				name: "权限管理",
				path: "/platform/permission",
				component: "./platform/permission",
				scope: "platform",
				children: [
					{
						id: 7,
						code: "MENU_ROLE",
						parentId: 48,
						name: "角色管理",
						path: "/platform/role",
						component: "./system/role",
						scope: "platform",
					},
					{
						id: 8,
						code: "MENU_MENU",
						parentId: 48,
						name: "菜单管理",
						path: "/platform/menu",
						component: "./platform/system/menu",
						scope: "platform",
					},
				],
			},
		];

		const routes = buildRoutesFromAdminMenuSnapshot(snapshotMenus, {
			normalizeMenuPath: identityPath,
			normalizeComponentPath: identityComponent,
		});

		expect(routes).toHaveLength(1);
		expect(routes[0]?.path).toBeUndefined();
		expect(routes[0]?.handle?.menuKey).toBe("/platform/permission");
		expect(routes[0]?.children?.map(child => child.path)).toEqual([
			"/platform/permission",
			"/platform/menu",
			"/platform/role",
		]);
	});
});
