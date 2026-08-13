import type React from "react";

import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	fetchCreatePlatformUser: vi.fn(),
	fetchUpdatePlatformUser: vi.fn(),
	messageSuccess: vi.fn(),
	messageError: vi.fn(),
	onClose: vi.fn(),
	onSuccess: vi.fn(),
	form: {
		setFieldsValue: vi.fn(),
		validateFields: vi.fn(),
		resetFields: vi.fn(),
	},
}));

vi.mock("#src/api/platform/iam", () => ({
	fetchCreatePlatformUser: mocks.fetchCreatePlatformUser,
	fetchUpdatePlatformUser: mocks.fetchUpdatePlatformUser,
}));

vi.mock("antd", () => ({
	App: {
		useApp: () => ({
			message: {
				success: mocks.messageSuccess,
				error: mocks.messageError,
			},
		}),
	},
	Modal: ({ open, title, onOk, onCancel, children }: {
		open: boolean;
		title: React.ReactNode;
		onOk?: () => void;
		onCancel?: () => void;
		children: React.ReactNode;
	}) => (open ? (
		<div>
			<h1>{title}</h1>
			{children}
			<button type="button" onClick={onOk}>保存</button>
			<button type="button" onClick={onCancel}>取消</button>
		</div>
	) : null),
	Form: Object.assign(({ children }: { children: React.ReactNode }) => <form>{children}</form>, {
		useForm: () => [mocks.form],
		Item: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	}),
	Input: Object.assign((props: React.InputHTMLAttributes<HTMLInputElement>) => <input {...props} />, {
		Password: (props: React.InputHTMLAttributes<HTMLInputElement>) => <input type="password" {...props} />,
		TextArea: (props: React.TextareaHTMLAttributes<HTMLTextAreaElement>) => <textarea {...props} />,
	}),
	Button: ({ htmlType, children, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { htmlType?: "button" | "submit" | "reset" }) => (
		<button type={htmlType ?? "button"} {...props}>
			{children}
		</button>
	),
	Select: ({ options = [], ...props }: { options?: Array<{ label: React.ReactNode; value: string | number }>; [key: string]: unknown }) => (
		<select {...props}>
			{options.map(option => (
				<option key={String(option.value)} value={option.value}>
					{option.label}
				</option>
			))}
		</select>
	),
	TreeSelect: ({ treeData = [], ...props }: { treeData?: Array<{ title: React.ReactNode; value: string | number; children?: unknown[] }>; [key: string]: unknown }) => (
		<select {...props}>
			{treeData.map(node => (
				<option key={String(node.value)} value={node.value}>
					{node.title}
				</option>
			))}
		</select>
	),
}));

import { Detail } from "./detail";

describe("platform user detail", () => {
	beforeEach(() => {
		mocks.fetchCreatePlatformUser.mockReset();
		mocks.fetchUpdatePlatformUser.mockReset();
		mocks.messageSuccess.mockReset();
		mocks.messageError.mockReset();
		mocks.onClose.mockReset();
		mocks.onSuccess.mockReset();
		mocks.form.setFieldsValue.mockReset();
		mocks.form.validateFields.mockReset();
		mocks.form.resetFields.mockReset();
	});

	it("submits create payload and returns saved user", async () => {
		mocks.form.validateFields.mockResolvedValue({
			username: " alice ",
			mobile: " 13800000000 ",
			email: " alice@example.com ",
			password: " 12345678 ",
			roleCodes: ["super_admin"],
			organizationId: 1,
			remark: " 新用户备注 ",
		});
		const savedUser = { id: 1, username: "alice" };
		mocks.fetchCreatePlatformUser.mockResolvedValue(savedUser);

		render(
			<Detail
				open
				mode="create"
				user={null}
				roles={[
					{ id: 7, code: "super_admin", name: "超级管理员", scope: "global", system: true },
				]}
				onClose={mocks.onClose}
				onSuccess={mocks.onSuccess}
				organizations={[]}
			/>,
		);

		await userEvent.click(screen.getByRole("button", { name: "保存" }));

		await waitFor(() => {
			expect(mocks.fetchCreatePlatformUser).toHaveBeenCalledWith({
				username: "alice",
				mobile: "13800000000",
				email: "alice@example.com",
				remark: "新用户备注",
				password: "12345678",
				accountType: "admin",
				roleCodes: ["super_admin"],
				businessDomainIds: [],
				organizationIds: [1],
			});
		});
		expect(mocks.onSuccess).toHaveBeenCalledWith(savedUser);
		expect(mocks.onClose).toHaveBeenCalledTimes(1);
	});

	it("does not render the password field in edit mode and omits password from update payload", async () => {
		mocks.form.validateFields.mockResolvedValue({
			username: " alice ",
			mobile: " 13800000000 ",
			email: " alice@example.com ",
			roleCodes: ["super_admin"],
			organizationId: 1,
			remark: "",
		});
		const savedUser = { id: 9, username: "alice" };
		mocks.fetchUpdatePlatformUser.mockResolvedValue(savedUser);

		render(
			<Detail
				open
				mode="edit"
				user={{
					id: 9,
					username: "alice",
					mobile: "13800000000",
					email: "alice@example.com",
					remark: "已有备注",
					accountType: "customer",
					status: 1,
					employmentStatus: "active",
					roleCodes: ["super_admin"],
					businessDomainIds: [1],
					organizationIds: [1, 2],
				}}
				roles={[
					{ id: 7, code: "super_admin", name: "超级管理员", scope: "global", system: true },
				]}
				onClose={mocks.onClose}
				onSuccess={mocks.onSuccess}
				organizations={[]}
			/>,
		);

		expect(screen.queryByRole("button", { name: "随机生成" })).not.toBeInTheDocument();

		await userEvent.click(screen.getByRole("button", { name: "保存" }));

		await waitFor(() => {
			expect(mocks.fetchUpdatePlatformUser).toHaveBeenCalledWith(9, {
				username: "alice",
				mobile: "13800000000",
				email: "alice@example.com",
				remark: "",
				accountType: "customer",
				roleCodes: ["super_admin"],
				businessDomainIds: [],
				organizationIds: [1],
			});
		});

		const payload = mocks.fetchUpdatePlatformUser.mock.calls[0]?.[1];
		expect(payload).not.toHaveProperty("password");
		expect(mocks.onSuccess).toHaveBeenCalledWith(savedUser);
		expect(mocks.onClose).toHaveBeenCalledTimes(1);
	});

	it("generates a password from the form button", async () => {
		mocks.form.validateFields.mockResolvedValue({
			username: "alice",
			mobile: "13800000000",
			email: "",
			password: "",
			roleCodes: ["super_admin"],
			organizationId: null,
			remark: "",
		});

		render(
			<Detail
				open
				mode="create"
				user={null}
				roles={[
					{ id: 7, code: "super_admin", name: "超级管理员", scope: "global", system: true },
				]}
				onClose={mocks.onClose}
				onSuccess={mocks.onSuccess}
				organizations={[]}
			/>,
		);

		await userEvent.click(screen.getByRole("button", { name: "随机生成" }));

		expect(mocks.form.setFieldsValue).toHaveBeenCalledWith(
			expect.objectContaining({
				password: expect.stringMatching(/^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*]).{16}$/),
			}),
		);
	});
});
