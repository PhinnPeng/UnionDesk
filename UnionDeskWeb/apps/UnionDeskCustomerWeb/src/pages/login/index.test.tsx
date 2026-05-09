import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import LoginPage from "./index";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
	const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
	return {
		...actual,
		useNavigate: () => mockNavigate,
	};
});

describe("LoginPage - 客户登录/注册页面", () => {
	beforeEach(() => {
		mockNavigate.mockClear();
	});

	it("TC-002: 应该渲染登录表单", () => {
		render(
			<BrowserRouter>
				<LoginPage />
			</BrowserRouter>,
		);

		expect(screen.getByText("登录")).toBeInTheDocument();
		expect(screen.getByPlaceholderText("请输入登录名")).toBeInTheDocument();
		expect(screen.getByPlaceholderText("请输入密码")).toBeInTheDocument();
	});

	it("TC-002: 应该能够切换到注册表单", async () => {
		render(
			<BrowserRouter>
				<LoginPage />
			</BrowserRouter>,
		);

		fireEvent.click(screen.getByRole("tab", { name: "注册" }));

		expect(screen.getByPlaceholderText("创建登录名")).toBeInTheDocument();
		expect(screen.getByPlaceholderText("客户姓名或企业名称")).toBeInTheDocument();
		expect(screen.getByPlaceholderText("请输入手机号")).toBeInTheDocument();
	});

	it("TC-002: 应该能够使用演示账号登录", async () => {
		render(
			<BrowserRouter>
				<LoginPage />
			</BrowserRouter>,
		);

		const loginNameInput = screen.getByPlaceholderText("请输入登录名");
		const passwordInput = screen.getByPlaceholderText("请输入密码");
		const submitButton = screen.getByRole("button", { name: /^登录并进入业务域$/ });

		fireEvent.change(loginNameInput, { target: { value: "customer" } });
		fireEvent.change(passwordInput, { target: { value: "customer123" } });
		fireEvent.click(submitButton);

		await waitFor(() => {
			expect(mockNavigate).toHaveBeenCalledWith("/domains", { replace: true });
		});
	});

	it("TC-003: 应该能够注册新账号", async () => {
		render(
			<BrowserRouter>
				<LoginPage />
			</BrowserRouter>,
		);

		fireEvent.click(screen.getByRole("tab", { name: "注册" }));

		const loginNameInput = screen.getByPlaceholderText("创建登录名");
		const displayNameInput = screen.getByPlaceholderText("客户姓名或企业名称");
		const phoneInput = screen.getByPlaceholderText("请输入手机号");
		const passwordInput = screen.getByPlaceholderText("设置登录密码");
		const submitButton = screen.getByRole("button", { name: /注册并开始使用/ });

		fireEvent.change(loginNameInput, { target: { value: "newcustomer" } });
		fireEvent.change(displayNameInput, { target: { value: "新客户" } });
		fireEvent.change(phoneInput, { target: { value: "13900000001" } });
		fireEvent.change(passwordInput, { target: { value: "password123" } });
		fireEvent.click(submitButton);

		await waitFor(() => {
			expect(mockNavigate).toHaveBeenCalledWith("/domains", { replace: true });
		});
	});

	it("TC-003: 注册时应该验证必填字段", async () => {
		render(
			<BrowserRouter>
				<LoginPage />
			</BrowserRouter>,
		);

		fireEvent.click(screen.getByRole("tab", { name: "注册" }));

		expect(screen.getByPlaceholderText("创建登录名")).toHaveAttribute("aria-required", "true");
		expect(screen.getByPlaceholderText("设置登录密码")).toHaveAttribute("aria-required", "true");
	});

	it("TC-003: 注册时应该支持邀请码入域", async () => {
		render(
			<BrowserRouter>
				<LoginPage />
			</BrowserRouter>,
		);

		fireEvent.click(screen.getByRole("tab", { name: "注册" }));

		const invitationCodeInput = screen.getByPlaceholderText("可选，填入邀请码直接入域");
		fireEvent.change(invitationCodeInput, { target: { value: "INV-0001" } });

		expect(invitationCodeInput).toHaveValue("INV-0001");
	});
});
