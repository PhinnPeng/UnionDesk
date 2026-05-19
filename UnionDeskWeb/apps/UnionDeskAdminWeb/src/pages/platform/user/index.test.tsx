import type React from "react";

import { isValidElement } from "react";
import { render, screen, waitFor, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	fetchPlatformUsers: vi.fn(),
	fetchPlatformOrganizations: vi.fn(),
	fetchRoleList: vi.fn(),
	fetchOffboardPlatformUser: vi.fn(),
	navigate: vi.fn(),
	tableColumns: [] as Array<{ key?: string; title?: React.ReactNode; dataIndex?: string; align?: string }>,
	allowedCodes: new Set<string>(),
	detailProps: undefined as
		| undefined
		| {
			open: boolean;
			mode: "create" | "edit";
			user: null | { id: number; username: string; organizationIds: number[] };
			onSuccess: (user: any) => void | Promise<void>;
		},
	resetPasswordProps: undefined as
		| undefined
		| {
			open: boolean;
			user: null | { id: number; username: string };
			onClose: () => void;
			onSuccess: (user: any) => void | Promise<void>;
		},
	confirm: vi.fn(),
	messageSuccess: vi.fn(),
	messageError: vi.fn(),
}));

vi.mock("#src/api/platform/iam", () => ({
	fetchPlatformUsers: mocks.fetchPlatformUsers,
	fetchOffboardPlatformUser: mocks.fetchOffboardPlatformUser,
}));

vi.mock("#src/api/platform/organization", () => ({
	fetchPlatformOrganizations: mocks.fetchPlatformOrganizations,
}));

vi.mock("#src/api/system/role", () => ({
	fetchRoleList: mocks.fetchRoleList,
}));

vi.mock("#src/components/basic-content", () => ({
	BasicContent: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
}));

vi.mock("#src/components/auth-guarded", () => ({
	AuthGuarded: ({ auth, children }: { auth?: string; children: React.ReactNode }) => {
		if (!auth || mocks.allowedCodes.has(auth)) {
			return <>{children}</>;
		}
		return null;
	},
}));

vi.mock("react-router", () => ({
	useNavigate: () => mocks.navigate,
}));

vi.mock("./components/search-panel", () => ({
	SearchPanel: (props: {
		onSearch: (values: { keyword?: string }) => void;
		onReset: () => void;
	}) => (
		<div data-testid="search-panel">
			<button type="button" onClick={() => props.onSearch({ keyword: "alice" })}>搜索 alice</button>
			<button type="button" onClick={props.onReset}>重置</button>
		</div>
	),
}));

vi.mock("./components/dept-tree-block", () => ({
	DeptTreeBlock: (props: {
		organizations: Array<{ id: number; name: string }>;
		selectedDepartmentId?: number | null;
		onSelect: (departmentId: number | null) => void;
	}) => (
		<div data-testid="department-tree">
			{props.organizations.map(organization => (
				<button
					key={organization.id}
					type="button"
					onClick={() => props.onSelect(organization.id)}
				>
					{organization.name}
				</button>
			))}
			<button type="button" onClick={() => props.onSelect(null)}>
				全部用户
			</button>
		</div>
	),
}));

vi.mock("./components/detail", () => ({
	Detail: (props: {
		open: boolean;
		mode: "create" | "edit";
		user: null | { id: number; username: string; organizationIds: number[] };
		onSuccess: (user: any) => void | Promise<void>;
	}) => {
		mocks.detailProps = props;
		return <div data-testid="user-detail" data-open={String(props.open)} data-mode={props.mode} />;
	},
}));

