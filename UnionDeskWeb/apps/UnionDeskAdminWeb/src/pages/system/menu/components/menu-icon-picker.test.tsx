import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { describe, expect, it, vi } from "vitest";

import { MenuIconPicker } from "./menu-icon-picker";

const storeState = {
	onIconChange: undefined as undefined | ((icon: { componentName: string }) => void),
};

vi.mock("antd", async () => {
	const actual = await vi.importActual<typeof import("antd")>("antd");
	return {
		...actual,
		Popover: ({
			children,
			content,
			open,
			onOpenChange,
		}: {
			children: ReactNode
			content: ReactNode
			onOpenChange?: (open: boolean) => void
			open?: boolean
		}) => (
			<div>
				<div data-testid="popover-trigger" onClick={() => onOpenChange?.(!open)}>
					{children}
				</div>
				{open ? <div data-testid="popover-content">{content}</div> : null}
			</div>
		),
	};
});

vi.mock("@ant-design/pro-editor/es/IconPicker/store", () => ({
	Provider: ({ children }: { children: ReactNode }) => <>{children}</>,
	createStore: () => ({}) as never,
}));

vi.mock("@ant-design/pro-editor/es/IconPicker/container/StoreUpdater", () => ({
	default: ({ onIconChange }: {
		onIconChange?: (icon: { componentName: string }) => void
	}) => {
		storeState.onIconChange = onIconChange;
		return null;
	},
}));

vi.mock("@ant-design/pro-editor/es/IconPicker/features/PickerPanel", () => ({
	default: () => (
		<button
			type="button"
			onClick={() => storeState.onIconChange?.({ componentName: "SettingOutlined" })}
		>
			picker-panel
		</button>
	),
}));

describe("MenuIconPicker", () => {
	it("uses an input trigger and closes after selecting an icon", async () => {
		const onChange = vi.fn();

		render(<MenuIconPicker value="HomeOutlined" onChange={onChange} placeholder="请选择图标" />);

		const trigger = screen.getByRole("textbox");
		expect(trigger).toHaveValue("HomeOutlined");

		fireEvent.click(trigger);

		expect(screen.getByTestId("popover-content")).toBeInTheDocument();
		expect(await screen.findByRole("button", { name: "picker-panel" })).toBeInTheDocument();

		fireEvent.click(screen.getByRole("button", { name: "picker-panel" }));

		await waitFor(() => {
			expect(onChange).toHaveBeenCalledWith("SettingOutlined");
		});
		await waitFor(() => {
			expect(screen.queryByTestId("popover-content")).not.toBeInTheDocument();
		});
	});
});
