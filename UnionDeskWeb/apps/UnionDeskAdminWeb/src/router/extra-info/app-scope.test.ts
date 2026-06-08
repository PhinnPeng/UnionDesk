import type { AppRouteRecordRaw } from "#src/router/types";

import { beforeEach, describe, expect, it, vi } from "vitest";

import { resolveHomePathFromMenus } from "./resolve-home-path";

const platformOnlyMenus: AppRouteRecordRaw[] = [
	{
		path: "/platform/home",
		handle: { scope: "platform", title: "平台首页" },
	},
];

const mixedScopeMenus: AppRouteRecordRaw[] = [
	{
		path: "/platform/home",
		handle: { scope: "platform", title: "平台首页" },
	},
	{
		path: "/system/menu",
		handle: { scope: "business", title: "菜单管理" },
	},
];

const businessOnlyMenus: AppRouteRecordRaw[] = [
	{
		path: "/system/menu",
		handle: { scope: "business", title: "菜单管理" },
	},
];

describe("resolveHomePathFromMenus", () => {
	beforeEach(() => {
		vi.stubEnv("VITE_BASE_HOME_PATH", "/system/menu");
	});

	it("platformAccess 为 true 时，即使存在 business 顶层菜单也进入平台首页", () => {
		expect(resolveHomePathFromMenus(mixedScopeMenus, true)).toBe("/platform/home");
	});

	it("platformAccess 为 false 但存在 platform 顶层菜单时仍进入平台首页", () => {
		expect(resolveHomePathFromMenus(platformOnlyMenus, false)).toBe("/platform/home");
	});

	it("platformAccess 为 false 且仅有 business 菜单时进入业务域首页", () => {
		expect(resolveHomePathFromMenus(businessOnlyMenus, false)).toBe("/system/menu");
	});

	it("platformAccess 为 false 但菜单含 platform 与 business 时仍进入平台首页", () => {
		expect(resolveHomePathFromMenus(mixedScopeMenus, false)).toBe("/platform/home");
	});

	it("platformAccess 为 true 且无菜单时仍进入平台首页", () => {
		expect(resolveHomePathFromMenus([], true)).toBe("/platform/home");
	});

	it("无 platformAccess 且无菜单时进入业务域默认首页", () => {
		expect(resolveHomePathFromMenus([], false)).toBe("/system/menu");
	});

	it("快照未就绪但 loginRole 为 super_admin 时仍进入平台首页", () => {
		expect(resolveHomePathFromMenus([], false, { loginRole: "super_admin" })).toBe("/platform/home");
	});

	it("嵌套 platform 子菜单时识别为平台首页", () => {
		const nestedPlatformMenus: AppRouteRecordRaw[] = [
			{
				path: "/platform/catalog/1",
				handle: { title: "权限管理" },
				children: [
					{
						path: "/platform/home",
						handle: { scope: "platform", title: "平台首页" },
					},
				],
			},
		];
		expect(resolveHomePathFromMenus(nestedPlatformMenus, false)).toBe("/platform/home");
	});
});
