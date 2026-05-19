import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	messageError: vi.fn(),
}));

vi.mock("#src/utils/static-antd", () => ({
	message: {
		error: mocks.messageError,
	},
}));

import { requestBackendJson } from "./backend";

describe("requestBackendJson", () => {
	beforeEach(() => {
		mocks.messageError.mockReset();
		vi.stubGlobal("fetch", vi.fn());
	});

	it("使用响应体错误信息并避免全局重复提示", async () => {
		vi.mocked(fetch).mockResolvedValue(new Response(JSON.stringify({
			message: "请先删除该菜单下的所有子节点",
		}), {
			status: 400,
			statusText: "Bad Request",
			headers: {
				"Content-Type": "application/json",
			},
		}));

		await expect(requestBackendJson("v1/iam/menus/1", { method: "DELETE" }))
			.rejects.toThrow("请先删除该菜单下的所有子节点");
		expect(mocks.messageError).not.toHaveBeenCalled();
	});
});
