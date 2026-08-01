import type { AppRouteRecordRaw } from "#src/router/types";

import { beforeEach, describe, expect, it, vi } from "vitest";

const routerMocks = vi.hoisted(() => ({
	patchRoutes: vi.fn(),
	_internalSetRoutes: vi.fn(),
	navigate: vi.fn(),
	routes: [{ path: "/", id: "root", children: [] as unknown[] }],
	state: {
		location: { pathname: "/home", search: "", hash: "", state: null },
	},
}));

vi.mock("#src/router", () => ({
	router: routerMocks,
	rootRoute: [{ path: "/", id: "root", children: [] }],
	createRootRouteConfig: () => [{ path: "/", id: "root", children: [] as unknown[] }],
}));

vi.mock("#src/router/routes", () => ({
	baseRoutes: [],
}));

vi.mock("#src/router/utils/generate-user-menus", () => ({
	generateUserMenus: (routes: AppRouteRecordRaw[]) => routes.map(route => ({ key: route.path ?? "" })),
}));

vi.mock("#src/store/auth", () => ({
	useAuthStore: Object.assign(
		vi.fn(),
		{
			getState: () => ({ user: { id: 1, actions: [], menus: [] }, setState: vi.fn() }),
			setState: vi.fn(),
		},
	),
}));

vi.mock("#src/store/user", () => ({
	useUserStore: Object.assign(
		vi.fn(),
		{
			getState: () => ({
				id: 1,
				actions: [] as string[],
				menus: [] as AppRouteRecordRaw[],
				setState: vi.fn(),
			}),
			setState: vi.fn(),
		},
	),
}));

import { appScopes } from "#src/router/extra-info/app-scope-core";
import { useAccessStore } from "./access";

describe("access store dual bucket", () => {
	beforeEach(() => {
		useAccessStore.getState().reset();
		routerMocks.patchRoutes.mockClear();
		routerMocks._internalSetRoutes.mockClear();
		routerMocks.navigate.mockClear();
		routerMocks.state.location = { pathname: "/home", search: "", hash: "", state: null };
	});

	it("forces replace navigate after _internalSetRoutes so RR commits HMR routes", () => {
		useAccessStore.getState().commitScopeBucket(appScopes.business, {
			userRoutes: [{ path: "/home", handle: { scope: "business", title: "概览" } }],
			dynamicRoutes: [{ path: "/home", handle: { scope: "business", title: "概览" } }],
			actions: ["domain.home.read"],
			domainId: 1,
		});

		expect(routerMocks._internalSetRoutes).toHaveBeenCalled();
		expect(routerMocks.navigate).toHaveBeenCalledWith(
			{ pathname: "/home", search: "", hash: "" },
			expect.objectContaining({
				replace: true,
				flushSync: true,
				state: expect.objectContaining({ __accessRemount: expect.any(Number) }),
			}),
		);
	});

	it("keeps platform bucket when committing business bucket", () => {
		const platformMenus: AppRouteRecordRaw[] = [
			{ path: "/platform/home", handle: { scope: "platform", title: "平台首页" } },
		];
		const businessMenus: AppRouteRecordRaw[] = [
			{ path: "/home", handle: { scope: "business", title: "概览" } },
		];

		useAccessStore.getState().commitScopeBucket(appScopes.platform, {
			userRoutes: platformMenus,
			dynamicRoutes: platformMenus,
			actions: ["platform.home.read"],
		});
		useAccessStore.getState().commitScopeBucket(appScopes.business, {
			userRoutes: businessMenus,
			dynamicRoutes: businessMenus,
			actions: ["domain.home.read"],
			domainId: 9,
		});

		const state = useAccessStore.getState();
		expect(state.platformBucket?.actions).toEqual(["platform.home.read"]);
		expect(state.businessBucket?.domainId).toBe(9);
		expect(state.activeScope).toBe(appScopes.business);
		expect(state.platformMenus.some(item => item.key === "/platform/home")).toBe(true);
		expect(state.userMenus.some(item => item.key === "/home")).toBe(true);
	});

	it("activates cached platform bucket without dropping business bucket", () => {
		useAccessStore.getState().commitScopeBucket(appScopes.business, {
			userRoutes: [{ path: "/home", handle: { scope: "business", title: "概览" } }],
			dynamicRoutes: [{ path: "/home", handle: { scope: "business", title: "概览" } }],
			actions: ["domain.home.read"],
			domainId: 1,
		});
		useAccessStore.getState().commitScopeBucket(appScopes.platform, {
			userRoutes: [{ path: "/platform/domains", handle: { scope: "platform", title: "业务域列表" } }],
			dynamicRoutes: [{ path: "/platform/domains", handle: { scope: "platform", title: "业务域列表" } }],
			actions: ["platform.domain.list.read"],
		});

		const activated = useAccessStore.getState().activateScope(appScopes.business);
		expect(activated?.activeScope).toBe(appScopes.business);
		expect(useAccessStore.getState().platformBucket).not.toBeNull();
		expect(useAccessStore.getState().businessBucket).not.toBeNull();
	});

	it("invalidateBusinessBucket keeps platform bucket", () => {
		useAccessStore.getState().commitScopeBucket(appScopes.platform, {
			userRoutes: [{ path: "/platform/home", handle: { scope: "platform", title: "平台首页" } }],
			dynamicRoutes: [{ path: "/platform/home", handle: { scope: "platform", title: "平台首页" } }],
			actions: ["platform.home.read"],
		});
		useAccessStore.getState().commitScopeBucket(appScopes.business, {
			userRoutes: [{ path: "/home", handle: { scope: "business", title: "概览" } }],
			dynamicRoutes: [{ path: "/home", handle: { scope: "business", title: "概览" } }],
			actions: ["domain.home.read"],
			domainId: 3,
		});

		useAccessStore.getState().invalidateBusinessBucket();
		const state = useAccessStore.getState();
		expect(state.businessBucket).toBeNull();
		expect(state.platformBucket).not.toBeNull();
	});
});
