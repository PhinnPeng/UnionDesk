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

	it("uses the platform staff create endpoint", async () => {
		const payload = {
			username: "alice",
			mobile: "13800000000",
			email: "alice@example.com",
			password: "12345678",
			accountType: "admin",
			roleCodes: ["super_admin"],
			businessDomainIds: [1, 2],
		};
		mocks.requestBackendJson.mockResolvedValue({ id: 1, username: "alice", phone: "13800000000", status: 1, employmentStatus: "active" });

		await expect(fetchCreatePlatformUser(payload)).resolves.toMatchObject({ id: 1, mobile: "13800000000" });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/admin/staff", {
			method: "POST",
			json: {
				username: "alice",
				phone: "13800000000",
				email: "alice@example.com",
				password: "12345678",
				accountType: "admin",
				roleCodes: ["super_admin"],
				businessDomainIds: [1, 2],
				organizationIds: [],
				real_name: undefined,
			},
		});
	});

	it("uses the platform staff update endpoint", async () => {
		const payload = {
			username: "alice",
			mobile: "13800000000",
			email: "alice@example.com",
			roleCodes: ["super_admin"],
			businessDomainIds: [1],
		};
		mocks.requestBackendJson.mockResolvedValue({ id: 9, username: "alice", phone: "13800000000", status: 1, employmentStatus: "active" });

		await expect(fetchUpdatePlatformUser(9, payload)).resolves.toMatchObject({ id: 9 });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/admin/staff/9", {
			method: "PUT",
			json: {
				username: "alice",
				phone: "13800000000",
				email: "alice@example.com",
				password: undefined,
				accountType: undefined,
				roleCodes: ["super_admin"],
				businessDomainIds: [1],
				organizationIds: undefined,
				status: undefined,
				real_name: undefined,
			},
		});
	});

	it("uses the platform staff offboard endpoint", async () => {
		mocks.requestBackendJson.mockResolvedValue({ id: 10, username: "bob", phone: "13900000000", status: 0, employmentStatus: "offboarded" });

		await expect(fetchOffboardPlatformUser(10, "流程完成")).resolves.toMatchObject({ id: 10 });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/admin/staff/10/offboard", {
			method: "POST",
			json: {
				reason: "流程完成",
			},
		});
	});

	it("uses the platform staff restore endpoint", async () => {
		mocks.requestBackendJson.mockResolvedValue({ id: 11, username: "bob", phone: "13900000000", status: 1, employmentStatus: "active" });

		await expect(fetchRestorePlatformUser(11)).resolves.toMatchObject({ id: 11 });
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/admin/staff/11/restore", {
			method: "POST",
		});
	});

	it("uses the staff list with offboarded status for offboard pool", async () => {
		mocks.requestBackendJson.mockResolvedValue({ total: 1, list: [{ id: 1, username: "left", phone: "13700000000", status: 0, employmentStatus: "offboarded" }] });

		await expect(fetchPlatformOffboardPoolUsers()).resolves.toEqual([
			expect.objectContaining({ id: 1, employmentStatus: "offboarded" }),
		]);
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/admin/staff?status=offboarded&page=1&page_size=1000");
	});
});
