import type React from "react";

import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => {
	return {
		fetchAddRole: vi.fn(),
		fetchUpdateRole: vi.fn(),
		fetchUpdateRolePermissions: vi.fn(),
		messageSuccess: vi.fn(),
		onCloseChange: vi.fn(),
		refreshTable: vi.fn(),
	};
});

vi.mock("react-i18next", () => ({
	initReactI18next: { type: "3rdParty", init: vi.fn() },
	useTranslation: () => ({
		t: (key: string) => key,
	}),
}));

vi.mock("#src/api/system/role", () => ({
	fetchAddRole: mocks.fetchAddRole,
	fetchUpdateRole: mocks.fetchUpdateRole,
	fetchUpdateRolePermissions: mocks.fetchUpdateRolePermissions,
}));

vi.mock("#src/components/basic-form", () => ({
	FormTreeItem: () => <div data-testid="form-tree" />,
}));

vi.mock("antd", () => ({
	App: {
		useApp: () => ({
			message: {
				success: mocks.messageSuccess,
			},
		}),
	},
	Form: {
		useForm: () => [{
			setFieldsValue: vi.fn(),
		}],
		Item: ({ children }: { children: React.ReactNode }) => <div>{children}</div>,
	},
}));

vi.mock("@ant-design/pro-components", () => ({
	DrawerForm: ({ children, onFinish }: { children: React.ReactNode, onFinish: (values: unknown) => Promise<boolean> }) => (
		<form
			onSubmit={(event) => {
				event.preventDefault();
				void onFinish({
					name: "测试角色",
					code: "custom_test",
					scope: "domain",
					menus: ["1", "2", "99"],
				});
			}}
		>
			{children}
			<button type="submit">提交</button>
		</form>
	),
	ProFormRadio: {
		Group: () => <div data-testid="scope" />,
	},
	ProFormText: () => <input />,
}));

import { Detail } from "./detail";

describe("Role Detail", () => {
	beforeEach(() => {
		mocks.fetchAddRole.mockReset();
		mocks.fetchUpdateRole.mockReset();
		mocks.fetchUpdateRolePermissions.mockReset();
		mocks.messageSuccess.mockReset();
		mocks.onCloseChange.mockReset();
		mocks.refreshTable.mockReset();
	});

	it("shows success and reloads list after updating a role", async () => {
		mocks.fetchUpdateRole.mockResolvedValue({ id: 9 });
		mocks.fetchUpdateRolePermissions.mockResolvedValue({ roleId: 9, menuIds: [1], buttonIds: [2] });
		mocks.refreshTable.mockResolvedValue(undefined);

		render(
			<Detail
				title="编辑角色"
				open
				detailData={{ id: 9, name: "旧角色", code: "custom_test", scope: "domain", system: false }}
				treeData={[
					{ id: "1", title: "菜单", nodeType: "menu", children: [] },
					{ id: "2", title: "按钮", nodeType: "button", children: [] },
					{ id: "99", title: "屏蔽词库", nodeType: "catalog", children: [] },
				]}
				onCloseChange={mocks.onCloseChange}
				refreshTable={mocks.refreshTable}
			/>,
		);

		await userEvent.click(screen.getByRole("button", { name: "提交" }));

		await waitFor(() => {
			expect(mocks.messageSuccess).toHaveBeenCalledWith("common.updateSuccess");
		});
		expect(mocks.fetchUpdateRolePermissions).toHaveBeenCalledWith(9, {
			menuIds: [1, 99],
			buttonIds: [2],
		});
		expect(mocks.onCloseChange).toHaveBeenCalledTimes(1);
	});
});
