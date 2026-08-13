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
		Tree: ({ checkedKeys, checkStrictly }: { checkedKeys?: React.Key[] | { checked: React.Key[], halfChecked: React.Key[] }, checkStrictly?: boolean }) => (
			<div
				data-testid="tree"
				data-checked={JSON.stringify(checkedKeys ?? [])}
				data-check-strictly={String(!!checkStrictly)}
			/>
		),
	};
});

import { FormTreeItem, applyStrictCascadeToggle, computeStrictTreeCheckedKeys } from "./form-tree-item";

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

	it("uses checkStrictly checkedKeys shape when enabled", async () => {
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
				checkStrictly
				onChange={mocks.onChange}
			/>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("tree")).toHaveAttribute(
				"data-checked",
				JSON.stringify({ checked: ["41"], halfChecked: [] }),
			);
		});
		expect(screen.getByTestId("tree")).toHaveAttribute("data-check-strictly", "true");
	});

	it("shows parent halfChecked when only some children are selected", async () => {
		render(
			<FormTreeItem
				treeData={[
					{
						id: "parent",
						title: "parent",
						children: [
							{ id: "child-a", title: "child-a" },
							{ id: "child-b", title: "child-b" },
						],
					},
				]}
				value={["child-a"]}
				checkStrictly
				onChange={mocks.onChange}
			/>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("tree")).toHaveAttribute(
				"data-checked",
				JSON.stringify({ checked: ["child-a"], halfChecked: ["parent"] }),
			);
		});
	});

	it("shows parent halfChecked when parent is selected but not all children", async () => {
		render(
			<FormTreeItem
				treeData={[
					{
						id: "parent",
						title: "parent",
						children: [
							{ id: "child-a", title: "child-a" },
							{ id: "child-b", title: "child-b" },
						],
					},
				]}
				value={["parent", "child-a"]}
				checkStrictly
				onChange={mocks.onChange}
			/>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("tree")).toHaveAttribute(
				"data-checked",
				JSON.stringify({ checked: ["child-a"], halfChecked: ["parent"] }),
			);
		});
	});
});

describe("computeStrictTreeCheckedKeys", () => {
	const tree = [
		{
			id: "root",
			title: "root",
			children: [
				{
					id: "group",
					title: "group",
					children: [
						{ id: "leaf-a", title: "leaf-a" },
						{ id: "leaf-b", title: "leaf-b" },
					],
				},
				{ id: "leaf-c", title: "leaf-c" },
			],
		},
	];

	it("marks ancestors halfChecked when subtree is partially selected", () => {
		expect(computeStrictTreeCheckedKeys(tree, ["leaf-a", "leaf-c"])).toEqual({
			checked: ["leaf-a", "leaf-c"],
			halfChecked: ["group", "root"],
		});
	});

	it("marks fully selected subtree nodes as checked", () => {
		expect(computeStrictTreeCheckedKeys(tree, ["root", "group", "leaf-a", "leaf-b", "leaf-c"])).toEqual({
			checked: ["leaf-a", "leaf-b", "group", "leaf-c", "root"],
			halfChecked: [],
		});
	});
});

