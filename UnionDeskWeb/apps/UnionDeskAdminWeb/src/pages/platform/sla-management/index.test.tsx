import { App } from "antd";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import PlatformSlaManagement from "./index";

describe("PlatformSlaManagement", () => {
	it("renders SLA management page", () => {
		render(
			<App>
				<PlatformSlaManagement />
			</App>,
		);

		expect(screen.getByText("SLA 管理")).toBeInTheDocument();
		expect(screen.getByText("规则、日历与优先级关联统一维护")).toBeInTheDocument();
	});
});
