import { describe, expect, it } from "vitest";

import { platformDomainDetailPath } from "./domain-routes";

describe("platformDomainDetailPath", () => {
	it("returns base path without tab query for overview", () => {
		expect(platformDomainDetailPath("abc-1")).toBe("/platform/domains/detail/abc-1");
		expect(platformDomainDetailPath("abc-1", "overview")).toBe("/platform/domains/detail/abc-1");
	});

	it("appends tab query for non-overview tabs", () => {
		expect(platformDomainDetailPath("abc-1", "basic")).toBe("/platform/domains/detail/abc-1?tab=basic");
	});
});
