import type { PermissionSnapshotMenu } from "@uniondesk/shared";

import { describe, expect, it } from "vitest";

import { addRouteIdByPath } from "#src/router/utils/add-route-id-by-path";
import { buildBackendRoutesFromSnapshot } from "./utils";

describe("buildBackendRoutesFromSnapshot", () => {
	it("uses backend menu identity instead of route path as data router id", () => {
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
						id: 38,
						code: "MENU_ROLE",
						parentId: 1,
						name: "角色管理",
						path: "/platform/role",
						component: "./system/role",
						scope: "platform",
					},
				],
			},
			{
				id: 39,
				code: "MENU_ROLE_ALIAS",
				parentId: null,
				name: "角色管理别名",
				path: "/platform/role",
				component: "./system/role",
				scope: "platform",
			},
		];

		const routes = addRouteIdByPath(buildBackendRoutesFromSnapshot(snapshotMenus));
		const ids = [
			routes[0].id,
			routes[0].children?.[0].id,
			routes[1].id,
		];

		expect(ids).toEqual([
			"backend:platform:1",
			"backend:platform:38",
			"backend:platform:39",
		]);
		expect(new Set(ids).size).toBe(ids.length);
	});
});
