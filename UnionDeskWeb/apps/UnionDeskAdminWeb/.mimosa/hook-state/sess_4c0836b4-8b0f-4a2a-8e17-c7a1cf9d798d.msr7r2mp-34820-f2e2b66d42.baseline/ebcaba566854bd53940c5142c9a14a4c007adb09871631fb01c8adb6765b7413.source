import { describe, expect, it } from "vitest";

import { paginateIcons } from "./icon-picker-utils";

describe("icon picker utils", () => {
	it("paginates icons by page size", () => {
		const icons = Array.from({ length: 50 }, (_, index) => `lucide:${index + 1}`);

		expect(paginateIcons(icons, 1, 40)).toHaveLength(40);
		expect(paginateIcons(icons, 2, 40)).toEqual([
			"lucide:41",
			"lucide:42",
			"lucide:43",
			"lucide:44",
			"lucide:45",
			"lucide:46",
			"lucide:47",
			"lucide:48",
			"lucide:49",
			"lucide:50",
		]);
	});

	it("treats invalid pages as the first page", () => {
		const icons = Array.from({ length: 3 }, (_, index) => `lucide:${index + 1}`);

		expect(paginateIcons(icons, 0, 40)).toEqual(icons);
		expect(paginateIcons(icons, -1, 40)).toEqual(icons);
	});
});