vi.mock("./components/reset-password-modal", () => ({
	ResetPasswordModal: (props: {
		open: boolean;
		user: null | { id: number; username: string };
		onClose: () => void;
		onSuccess: (user: any) => void | Promise<void>;
	}) => {
		mocks.resetPasswordProps = props;
		return <div data-testid="reset-password-modal" data-open={String(props.open)} data-user={props.user?.username ?? ""} />;
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
	Button: ({ children, onClick, disabled }: { children: React.ReactNode; onClick?: () => void; disabled?: boolean }) => (
		<button type="button" onClick={onClick} disabled={disabled}>{children}</button>
	),
	Card: ({ title, extra, children }: { title?: React.ReactNode; extra?: React.ReactNode; children: React.ReactNode }) => (
		<section>
			<header>{title}{extra}</header>
			{children}
		</section>
	),
	Col: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Dropdown: ({ menu, children }: { menu?: { items?: Array<{ key: string; label: React.ReactNode }>; onClick?: (event: { key: string }) => void }; children: React.ReactNode }) => (
		<div>
			{children}
			{menu?.items?.map(item => (
				<button type="button" key={item.key} onClick={() => menu.onClick?.({ key: item.key })}>
					{item.label}
				</button>
			))}
		</div>
	),
	Modal: {
		confirm: mocks.confirm,
	},
	Input: (props: React.InputHTMLAttributes<HTMLInputElement>) => <input {...props} />,
	Row: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Space: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Table: ({
		dataSource = [],
		columns = [],
		rowSelection,
	}: {
		dataSource?: Array<{ id: number; username: string; mobile: string; status: string }>;
		columns?: Array<{ key?: string; title?: React.ReactNode; dataIndex?: string; align?: string; render?: (...args: any[]) => React.ReactNode }>;
		rowSelection?: { selectedRowKeys?: React.Key[]; onChange?: (keys: React.Key[]) => void };
	}) => {
		const actionColumn = columns.find(column => column.key === "action");
		mocks.tableColumns = columns;
		return (
			<div>
				{dataSource.map(record => (
					<div key={record.id} data-testid={`user-row-${record.id}`}>
						<button type="button" onClick={() => rowSelection?.onChange?.([record.id])}>选择</button>
						<span>{record.username}</span>
						<span>{record.mobile}</span>
						<span>{record.status}</span>
						{actionColumn?.render?.(undefined, record, undefined)}
					</div>
				))}
			</div>
		);
	},
	Tag: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
	Typography: {
		Text: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
		Title: ({ children }: { children: React.ReactNode }) => <h2>{children}</h2>,
	},
}));

import PlatformUser from "./index";

function getNodeText(node: React.ReactNode): string {
	if (typeof node === "string" || typeof node === "number") {
		return String(node);
	}
	if (Array.isArray(node)) {
		return node.map(getNodeText).join("");
	}
	if (isValidElement(node)) {
		return getNodeText(node.props.children);
	}
	return "";
}

function isCenteredTitle(node: React.ReactNode): boolean {
	return isValidElement(node) && String(node.props.className ?? "").includes("text-center");
}

const userRows = [
	{
		id: 1,
		username: "alice",
		mobile: "13800000000",
		email: "alice@example.com",
		accountType: "admin",
		status: 1,
		employmentStatus: "active",
		roleCodes: ["super_admin"],
		businessDomainIds: [1],
		organizationIds: [10],
	},
	{
		id: 2,
		username: "bob",
		mobile: "13900000000",
		email: "bob@example.com",
		accountType: "admin",
		status: 1,
		employmentStatus: "active",
		roleCodes: [],
		businessDomainIds: [],
		organizationIds: [11],
	},
	{
		id: 3,
		username: "charlie",
		mobile: "13700000000",
		email: "charlie@example.com",
		accountType: "admin",
		status: 1,
		employmentStatus: "active",
		roleCodes: [],
		businessDomainIds: [],
		organizationIds: [12],
	},
];

function mockPageData() {
	mocks.fetchPlatformUsers.mockResolvedValue(userRows);
	mocks.fetchPlatformOrganizations.mockResolvedValue([
		{
			id: 10,
			code: "root",
			name: "平台组织",
			parentId: null,
			parentName: null,
			leaderUserId: null,
			leaderName: null,
			orderNo: 1,
			status: 1,
			remark: null,
			createdAt: "2026-05-01T00:00:00",
		},
		{
			id: 11,
			code: "ops",
			name: "平台运营部",
			parentId: 10,
			parentName: "平台组织",
			leaderUserId: null,
			leaderName: null,
			orderNo: 2,
			status: 1,
			remark: null,
			createdAt: "2026-05-02T00:00:00",
		},
		{
			id: 12,
			code: "support",
			name: "客户支持组",
			parentId: 11,
			parentName: "平台运营部",
			leaderUserId: null,
			leaderName: null,
			orderNo: 3,
			status: 1,
			remark: null,
			createdAt: "2026-05-03T00:00:00",
		},
	]);
	mocks.fetchRoleList.mockResolvedValue([
		{ id: 7, code: "super_admin", name: "超级管理员", scope: "global", system: true },
	]);
}

describe("PlatformUser page", () => {
	beforeEach(() => {
		mocks.fetchPlatformUsers.mockReset();
		mocks.fetchPlatformOrganizations.mockReset();
		mocks.fetchRoleList.mockReset();
		mocks.fetchOffboardPlatformUser.mockReset();
		mocks.navigate.mockReset();
		mocks.tableColumns = [];
		mocks.allowedCodes.clear();
		mocks.detailProps = undefined;
		mocks.resetPasswordProps = undefined;
		mocks.confirm.mockReset();
		mocks.messageSuccess.mockReset();
		mocks.messageError.mockReset();
	});

	it("shows department tree, centers headers, and filters users by keyword", async () => {
		mocks.allowedCodes.add("platform.user.create");
		mockPageData();

		render(<PlatformUser />);

		await waitFor(() => {
			expect(screen.getByTestId("department-tree")).toHaveTextContent("平台组织");
			expect(screen.getByTestId("user-row-1")).toBeInTheDocument();
			expect(screen.getByTestId("user-row-2")).toBeInTheDocument();
			expect(mocks.tableColumns.map(column => getNodeText(column.title))).toEqual([
				"账号",
				"手机号",
				"邮箱",
				"部门",
				"角色",
				"状态",
				"最近登录",
				"操作",
			]);
			expect(mocks.tableColumns.every(column => isCenteredTitle(column.title))).toBe(true);
			expect(mocks.tableColumns.every(column => column.align === "center")).toBe(true);
		});

		await userEvent.click(screen.getByRole("button", { name: "搜索 alice" }));

		await waitFor(() => {
			expect(screen.getByTestId("user-row-1")).toBeInTheDocument();
			expect(screen.queryByTestId("user-row-2")).not.toBeInTheDocument();
		});
	});

	it("filters users by department tree selection and includes child departments", async () => {
		mockPageData();

		render(<PlatformUser />);

		await waitFor(() => {
			expect(screen.getByTestId("user-row-1")).toBeInTheDocument();
			expect(screen.getByTestId("user-row-2")).toBeInTheDocument();
			expect(screen.getByTestId("user-row-3")).toBeInTheDocument();
		});

		await userEvent.click(within(screen.getByTestId("department-tree")).getByRole("button", { name: "平台运营部" }));

		await waitFor(() => {
			expect(screen.queryByTestId("user-row-1")).not.toBeInTheDocument();
			expect(screen.getByTestId("user-row-2")).toBeInTheDocument();
			expect(screen.getByTestId("user-row-3")).toBeInTheDocument();
		});
	});

	it("opens create and edit modal, then syncs created user into table", async () => {
		mocks.allowedCodes.add("platform.user.create");
		mocks.allowedCodes.add("platform.user.update");
		mockPageData();

		render(<PlatformUser />);

		await waitFor(() => {
			expect(screen.getByText("新增用户")).toBeInTheDocument();
		});

		await userEvent.click(screen.getByText("新增用户"));
		expect(mocks.detailProps).toMatchObject({ open: true, mode: "create", user: null });

		await mocks.detailProps?.onSuccess({
			id: 3,
			username: "cindy",
			mobile: "13700000000",
			email: "cindy@example.com",
			accountType: "admin",
			status: 1,
			employmentStatus: "active",
			roleCodes: [],
			businessDomainIds: [],
			organizationIds: [],
		});

		await waitFor(() => {
			expect(screen.getByText("cindy")).toBeInTheDocument();
		});

		await userEvent.click(within(screen.getByTestId("user-row-2")).getByRole("button", { name: "编辑" }));
		expect(mocks.detailProps).toMatchObject({
			open: true,
			mode: "edit",
			user: {
				id: 2,
				username: "bob",
			},
		});
	});

	it("supports row offboard, batch offboard and opens reset password modal", async () => {
		mocks.allowedCodes.add("platform.user.disable");
		mocks.allowedCodes.add("platform.user.reset_password");
		mockPageData();
		mocks.fetchOffboardPlatformUser.mockImplementation(async (id: number) => ({
			...userRows.find(user => user.id === id),
			employmentStatus: "offboarded",
		}));

		render(<PlatformUser />);

		await waitFor(() => {
			expect(screen.getByTestId("user-row-2")).toBeInTheDocument();
		});

		await userEvent.click(within(screen.getByTestId("user-row-2")).getByRole("button", { name: "离职" }));
		let confirmArgs = mocks.confirm.mock.calls.at(-1)?.[0];
		await confirmArgs.onOk();
		expect(mocks.fetchOffboardPlatformUser).toHaveBeenCalledWith(2);

		await userEvent.click(within(screen.getByTestId("user-row-1")).getByRole("button", { name: "选择" }));
		await userEvent.click(screen.getByRole("button", { name: "批量离职" }));
		confirmArgs = mocks.confirm.mock.calls.at(-1)?.[0];
		await confirmArgs.onOk();
		expect(mocks.fetchOffboardPlatformUser).toHaveBeenCalledWith(1);

		await userEvent.click(within(screen.getByTestId("user-row-1")).getByRole("button", { name: "重置密码" }));

		await waitFor(() => {
			expect(mocks.resetPasswordProps).toMatchObject({
				open: true,
				user: {
					id: 1,
					username: "alice",
				},
			});
		});
	});

	it("navigates to import export when the entry is allowed", async () => {
		mocks.allowedCodes.add("platform.user.import");
		mockPageData();

		render(<PlatformUser />);

		await waitFor(() => {
			expect(screen.getByText("导入导出")).toBeInTheDocument();
		});

		await userEvent.click(screen.getByText("导入导出"));

		expect(mocks.navigate).toHaveBeenCalledWith("/platform/import-export");
	});
});
