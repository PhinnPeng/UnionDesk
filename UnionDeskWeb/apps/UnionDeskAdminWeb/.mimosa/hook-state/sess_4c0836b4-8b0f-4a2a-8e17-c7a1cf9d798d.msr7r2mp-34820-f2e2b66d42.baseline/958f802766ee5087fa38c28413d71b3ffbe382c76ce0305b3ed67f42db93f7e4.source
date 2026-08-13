import type React from "react";
import type { PlatformOrganizationView } from "@uniondesk/shared";

import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	onSubmit: vi.fn(),
	onClose: vi.fn(),
	onDelete: vi.fn(),
	form: {
		setFieldsValue: vi.fn(),
		validateFields: vi.fn(),
	},
}));

vi.mock("#src/components/auth-guarded", () => ({
	AuthGuarded: ({ children }: { children: React.ReactNode }) => <>{children}</>,
}));

vi.mock("antd", () => ({
	Button: ({ children, onClick, loading }: { children: React.ReactNode; onClick?: () => void; icon?: React.ReactNode; loading?: boolean; [key: string]: unknown }) => (
		<button type="button" onClick={onClick} data-loading={String(Boolean(loading))}>
			{children}
		</button>
	),
	Col: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Form: Object.assign(({ children }: { children: React.ReactNode }) => <form>{children}</form>, {
		useForm: () => [mocks.form],
		Item: ({ label, children }: { label?: React.ReactNode; children: React.ReactNode }) => (
			<div data-testid={label ? "form-item" : undefined}>
				{label ? <span data-testid="form-item-label">{label}</span> : null}
				{children}
			</div>
		),
	}),
	Input: Object.assign(({ value, ...props }: React.InputHTMLAttributes<HTMLInputElement> & { value?: string }) => <input defaultValue={value} {...props} />, {
		TextArea: ({ value, ...props }: React.TextareaHTMLAttributes<HTMLTextAreaElement> & { value?: string }) => <textarea defaultValue={value} {...props} />,
	}),
	InputNumber: ({ value, ...props }: { value?: number; [key: string]: unknown }) => <input type="number" defaultValue={value ?? ""} {...props} />,
	Modal: ({ open, title, footer, children }: { open?: boolean; title?: React.ReactNode; footer?: React.ReactNode; children: React.ReactNode }) => (
		open ? (
			<section>
				<header>{title}</header>
				{children}
				<footer>{footer}</footer>
			</section>
		) : null
	),
	Row: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Select: ({ options = [], ...props }: { options?: Array<{ label: React.ReactNode; value: string | number }>; [key: string]: unknown }) => (
		<select {...props}>
			{options.map(option => (
				<option key={String(option.value)} value={option.value}>
					{option.label}
				</option>
			))}
		</select>
	),
	Space: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Tag: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
	TreeSelect: ({
		treeData = [],
		placeholder,
	}: {
		treeData?: Array<{ title: React.ReactNode; value: number; children?: Array<{ title: React.ReactNode; value: number }> }>;
		placeholder?: string;
	}) => (
		<div data-testid="parent-tree-select" data-placeholder={placeholder ?? ""}>
			{treeData.map(node => (
				<span key={String(node.value)}>{node.title}</span>
			))}
		</div>
	),
	Typography: {
		Text: ({ children }: { children: React.ReactNode }) => <span>{children}</span>,
	},
}));

import { Detail } from "./detail";

const rootOrganization: PlatformOrganizationView = {
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
};

const opsOrganization: PlatformOrganizationView = {
	id: 2,
	code: "ops",
	name: "运营部",
	parentId: 1,
	parentName: "平台组织",
	leaderUserId: 2,
	leaderName: "ops",
	orderNo: 10,
	status: 1,
	remark: "平台组织",
	createdAt: "2026-05-04T08:00:00",
};

const organizations = [rootOrganization, opsOrganization];

describe("organization detail modal", () => {
	beforeEach(() => {
		mocks.onSubmit.mockReset();
		mocks.onClose.mockReset();
		mocks.onDelete.mockReset();
		mocks.form.setFieldsValue.mockReset();
		mocks.form.validateFields.mockReset();
	});

	it("auto fills code and keeps key fields in requested order when creating", () => {
		render(
			<Detail
				open
				mode="create"
				organization={null}
				parentOrganization={rootOrganization}
				organizations={organizations}
				leaderOptions={[
					{ label: "admin", value: 1 },
					{ label: "ops", value: 2 },
				]}
				saving={false}
				onSubmit={mocks.onSubmit}
				onClose={mocks.onClose}
				onDelete={mocks.onDelete}
			/>,
		);

		const initialValues = mocks.form.setFieldsValue.mock.calls[0]?.[0];
		expect(initialValues.code).toMatch(/^dept-[a-z0-9]{8}$/);
		expect(initialValues.parentId).toBe(1);
		expect(screen.queryByText("创建时间")).not.toBeInTheDocument();
		expect(screen.queryByText("无上级部门")).not.toBeInTheDocument();
		expect(screen.getByTestId("parent-tree-select")).toHaveAttribute("data-placeholder", "请选择上级部门，留空表示顶级部门");
		expect(screen.getAllByTestId("form-item-label").map(item => item.textContent).slice(0, 3)).toEqual([
			"部门编码",
			"部门名称",
			"上级部门",
		]);
	});

	it("submits trimmed create values with null parent id", async () => {
		mocks.form.validateFields.mockResolvedValue({
			code: " dept-abc12345 ",
			name: " 运营部 ",
			parentId: null,
			leaderUserId: 2,
			orderNo: 10,
			status: 1,
			remark: " 备注 ",
		});

		render(
			<Detail
				open
				mode="create"
				organization={null}
				parentOrganization={null}
				organizations={organizations}
				leaderOptions={[
					{ label: "admin", value: 1 },
					{ label: "ops", value: 2 },
				]}
				saving={false}
				onSubmit={mocks.onSubmit}
				onClose={mocks.onClose}
				onDelete={mocks.onDelete}
			/>,
		);

		await userEvent.click(screen.getByRole("button", { name: "保存" }));

		await waitFor(() => {
			expect(mocks.onSubmit).toHaveBeenCalledWith({
				code: "dept-abc12345",
				name: "运营部",
				parentId: null,
				leaderUserId: 2,
				orderNo: 10,
				status: 1,
				remark: "备注",
			});
		});

		expect(screen.queryByRole("button", { name: "删除部门" })).not.toBeInTheDocument();
	});

	it("keeps existing code in edit mode and exposes delete action", async () => {
		render(
			<Detail
				open
				mode="edit"
				organization={opsOrganization}
				parentOrganization={rootOrganization}
				organizations={organizations}
				leaderOptions={[
					{ label: "admin", value: 1 },
					{ label: "ops", value: 2 },
				]}
				saving={false}
				onSubmit={mocks.onSubmit}
				onClose={mocks.onClose}
				onDelete={mocks.onDelete}
			/>,
		);

		const initialValues = mocks.form.setFieldsValue.mock.calls[0]?.[0];
		expect(initialValues.code).toBe("ops");
		expect(initialValues.parentId).toBe(1);

		await userEvent.click(screen.getByRole("button", { name: "删除部门" }));
		expect(mocks.onDelete).toHaveBeenCalledTimes(1);
	});
});
