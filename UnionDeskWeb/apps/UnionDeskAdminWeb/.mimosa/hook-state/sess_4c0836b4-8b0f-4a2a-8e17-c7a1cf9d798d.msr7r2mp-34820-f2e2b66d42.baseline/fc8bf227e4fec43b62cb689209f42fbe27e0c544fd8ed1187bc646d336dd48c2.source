import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	fetchBusinessDomains: vi.fn(),
	fetchPlatformUsers: vi.fn(),
	fetchPlatformOffboardPoolUsers: vi.fn(),
	requestBackendJson: vi.fn(),
}));

vi.mock("./domain", () => ({
	fetchBusinessDomains: mocks.fetchBusinessDomains,
}));

vi.mock("./iam", () => ({
	fetchPlatformUsers: mocks.fetchPlatformUsers,
	fetchPlatformOffboardPoolUsers: mocks.fetchPlatformOffboardPoolUsers,
}));

vi.mock("#src/api/backend", () => ({
	requestBackendJson: mocks.requestBackendJson,
}));

import { fetchPlatformOverview } from "./overview";

describe("platform overview api", () => {
	beforeEach(() => {
		mocks.fetchBusinessDomains.mockReset();
		mocks.fetchPlatformUsers.mockReset();
		mocks.fetchPlatformOffboardPoolUsers.mockReset();
		mocks.requestBackendJson.mockReset();
	});

	it("counts domains from the normalized list response", async () => {
		mocks.fetchBusinessDomains.mockResolvedValue([
			{ id: 1, code: "default", name: "Default Domain" },
			{ id: 2, code: "ops", name: "Operations" },
		]);
		mocks.fetchPlatformUsers.mockResolvedValue([
			{ id: 1, employmentStatus: "active", status: 1 },
			{ id: 2, employmentStatus: "active", status: 0 },
			{ id: 3, employmentStatus: "offboarded", status: 1 },
		]);
		mocks.fetchPlatformOffboardPoolUsers.mockResolvedValue([
			{ id: 10 },
			{ id: 11 },
		]);
		mocks.requestBackendJson.mockResolvedValue([
			{ id: 1 },
			{ id: 2 },
			{ id: 3 },
		]);

		await expect(fetchPlatformOverview()).resolves.toMatchObject({
			domainCount: 2,
			activeUserCount: 2,
			disabledUserCount: 1,
			offboardUserCount: 2,
			recentAuditCount: 3,
			loginLogs: [
				{ id: 1 },
				{ id: 2 },
				{ id: 3 },
			],
		});
		expect(mocks.fetchBusinessDomains).toHaveBeenCalledTimes(1);
	});
});
