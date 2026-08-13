import { describe, expect, it } from "vitest";

import { getSelectedMenuKeys } from "./utils";

describe("getSelectedMenuKeys", () => {
	it("浣跨敤 pathname 鑰屼笉鏄?backend route id 纭畾鑿滃崟鍚敤椤?", () => {
		const matches = [
			{
				id: "backend:platform:root",
				pathname: "/platform",
				handle: {
					hideInMenu: false,
				},
			},
			{
				id: "backend:platform:49",
				pathname: "/platform/role",
				handle: {
					hideInMenu: false,
				},
			},
		];

		expect(getSelectedMenuKeys(matches, {
			"/platform/role": ["/platform"],
		})).toEqual(["/platform", "/platform/role"]);
	});

	it("淇濇寔 currentActiveMenu 鎸囧悜鏃剁殑鑿滃崟鍚敤", () => {
		const matches = [
			{
				id: "backend:platform:detail",
				pathname: "/platform/ticket-detail",
				handle: {
					currentActiveMenu: "/platform/ticket-pool",
				},
			},
		];

		expect(getSelectedMenuKeys(matches, {
			"/platform/ticket-pool": ["/platform"],
		})).toEqual(["/platform", "/platform/ticket-pool"]);
	});
});
