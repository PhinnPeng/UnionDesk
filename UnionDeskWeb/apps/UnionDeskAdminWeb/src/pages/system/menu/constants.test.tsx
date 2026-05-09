import { render, screen } from "@testing-library/react";
import type { TFunction } from "i18next";
import { describe, expect, it } from "vitest";

import { getConstantColumns } from "./constants";

describe("menu constant columns", () => {
	it("uses digit index columns for menu table numbering", () => {
		const t = ((key: string) => key) as unknown as TFunction<"translation", undefined>;
		const columns = getConstantColumns(t);
		const indexColumn = columns.find(column => column.dataIndex === "index");

		expect(indexColumn?.valueType).toBe("index");
	});

	it("renders the menu icon column with the real icon component", () => {
		const t = ((key: string) => key) as unknown as TFunction<"translation", undefined>;
		const columns = getConstantColumns(t);
		const iconColumn = columns.find(column => column.dataIndex === "icon");
		const renderIcon = iconColumn?.render as any;

		render(<>{renderIcon?.(undefined, { icon: "AppstoreOutlined" }, 0, undefined, undefined)}</>);

		const iconWrapper = screen.getByTitle("AppstoreOutlined");
		expect(iconWrapper.querySelector("svg")).not.toBeNull();
		expect(iconWrapper).not.toHaveTextContent("AppstoreOutlined");
	});

	it("renders a compact warning icon for unknown menu icons", () => {
		const t = ((key: string) => key) as unknown as TFunction<"translation", undefined>;
		const columns = getConstantColumns(t);
		const iconColumn = columns.find(column => column.dataIndex === "icon");
		const renderIcon = iconColumn?.render as any;

		render(<>{renderIcon?.(undefined, { icon: "MissingOutlined" }, 0, undefined, undefined)}</>);

		const iconWrapper = screen.getByTitle("Unknown icon: MissingOutlined");
		expect(iconWrapper.querySelector("svg")).not.toBeNull();
		expect(iconWrapper).not.toHaveTextContent("MissingOutlined");
	});

	it("always includes the scope column", () => {
		const t = ((key: string) => key) as unknown as TFunction<"translation", undefined>;
		const columns = getConstantColumns(t);
		const scopeColumn = columns.find(column => column.dataIndex === "scope");

		expect(scopeColumn).toBeDefined();
	});
});
