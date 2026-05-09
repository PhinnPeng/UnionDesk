import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { BrowserRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import PortalShell from "./PortalShell";

const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
	const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
	return {
		...actual,
		useNavigate: () => mockNavigate,
		useLocation: () => ({ pathname: "/workspace" }),
	};
});

describe("PortalShell - 客户端外壳组件", () => {
	beforeEach(() => {
		mockNavigate.mockClear();
	});

	it("应该渲染客户端标题", () => {
		render(
			<BrowserRouter>
				<PortalShell />
			</BrowserRouter>,
		);

		expect(screen.getByText("UnionDesk 客户端")).toBeInTheDocument();
		expect(screen.getByText("客户注册、入域、提单与消息闭环")).toBeInTheDocument();
	});

	it("应该显示导航菜单", () => {
		render(
			<BrowserRouter>
				<PortalShell />
			</BrowserRouter>,
		);

		expect(screen.getByText("工作台")).toBeInTheDocument();
		expect(screen.getByText("业务域")).toBeInTheDocument();
		expect(screen.getByText("通知")).toBeInTheDocument();
		expect(screen.getByText("退出")).toBeInTheDocument();
	});

	it("应该显示当前账号信息", () => {
		render(
			<BrowserRouter>
				<PortalShell />
			</BrowserRouter>,
		);

		expect(screen.getByText(/当前账号/)).toBeInTheDocument();
		expect(screen.getByText(/当前业务域/)).toBeInTheDocument();
		expect(screen.getByText(/未读通知/)).toBeInTheDocument();
	});

	it("应该能够导航到工作台", async () => {
		const user = userEvent.setup();
		render(
			<BrowserRouter>
				<PortalShell />
			</BrowserRouter>,
		);

		await user.click(screen.getByRole("button", { name: /工作台/ }));

		await waitFor(() => {
			expect(mockNavigate).toHaveBeenCalledWith("/workspace");
		});
	});

	it("应该能够导航到业务域", async () => {
		const user = userEvent.setup();
		render(
			<BrowserRouter>
				<PortalShell />
			</BrowserRouter>,
		);

		await user.click(screen.getByRole("button", { name: /业务域/ }));

		await waitFor(() => {
			expect(mockNavigate).toHaveBeenCalledWith("/domains");
		});
	});

	it("应该能够导航到通知中心", async () => {
		const user = userEvent.setup();
		render(
			<BrowserRouter>
				<PortalShell />
			</BrowserRouter>,
		);

		await user.click(screen.getByRole("button", { name: /通知$/ }));

		await waitFor(() => {
			expect(mockNavigate).toHaveBeenCalledWith("/inbox");
		});
	});

	it("应该能够退出登录", async () => {
		const user = userEvent.setup();
		render(
			<BrowserRouter>
				<PortalShell />
			</BrowserRouter>,
		);

		await user.click(screen.getByRole("button", { name: /退出$/ }));

		await waitFor(() => {
			expect(mockNavigate).toHaveBeenCalledWith("/login", { replace: true });
		});
	});
});
