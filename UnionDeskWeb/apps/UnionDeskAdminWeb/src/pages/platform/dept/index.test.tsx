import type React from "react";

import { isValidElement } from "react";

import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	fetchPlatformOrganizations: vi.fn(),
	fetchCreatePlatformOrganization: vi.fn(),
	fetchUpdatePlatformOrganization: vi.fn(),
	fetchDeletePlatformOrganization: vi.fn(),
	fetchPlatformUsers: vi.fn(),
	searchProps: undefined as
		| undefined
		| {
			onSearch: (values: { keyword?: string; createdRange?: null }) => void;
			onReset: () => void;
		},
	tableColumns: [] as Array<{ title?: React.ReactNode; align?: string; key?: string }>,
	expandable: undefined as
		| undefined
		| {
			expandedRowKeys?: Array<string | number>;
			expandIcon?: (props: { expanded: boolean; onExpand: (record: unknown, event: unknown) => void; record: { children?: unknown[] } }) => React.ReactNode;
		},
	detailProps: undefined as
		| undefined
		| {
			open: boolean;
			mode: "create" | "edit";
			organization: null | { id: number; name: string };
			parentOrganization: null | { id: number; name: string };
			onSubmit: (values: {
				code: string;
				name: string;
				parentId: number | null;
				leaderUserId?: number | null;
				orderNo?: number;
				status?: number;
				remark?: string | null;
			}) => Promise<void>;
		},
	confirm: vi.fn(),
	messageSuccess: vi.fn(),
	messageError: vi.fn(),
}));

vi.mock("#src/api/platform/organization", () => ({
	fetchPlatformOrganizations: mocks.fetchPlatformOrganizations,
	fetchCreatePlatformOrganization: mocks.fetchCreatePlatformOrganization,
	fetchUpdatePlatformOrganization: mocks.fetchUpdatePlatformOrganization,
	fetchDeletePlatformOrganization: mocks.fetchDeletePlatformOrganization,
}));

vi.mock("#src/api/platform/iam", () => ({
	fetchPlatformUsers: mocks.fetchPlatformUsers,
}));

