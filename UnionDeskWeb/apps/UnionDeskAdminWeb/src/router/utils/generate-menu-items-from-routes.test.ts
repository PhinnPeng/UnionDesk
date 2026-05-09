import type { AppRouteRecordRaw } from "#src/router/types";

import { describe, expect, it } from "vitest";

import { appScopes } from "#src/router/extra-info/app-scope";
import { generateMenuItemsFromRoutes } from "./generate-menu-items-from-routes";

function createRoute(route: Partial<AppRouteRecordRaw>) {
	return route as AppRouteRecordRaw;
}

describe("generateMenuItemsFromRoutes", () => {
	const routeList = [
		createRoute({
			path: "/system",
			handle: {
				title: "系统管理",
				scope: appScopes.business,
			},
			children: [
				createRoute({
					path: "/system/user",
					handle: {
						title: "用户管理",
						scope: appScopes.business,
					},
				}),
			],
		}),
		createRoute({
			path: "/platform",
			handle: {
				title: "平台管理",
				hideInMenu: true,
				scope: appScopes.platform,
			},
			children: [
				createRoute({
					path: "/platform/home",
					handle: {
						title: "平台首页",
						scope: appScopes.platform,
					},
				}),
				createRoute({
					path: "/platform/user",
					handle: {
						title: "平台用户",
						scope: appScopes.platform,
					},
				}),
			],
		}),
	] satisfies AppRouteRecordRaw[];

	it("keeps business menus in the business scope", () => {
		const businessMenus = generateMenuItemsFromRoutes(routeList, appScopes.business);

		expect(businessMenus).toEqual([
			{
				key: "/system",
				label: "系统管理",
				children: [
					{
						key: "/system/user",
						label: "用户管理",
					},
				],
			},
		]);
	});

	it("bubbles visible platform children into the platform scope", () => {
		const platformMenus = generateMenuItemsFromRoutes(routeList, appScopes.platform);

		expect(platformMenus).toEqual([
			{
				key: "/platform/home",
				label: "平台首页",
			},
			{
				key: "/platform/user",
				label: "平台用户",
			},
		]);
	});

	it("prefers explicit scope over the route path prefix", () => {
		const platformMenus = generateMenuItemsFromRoutes([
			createRoute({
				path: "/system/legacy-platform",
				handle: {
					title: "平台入口",
					scope: appScopes.platform,
				},
			}),
		], appScopes.platform);

		expect(platformMenus).toEqual([
			{
				key: "/system/legacy-platform",
				label: "平台入口",
			},
		]);
	});

	it("groups platform role and menu under permission management", () => {
		const platformMenus = generateMenuItemsFromRoutes([
			createRoute({
				path: "/platform",
				handle: {
					title: "平台管理",
					hideInMenu: true,
					scope: appScopes.platform,
				},
				children: [
					createRoute({
						path: "/platform/role",
						handle: {
							title: "common.menu.role",
							scope: appScopes.platform,
						},
					}),
					createRoute({
						path: "/platform/menu",
						handle: {
							title: "common.menu.menu",
							scope: appScopes.platform,
						},
					}),
				],
			}),
		], appScopes.platform);

		expect(platformMenus).toHaveLength(2);
		expect(platformMenus).toEqual([
			expect.objectContaining({
				key: "/platform/role",
				label: "common.menu.role",
			}),
			expect.objectContaining({
				key: "/platform/menu",
				label: "common.menu.menu",
			}),
		]);
	});
});
