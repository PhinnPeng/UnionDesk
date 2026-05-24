import type { PermissionSnapshotMenu } from "@uniondesk/shared";

import { describe, expect, it } from "vitest";

import { isAdminMenuCatalog, resolveCatalogRoutePath } from "./catalog-path";

describe("admin-menu-tree/catalog-path", () => {
	it("treats nodes without path and component as catalog", () => {
		const catalog: PermissionSnapshotMenu = {
			id: 70,
			code: "ADM0000000070",
			parentId: null,
			name: "组织管理",
			path: null,
			component: null,
			scope: "platform",
		};

		expect(isAdminMenuCatalog(catalog)).toBe(true);
	});

	it("does not treat leaf menu as catalog", () => {
		const menu: PermissionSnapshotMenu = {
			id: 2,
			code: "MENU_USER",
			parentId: 1,
			name: "用户管理",
			path: "/platform/user",
			component: "platform/user",
			scope: "platform",
		};

		expect(isAdminMenuCatalog(menu)).toBe(false);
	});

	it("generates stable catalog route path from menu id", () => {
		expect(resolveCatalogRoutePath({
			id: 70,
			code: "ADM0000000070",
			name: "组织管理",
			path: null,
			scope: "platform",
		})).toBe("/platform/catalog/70");
	});
});
