import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	requestBackendJson: vi.fn(),
}));

vi.mock("#src/api/backend", () => ({
	requestBackendJson: mocks.requestBackendJson,
}));

import { fetchPlatformOverview } from "./overview";

describe("platform overview api", () => {
	beforeEach(() => {
		mocks.requestBackendJson.mockReset();
	});

	it("merges backend overview counts with recent login logs", async () => {
		mocks.requestBackendJson
			.mockResolvedValueOnce({
				domainCount: 2,
				staffCount: 5,
				activeUserCount: 2,
				disabledUserCount: 1,
				offboardUserCount: 2,
				customerCount: 3,
				ticketCount: 4,
				consultationCount: 1,
				recentAuditCount: 9,
			})
			.mockResolvedValue({
				total: 3,
				list: [
					{ id: 1 },
					{ id: 2 },
					{ id: 3 },
				],
			});

		await expect(fetchPlatformOverview()).resolves.toMatchObject({
			domainCount: 2,
			activeUserCount: 2,
			disabledUserCount: 1,
			offboardUserCount: 2,
			ticketCount: 4,
			consultationCount: 1,
			recentAuditCount: 9,
			loginLogs: [
				{ id: 1 },
				{ id: 2 },
				{ id: 3 },
			],
		});
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/dashboard/overview");
	});
});
