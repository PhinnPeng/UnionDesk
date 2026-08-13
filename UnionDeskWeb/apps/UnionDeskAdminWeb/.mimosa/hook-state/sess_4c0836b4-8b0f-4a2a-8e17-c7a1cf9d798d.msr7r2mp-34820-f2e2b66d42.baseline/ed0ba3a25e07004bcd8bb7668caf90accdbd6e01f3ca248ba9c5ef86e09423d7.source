import type { InputHTMLAttributes, ReactNode } from "react";

import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	fetchLoginConfig: vi.fn(),
	login: vi.fn(),
	navigate: vi.fn(),
	resetAuth: vi.fn(),
}));

vi.mock("#src/api/auth", () => ({
	fetchLoginConfig: mocks.fetchLoginConfig,
}));

vi.mock("#src/store/auth", () => {
	const useAuthStore = ((selector: (state: { login: typeof mocks.login }) => unknown) => selector({
		login: mocks.login,
	})) as {
		(selector: (state: { login: typeof mocks.login }) => unknown): unknown
		getState: () => { reset: typeof mocks.resetAuth }
	};

	useAuthStore.getState = () => ({
		reset: mocks.resetAuth,
	});

	return {
		useAuthStore,
	};
});

vi.mock("#src/store/user", () => ({
	useUserStore: {
		getState: () => ({
			platformAccess: false,
		}),
	},
}));

vi.mock("react-router", () => ({
	useNavigate: () => mocks.navigate,
	useSearchParams: () => [new URLSearchParams(), vi.fn()],
}));

vi.mock("react-i18next", () => ({
	useTranslation: () => ({
		t: (key: string) => key,
	}),
}));

vi.mock("./login-captcha", () => ({
	LoginCaptcha: ({
		disabled,
		enabled,
		hint,
	}: {
		disabled?: boolean
		enabled: boolean
		hint?: string
	}) => (
		<div
			data-disabled={String(Boolean(disabled))}
			data-enabled={String(enabled)}
			data-hint={hint ?? ""}
			data-testid="login-captcha"
		/>
	),
}));

vi.mock("antd", async () => {
	const Button = ({
		children,
		disabled,
		htmlType,
		loading,
		...rest
	}: {
		children?: ReactNode
		disabled?: boolean
		htmlType?: "button" | "submit" | "reset"
		loading?: boolean
		[key: string]: unknown
	}) => (
		<button
			disabled={Boolean(disabled || loading)}
			type={htmlType ?? "button"}
			{...rest}
		>
			{children}
		</button>
	);

	const Form = Object.assign(
		({
			children,
			onFinish,
		}: {
			children?: ReactNode
			onFinish?: (values: { username: string; password: string }) => void | Promise<void>
		}) => (
			<form
				onSubmit={(event) => {
					event.preventDefault();
					void onFinish?.({
						password: "admin123",
						username: "admin",
					});
				}}
			>
				{children}
			</form>
		),
		{
			Item: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
			useForm: () => [{}],
		},
	);

	const Input = Object.assign(
		(props: InputHTMLAttributes<HTMLInputElement>) => <input {...props} />,
		{
			Password: (props: InputHTMLAttributes<HTMLInputElement>) => <input type="password" {...props} />,
		},
	);

	return {
		Button,
		Form,
		Input,
		Space: ({ children }: { children?: ReactNode }) => <div>{children}</div>,
		message: {
			useMessage: () => [{
				destroy: vi.fn(),
				loading: vi.fn(),
			}, <div data-testid="message-holder" />],
		},
	};
});

import { PasswordLogin } from "./password-login";

describe("PasswordLogin", () => {
	beforeEach(() => {
		mocks.fetchLoginConfig.mockReset();
		mocks.login.mockReset();
		mocks.navigate.mockReset();
		mocks.resetAuth.mockReset();
		mocks.fetchLoginConfig.mockRejectedValue(new Error("network error"));
	});

	it("login-config failed 时仍展示滑块验证码并保持登录受控", async () => {
		render(<PasswordLogin />);

		await waitFor(() => {
			expect(screen.getByTestId("login-captcha")).toHaveAttribute("data-enabled", "true");
		});

		expect(screen.getByTestId("login-captcha")).toHaveAttribute("data-hint", "请拖动滑块完成验证");
		expect(screen.getByRole("button", { name: "authority.login" })).toBeDisabled();
	});
});
