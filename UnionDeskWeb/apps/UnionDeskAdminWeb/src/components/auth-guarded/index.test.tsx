import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("#src/hooks/use-auth", () => ({
	useAuth: () => ({
		hasPermission: (code?: string | null) => code === "platform.menu.create",
	}),
}));

import { AuthGuarded } from "./index";

describe("AuthGuarded", () => {
	it("renders children when permission is granted", () => {
		render(
			<AuthGuarded auth="platform.menu.create">
				<button type="button">新增菜单</button>
			</AuthGuarded>,
		);

		expect(screen.getByRole("button", { name: "新增菜单" })).toBeTruthy();
	});

	it("renders fallback when permission is missing", () => {
		render(
			<AuthGuarded auth="platform.menu.delete" fallback={<span>无权限</span>}>
				<button type="button">删除菜单</button>
			</AuthGuarded>,
		);

		expect(screen.getByText("无权限")).toBeTruthy();
		expect(screen.queryByRole("button", { name: "删除菜单" })).toBeNull();
	});
});
