import { render, screen, waitFor } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => {
	return {
		onChange: vi.fn(),
	};
});

vi.mock("react-i18next", () => ({
	initReactI18next: { type: "3rdParty", init: vi.fn() },
	useTranslation: () => ({
		t: (key: string) => key,
	}),
}));

vi.mock("antd", async () => {
	const actual = await vi.importActual<typeof import("antd")>("antd");

	return {
		...actual,
		Checkbox: {
			Group: ({ options, value }: { options?: Array<{ label: string, value: string }>, value?: string[] }) => (
				<div
					data-testid="checkbox-group"
					data-value={JSON.stringify(value ?? [])}
					data-options={JSON.stringify(options ?? [])}
				/>
			),
		},
		Input: {
			Search: ({ value }: { value?: string }) => <input data-testid="search" value={value ?? ""} readOnly />,
		},
		Tree: ({ checkedKeys }: { checkedKeys?: React.Key[] }) => (
			<div data-testid="tree" data-checked={JSON.stringify(checkedKeys ?? [])} />
		),
	};
});

import { FormTreeItem } from "./form-tree-item";

describe("FormTreeItem", () => {
	beforeEach(() => {
		mocks.onChange.mockReset();
	});

	it("keeps the initial checked value instead of clearing it on mount", async () => {
		render(
			<FormTreeItem
				treeData={[
					{
						id: "41",
						title: "platform-ticket-detail",
						children: [],
					},
				]}
				value={["41"]}
				onChange={mocks.onChange}
			/>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("tree")).toHaveAttribute("data-checked", JSON.stringify(["41"]));
		});

		expect(mocks.onChange).not.toHaveBeenCalled();
	});
});
