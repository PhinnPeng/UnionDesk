import type { AppRouteRecordRaw } from "#src/router/types";
import type { PermissionSnapshotMenu } from "@uniondesk/shared";

import { matchRoutes } from "react-router";
import { describe, expect, it } from "vitest";

import { buildBackendRoutesFromSnapshot } from "#src/api/user/utils";
import { removeDuplicateRoutes } from "#src/router/guard/utils";
import { generateRoutesFromBackend } from "./generate-routes-from-backend";

describe("generateRoutesFromBackend", () => {
	it("registers platform role and menu pages returned under the permission catalog", async () => {
		const snapshotMenus: PermissionSnapshotMenu[] = [
			{
				id: 1,
				code: "CAT_PERMISSION",
				parentId: null,
				name: "权限管理",
				path: null,
				component: null,
				scope: "platform",
				children: [
					{
						id: 2,
						code: "MENU_ROLE",
						parentId: 1,
						name: "角色管理",
						path: "/platform/role",
						component: "./system/role",
						scope: "platform",
					},
					{
						id: 3,
						code: "MENU_MENU",
						parentId: 1,
						name: "菜单管理",
						path: "/platform/menu",
						component: "./system/menu",
						scope: "platform",
					},
				],
			},
		];

		const backendRoutes: AppRouteRecordRaw[] = buildBackendRoutesFromSnapshot(snapshotMenus);
		const routes = removeDuplicateRoutes(await generateRoutesFromBackend(backendRoutes));

		for (const pathname of ["/platform/role", "/platform/menu"]) {
			const matches = matchRoutes(routes, pathname) ?? [];
			const leafRoute = matches.at(-1)?.route;

			expect(matches.length, pathname).toBeGreaterThan(0);
			expect(leafRoute?.path ?? leafRoute?.index, pathname).not.toBe("*");
		}
	});

	it("keeps children from multiple platform catalogs normalized to the same route path", async () => {
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
						id: 6,
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
					{
						id: 5,
						code: "MENU_MENU",
						parentId: 3,
						name: "菜单管理",
						path: "/platform/menu",
						component: "./system/menu",
						scope: "platform",
					},
				],
			},
		];

		const backendRoutes = buildBackendRoutesFromSnapshot(snapshotMenus);
		const routes = removeDuplicateRoutes(await generateRoutesFromBackend(backendRoutes));

		for (const pathname of ["/platform/user", "/platform/role", "/platform/menu"]) {
			const matches = matchRoutes(routes, pathname) ?? [];
			const leafRoute = matches.at(-1)?.route;

			expect(matches.length, pathname).toBeGreaterThan(0);
			expect(leafRoute?.path ?? leafRoute?.index, pathname).not.toBe("*");
		}
	});
});
