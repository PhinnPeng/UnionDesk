import { beforeEach, describe, expect, it, vi } from "vitest";

import { fetchCreateMenu, fetchDeleteMenu, fetchMenuTree, fetchUpdateMenu } from "./index";

const requestBackendJsonMock = vi.fn();

vi.mock("#src/api/backend", () => ({
	requestBackendJson: (...args: unknown[]) => requestBackendJsonMock(...args),
}));

describe("system menu api", () => {
	beforeEach(() => {
		requestBackendJsonMock.mockReset();
	});

	it("calls the backend tree endpoint", async () => {
		requestBackendJsonMock.mockResolvedValueOnce([{ id: 1, name: "菜单", nodeType: "menu", orderNo: 1, hidden: false, status: 1, required: false, code: "menu", routePath: "/menu", children: [] }]);

		await fetchMenuTree();

		expect(requestBackendJsonMock).toHaveBeenCalledWith("v1/iam/menus/tree");
	});

	it("forwards the scope query to the tree endpoint", async () => {
		requestBackendJsonMock.mockResolvedValueOnce([]);

		await fetchMenuTree({ scope: "platform" });

		expect(requestBackendJsonMock).toHaveBeenCalledWith("v1/iam/menus/tree?scope=platform");
	});

	it("calls the backend create endpoint", async () => {
		const payload = {
			name: "平台菜单",
			nodeType: "menu",
			parentId: null,
			orderNo: 1,
			hidden: false,
			status: 1,
		};

		await fetchCreateMenu(payload);

		expect(requestBackendJsonMock).toHaveBeenCalledWith("v1/iam/menus", {
			method: "POST",
			json: payload,
		});
	});

	it("calls the backend update endpoint", async () => {
		const payload = {
			name: "平台菜单",
			nodeType: "menu",
		};

		await fetchUpdateMenu(12, payload);

		expect(requestBackendJsonMock).toHaveBeenCalledWith("v1/iam/menus/12", {
			method: "PUT",
			json: payload,
		});
	});

	it("calls the backend delete endpoint", async () => {
		await fetchDeleteMenu(12);

		expect(requestBackendJsonMock).toHaveBeenCalledWith("v1/iam/menus/12", {
			method: "DELETE",
		});
	});
});
