import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
	requestBackendJson: vi.fn(),
}));

vi.mock("#src/api/backend", () => ({
	requestBackendJson: mocks.requestBackendJson,
}));

import { fetchBusinessDomains } from "./domain";

describe("platform domain api", () => {
	beforeEach(() => {
		mocks.requestBackendJson.mockReset();
	});

	it("unwraps page result list from the domains endpoint", async () => {
		mocks.requestBackendJson.mockResolvedValue({
			total: 2,
			list: [
				{ id: 1, code: "default", name: "Default Domain" },
				{ id: 2, code: "ops", name: "Operations" },
			],
		});

		await expect(fetchBusinessDomains()).resolves.toEqual([
			{ id: 1, code: "default", name: "Default Domain" },
			{ id: 2, code: "ops", name: "Operations" },
		]);
		expect(mocks.requestBackendJson).toHaveBeenCalledWith("v1/domains");
	});

	it("passes through raw list responses", async () => {
		mocks.requestBackendJson.mockResolvedValue([
			{ id: 3, code: "sales", name: "Sales" },
		]);

		await expect(fetchBusinessDomains()).resolves.toEqual([
			{ id: 3, code: "sales", name: "Sales" },
		]);
	});
});
