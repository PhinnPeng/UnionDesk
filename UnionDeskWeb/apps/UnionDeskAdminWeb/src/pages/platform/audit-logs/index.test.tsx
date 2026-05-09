import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import { App } from "antd";

vi.mock("#src/api/platform/domain", () => ({
	fetchBusinessDomains: vi.fn().mockResolvedValue([
		{ id: 1, code: "default", name: "默认业务域" },
	]),
}));

vi.mock("#src/api/platform/audit", () => ({
	fetchPlatformAuditLogs: vi.fn().mockResolvedValue({ total: 0, list: [] }),
	fetchLoginLogsPage: vi.fn().mockResolvedValue({ total: 0, list: [] }),
}));

import PlatformAuditLogs from "./index";

describe("PlatformAuditLogs", () => {
	it("renders audit log page title", () => {
		render(
			<App>
				<PlatformAuditLogs />
			</App>,
		);

		expect(screen.getByText("审计日志")).toBeInTheDocument();
		expect(screen.getByText("平台与登录日志统一查看")).toBeInTheDocument();
	});
});

