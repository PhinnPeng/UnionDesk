import type { PermissionSnapshot } from "@uniondesk/shared";

import { describe, expect, it } from "vitest";

import { buildBackendRoutesFromSnapshot, buildUserInfoFromPermissionSnapshot } from "./utils";

describe("user utils", () => {
	it("marks platform access and preserves actions from the permission snapshot", () => {
		const snapshot: PermissionSnapshot = {
			user: {
				id: 1,
				username: "admin",
				mobile: null,
				email: null,
			},
			clientCode: "ud-admin-web",
			roles: ["domain_admin"],
			domains: [],
			menus: [],
			actions: [
				{
					code: "platform.user.read",
					name: "平台用户查看",
				},
				{
					code: "platform.menu.create",
					name: "新增菜单",
				},
			],
			issuedAt: "2026-04-30T22:00:00",
		};

		const userInfo = buildUserInfoFromPermissionSnapshot(snapshot);

		expect(userInfo.platformAccess).toBe(true);
		expect(userInfo.actions).toEqual([
			"platform.user.read",
			"platform.menu.create",
		]);
	});

	it("preserves backend menu scope, component paths, and auth codes when building routes", () => {
		const routes = buildBackendRoutesFromSnapshot([
			{
				id: 1,
				code: "platform_permission",
				name: "权限管理",
				path: "/platform/permission",
				parentId: null,
				orderNo: 1,
				icon: "SafetyCertificateOutlined",
				component: "./platform/permission",
				scope: "platform",
				hidden: false,
				permissionCode: "platform.menu.read",
			},
			{
				id: 2,
				code: "platform_roles",
				name: "平台角色",
				path: "/system/roles",
				parentId: 1,
				orderNo: 2,
				icon: "TeamOutlined",
				component: "./system/roles",
				scope: "platform",
				hidden: false,
			},
			{
				id: 3,
				code: "platform_menus",
				name: "平台菜单",
				path: "/system/menus",
				parentId: 1,
				orderNo: 3,
				icon: "MenuOutlined",
				component: "./system/menus",
				scope: "platform",
				hidden: false,
			},
		]);

		expect(routes).toHaveLength(1);
		expect(routes[0]).toMatchObject({
			path: "/platform",
			handle: {
				scope: "platform",
				title: "权限管理",
				auth: "platform.menu.read",
			},
			component: "/src/pages/platform/permission/index.tsx",
			children: [
				{
					path: "/platform/role",
					handle: {
						scope: "platform",
						title: "平台角色",
					},
					component: "/src/pages/platform/role/index.tsx",
				},
				{
					path: "/platform/menu",
					handle: {
						scope: "platform",
						title: "平台菜单",
					},
					component: "/src/pages/platform/menu/index.tsx",
				},
			],
		});
	});
});
