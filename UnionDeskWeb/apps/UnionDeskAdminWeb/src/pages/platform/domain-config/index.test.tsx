import { App } from "antd";
import { render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import PlatformDomainConfig from "./index";

describe("PlatformDomainConfig", () => {
	it("renders domain config page", () => {
		render(
			<App>
				<PlatformDomainConfig />
			</App>,
		);

		expect(screen.getByText("域配置")).toBeInTheDocument();
		expect(screen.getByText("按业务域独立维护工作时间、时区和语言等基础配置")).toBeInTheDocument();
	});
});
