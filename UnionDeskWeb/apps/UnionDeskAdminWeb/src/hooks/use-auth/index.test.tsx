import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => {
	return {
		currentRoute: {
			handle: {
				scope: "platform",
			},
		},
		userState: {
			actions: ["platform.menu.create"],
		},
	};
});

vi.mock("#src/hooks/use-current-route", () => ({
	useCurrentRoute: () => mocks.currentRoute,
}));

vi.mock("react-router", () => ({
	useLocation: () => ({
		pathname: "/platform/menu",
	}),
}));

vi.mock("#src/store/user", () => ({
	useUserStore: (selector: (state: typeof mocks.userState) => unknown) => selector(mocks.userState),
}));

import { useAuth } from "./index";

function Probe() {
	const { hasPermission, routeScope } = useAuth();

	return (
		<div>
			<span data-testid="scope">{routeScope}</span>
			<span data-testid="create">{String(hasPermission("platform.menu.create"))}</span>
			<span data-testid="domain">{String(hasPermission("domain.menu.create"))}</span>
			<span data-testid="fallback">{String(hasPermission(undefined))}</span>
		</div>
	);
}

describe("useAuth", () => {
	it("exposes route scope and permission checks from store actions", () => {
		render(<Probe />);

		expect(screen.getByTestId("scope").textContent).toBe("platform");
		expect(screen.getByTestId("create").textContent).toBe("true");
		expect(screen.getByTestId("domain").textContent).toBe("false");
		expect(screen.getByTestId("fallback").textContent).toBe("true");
	});
});
