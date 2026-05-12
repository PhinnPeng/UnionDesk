import type React from "react";

import { render, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	detailProps: undefined as undefined | { refreshTable?: () => unknown },
	reload: vi.fn(),
}));

vi.mock("react-i18next", () => ({
	initReactI18next: { type: "3rdParty", init: vi.fn() },
	useTranslation: () => ({
		t: (key: string) => key,
	}),
}));

vi.mock("@tanstack/react-query", () => ({
	useQuery: () => ({ data: [] }),
}));

vi.mock("#src/hooks/use-auth", () => ({
	useAuth: () => ({ routeScope: "platform" }),
}));

vi.mock("#src/api/system/menu", () => ({
	fetchMenuTree: vi.fn(),
}));

vi.mock("#src/api/system/role", () => ({
	fetchDeleteRole: vi.fn(),
	fetchRoleList: vi.fn().mockResolvedValue([]),
	fetchRolePermissions: vi.fn(),
}));

vi.mock("#src/components/auth-guarded", () => ({
	AuthGuarded: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock("#src/components/basic-button", () => ({
	BasicButton: ({ children }: { children: React.ReactNode }) => <button type="button">{children}</button>,
}));

vi.mock("#src/components/basic-content", () => ({
	BasicContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock("#src/components/basic-table", () => ({
	BasicTable: ({ actionRef }: { actionRef?: React.MutableRefObject<{ reload?: () => Promise<void> } | null> }) => {
		if (actionRef) {
			actionRef.current = { reload: mocks.reload };
		}
		return <div data-testid="role-table" />;
	},
}));

vi.mock("#src/components/confirm-popover", () => ({
	ConfirmPopover: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock("./components/detail", () => ({
	Detail: (props: { refreshTable?: () => unknown }) => {
		mocks.detailProps = props;
		return <div data-testid="role-detail" />;
	},
}));

import Role from "./index";

describe("Role page refresh callback", () => {
	beforeEach(() => {
		mocks.detailProps = undefined;
		mocks.reload.mockReset();
	});

	it("returns the table reload promise to the detail drawer", async () => {
		const reloadPromise = Promise.resolve();
		mocks.reload.mockReturnValue(reloadPromise);

		render(<Role />);

		await waitFor(() => {
			expect(mocks.detailProps?.refreshTable).toBeTypeOf("function");
		});

		const result = mocks.detailProps!.refreshTable!();

		expect(mocks.reload).toHaveBeenCalledTimes(1);
		expect(result).toBe(reloadPromise);
	});
});
