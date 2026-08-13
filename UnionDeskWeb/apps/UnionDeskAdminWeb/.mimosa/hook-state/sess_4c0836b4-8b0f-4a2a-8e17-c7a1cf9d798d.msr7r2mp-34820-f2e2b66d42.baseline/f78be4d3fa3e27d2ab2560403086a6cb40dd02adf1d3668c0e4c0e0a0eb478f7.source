import { describe, expect, it } from "vitest";

import { createIconPickerCatalog } from "./iconify-offline-catalog";

describe("iconify offline catalog", () => {
	it("searches icons across collections and keeps collection order stable", () => {
		const lucideData = {
			prefix: "lucide",
			icons: {
				user: {},
				home: {},
			},
			aliases: {
				"settings-2": {},
			},
		} as any;
		const riData = {
			prefix: "ri",
			icons: {
				home: {},
				settings: {},
			},
		} as any;

		const catalog = createIconPickerCatalog([
			{
				label: "Lucide",
				value: "lucide",
				data: lucideData,
			},
			{
				label: "Remix Icon",
				value: "ri",
				data: riData,
			},
		]);

		expect(catalog.collectionOptions).toEqual([
			{ label: "Lucide", value: "lucide" },
			{ label: "Remix Icon", value: "ri" },
		]);
		expect(catalog.getCollectionIcons("lucide")).toEqual(["home", "settings-2", "user"]);
		expect(catalog.searchIcons("home")).toEqual(["lucide:home", "ri:home"]);
		expect(catalog.searchIcons("lucide")).toEqual(["lucide:home", "lucide:settings-2", "lucide:user"]);
	});
});
