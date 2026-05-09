import { describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => {
	return {
		fetchPermissionSnapshot: vi.fn(),
	};
});

vi.mock("#src/api/auth", () => ({
	fetchPermissionSnapshot: mocks.fetchPermissionSnapshot,
}));

import { fetchAsyncRoutes, fetchUserInfo, fetchUserInfoAndRoutes } from "./index";

describe("user api", () => {
	it("loads permission snapshot only once for user info and backend routes", async () => {
		mocks.fetchPermissionSnapshot.mockResolvedValue({
			user: {
				id: 1,
				username: "admin",
				mobile: null,
				email: null,
			},
			clientCode: "ud-admin-web",
			roles: ["super_admin"],
			domains: [],
			menus: [],
			actions: [],
			issuedAt: "2026-05-08T00:00:00Z",
		});

		const result = await fetchUserInfoAndRoutes();

		expect(mocks.fetchPermissionSnapshot).toHaveBeenCalledTimes(1);
		expect(result.userInfo.username).toBe("admin");
		expect(result.routes).toEqual([]);
	});

	it("reuses the short cache for separate user info and route loads", async () => {
		mocks.fetchPermissionSnapshot.mockResolvedValue({
			user: {
				id: 1,
				username: "admin",
				mobile: null,
				email: null,
			},
			clientCode: "ud-admin-web",
			roles: ["super_admin"],
			domains: [],
			menus: [],
			actions: [],
			issuedAt: "2026-05-08T00:00:00Z",
		});

		const userInfo = await fetchUserInfo();
		const routes = await fetchAsyncRoutes();

		expect(mocks.fetchPermissionSnapshot).toHaveBeenCalledTimes(1);
		expect(userInfo.username).toBe("admin");
		expect(routes).toEqual([]);
	});
});