vi.mock("#src/components/auth-guarded", () => ({
	AuthGuarded: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock("#src/components/basic-button", () => ({
	BasicButton: ({ children, onClick }: { children: React.ReactNode; onClick?: () => void; icon?: React.ReactNode; [key: string]: unknown }) => (
		<button type="button" onClick={onClick}>
			{children}
		</button>
	),
}));

vi.mock("#src/components/basic-content", () => ({
	BasicContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock("#src/components/basic-table", () => ({
	BasicTable: ({
		dataSource = [],
		columns = [],
		headerTitle,
		toolBarRender,
		tableExtraRender,
		expandable,
	}: {
		dataSource?: Array<{ id: number; name: string; code: string; children?: any[] }>;
		columns?: Array<{ key?: string; render?: (...args: any[]) => React.ReactNode }>;
		headerTitle?: React.ReactNode;
		toolBarRender?: () => React.ReactNode[];
		tableExtraRender?: () => React.ReactNode;
		expandable?: {
			expandedRowKeys?: Array<string | number>;
			expandIcon?: (props: { expanded: boolean; onExpand: (record: unknown, event: unknown) => void; record: { children?: unknown[] } }) => React.ReactNode;
		};
	}) => {
		mocks.tableColumns = columns;
		mocks.expandable = expandable;
		const actionColumn = columns.find(column => column.key === "action");
		const renderRow = (record: { id: number; name: string; code: string; children?: any[] }) => (
			<div key={record.id} data-testid={`dept-row-${record.id}`}>
				<div>{record.name}</div>
				<div>{record.code}</div>
				<div>{actionColumn?.render?.(undefined, record, undefined, undefined)}</div>
				{record.children?.length ? record.children.map(child => renderRow(child)) : null}
			</div>
		);

		return (
			<section>
				<div>{headerTitle}</div>
				<div>{tableExtraRender?.()}</div>
				<div data-testid="dept-expanded-keys" data-keys={expandable?.expandedRowKeys?.join(",") ?? ""} />
				<div>{toolBarRender?.().map((item, index) => <span key={index}>{item}</span>)}</div>
				<div>{dataSource.map(record => renderRow(record))}</div>
			</section>
		);
	},
}));

vi.mock("antd", () => ({
	Alert: ({ message }: { message: React.ReactNode }) => <div>{message}</div>,
	App: {
		useApp: () => ({
			message: {
				success: mocks.messageSuccess,
				error: mocks.messageError,
			},
		}),
	},
	Button: ({ children, onClick }: { children: React.ReactNode; onClick?: () => void; icon?: React.ReactNode; [key: string]: unknown }) => (
		<button type="button" onClick={onClick}>
			{children}
		</button>
	),
	Empty: ({ description }: { description: React.ReactNode }) => <div>{description}</div>,
	Modal: {
		confirm: mocks.confirm,
	},
	Space: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Tag: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
	Typography: {
		Text: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
	},
}));

vi.mock("./components/search-panel", () => ({
	SearchPanel: (props: {
		onSearch: (values: { keyword?: string; createdRange?: null }) => void;
		onReset: () => void;
	}) => {
		mocks.searchProps = props;
		return (
			<div>
				<button type="button" onClick={() => props.onSearch({ keyword: "运营", createdRange: null })}>搜索运营</button>
				<button type="button" onClick={props.onReset}>重置</button>
			</div>
		);
	},
}));

vi.mock("./components/detail", () => ({
	Detail: (props: {
		open: boolean;
		mode: "create" | "edit";
		organization: null | { id: number; name: string };
		parentOrganization: null | { id: number; name: string };
		onSubmit: (values: {
			code: string;
			name: string;
			parentId: number | null;
			leaderUserId?: number | null;
			orderNo?: number;
			status?: number;
			remark?: string | null;
		}) => Promise<void>;
	}) => {
		mocks.detailProps = props;
		return (
			<div
				data-testid="organization-detail"
				data-open={String(props.open)}
				data-mode={props.mode}
				data-organization={props.organization?.name ?? ""}
				data-parent={props.parentOrganization?.name ?? ""}
			/>
		);
	},
}));

import PlatformDept from "./index";

function isCenteredTitle(node: React.ReactNode): boolean {
	if (!isValidElement(node)) {
		return false;
	}

	const element = node as React.ReactElement<{ className?: string }>;
	return String(element.props.className ?? "").includes("text-center");
}

const organizationRows = [
	{
		id: 1,
		code: "root",
		name: "平台组织",
		parentId: null,
		parentName: null,
		leaderUserId: 1,
		leaderName: "admin",
		orderNo: 1,
		status: 1,
		remark: null,
		createdAt: "2026-05-03T08:00:00",
	},
	{
		id: 2,
		code: "ops",
		name: "运营部",
		parentId: 1,
		parentName: "平台组织",
		leaderUserId: 2,
		leaderName: "ops",
		orderNo: 2,
		status: 1,
		remark: null,
		createdAt: "2026-05-04T08:00:00",
	},
	{
		id: 3,
		code: "audit",
		name: "审计部",
		parentId: 1,
		parentName: "平台组织",
		leaderUserId: 3,
		leaderName: "audit",
		orderNo: 3,
		status: 1,
		remark: null,
		createdAt: "2026-05-05T08:00:00",
	},
];

describe("PlatformDept page", () => {
	beforeEach(() => {
		mocks.fetchPlatformOrganizations.mockReset();
		mocks.fetchCreatePlatformOrganization.mockReset();
		mocks.fetchUpdatePlatformOrganization.mockReset();
		mocks.fetchDeletePlatformOrganization.mockReset();
		mocks.fetchPlatformUsers.mockReset();
		mocks.searchProps = undefined;
		mocks.tableColumns = [];
		mocks.expandable = undefined;
		mocks.detailProps = undefined;
		mocks.confirm.mockReset();
		mocks.messageSuccess.mockReset();
		mocks.messageError.mockReset();
	});

	it("filters tree by keyword and toggles expand all / collapse all", async () => {
		mocks.fetchPlatformOrganizations.mockResolvedValue(organizationRows);
		mocks.fetchPlatformUsers.mockResolvedValue([
			{ id: 1, username: "admin", mobile: "13800000000", email: null, roles: ["super_admin"] },
			{ id: 2, username: "ops", mobile: "13800000001", email: null, roles: ["super_admin"] },
		]);

		render(<PlatformDept />);

		await waitFor(() => {
			expect(screen.getByTestId("organization-detail")).toHaveAttribute("data-open", "false");
			expect(screen.getByTestId("dept-row-1")).toBeInTheDocument();
			expect(screen.getByTestId("dept-row-2")).toBeInTheDocument();
			expect(screen.getByTestId("dept-row-3")).toBeInTheDocument();
			expect(mocks.tableColumns.every(column => column.align === "center")).toBe(true);
			expect(mocks.tableColumns.every(column => isCenteredTitle(column.title))).toBe(true);
		});

		await waitFor(() => {
			expect(screen.getByTestId("dept-expanded-keys")).toHaveAttribute("data-keys", "1,2,3");
		});

		const expandIcon = mocks.expandable?.expandIcon?.({
			expanded: false,
			onExpand: vi.fn(),
			record: { children: [{}] },
		});
		expect(isValidElement(expandIcon)).toBe(true);
		expect(String((expandIcon as React.ReactElement<{ className?: string }>).props.className ?? "")).toContain("mr-1.5");

		await userEvent.click(screen.getByRole("button", { name: "收起全部" }));
		expect(screen.getByTestId("dept-expanded-keys")).toHaveAttribute("data-keys", "");

		await userEvent.click(screen.getByRole("button", { name: "展开全部" }));
		expect(screen.getByTestId("dept-expanded-keys")).toHaveAttribute("data-keys", "1,2,3");

		await userEvent.click(screen.getByRole("button", { name: "搜索运营" }));

		await waitFor(() => {
			expect(screen.getByTestId("dept-row-1")).toBeInTheDocument();
			expect(screen.getByTestId("dept-row-2")).toBeInTheDocument();
			expect(screen.queryByTestId("dept-row-3")).not.toBeInTheDocument();
		});
	});

	it("opens create, edit and delete actions from rows", async () => {
		mocks.fetchPlatformOrganizations.mockResolvedValue(organizationRows.slice(0, 2));
		mocks.fetchPlatformUsers.mockResolvedValue([
			{ id: 1, username: "admin", mobile: "13800000000", email: null, roles: ["super_admin"] },
			{ id: 2, username: "ops", mobile: "13800000001", email: null, roles: ["super_admin"] },
		]);
		mocks.fetchDeletePlatformOrganization.mockResolvedValue(undefined);

		render(<PlatformDept />);

		await waitFor(() => {
			expect(screen.getByTestId("dept-row-2")).toBeInTheDocument();
		});

		await userEvent.click(within(screen.getByTestId("dept-row-2")).getByRole("button", { name: "新增下级" }));

		await waitFor(() => {
			expect(screen.getByTestId("organization-detail")).toHaveAttribute("data-open", "true");
			expect(screen.getByTestId("organization-detail")).toHaveAttribute("data-mode", "create");
			expect(screen.getByTestId("organization-detail")).toHaveAttribute("data-parent", "运营部");
		});

		await userEvent.click(within(screen.getByTestId("dept-row-2")).getByRole("button", { name: "编辑" }));

		await waitFor(() => {
			expect(screen.getByTestId("organization-detail")).toHaveAttribute("data-mode", "edit");
			expect(screen.getByTestId("organization-detail")).toHaveAttribute("data-organization", "运营部");
		});

		await userEvent.click(within(screen.getByTestId("dept-row-2")).getByRole("button", { name: "删除" }));
		expect(mocks.confirm).toHaveBeenCalledTimes(1);

		const confirmArgs = mocks.confirm.mock.calls.at(-1)?.[0];
		await confirmArgs.onOk();

		await waitFor(() => {
			expect(mocks.fetchDeletePlatformOrganization).toHaveBeenCalledWith(2);
		});
	});

	it("submits create payload with generated code and real parent id only", async () => {
		mocks.fetchPlatformOrganizations.mockResolvedValue(organizationRows.slice(0, 2));
		mocks.fetchPlatformUsers.mockResolvedValue([]);
		mocks.fetchCreatePlatformOrganization.mockResolvedValue(undefined);

		render(<PlatformDept />);

		await waitFor(() => {
			expect(screen.getByTestId("dept-row-2")).toBeInTheDocument();
		});

		await userEvent.click(within(screen.getByTestId("dept-row-2")).getByRole("button", { name: "新增下级" }));
		await mocks.detailProps?.onSubmit({
			code: "dept-abc12345",
			name: "子部门",
			parentId: 2,
			leaderUserId: null,
			orderNo: 0,
			status: 1,
			remark: null,
		});

		await waitFor(() => {
			expect(mocks.fetchCreatePlatformOrganization).toHaveBeenCalledWith({
				code: "dept-abc12345",
				name: "子部门",
				parentId: 2,
				leaderUserId: null,
				orderNo: 0,
				status: 1,
				remark: null,
			});
		});
	});
});
