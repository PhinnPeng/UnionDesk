import { App } from "antd";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import PlatformSystemSettings from "./index";

describe("PlatformSystemSettings", () => {
	it("renders system settings page", () => {
		render(
			<App>
				<PlatformSystemSettings />
			</App>,
		);

		expect(screen.getByText("系统设置")).toBeInTheDocument();
		expect(screen.getByText("密码策略、会话超时和安全参数统一维护")).toBeInTheDocument();
	});
});