describe("applyStrictCascadeToggle", () => {
	const tree = [
		{
			id: "parent",
			title: "parent",
			children: [
				{ id: "child-a", title: "child-a" },
				{
					id: "child-b",
					title: "child-b",
					children: [{ id: "grandchild", title: "grandchild" }],
				},
			],
		},
	];

	it("checks parent and all descendants when parent is checked", () => {
		expect(applyStrictCascadeToggle(tree, [], "parent", true)).toEqual([
			"parent",
			"child-a",
			"child-b",
			"grandchild",
		]);
	});

	it("unchecks parent and all descendants when parent is unchecked", () => {
		expect(
			applyStrictCascadeToggle(
				tree,
				["parent", "child-a", "child-b", "grandchild"],
				"parent",
				false,
			),
		).toEqual([]);
	});

	it("unchecks parent when all children are selected but parent id is not stored", () => {
		expect(
			applyStrictCascadeToggle(
				tree,
				["child-a", "child-b", "grandchild"],
				"parent",
				false,
			),
		).toEqual([]);
	});

	it("checks child subtree when a child is checked", () => {
		expect(applyStrictCascadeToggle(tree, [], "child-b", true)).toEqual(["child-b", "grandchild"]);
	});

	it("unchecks child subtree when a child is unchecked", () => {
		expect(
			applyStrictCascadeToggle(tree, ["parent", "child-a", "child-b", "grandchild"], "child-b", false),
		).toEqual(["child-a"]);
	});

	it("auto-checks ancestor when all sibling subtrees become selected", () => {
		const multiLevelTree = [
			{
				id: "root",
				title: "root",
				children: [
					{
						id: "group-a",
						title: "group-a",
						children: [
							{ id: "leaf-a1", title: "leaf-a1" },
							{ id: "leaf-a2", title: "leaf-a2" },
						],
					},
					{
						id: "group-b",
						title: "group-b",
						children: [
							{ id: "leaf-b1", title: "leaf-b1" },
							{ id: "leaf-b2", title: "leaf-b2" },
						],
					},
				],
			},
		];

		expect(applyStrictCascadeToggle(multiLevelTree, ["leaf-b1", "leaf-b2"], "group-a", true)).toEqual(
			expect.arrayContaining(["group-a", "leaf-a1", "leaf-a2", "group-b", "leaf-b1", "leaf-b2", "root"]),
		);
		expect(applyStrictCascadeToggle(multiLevelTree, ["leaf-b1", "leaf-b2"], "group-a", true)).toHaveLength(7);
	});

	it("does not auto-check ancestor when sibling subtree is incomplete", () => {
		const multiLevelTree = [
			{
				id: "root",
				title: "root",
				children: [
					{
						id: "group-a",
						title: "group-a",
						children: [
							{ id: "leaf-a1", title: "leaf-a1" },
							{ id: "leaf-a2", title: "leaf-a2" },
						],
					},
					{
						id: "group-b",
						title: "group-b",
						children: [
							{ id: "leaf-b1", title: "leaf-b1" },
							{ id: "leaf-b2", title: "leaf-b2" },
						],
					},
				],
			},
		];

		expect(applyStrictCascadeToggle(multiLevelTree, ["leaf-b1"], "group-a", true)).toEqual(
			expect.arrayContaining(["leaf-b1", "group-a", "leaf-a1", "leaf-a2"]),
		);
		expect(applyStrictCascadeToggle(multiLevelTree, ["leaf-b1"], "group-a", true)).toHaveLength(4);
	});

	it("removes ancestor when uncheck breaks full sibling selection", () => {
		const multiLevelTree = [
			{
				id: "root",
				title: "root",
				children: [
					{
						id: "group-a",
						title: "group-a",
						children: [
							{ id: "leaf-a1", title: "leaf-a1" },
							{ id: "leaf-a2", title: "leaf-a2" },
						],
					},
					{
						id: "group-b",
						title: "group-b",
						children: [
							{ id: "leaf-b1", title: "leaf-b1" },
							{ id: "leaf-b2", title: "leaf-b2" },
						],
					},
				],
			},
		];

		expect(
			applyStrictCascadeToggle(
				multiLevelTree,
				["root", "group-a", "leaf-a1", "leaf-a2", "group-b", "leaf-b1", "leaf-b2"],
				"group-a",
				false,
			),
		).toEqual(expect.arrayContaining(["leaf-b1", "leaf-b2", "group-b"]));
		expect(
			applyStrictCascadeToggle(
				multiLevelTree,
				["root", "group-a", "leaf-a1", "leaf-a2", "group-b", "leaf-b1", "leaf-b2"],
				"group-a",
				false,
			),
		).toHaveLength(3);
	});
});
