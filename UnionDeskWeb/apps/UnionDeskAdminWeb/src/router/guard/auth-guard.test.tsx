import type React from "react";

import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => {
	const authState = {
		token: "token",
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
		actions: [],
		menus: [] as typeof authState.user.menus,
		setUserInfo: vi.fn(),
		reset: vi.fn(),
	};
	const accessState = {
		isAccessChecked: false,
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
		navigate: vi.fn(),
		fetchUserInfoAndRoutes: vi.fn().mockResolvedValue({
			userInfo: authState.user,
			routes: [],
		}),
		generateRoutesFromBackend: vi.fn().mockResolvedValue([]),
		generateRoutesByFrontend: vi.fn().mockReturnValue([]),
		removeDuplicateRoutes: vi.fn((routes: unknown[]) => routes),
		goLogin: vi.fn(),
		hideLoading: vi.fn(),
		setupLoading: vi.fn(),
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
	fetchUserInfoAndRoutes: mocks.fetchUserInfoAndRoutes,
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
}));

vi.mock("#src/router/guard/utils", () => ({
	removeDuplicateRoutes: mocks.removeDuplicateRoutes,
}));

vi.mock("#src/router/utils/generate-routes-from-backend", () => ({
	generateRoutesFromBackend: mocks.generateRoutesFromBackend,
}));

vi.mock("#src/router/utils/generate-routes-from-frontend", () => ({
	generateRoutesByFrontend: mocks.generateRoutesByFrontend,
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
			search: "",
		}),
		useNavigate: () => mocks.navigate,
		matchRoutes: () => [],
	};
});

import { AuthGuard } from "./auth-guard";

describe("AuthGuard", () => {
	beforeEach(() => {
		mocks.pathname = "/";
		mocks.navigate.mockReset();
		mocks.fetchUserInfoAndRoutes.mockClear();
		mocks.generateRoutesFromBackend.mockClear();
		mocks.generateRoutesByFrontend.mockClear();
		mocks.removeDuplicateRoutes.mockClear();
		mocks.goLogin.mockClear();
		mocks.hideLoading.mockClear();
		mocks.setupLoading.mockClear();
		mocks.authState.reset.mockClear();
		mocks.userState.setUserInfo.mockClear();
		mocks.accessState.setAccessStore.mockClear();
	});

	it("redirects the root path to the cached home path before access data is ready", async () => {
		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("navigate")).toHaveTextContent("/platform/home");
		});
	});

	it("redirects logged-in users away from the login page", async () => {
		mocks.pathname = "/login";

		render(
			<AuthGuard>
				<div>child</div>
			</AuthGuard>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("navigate")).toHaveTextContent("/platform/home");
		});
	});

	it("clears stale session and redirects to login when permission snapshot returns 401", async () => {
		const { HttpRequestError } = await import("#src/utils/http-request-error");
		mocks.pathname = "/platform/home";
		mocks.accessState.isAccessChecked = false;
		mocks.userState.id = 0;
		mocks.fetchUserInfoAndRoutes.mockRejectedValueOnce(new HttpRequestError(401, "Unauthorized"));

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
