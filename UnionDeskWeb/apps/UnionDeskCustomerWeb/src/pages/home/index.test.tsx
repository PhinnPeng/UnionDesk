import { fireEvent, render, screen } from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

import HomePage from "./index";

const mockNavigate = vi.fn();

const openTicket = {
	id: 1,
	ticketNo: "T-2026-0001",
	status: "open",
	title: "示例工单",
	updatedAt: "2026-05-07T09:00:00.000Z",
};

const portalState = {
	activeDomain: {
		id: 1,
		name: "默认业务域",
		description: "后端当前默认接入的演示业务域，适合展示工单创建和流转。",
	},
	currentDomainTickets: [openTicket],
	unreadCount: 1,
	inboxMessages: [
		{
			id: 1,
			title: "系统通知",
			content: "测试消息",
			createdAt: "2026-05-07T09:00:00.000Z",
			isRead: false,
		},
	],
	ticketTypes: [{ id: "1", name: "故障" }],
	uploadAttachment: vi.fn(),
	createTicket: vi.fn(),
	withdrawTicket: vi.fn(),
	markInboxRead: vi.fn(),
};

vi.mock("react-router-dom", async () => {
	const actual = await vi.importActual<typeof import("react-router-dom")>("react-router-dom");
	return {
		...actual,
		useNavigate: () => mockNavigate,
	};
});

vi.mock("@uniondesk/shared", () => ({
	useCustomerPortal: () => portalState,
}));

describe("HomePage - 工作台页面", () => {
	beforeEach(() => {
		mockNavigate.mockClear();
		portalState.uploadAttachment.mockClear();
		portalState.createTicket.mockClear();
		portalState.withdrawTicket.mockClear();
		portalState.markInboxRead.mockClear();
	});

	it("TC-013: 应该渲染工作台页面", () => {
		render(
			<BrowserRouter>
				<HomePage />
			</BrowserRouter>,
		);

		expect(screen.getByText("提交工单", { selector: ".ant-card-head-title" })).toBeInTheDocument();
		expect(screen.getByText("我的工单")).toBeInTheDocument();
		expect(screen.getByText("最新通知")).toBeInTheDocument();
	});

	it("TC-013: 应该显示当前业务域信息", () => {
		render(
			<BrowserRouter>
				<HomePage />
			</BrowserRouter>,
		);

		expect(screen.getByText("当前业务域")).toBeInTheDocument();
		expect(screen.getByText("默认业务域")).toBeInTheDocument();
	});

	it("TC-013: 应该显示统计数据", () => {
		render(
			<BrowserRouter>
				<HomePage />
			</BrowserRouter>,
		);

		expect(screen.getByText("待处理工单")).toBeInTheDocument();
		expect(screen.getByText("未读通知")).toBeInTheDocument();
	});

	it("TC-013: 应该渲染提单表单", () => {
		render(
			<BrowserRouter>
				<HomePage />
			</BrowserRouter>,
		);

		expect(screen.getByText("工单类型")).toBeInTheDocument();
		expect(screen.getByText("标题")).toBeInTheDocument();
		expect(screen.getByText("详细说明")).toBeInTheDocument();
		expect(screen.getByText("附件")).toBeInTheDocument();
	});

	it("TC-013: 提单表单应该验证必填字段", () => {
		render(
			<BrowserRouter>
				<HomePage />
			</BrowserRouter>,
		);

		const titleInput = screen.getByPlaceholderText("简要描述你的问题");
		const descriptionInput = screen.getByPlaceholderText("请尽量写清楚业务背景、问题现象和期望结果");

		expect(titleInput).toHaveAttribute("aria-required", "true");
		expect(descriptionInput).toHaveAttribute("aria-required", "true");
	});

	it("TC-015: 应该显示我的工单列表", () => {
		render(
			<BrowserRouter>
				<HomePage />
			</BrowserRouter>,
		);

		expect(screen.getByText("我的工单")).toBeInTheDocument();
	});

	it("TC-015: 应该能够查看工单详情", () => {
		render(
			<BrowserRouter>
				<HomePage />
			</BrowserRouter>,
		);

		const viewLinks = screen.getAllByRole("link", { name: "查看" });
		expect(viewLinks[0]).toHaveAttribute("href", "/tickets/1");
	});

	it("TC-015: 应该能够撤回待受理工单", async () => {
		render(
			<BrowserRouter>
				<HomePage />
			</BrowserRouter>,
		);

		fireEvent.click(screen.getByRole("button", { name: "撤回" }));

		expect(portalState.withdrawTicket).toHaveBeenCalledWith(1);
	});

	it("TC-048: 应该显示最新通知预览", () => {
		render(
			<BrowserRouter>
				<HomePage />
			</BrowserRouter>,
		);

		expect(screen.getByText("最新通知")).toBeInTheDocument();
	});
});
