import { act, fireEvent, render, screen } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => {
	return {
		basicTableProps: undefined as unknown as Record<string, any>,
		fetchMenuTree: vi.fn(),
		fetchDeleteMenu: vi.fn(),
		routeScope: "platform",
	};
});

vi.mock("#src/api/system/menu", () => ({
	fetchDeleteMenu: (...args: unknown[]) => mocks.fetchDeleteMenu(...args),
	fetchMenuTree: (...args: unknown[]) => mocks.fetchMenuTree(...args),
}));

vi.mock("#src/components/auth-guarded", () => ({
	AuthGuarded: ({ children }: { children?: ReactNode }) => <>{children}</>,
}));

vi.mock("#src/components/basic-button", () => ({
	BasicButton: ({ children, ...props }: { children?: ReactNode }) => <button {...props}>{children}</button>,
}));

vi.mock("#src/components/basic-content", () => ({
	BasicContent: ({ children, className }: { children?: ReactNode, className?: string }) => <div className={className}>{children}</div>,
}));

vi.mock("#src/components/basic-table", () => ({
	BasicTable: (props: Record<string, any>) => {
		mocks.basicTableProps = props;
		return <div data-testid="basic-table" />;
	},
}));

vi.mock("#src/hooks/use-auth", () => ({
	useAuth: () => ({
		routeScope: mocks.routeScope,
		hasPermission: vi.fn(() => true),
	}),
}));

vi.mock("react-i18next", () => ({
	initReactI18next: { type: "3rdParty", init: vi.fn() },
	useTranslation: () => ({
		t: (key: string) => key,
	}),
}));

vi.mock("./components/detail", () => ({
	Detail: () => <div data-testid="detail" />,
}));

import Menu from "./index";

describe("menu page", () => {
	beforeEach(() => {
		mocks.basicTableProps = undefined as unknown as Record<string, any>;
		mocks.fetchMenuTree.mockReset();
		mocks.fetchDeleteMenu.mockReset();
		mocks.routeScope = "platform";
		mocks.fetchMenuTree.mockResolvedValue([
			{
				id: 1,
				name: "platform-root",
				nodeType: "catalog",
				scope: "platform",
				orderNo: 1,
				hidden: false,
				status: 1,
				required: false,
				code: "platform-root",
				routePath: "/platform",
				children: [
					{
						id: 2,
						name: "platform-menu",
						nodeType: "menu",
						scope: "platform",
						orderNo: 1,
						hidden: false,
						status: 1,
						required: false,
						code: "platform-menu",
						routePath: "/platform/menu",
						children: [],
					},
				],
			},
			{
				id: 3,
				name: "business-root",
				nodeType: "catalog",
				scope: "business",
				orderNo: 1,
				hidden: false,
				status: 1,
				required: false,
				code: "business-root",
				routePath: "/system",
				children: [],
			},
		]);
	});

	it("defaults to platform scope and filters tree data", async () => {
		render(<Menu />);

		expect(screen.getByText("system.menu.platformScope")).toBeInTheDocument();
		expect(screen.getByText("system.menu.businessScope")).toBeInTheDocument();
		expect(mocks.basicTableProps.columns.some((column: Record<string, any>) => column.dataIndex === "scope")).toBe(true);

		let requestResult: { data: Array<Record<string, any>> };
		await act(async () => {
			requestResult = await mocks.basicTableProps.request();
		});

		expect(mocks.fetchMenuTree).toHaveBeenCalledWith({ scope: "platform" });
		expect(requestResult!.data).toHaveLength(1);
		expect(requestResult!.data[0].scope).toBe("platform");
		expect(requestResult!.data[0].children?.[0].routePath).toBe("/platform/menu");
	});

	it("switches to business scope and keeps the scope column visible", async () => {
		render(<Menu />);

		await act(async () => {
			fireEvent.click(screen.getByText("system.menu.businessScope"));
		});

		let requestResult: { data: Array<Record<string, any>> };
		await act(async () => {
			requestResult = await mocks.basicTableProps.request();
		});

		expect(mocks.fetchMenuTree).toHaveBeenCalledWith({ scope: "business" });
		expect(requestResult!.data).toHaveLength(1);
		expect(requestResult!.data[0].scope).toBe("business");
		expect(mocks.basicTableProps.columns.some((column: Record<string, any>) => column.dataIndex === "scope")).toBe(true);
	});
});
