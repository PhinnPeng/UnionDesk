import type React from "react";

import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	fetchMenuTree: vi.fn(),
	useAuth: vi.fn(),
}));

vi.mock("#src/api/system/menu", () => ({
	fetchDeleteMenu: vi.fn(),
	fetchMenuTree: mocks.fetchMenuTree,
}));

vi.mock("#src/components/auth-guarded", () => ({
	AuthGuarded: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock("#src/components/confirm-popover", () => ({
	ConfirmPopover: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock("#src/components/basic-button", () => ({
	BasicButton: ({ children, onClick, ...props }: { children: React.ReactNode; onClick?: () => void; [key: string]: unknown }) => (
		<button type="button" onClick={onClick} {...props}>{children}</button>
	),
}));

vi.mock("#src/components/basic-content", () => ({
	BasicContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock("#src/components/basic-table", () => ({
	BasicTable: ({
		request,
		columns = [],
		toolBarRender,
		expandable,
	}: {
		request?: () => Promise<{ data: Array<{ id: number; name: string; code: string; children?: any[] }> }>;
		columns?: Array<{ key?: string; render?: (...args: any[]) => React.ReactNode }>;
		toolBarRender?: () => React.ReactNode[];
		expandable?: { expandedRowKeys?: Array<string | number> };
	}) => {
		const [rows, setRows] = useState<Array<{ id: number; name: string; code: string; children?: any[] }>>([]);
		const actionColumn = columns.find(column => column.key === "option");

		useEffect(() => {
			void (async () => {
				if (!request) {
					return;
				}
				const result = await request();
				setRows(result.data ?? []);
			})();
		}, []);

		const renderRow = (record: { id: number; name: string; code: string; children?: any[] }) => (
			<div key={record.id} data-testid={`menu-row-${record.id}`}>
				<div>{record.name}</div>
				<div>{record.code}</div>
				<div>{actionColumn?.render?.(undefined, record, undefined, undefined)}</div>
				{record.children?.length ? record.children.map(child => renderRow(child)) : null}
			</div>
		);

		return (
			<section>
				<div data-testid="menu-expanded-keys" data-keys={expandable?.expandedRowKeys?.join(",") ?? ""} />
				<div>{toolBarRender?.().map((item, index) => <span key={index}>{item}</span>)}</div>
				<div>{rows.map(record => renderRow(record))}</div>
			</section>
		);
	},
}));

vi.mock("#src/hooks/use-auth", () => ({
	useAuth: mocks.useAuth,
}));

vi.mock("react-i18next", () => ({
	useTranslation: () => ({
		t: (key: string) => key,
	}),
}));

vi.mock("antd", () => ({
	Button: ({ children, onClick, ...props }: { children: React.ReactNode; onClick?: () => void; [key: string]: unknown }) => (
		<button type="button" onClick={onClick} {...props}>{children}</button>
	),
	Card: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Space: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Tag: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
}));

vi.mock("./components/detail", () => ({
	Detail: () => <div data-testid="menu-detail" />,
}));

vi.mock("./components/menu-scope-filter", () => ({
	MenuScopeFilter: ({ value }: { value: "platform" | "business" }) => <div data-testid="menu-scope">{value}</div>,
}));

import { useEffect, useState } from "react";

import Menu from "./index";

describe("system menu page", () => {
	beforeEach(() => {
		mocks.fetchMenuTree.mockReset();
		mocks.useAuth.mockReturnValue({ routeScope: "platform" });
	});

	it("supports expand all and collapse all", async () => {
		mocks.fetchMenuTree.mockResolvedValue([
			{
				id: 1,
				name: "平台首页",
				code: "home",
				scope: "platform",
				nodeType: "menu",
				required: true,
				children: [
					{
						id: 2,
						name: "组织管理",
						code: "org",
						scope: "platform",
						nodeType: "catalog",
						required: false,
						children: [
							{
								id: 3,
								name: "组织架构",
								code: "org-tree",
								scope: "platform",
								nodeType: "menu",
								required: false,
								children: [],
							},
						],
					},
				],
			},
		]);

		render(<Menu />);

		await waitFor(() => {
			expect(screen.getByTestId("menu-row-1")).toBeInTheDocument();
			expect(screen.getByTestId("menu-row-2")).toBeInTheDocument();
			expect(screen.getByTestId("menu-row-3")).toBeInTheDocument();
		});

		expect(screen.getByTestId("menu-expanded-keys")).toHaveAttribute("data-keys", "1,2,3");

		await userEvent.click(screen.getByRole("button", { name: "收起全部" }));
		await waitFor(() => {
			expect(screen.getByTestId("menu-expanded-keys")).toHaveAttribute("data-keys", "");
		});

		await userEvent.click(screen.getByRole("button", { name: "展开全部" }));
		await waitFor(() => {
			expect(screen.getByTestId("menu-expanded-keys")).toHaveAttribute("data-keys", "1,2,3");
		});
	});
});
