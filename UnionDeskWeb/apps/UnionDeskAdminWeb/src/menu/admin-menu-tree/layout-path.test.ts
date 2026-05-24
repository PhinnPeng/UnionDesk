import type { PermissionSnapshotMenu } from "@uniondesk/shared";

import { describe, expect, it } from "vitest";

import {
	isDescendantRoutePath,
	shouldUsePathlessLayout,
} from "./layout-path";
import type { AdminMenuRouteNode } from "./types";

function createNode(
	menu: PermissionSnapshotMenu,
	path: string,
	children: AdminMenuRouteNode[] = [],
): AdminMenuRouteNode {
	return { menu, scope: menu.scope, path, children };
}

describe("admin-menu-tree/layout-path", () => {
	it("detects non-descendant child paths for permission menu parent", () => {
		const permissionNode = createNode(
			{
				id: 48,
				code: "ADM0000000048",
				name: "权限管理",
				path: "/platform/permission",
				component: "./platform/permission",
				scope: "platform",
			},
			"/platform/permission",
			[
				createNode(
					{
						id: 7,
						code: "MENU_ROLE",
						parentId: 48,
						name: "角色管理",
						path: "/platform/role",
						component: "./system/role",
						scope: "platform",
					},
					"/platform/role",
				),
			],
		);

		expect(isDescendantRoutePath("/platform/role", "/platform/permission")).toBe(false);
		expect(shouldUsePathlessLayout(permissionNode)).toBe(true);
	});

	it("keeps descendant child paths on normal nested menu", () => {
		const parentNode = createNode(
			{
				id: 1,
				code: "CAT_DOMAIN",
				name: "业务域",
				path: "/platform/domains",
				component: "./platform/domains",
				scope: "platform",
			},
			"/platform/domains",
			[
				createNode(
					{
						id: 2,
						code: "MENU_DOMAIN_DETAIL",
						parentId: 1,
						name: "业务域详情",
						path: "/platform/domains/detail",
						component: "./platform/domains/detail",
						scope: "platform",
					},
					"/platform/domains/detail",
				),
			],
		);

		expect(isDescendantRoutePath("/platform/domains/detail", "/platform/domains")).toBe(true);
		expect(shouldUsePathlessLayout(parentNode)).toBe(false);
	});
});
