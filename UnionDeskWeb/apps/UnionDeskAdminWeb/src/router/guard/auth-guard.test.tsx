import type React from "react";

import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => {
	const authState = {
		token: "token",
		role: "super_admin",
		user: {
			id: 2,
			menus: [
				{
					path: "/platform/home",
					handle: { scope: "platform" },
				},
			],
			platformAccess: true,
			businessDomainAccess: false,
		},
		reset: vi.fn(),
	};
	const userState = {
		id: 2,
		roles: ["admin"],
		actions: [] as string[],
		menus: [] as typeof authState.user.menus,
		platformAccess: true,
		businessDomainAccess: false,
		setUserInfo: vi.fn(),
		reset: vi.fn(),
	};
	const accessState = {
		isAccessChecked: false,
		activeScope: null as string | null,
		routeList: [],
		setAccessStore: vi.fn(),
		reset: vi.fn(),
	};
	const preferencesState = {
		enableBackendAccess: true,
		enableFrontendAceess: false,
	};

	return {
		authState,
		userState,
		accessState,
		preferencesState,
		pathname: "/",
		search: "",
		navigate: vi.fn(),
		bootstrapAccessForPath: vi.fn().mockResolvedValue({}),
		ensureScopeAccess: vi.fn().mockResolvedValue({}),
		goLogin: vi.fn(),
		hideLoading: vi.fn(),
		setupLoading: vi.fn(),
		showLoading: vi.fn(),
	};
});

function createStoreMock<TState extends object>(state: TState) {
	return Object.assign(
		vi.fn((selector?: (currentState: TState) => unknown) => {
			if (typeof selector === "function") {
				return selector(state);
			}
			return state;
		}),
		{
			getState: () => state,
		},
	);
}

vi.mock("#src/api/user", () => ({
	bootstrapAccessForPath: mocks.bootstrapAccessForPath,
	ensureScopeAccess: mocks.ensureScopeAccess,
}));

vi.mock("#src/hooks/use-current-route", () => ({
	useCurrentRoute: () => ({
		handle: {},
	}),
}));

vi.mock("#src/plugins/hide-loading", () => ({
	hideLoading: mocks.hideLoading,
}));

vi.mock("#src/plugins/loading", () => ({
	setupLoading: mocks.setupLoading,
	showLoading: mocks.showLoading,
}));

vi.mock("#src/store/access", () => ({
	useAccessStore: createStoreMock(mocks.accessState),
}));

vi.mock("#src/store/auth", () => ({
	useAuthStore: createStoreMock(mocks.authState),
}));

vi.mock("#src/store/preferences", () => ({
	usePreferencesStore: createStoreMock(mocks.preferencesState),
}));

vi.mock("#src/store/user", () => ({
	useUserStore: createStoreMock(mocks.userState),
}));

vi.mock("#src/utils/request/go-login", () => ({
	goLogin: mocks.goLogin,
}));

vi.mock("react-router", async () => {
	const actual = await vi.importActual<typeof import("react-router")>("react-router");
	return {
		...actual,
		Navigate: ({ to }: { to: string }) => <div data-testid="navigate">{to}</div>,
		useLocation: () => ({
			pathname: mocks.pathname,
			search: mocks.search,
		}),
		useNavigate: () => mocks.navigate,
		matchRoutes: () => [],
	};
});

import { AuthGuard } from "./auth-guard";

