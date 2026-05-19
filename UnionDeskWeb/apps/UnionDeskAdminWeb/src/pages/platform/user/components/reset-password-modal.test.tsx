import type React from "react";

import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
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
			<button type="button" onClick={onOk}>重置</button>
			<button type="button" onClick={onCancel}>取消</button>
		</div>
	) : null),
	Form: Object.assign(({ children }: { children: React.ReactNode }) => <form>{children}</form>, {
		useForm: () => [mocks.form],
		Item: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	}),
	Input: Object.assign((props: React.InputHTMLAttributes<HTMLInputElement>) => <input {...props} />, {
		Password: (props: React.InputHTMLAttributes<HTMLInputElement>) => <input type="password" {...props} />,
	}),
	Button: ({ htmlType, children, ...props }: React.ButtonHTMLAttributes<HTMLButtonElement> & { htmlType?: "button" | "submit" | "reset" }) => (
		<button type={htmlType ?? "button"} {...props}>
			{children}
		</button>
	),
}));

import { ResetPasswordModal } from "./reset-password-modal";

describe("reset password modal", () => {
	beforeEach(() => {
		mocks.fetchUpdatePlatformUser.mockReset();
		mocks.messageSuccess.mockReset();
		mocks.messageError.mockReset();
		mocks.onClose.mockReset();
		mocks.onSuccess.mockReset();
		mocks.form.setFieldsValue.mockReset();
		mocks.form.validateFields.mockReset();
		mocks.form.resetFields.mockReset();
	});

	it("generates the same password for both fields", async () => {
		mocks.form.validateFields.mockResolvedValue({
			password: "12345678Aa!",
			confirmPassword: "12345678Aa!",
		});

		render(
			<ResetPasswordModal
				open
				user={{ id: 1, username: "alice" } as any}
				onClose={mocks.onClose}
				onSuccess={mocks.onSuccess}
			/>,
		);

		await userEvent.click(screen.getByRole("button", { name: "随机生成" }));

		expect(mocks.form.setFieldsValue).toHaveBeenCalledWith(
			expect.objectContaining({
				password: expect.any(String),
				confirmPassword: expect.any(String),
			}),
		);
		const payload = mocks.form.setFieldsValue.mock.calls.at(-1)?.[0];
		expect(payload.password).toBe(payload.confirmPassword);
	});

	it("submits the reset password payload", async () => {
		mocks.form.validateFields.mockResolvedValue({
			password: "12345678Aa!",
			confirmPassword: "12345678Aa!",
		});
		const updatedUser = { id: 1, username: "alice" };
		mocks.fetchUpdatePlatformUser.mockResolvedValue(updatedUser);

		render(
			<ResetPasswordModal
				open
				user={{ id: 1, username: "alice" } as any}
				onClose={mocks.onClose}
				onSuccess={mocks.onSuccess}
			/>,
		);

		await userEvent.click(screen.getByRole("button", { name: "重置" }));

		await waitFor(() => {
			expect(mocks.fetchUpdatePlatformUser).toHaveBeenCalledWith(1, {
				password: "12345678Aa!",
			});
		});
		expect(mocks.messageSuccess).toHaveBeenCalledWith("密码重置成功");
		expect(mocks.onSuccess).toHaveBeenCalledWith(updatedUser);
		expect(mocks.onClose).toHaveBeenCalledTimes(1);
	});

	it("blocks submit when password confirmation does not match", async () => {
		mocks.form.validateFields.mockResolvedValue({
			password: "12345678Aa!",
			confirmPassword: "12345678Bb!",
		});

		render(
			<ResetPasswordModal
				open
				user={{ id: 1, username: "alice" } as any}
				onClose={mocks.onClose}
				onSuccess={mocks.onSuccess}
			/>,
		);

		await userEvent.click(screen.getByRole("button", { name: "重置" }));

		await waitFor(() => {
			expect(mocks.fetchUpdatePlatformUser).not.toHaveBeenCalled();
		});
		expect(mocks.messageError).toHaveBeenCalledWith("两次输入的密码不一致");
	});
});
