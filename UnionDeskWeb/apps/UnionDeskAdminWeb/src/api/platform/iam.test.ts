import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	requestBackendJson: vi.fn(),
}));

vi.mock("#src/api/backend", () => ({
	requestBackendJson: mocks.requestBackendJson,
}));

import {
	fetchCreatePlatformUser,
	fetchOffboardPlatformUser,
	fetchPlatformOffboardPoolUsers,
	fetchRestorePlatformUser,
	fetchUpdatePlatformUser,
} from "./iam";

describe("platform iam api", () => {
	beforeEach(() => {
		mocks.requestBackendJson.mockReset();
	});

	it("uses the platform user create endpoint", async () => {
		const payload = {
			username: "alice",
			mobile: "13800000000",
			email: "alice@example.com",
			password: "12345678",
			accountType: "admin",
			roleCodes: ["super_admin"],
			businessDomainIds: [1, 2],
		};
		mocks.requestBackendJson.mockResolvedValue({ id: 1 });

		await expect(fetchCreatePlatformUser(payload)).resolves.toEqual({ id: 1 });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/iam/users", {
			method: "POST",
			json: payload,
		});
	});

	it("uses the platform user update endpoint", async () => {
		const payload = {
			username: "alice",
			mobile: "13800000000",
			email: "alice@example.com",
			roleCodes: ["super_admin"],
			businessDomainIds: [1],
		};
		mocks.requestBackendJson.mockResolvedValue({ id: 9 });

		await expect(fetchUpdatePlatformUser(9, payload)).resolves.toEqual({ id: 9 });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/iam/users/9", {
			method: "PUT",
			json: payload,
		});
	});

	it("uses the platform user offboard endpoint", async () => {
		mocks.requestBackendJson.mockResolvedValue({ id: 10 });

		await expect(fetchOffboardPlatformUser(10, "流程完成")).resolves.toEqual({ id: 10 });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/iam/users/10/offboard", {
			method: "POST",
			json: {
				reason: "流程完成",
			},
		});
	});

	it("uses the platform user restore endpoint", async () => {
		mocks.requestBackendJson.mockResolvedValue({ id: 11 });

		await expect(fetchRestorePlatformUser(11)).resolves.toEqual({ id: 11 });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/iam/users/11/restore", {
			method: "POST",
		});
	});

	it("uses the offboard pool endpoint", async () => {
		mocks.requestBackendJson.mockResolvedValue([{ id: 1 }]);

		await expect(fetchPlatformOffboardPoolUsers()).resolves.toEqual([{ id: 1 }]);
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/iam/users/offboard-pool");
	});
});