describe("AuthGuard", () => {
	beforeEach(() => {
		mocks.pathname = "/";
		mocks.search = "";
		mocks.accessState.isAccessChecked = false;
		mocks.accessState.activeScope = null;
		mocks.userState.id = 2;
		mocks.userState.actions = [];
		mocks.userState.businessDomainAccess = false;
		mocks.authState.user.menus = [
			{
				path: "/platform/home",
				handle: { scope: "platform" },
			},
		];
		mocks.userState.platformAccess = true;
		mocks.navigate.mockReset();
		mocks.bootstrapAccessForPath.mockClear();
		mocks.bootstrapAccessForPath.mockResolvedValue({});
		mocks.ensureScopeAccess.mockClear();
		mocks.ensureScopeAccess.mockResolvedValue({});
		mocks.goLogin.mockClear();
		mocks.hideLoading.mockClear();
		mocks.setupLoading.mockClear();
		mocks.showLoading.mockClear();
		mocks.authState.reset.mockClear();
		mocks.userState.setUserInfo.mockClear();
		mocks.accessState.setAccessStore.mockClear();
	});

	it("redirects the root path to platform home when only platform access", async () => {
		mocks.accessState.isAccessChecked = true;
		mocks.accessState.activeScope = "business";
		mocks.userState.id = 2;
		mocks.userState.platformAccess = true;
		mocks.userState.businessDomainAccess = false;

		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(mocks.navigate).toHaveBeenCalledWith(
				"/platform/home",
				expect.objectContaining({ replace: true }),
			);
		});
	});

	it("redirects root to platform home when platformAccess and no domain membership", async () => {
		mocks.accessState.isAccessChecked = true;
		mocks.accessState.activeScope = "business";
		mocks.userState.id = 2;
		mocks.userState.platformAccess = true;
		mocks.userState.businessDomainAccess = false;
		mocks.userState.actions = ["platform.menu.read"];

		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(mocks.navigate).toHaveBeenCalledWith(
				"/platform/home",
				expect.objectContaining({ replace: true }),
			);
		});
	});

	it("waits for access data before redirecting root path", () => {
		mocks.accessState.isAccessChecked = false;

		const { container } = render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		expect(container.firstChild).toBeNull();
		expect(screen.queryByTestId("navigate")).toBeNull();
	});

	it("redirects root to business home when dual access prefers business", async () => {
		mocks.accessState.isAccessChecked = true;
		mocks.accessState.activeScope = "business";
		mocks.userState.id = 2;
		mocks.authState.user.menus = [
			{ path: "/platform/home", handle: { scope: "platform", title: "平台首页" } } as (typeof mocks.authState.user.menus)[number],
			{ path: "/system/menu", handle: { scope: "business", title: "菜单管理" } } as (typeof mocks.authState.user.menus)[number],
		];
		mocks.userState.platformAccess = true;
		mocks.userState.businessDomainAccess = true;
		mocks.userState.actions = ["platform.menu.read", "domain.menu.read"];

		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(mocks.navigate).toHaveBeenCalledWith(
				"/home",
				expect.objectContaining({ replace: true }),
			);
		});
	});

	it("after bootstrap on root, navigates to home instead of rematching /", async () => {
		mocks.accessState.isAccessChecked = false;
		mocks.userState.businessDomainAccess = true;
		mocks.userState.platformAccess = true;
		mocks.userState.actions = ["domain.menu.read"];
		mocks.authState.user.menus = [
			{ path: "/home", handle: { scope: "business", title: "概览" } } as (typeof mocks.authState.user.menus)[number],
		];
		mocks.bootstrapAccessForPath.mockImplementation(async () => {
			mocks.accessState.isAccessChecked = true;
			mocks.accessState.activeScope = "business";
			mocks.userState.id = 2;
			mocks.userState.menus = mocks.authState.user.menus;
		});

		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(mocks.bootstrapAccessForPath).toHaveBeenCalledWith("/");
			expect(mocks.navigate).toHaveBeenCalledWith(
				"/home",
				expect.objectContaining({ replace: true, flushSync: true }),
			);
		});
		expect(mocks.navigate).not.toHaveBeenCalledWith("/", expect.anything());
	});

	it("redirects logged-in users away from the login page", async () => {
		mocks.pathname = "/login";
		mocks.userState.actions = ["platform.menu.read"];
		mocks.userState.platformAccess = true;
		mocks.userState.businessDomainAccess = false;

		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("navigate")).toHaveTextContent("/platform/home");
		});
	});

	it("honors safe redirect query when logged-in user is on the login page", async () => {
		mocks.pathname = "/login";
		mocks.search = `?redirect=${encodeURIComponent("/platform/ticket-config?section=types")}`;
		mocks.userState.actions = ["platform.menu.read"];

		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("navigate")).toHaveTextContent("/platform/ticket-config?section=types");
		});
	});

	it("encodes current path into login redirect when unauthenticated", async () => {
		mocks.authState.token = "";
		mocks.pathname = "/platform/ticket-config";
		mocks.search = "?section=types";

		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("navigate")).toHaveTextContent(
				`/login?redirect=${encodeURIComponent("/platform/ticket-config?section=types")}`,
			);
		});

		mocks.authState.token = "token";
	});

	it("clears stale session and redirects to login when permission snapshot returns 401", async () => {
		const { HttpRequestError } = await import("#src/utils/http-request-error");
		mocks.pathname = "/platform/home";
		mocks.accessState.isAccessChecked = false;
		mocks.userState.id = 0;
		mocks.bootstrapAccessForPath.mockRejectedValueOnce(new HttpRequestError(401, "Unauthorized"));

		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(mocks.goLogin).toHaveBeenCalledTimes(1);
		});
		expect(mocks.accessState.setAccessStore).not.toHaveBeenCalled();
		expect(mocks.navigate).not.toHaveBeenCalled();
	});
});
