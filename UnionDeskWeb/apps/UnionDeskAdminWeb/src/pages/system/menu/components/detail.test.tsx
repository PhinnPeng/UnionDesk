import { render, screen, waitFor } from "@testing-library/react";
import type { ReactNode } from "react";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => {
	return {
		fetchAdminPermissionCodes: vi.fn(),
		fetchCreateMenu: vi.fn(),
		fetchUpdateMenu: vi.fn(),
		handleTree: vi.fn((value: unknown) => value),
	};
});

vi.mock("#src/api/platform/iam", () => ({
	fetchAdminPermissionCodes: () => mocks.fetchAdminPermissionCodes(),
}));

vi.mock("#src/api/system/menu", () => ({
	fetchCreateMenu: () => mocks.fetchCreateMenu(),
	fetchUpdateMenu: () => mocks.fetchUpdateMenu(),
}));

vi.mock("#src/utils/tree", () => ({
	handleTree: (value: unknown) => mocks.handleTree(value),
}));

vi.mock("react-i18next", () => ({
	initReactI18next: { type: "3rdParty", init: vi.fn() },
	useTranslation: () => ({
		t: (key: string, options?: Record<string, unknown>) => {
			if (key === "form.length") {
				return `form.length:${options?.length}`;
			}
			return key;
		},
	}),
}));

vi.mock("./component-registry", () => ({
	componentRegistry: [
		{ label: "system/menu", value: "system/menu" },
	],
	registeredComponentKeys: new Set(["system/menu"]),
}));

vi.mock("./menu-icon-picker", () => ({
	MenuIconPicker: ({ placeholder }: { placeholder?: string }) => <div data-testid="menu-icon-picker">{placeholder}</div>,
}));

vi.mock("antd", async () => {
	const actual = await vi.importActual<typeof import("antd")>("antd");
	const formInstance = {
		setFieldsValue: vi.fn(),
	};

	return {
		...actual,
		Alert: ({ message }: { message?: ReactNode }) => <div data-testid="alert">{message}</div>,
		Form: {
			useForm: () => [formInstance],
			useWatch: () => "menu",
		},
	};
});

vi.mock("@ant-design/pro-components", () => {
	const Field = ({
		children,
		colProps,
		labelCol,
		name,
		tooltip,
	}: {
		children?: ReactNode
		colProps?: Record<string, unknown>
		labelCol?: Record<string, unknown>
		name?: string
		tooltip?: ReactNode
	}) => (
		<div
			data-testid={`field-${name ?? "unknown"}`}
			data-tooltip={typeof tooltip === "string" ? tooltip : ""}
			data-col-props={colProps ? JSON.stringify(colProps) : ""}
			data-label-col={labelCol ? JSON.stringify(labelCol) : ""}
		>
			{children}
		</div>
	);

	return {
		ModalForm: ({ children, labelAlign, labelCol }: { children?: ReactNode, labelAlign?: string, labelCol?: Record<string, unknown> }) => (
			<div
				data-testid="modal-form"
				data-label-align={labelAlign}
				data-label-col={labelCol ? JSON.stringify(labelCol) : ""}
			>
				{children}
			</div>
		),
		ProForm: {
			Item: ({
				children,
				colProps,
				labelCol,
				name,
				tooltip,
			}: {
				children?: ReactNode
				colProps?: Record<string, unknown>
				labelCol?: Record<string, unknown>
				name?: string
				tooltip?: ReactNode
			}) => (
				<div
					data-testid={`field-${name ?? "unknown"}`}
					data-tooltip={typeof tooltip === "string" ? tooltip : ""}
					data-col-props={colProps ? JSON.stringify(colProps) : ""}
					data-label-col={labelCol ? JSON.stringify(labelCol) : ""}
				>
					{children}
				</div>
			),
		},
		ProFormCascader: Field,
		ProFormDependency: ({ children }: { children: (values: { nodeType: string }) => ReactNode }) => <>{children({ nodeType: "menu" })}</>,
		ProFormDigit: Field,
		ProFormRadio: {
			Group: Field,
		},
		ProFormSelect: Field,
		ProFormText: Field,
	};
});

import { Detail } from "./detail";

describe("menu detail form", () => {
	beforeEach(() => {
		mocks.fetchAdminPermissionCodes.mockResolvedValue([]);
		mocks.fetchCreateMenu.mockResolvedValue(undefined);
		mocks.fetchUpdateMenu.mockResolvedValue(undefined);
		mocks.handleTree.mockClear();
	});

	it("aligns labels to the left and shows help tooltips for every field", async () => {
		render(
			<Detail
				title="test"
				open
				flatParentMenus={[]}
				detailData={{}}
				onCloseChange={() => undefined}
			/>,
		);

		await waitFor(() => {
			expect(screen.getByTestId("modal-form")).toHaveAttribute("data-label-align", "left");
			expect(screen.getByTestId("field-nodeType")).toHaveAttribute("data-label-col", JSON.stringify({ span: 3 }));
			expect(screen.getByTestId("field-nodeType")).toHaveAttribute("data-col-props", JSON.stringify({ md: 24, xl: 24 }));
			expect(screen.getByTestId("field-parentId")).toHaveAttribute("data-label-col", JSON.stringify({ span: 3 }));
			expect(screen.getByTestId("field-parentId")).toHaveAttribute("data-col-props", JSON.stringify({ md: 24, xl: 24 }));
			expect(screen.getByTestId("field-nodeType")).toHaveAttribute("data-tooltip", "system.menu.menuTypeTooltip");
			expect(screen.getByTestId("field-parentId")).toHaveAttribute("data-tooltip", "system.menu.parentMenuTooltip");
			expect(screen.getByTestId("field-scope")).toHaveAttribute("data-tooltip", "system.menu.scopeTooltip");
			expect(screen.getByTestId("field-name")).toHaveAttribute("data-tooltip", "system.menu.nameTooltip");
			expect(screen.getByTestId("field-routePath")).toHaveAttribute("data-tooltip", "system.menu.routePathTooltip");
			expect(screen.getByTestId("field-orderNo")).toHaveAttribute("data-tooltip", "system.menu.menuOrderTooltip");
			expect(screen.getByTestId("field-icon")).toHaveAttribute("data-tooltip", "system.menu.menuIconTooltip");
			expect(screen.getByTestId("field-componentKey")).toHaveAttribute("data-tooltip", "system.menu.componentUrlTooltip");
			expect(screen.getByTestId("field-permissionCode")).toHaveAttribute("data-tooltip", "system.menu.permissionCodeTooltip");
			expect(screen.getByTestId("field-status")).toHaveAttribute("data-tooltip", "system.menu.statusTooltip");
			expect(screen.getByTestId("field-hidden")).toHaveAttribute("data-tooltip", "system.menu.hideInMenuTooltip");
		});
	});
});
