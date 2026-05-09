import type { TFunction } from "i18next";

import { describe, expect, it } from "vitest";

import { getConstantColumns } from "./constants";

describe("role constant columns", () => {
	it("uses digit index columns for role table numbering", () => {
		const t = ((key: string) => key) as unknown as TFunction<"translation", undefined>;
		const columns = getConstantColumns(t);
		const indexColumn = columns.find(column => column.dataIndex === "index");

		expect(indexColumn?.valueType).toBe("index");
	});
});
