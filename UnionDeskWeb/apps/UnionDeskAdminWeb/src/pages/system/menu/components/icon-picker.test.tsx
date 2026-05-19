import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

import { IconPicker } from "./icon-picker";

vi.mock("@iconify/react/offline", () => ({
	Icon: ({ icon }: { icon: string }) => <span data-testid="icon">{icon}</span>,
	addCollection: vi.fn(),
}));

describe("IconPicker", () => {
	beforeEach(() => {
		const fetchMock = vi.fn(async () => {
			throw new Error("网络已禁用");
		});
		vi.stubGlobal("fetch", fetchMock);
	});

	afterEach(() => {
		vi.unstubAllGlobals();
	});

	it("keeps search results and pagination visible when the iconify api is unavailable", async () => {
		const user = userEvent.setup();

		render(<IconPicker />);

		await user.click(screen.getByText("选择图标"));
		const input = await screen.findByPlaceholderText("搜索图标，例如：home、user、setting");
		await user.type(input, "home");

		await waitFor(() => expect(screen.getAllByTestId("icon").length).toBeGreaterThan(0));
		expect(document.querySelector(".ant-pagination")).not.toBeNull();
	});

	it("keeps browse results and pagination visible when the iconify api is unavailable", async () => {
		const user = userEvent.setup();

		render(<IconPicker />);

		await user.click(screen.getByText("选择图标"));
		await user.click(screen.getByRole("tab", { name: "分类浏览" }));

		await waitFor(() => expect(screen.getAllByTestId("icon").length).toBeGreaterThan(0));
		expect(document.querySelector(".ant-pagination")).not.toBeNull();
	});
});
