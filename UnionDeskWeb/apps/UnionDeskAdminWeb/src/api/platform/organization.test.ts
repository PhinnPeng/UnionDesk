import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	requestBackendJson: vi.fn(),
}));

vi.mock("#src/api/backend", () => ({
	requestBackendJson: mocks.requestBackendJson,
}));

import {
	fetchCreatePlatformOrganization,
	fetchDeletePlatformOrganization,
	fetchPlatformOrganizations,
	fetchUpdatePlatformOrganization,
} from "./organization";

describe("platform organization api", () => {
	beforeEach(() => {
		mocks.requestBackendJson.mockReset();
	});

	it("uses the organization list endpoint", async () => {
		mocks.requestBackendJson.mockResolvedValue([]);

		await expect(fetchPlatformOrganizations()).resolves.toEqual([]);
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/iam/organizations");
	});

	it("uses the organization create endpoint", async () => {
		const payload = {
			code: "ops",
			name: "运营部",
			parentId: 1,
			leaderUserId: 2,
			orderNo: 10,
			status: 1,
			remark: "平台组织",
		};
		mocks.requestBackendJson.mockResolvedValue({ id: 9 });

		await expect(fetchCreatePlatformOrganization(payload)).resolves.toEqual({ id: 9 });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/iam/organizations", {
			method: "POST",
			json: payload,
		});
	});

	it("uses the organization update endpoint", async () => {
		const payload = {
			code: "ops",
			name: "运营中心",
			leaderUserId: 2,
			orderNo: 12,
			status: 1,
			remark: "更新备注",
		};
		mocks.requestBackendJson.mockResolvedValue({ id: 10 });

		await expect(fetchUpdatePlatformOrganization(10, payload)).resolves.toEqual({ id: 10 });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/iam/organizations/10", {
			method: "PUT",
			json: payload,
		});
	});

	it("uses the organization delete endpoint", async () => {
		mocks.requestBackendJson.mockResolvedValue(undefined);

		await expect(fetchDeletePlatformOrganization(11)).resolves.toBeUndefined();
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/iam/organizations/11", {
			method: "DELETE",
		});
	});
});
