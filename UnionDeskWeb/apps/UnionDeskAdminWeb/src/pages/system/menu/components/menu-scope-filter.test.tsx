import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

vi.mock("react-i18next", () => ({
	initReactI18next: { type: "3rdParty", init: vi.fn() },
	useTranslation: () => ({
		t: (key: string) => key,
	}),
}));

import { MenuScopeFilter } from "./menu-scope-filter";

describe("MenuScopeFilter", () => {
	it("renders two scope options and forwards changes", () => {
		const onChange = vi.fn();

		render(
			<MenuScopeFilter
				value="platform"
				onChange={onChange}
			/>,
		);

		fireEvent.click(screen.getByText("system.menu.businessScope"));
		expect(onChange).toHaveBeenCalledWith("business");
		expect(screen.getByText("system.menu.platformScope")).toBeInTheDocument();
	});
});
