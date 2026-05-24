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
						component: "./platform/system/menu",
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
		expect(matchRoutes(routes, "/platform/role")?.at(-1)?.route?.Component).toBeDefined();
	});

	it("keeps the organization catalog and its child routes aligned with the platform permission tree", async () => {
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
						permissionCode: "platform.user.read",
					},
					{
						id: 3,
						code: "MENU_DEPT",
						parentId: 1,
						name: "组织架构",
						path: "/platform/dept",
						component: "platform/dept",
						scope: "platform",
						permissionCode: "platform.organization.read",
					},
					{
						id: 4,
						code: "MENU_OFFBOARD_POOL",
						parentId: 1,
						name: "离职池",
						path: "/platform/offboard-pool",
						component: "platform/offboard-pool",
						scope: "platform",
						permissionCode: "platform.user.offboard_pool.read",
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

		const backendRoutes = buildBackendRoutesFromSnapshot(snapshotMenus);
		const routes = removeDuplicateRoutes(await generateRoutesFromBackend(backendRoutes));

		const organizationMatches = matchRoutes(routes, "/platform/dept") ?? [];
		const organizationRoute = organizationMatches.at(-1)?.route;
		const organizationCatalogRoute = organizationMatches.find(
			match => match.route.handle?.title === "组织管理",
		)?.route;

		expect(organizationCatalogRoute?.handle?.title).toBe("组织管理");
		expect(organizationMatches.length).toBeGreaterThanOrEqual(2);
		expect(matchRoutes(routes, "/platform/user")?.at(-1)?.route?.handle?.auth).toBe("platform.user.read");
		expect(organizationRoute?.handle?.auth).toBe("platform.organization.read");
		expect(matchRoutes(routes, "/platform/offboard-pool")?.at(-1)?.route?.handle?.auth).toBe("platform.user.offboard_pool.read");
		for (const pathname of ["/platform/user", "/platform/dept", "/platform/offboard-pool", "/platform/org-config", "/platform/role", "/platform/menu"]) {
			const matches = matchRoutes(routes, pathname) ?? [];
			const leafRoute = matches.at(-1)?.route;

			expect(matches.length, pathname).toBeGreaterThan(0);
			expect(leafRoute?.path ?? leafRoute?.index, pathname).not.toBe("*");
		}
	});

	it("registers backend permission menu with absolute child routes without route nesting errors", async () => {
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

		const backendRoutes = buildBackendRoutesFromSnapshot(snapshotMenus);
		const routes = removeDuplicateRoutes(await generateRoutesFromBackend(backendRoutes));

		expect(backendRoutes[0]?.path).toBeUndefined();
		expect(backendRoutes[0]?.handle?.menuKey).toBe("/platform/permission");

		for (const pathname of ["/platform/permission", "/platform/role", "/platform/menu"]) {
			const matches = matchRoutes(routes, pathname) ?? [];
			const leafRoute = matches.at(-1)?.route;

			expect(matches.length, pathname).toBeGreaterThan(0);
			expect(leafRoute?.path ?? leafRoute?.index, pathname).not.toBe("*");
		}
	});
});
