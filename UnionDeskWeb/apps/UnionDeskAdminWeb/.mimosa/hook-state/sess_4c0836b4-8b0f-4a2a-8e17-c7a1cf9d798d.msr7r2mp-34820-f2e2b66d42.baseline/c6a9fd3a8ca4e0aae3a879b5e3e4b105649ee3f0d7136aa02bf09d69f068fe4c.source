import type React from "react";
import type { PlatformOrganizationView } from "@uniondesk/shared";

import { isValidElement } from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

type MockTreeNode = {
	key: React.Key;
	title: React.ReactNode;
	children?: MockTreeNode[];
};

const mocks = vi.hoisted(() => {
	return {
		treeProps: undefined as
			| undefined
			| {
				treeData?: MockTreeNode[];
				selectedKeys?: React.Key[];
				expandedKeys?: React.Key[];
				autoExpandParent?: boolean;
				onSelect?: (keys: React.Key[]) => void;
			},
	};
});

vi.mock("antd", () => ({
	Card: ({ title, extra, children }: { title?: React.ReactNode; extra?: React.ReactNode; children: React.ReactNode }) => (
		<section>
			<header>{title}{extra}</header>
			{children}
		</section>
	),
	Button: ({ children, onClick }: { children: React.ReactNode; onClick?: () => void }) => (
		<button type="button" onClick={onClick}>
			{children}
		</button>
	),
	Input: (props: React.InputHTMLAttributes<HTMLInputElement>) => <input {...props} />,
	Space: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	Tooltip: ({ children }: { children: React.ReactNode }) => <>{children}</>,
	Tree: (props: {
		treeData?: MockTreeNode[];
		selectedKeys?: React.Key[];
		expandedKeys?: React.Key[];
		autoExpandParent?: boolean;
		onSelect?: (keys: React.Key[]) => void;
	}) => {
		mocks.treeProps = props;

		const renderNodes = (nodes: MockTreeNode[]) => nodes.map(node => (
			<div key={String(node.key)} data-testid={`tree-node-${String(node.key)}`}>
				<button type="button" onClick={() => props.onSelect?.([node.key])}>
					{node.title}
				</button>
				{node.children?.length ? <div>{renderNodes(node.children)}</div> : null}
			</div>
		));

		return <div data-testid="dept-tree">{renderNodes(props.treeData ?? [])}</div>;
	},
}));

import { ALL_DEPARTMENTS_TREE_KEY, DeptTreeBlock } from "./dept-tree-block";

function getNodeText(node: React.ReactNode): string {
	if (typeof node === "string" || typeof node === "number") {
		return String(node);
	}
	if (Array.isArray(node)) {
		return node.map(getNodeText).join("");
	}
	if (isValidElement(node)) {
		const element = node as React.ReactElement<{ children?: React.ReactNode }>;
		return getNodeText(element.props.children);
	}
	return "";
}

const organizations: PlatformOrganizationView[] = [
	{
		id: 10,
		code: "root",
		name: "平台组织",
		parentId: null,
		parentName: null,
		leaderUserId: null,
		leaderName: null,
		orderNo: 1,
		status: 1,
		remark: null,
		createdAt: "2026-05-01T00:00:00",
	},
	{
		id: 11,
		code: "ops",
		name: "运营中心",
		parentId: 10,
		parentName: "平台组织",
		leaderUserId: null,
		leaderName: null,
		orderNo: 1,
		status: 1,
		remark: null,
		createdAt: "2026-05-02T00:00:00",
	},
	{
		id: 12,
		code: "support",
		name: "客户支持",
		parentId: null,
		parentName: null,
		leaderUserId: null,
		leaderName: null,
		orderNo: 2,
		status: 1,
		remark: null,
		createdAt: "2026-05-03T00:00:00",
	},
];

describe("DeptTreeBlock", () => {
	beforeEach(() => {
		mocks.treeProps = undefined;
	});

	it("wraps top-level organizations under the virtual all-departments root", async () => {
		const onSelect = vi.fn();

		render(
			<DeptTreeBlock
				organizations={organizations}
				selectedDepartmentId={null}
				onSelect={onSelect}
			/>,
		);

		await waitFor(() => {
			expect(mocks.treeProps).toBeDefined();
		});

		expect(mocks.treeProps?.selectedKeys).toEqual([ALL_DEPARTMENTS_TREE_KEY]);
		expect(mocks.treeProps?.expandedKeys).toEqual([ALL_DEPARTMENTS_TREE_KEY]);

		const rootNode = mocks.treeProps?.treeData?.[0];
		expect(rootNode?.key).toBe(ALL_DEPARTMENTS_TREE_KEY);
		expect(getNodeText(rootNode?.title)).toBe("所有部门");
		expect(rootNode?.children?.map(node => getNodeText(node.title))).toEqual([
			"平台组织",
			"客户支持",
		]);
		expect(getNodeText(rootNode?.children?.[0]?.children?.[0]?.title)).toBe("运营中心");

		await userEvent.click(screen.getByRole("button", { name: "所有部门" }));

		expect(onSelect).toHaveBeenCalledWith(null);
	});
});
