import { renderHook } from "@testing-library/react";
import { describe, expect, it, beforeEach, vi } from "vitest";

import { useAccess } from "./index";

const useMatchesMock = vi.fn();
const useUserStoreMock = vi.fn();

vi.mock("react-router", () => ({
	useMatches: () => useMatchesMock(),
}));

vi.mock("#src/store/user", () => ({
	useUserStore: () => useUserStoreMock(),
}));

describe("useAccess", () => {
	beforeEach(() => {
		useMatchesMock.mockReset();
		useUserStoreMock.mockReset();
		useUserStoreMock.mockReturnValue({ roles: ["admin"] });
	});

	it("allows all permission codes on backstage routes", () => {
		useMatchesMock.mockReturnValue([
			{
				handle: {
					backstage: true,
				},
			},
		]);

		const { result } = renderHook(() => useAccess());

		expect(result.current.hasAccessByCodes("permission:button:add")).toBe(true);
		expect(result.current.hasAccessByCodes()).toBe(true);
	});

	it("keeps normal permission checks on frontend routes", () => {
		useMatchesMock.mockReturnValue([
			{
				handle: {
					permissions: ["permission:button:add"],
				},
			},
		]);

		const { result } = renderHook(() => useAccess());

		expect(result.current.hasAccessByCodes("permission:button:add")).toBe(true);
		expect(result.current.hasAccessByCodes("permission:button:delete")).toBe(false);
	});
});
